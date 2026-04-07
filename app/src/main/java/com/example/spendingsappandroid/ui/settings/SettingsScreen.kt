package com.example.spendingsappandroid.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppSelection: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val monitoredPackages by viewModel.monitoredPackages.collectAsState()
    val selectedCurrency by viewModel.currency.collectAsState()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsState()
    var showThemePicker by remember { mutableStateOf(false) }

    // Metric toggle states
    val showDailyAverage by viewModel.showDailyAverage.collectAsState()
    val showMedianTransaction by viewModel.showMedianTransaction.collectAsState()
    val showTopMerchant by viewModel.showTopMerchant.collectAsState()
    val showSpendingByApp by viewModel.showSpendingByApp.collectAsState()
    val showActiveDays by viewModel.showActiveDays.collectAsState()
    val showHistoricalAverage by viewModel.showHistoricalAverage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Currency section ---
            item {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyPicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Display currency",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Used for the dashboard and as default for new transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = selectedCurrency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Theme section ---
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                val themeModeLabel = when (themeMode) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "System default"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemePicker = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Choose light, dark, or follow your system setting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = themeModeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Dashboard Metrics section ---
            item {
                Text(
                    text = "Dashboard Metrics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                )
            }

            item {
                Text(
                    text = "Choose which additional metrics to show on the dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                MetricToggleRow(
                    label = "Daily Average",
                    description = "Average spending per day this month",
                    isEnabled = showDailyAverage,
                    onToggle = viewModel::setShowDailyAverage
                )
            }

            item {
                MetricToggleRow(
                    label = "Median Transaction",
                    description = "Middle value of all transactions",
                    isEnabled = showMedianTransaction,
                    onToggle = viewModel::setShowMedianTransaction
                )
            }

            item {
                MetricToggleRow(
                    label = "Top Merchant",
                    description = "Merchant with the highest total spend",
                    isEnabled = showTopMerchant,
                    onToggle = viewModel::setShowTopMerchant
                )
            }

            item {
                MetricToggleRow(
                    label = "Spending by App",
                    description = "Source app with the most spend",
                    isEnabled = showSpendingByApp,
                    onToggle = viewModel::setShowSpendingByApp
                )
            }

            item {
                MetricToggleRow(
                    label = "Active Days",
                    description = "Days with at least one transaction",
                    isEnabled = showActiveDays,
                    onToggle = viewModel::setShowActiveDays
                )
            }

            item {
                MetricToggleRow(
                    label = "Historical Average",
                    description = "Average spending across all past months",
                    isEnabled = showHistoricalAverage,
                    onToggle = viewModel::setShowHistoricalAverage
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Monitored apps section ---
            item {
                Text(
                    text = "Monitored Apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAppSelection() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select apps to monitor",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Choose which apps to track for transaction notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${monitoredPackages.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Currency picker dialog
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currencies = viewModel.supportedCurrencies,
            selected = selectedCurrency,
            onSelect = { code ->
                viewModel.setCurrency(code)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    // Theme picker dialog
    if (showThemePicker) {
        ThemePickerDialog(
            selected = themeMode,
            onSelect = { mode ->
                viewModel.setThemeMode(mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }
}

@Composable
private fun MetricToggleRow(
    label: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun CurrencyPickerDialog(
    currencies: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            LazyColumn {
                items(currencies.size) { index ->
                    val code = currencies[index]
                    val label = try {
                        val c = java.util.Currency.getInstance(code)
                        "$code — ${c.displayName}"
                    } catch (_: Exception) {
                        code
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == selected,
                            onClick = { onSelect(code) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ThemePickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "system" to "System default",
        "light" to "Light",
        "dark" to "Dark"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


