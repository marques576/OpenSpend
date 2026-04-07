package com.example.spendingsappandroid.ui.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val stats by viewModel.monthlyStats.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val monthLabel by viewModel.monthLabel.collectAsState()
    val isCurrentMonth by viewModel.isCurrentMonth.collectAsState()
    val context = LocalContext.current
    val notificationAccessEnabled = remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }

    // Metric toggle states
    val showDailyAverage by viewModel.showDailyAverage.collectAsState()
    val showMedianTransaction by viewModel.showMedianTransaction.collectAsState()
    val showTopMerchant by viewModel.showTopMerchant.collectAsState()
    val showSmallestPurchase by viewModel.showSmallestPurchase.collectAsState()
    val showSpendingByApp by viewModel.showSpendingByApp.collectAsState()
    val showActiveDays by viewModel.showActiveDays.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!notificationAccessEnabled.value) {
            NotificationAccessCard(
                onEnableClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                }
            )
        }

        // Month navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.goToNextMonth() },
                enabled = !isCurrentMonth
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    modifier = Modifier.size(28.dp),
                    tint = if (isCurrentMonth)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // --- Core metrics (always visible) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Spent",
                value = formatCurrency(stats.totalSpent, currency),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Transactions",
                value = stats.transactionCount.toString(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Average",
                value = formatCurrency(stats.averageTransaction, currency),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Largest Purchase",
                value = formatCurrency(stats.largestTransaction, currency),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // --- Toggleable metrics ---
        // Build pairs of visible cards for row layout
        val extraCards = buildList {
            if (showDailyAverage) add(
                Triple("Daily Average", formatCurrency(stats.dailyAverage, currency), "daily")
            )
            if (showMedianTransaction) add(
                Triple("Median", formatCurrency(stats.medianTransaction, currency), "median")
            )
            if (showSmallestPurchase) add(
                Triple("Smallest Purchase", formatCurrency(stats.smallestTransaction, currency), "smallest")
            )
            if (showTopMerchant) add(
                Triple("Top Merchant", stats.topMerchant.ifEmpty { "—" }, "merchant")
            )
            if (showSpendingByApp) add(
                Triple("Top Source App", stats.topSourceApp.ifEmpty { "—" }, "app")
            )
            if (showActiveDays) add(
                Triple("Active Days", "${stats.activeDays} day${if (stats.activeDays != 1) "s" else ""}", "days")
            )
        }

        val extraColors = mapOf(
            "daily" to Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
            "median" to Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
            "smallest" to Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer),
            "merchant" to Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer),
            "app" to Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
            "days" to Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        )

        extraCards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { (title, value, key) ->
                    val (containerColor, contentColor) = extraColors[key]!!
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = title,
                        value = value,
                        containerColor = containerColor,
                        contentColor = contentColor
                    )
                }
                // If odd number of cards, add spacer to fill the row
                if (rowCards.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier.height(140.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotificationAccessCard(onEnableClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Access Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Enable to track spending automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onEnableClick) {
                Text("Enable")
            }
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String = "USD"): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    try {
        format.currency = Currency.getInstance(currencyCode)
    } catch (_: Exception) {
        // Fall back to default locale currency
    }
    return format.format(amount)
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat?.contains(context.packageName) == true
}
