package com.example.spendingsappandroid.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.data.repository.MonitoredAppsRepository
import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.parser.ParserEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Listens to financial app notifications and extracts transaction data.
 *
 * GOOGLE PLAY COMPLIANCE NOTE:
 * This service requires the BIND_NOTIFICATION_LISTENER_SERVICE permission.
 * Notification access is used solely to parse financial transaction amounts
 * for the user's personal spending tracking. No raw notification content is stored —
 * only structured transaction data (amount, merchant, currency) is persisted.
 * The user must explicitly grant notification access in device Settings.
 * See: https://support.google.com/googleplay/android-developer/answer/9888170
 */
class TransactionNotificationListener : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ListenerEntryPoint {
        fun transactionRepository(): TransactionRepository
        fun parserEngine(): ParserEngine
        fun monitoredAppsRepository(): MonitoredAppsRepository
    }

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repository: TransactionRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ListenerEntryPoint::class.java)
            .transactionRepository()
    }

    private val parserEngine: ParserEngine by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ListenerEntryPoint::class.java)
            .parserEngine()
    }

    private val monitoredAppsRepository: MonitoredAppsRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, ListenerEntryPoint::class.java)
            .monitoredAppsRepository()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName ?: return
        if (!monitoredAppsRepository.isMonitored(packageName)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val timestamp = sbn.postTime

        // Process in background — do not store raw notification text (privacy)
        serviceScope.launch {
            processNotification(packageName, title, text, timestamp)
        }
    }

    private suspend fun processNotification(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long
    ) {
        val result = parserEngine.parse(packageName, title, text) ?: return

        val amount = result.amount ?: return
        val currency = result.currency ?: monitoredAppsRepository.getCurrency()
        val merchant = result.merchant ?: "Unknown"

        // Dedup hash: amount + merchant + timestamp rounded to minute precision
        val minuteTimestamp = (timestamp / 60_000) * 60_000
        val hash = generateHash("$amount|$merchant|$minuteTimestamp")

        if (repository.existsByHash(hash)) return

        val transaction = TransactionEntity(
            amount = amount,
            currency = currency,
            merchant = merchant,
            timestamp = timestamp,
            sourceApp = packageName,
            hash = hash,
            notificationTitle = title,
            notificationText = text
        )

        repository.insertTransaction(transaction)
    }

    private fun generateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        serviceScope.cancel()
    }
}
