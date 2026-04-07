package com.example.spendingsappandroid.ui.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private data class OptionalMetric(
    val title: String,
    val value: String,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val stats by viewModel.monthlyStats.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val context = LocalContext.current
    val notificationAccessEnabled = remember {
        mutableStateOf(isNotificationServiceEnabled(context))
    }
    val monthOffset by viewModel.monthOffset.collectAsState()

    val showDailyAverage by viewModel.showDailyAverage.collectAsState()
    val showMedianTransaction by viewModel.showMedianTransaction.collectAsState()
    val showTopMerchant by viewModel.showTopMerchant.collectAsState()
    val showSpendingByApp by viewModel.showSpendingByApp.collectAsState()
    val showActiveDays by viewModel.showActiveDays.collectAsState()
    val showHistoricalAverage by viewModel.showHistoricalAverage.collectAsState()
    val historicalStats by viewModel.historicalStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

        MonthNavigator(
            monthOffset = monthOffset,
            onPreviousMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() }
        )

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

        val optionalMetrics = buildList {
            if (showDailyAverage) add(OptionalMetric("Daily Average", formatCurrency(stats.dailyAverage, currency), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer))
            if (showMedianTransaction) add(OptionalMetric("Median", formatCurrency(stats.medianTransaction, currency), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer))
            if (showTopMerchant) add(OptionalMetric("Top Merchant", stats.topMerchant.ifEmpty { "-" }, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer))
            if (showActiveDays) add(OptionalMetric("Active Days", stats.activeDays.toString(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer))
            if (showHistoricalAverage) {
                val label = if (historicalStats.totalMonths > 0) "Avg Month (${historicalStats.totalMonths} mo)" else "Avg Month"
                add(OptionalMetric(label, formatCurrency(historicalStats.averageMonthlySpent, currency), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer))
            }
        }

        optionalMetrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMetrics.forEach { metric ->
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = metric.title,
                        value = metric.value,
                        containerColor = metric.containerColor,
                        contentColor = metric.contentColor
                    )
                }
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (showSpendingByApp && stats.spendingByApp.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Spending by App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    stats.spendingByApp.forEach { (app, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = app)
                            Text(
                                text = formatCurrency(amount, currency),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                color = contentColor
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

@Composable
private fun MonthNavigator(
    monthOffset: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous month",
            modifier = Modifier
                .size(32.dp)
                .clickable { onPreviousMonth() },
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (monthOffset == 0) "This Month" else getMonthLabel(monthOffset),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next month",
            modifier = Modifier
                .size(32.dp)
                .clickable { onNextMonth() },
            tint = if (monthOffset < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

private fun getMonthLabel(offset: Int): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.MONTH, offset)
    val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
    return formatter.format(calendar.time)
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
