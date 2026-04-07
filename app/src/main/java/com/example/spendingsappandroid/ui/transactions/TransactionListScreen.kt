package com.example.spendingsappandroid.ui.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.ui.dashboard.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
fun TransactionListScreen(viewModel: DashboardViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transactions this month",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDeleteRequest = { transactionToDelete = it }
                )
            }
        }
    }

    // Confirmation dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text(
                    "Remove ${formatAmount(tx.amount, tx.currency)}" +
                            if (tx.merchant.isNotBlank()) " at ${tx.merchant}" else "" +
                                    "?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(tx.id)
                    transactionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onDeleteRequest: (TransactionEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { /* no-op for now */ },
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friendlyAppName(transaction.sourceApp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = transaction.notificationTitle.ifBlank {
                            transaction.merchant.ifBlank { "Transaction" }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.notificationText.isNotBlank()) {
                        Text(
                            text = transaction.notificationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = formatAmount(transaction.amount, transaction.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    showMenu = false
                    onDeleteRequest(transaction)
                }
            )
        }
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    try {
        format.currency = Currency.getInstance(currency)
    } catch (_: Exception) {
        try {
            format.currency = Currency.getInstance("USD")
        } catch (_: Exception) { /* fallback */ }
    }
    return format.format(amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun friendlyAppName(packageName: String): String = when (packageName) {
    "com.chase.sig.android" -> "Chase"
    "com.wf.wellsfargomobile" -> "Wells Fargo"
    "com.bankofamerica.cashpromobile" -> "Bank of America"
    "com.citi.citimobile" -> "Citi"
    "com.usbank.mobilebanking" -> "US Bank"
    "com.paypal.android.p2pmobile" -> "PayPal"
    "com.venmo" -> "Venmo"
    "com.squareup.cash" -> "Cash App"
    "com.google.android.apps.walletnfcrel" -> "Google Pay"
    "com.google.android.apps.nbu.paisa.user" -> "Google Pay (India)"
    "com.phonepe.app" -> "PhonePe"
    "in.org.npci.upiapp" -> "BHIM UPI"
    "com.revolut.revolut" -> "Revolut"
    "com.transferwise.android" -> "Wise"
    "de.number26.android" -> "N26"
    else -> packageName.substringAfterLast(".")
        .replaceFirstChar { it.uppercaseChar() }
}
