package com.example.spendingsappandroid.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the set of app package names that should be monitored
 * for spending notifications. Uses SharedPreferences under the hood.
 */
@Singleton
class MonitoredAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "monitored_apps_prefs"
        private const val KEY_PACKAGES = "monitored_packages"
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_CURRENCY = "display_currency"
        private const val KEY_SHOW_DAILY_AVERAGE = "show_daily_average"
        private const val KEY_SHOW_MEDIAN = "show_median_transaction"
        private const val KEY_SHOW_TOP_MERCHANT = "show_top_merchant"
        private const val KEY_SHOW_SPENDING_BY_APP = "show_spending_by_app"
        private const val KEY_SHOW_ACTIVE_DAYS = "show_active_days"
        private const val KEY_THEME_MODE = "theme_mode"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val DEFAULT_CURRENCY = "USD"

        val SUPPORTED_CURRENCIES = listOf(
            "USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD",
            "CHF", "CNY", "KRW", "BRL", "TRY", "RUB", "PHP",
            "THB", "PLN", "SGD", "HKD", "MXN", "ZAR", "SEK",
            "NOK", "DKK", "NZD", "CZK", "HUF", "ILS", "CLP",
            "ARS", "COP", "PEN", "TWD", "IDR", "MYR", "VND"
        )

        /** Default apps pre-selected on first launch. */
        val DEFAULT_PACKAGES = setOf(
            // US Banks
            "com.chase.sig.android",
            "com.wf.wellsfargomobile",
            "com.bankofamerica.cashpromobile",
            "com.citi.citimobile",
            "com.usbank.mobilebanking",
            // Payment apps
            "com.paypal.android.p2pmobile",
            "com.venmo",
            "com.squareup.cash",
            "com.google.android.apps.walletnfcrel",
            // India UPI
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "in.org.npci.upiapp",
            // International
            "com.revolut.revolut",
            "com.transferwise.android",
            "de.number26.android"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _monitoredPackages = MutableStateFlow(loadPackages())
    val monitoredPackages: StateFlow<Set<String>> = _monitoredPackages.asStateFlow()

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY)
    val currency: StateFlow<String> = _currency.asStateFlow()

    // --- Metric toggle preferences (default: all enabled) ---

    private val _showDailyAverage = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DAILY_AVERAGE, true))
    val showDailyAverage: StateFlow<Boolean> = _showDailyAverage.asStateFlow()

    private val _showMedianTransaction = MutableStateFlow(prefs.getBoolean(KEY_SHOW_MEDIAN, true))
    val showMedianTransaction: StateFlow<Boolean> = _showMedianTransaction.asStateFlow()

    private val _showTopMerchant = MutableStateFlow(prefs.getBoolean(KEY_SHOW_TOP_MERCHANT, true))
    val showTopMerchant: StateFlow<Boolean> = _showTopMerchant.asStateFlow()

    private val _showSpendingByApp = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SPENDING_BY_APP, true))
    val showSpendingByApp: StateFlow<Boolean> = _showSpendingByApp.asStateFlow()

    private val _showActiveDays = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ACTIVE_DAYS, true))
    val showActiveDays: StateFlow<Boolean> = _showActiveDays.asStateFlow()

    // --- Theme preference ---

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        require(mode in listOf(THEME_SYSTEM, THEME_LIGHT, THEME_DARK))
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setCurrency(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _currency.value = code
    }

    fun getCurrency(): String = _currency.value

    // --- Metric toggle setters ---

    fun setShowDailyAverage(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_DAILY_AVERAGE, enabled).apply()
        _showDailyAverage.value = enabled
    }

    fun setShowMedianTransaction(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_MEDIAN, enabled).apply()
        _showMedianTransaction.value = enabled
    }

    fun setShowTopMerchant(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_TOP_MERCHANT, enabled).apply()
        _showTopMerchant.value = enabled
    }

    fun setShowSpendingByApp(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SPENDING_BY_APP, enabled).apply()
        _showSpendingByApp.value = enabled
    }

    fun setShowActiveDays(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ACTIVE_DAYS, enabled).apply()
        _showActiveDays.value = enabled
    }

    /** Quick check used by the notification listener (no Flow overhead). */
    fun isMonitored(packageName: String): Boolean =
        _monitoredPackages.value.contains(packageName)

    fun addPackage(packageName: String) {
        val updated = _monitoredPackages.value + packageName
        save(updated)
        _monitoredPackages.value = updated
    }

    fun removePackage(packageName: String) {
        val updated = _monitoredPackages.value - packageName
        save(updated)
        _monitoredPackages.value = updated
    }

    fun setMonitored(packageName: String, monitored: Boolean) {
        if (monitored) addPackage(packageName) else removePackage(packageName)
    }

    private fun loadPackages(): Set<String> {
        val initialized = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!initialized) {
            // First launch — seed with defaults
            save(DEFAULT_PACKAGES)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            return DEFAULT_PACKAGES
        }
        return prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
    }

    private fun save(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
    }
}
