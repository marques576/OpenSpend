package com.example.spendingsappandroid.domain.usecase

import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.HistoricalStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class HistoricalAverageUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(): Flow<HistoricalStats> {
        return repository.getAllTransactions().map { transactions ->
            calculateHistoricalStats(transactions)
        }
    }

    private fun calculateHistoricalStats(transactions: List<TransactionEntity>): HistoricalStats {
        if (transactions.isEmpty()) {
            return HistoricalStats()
        }

        val monthlyTotals = transactions
            .groupBy { transaction ->
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = transaction.timestamp
                "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val totalMonths = monthlyTotals.size
        val totalSpentAllTime = monthlyTotals.values.sum()
        val averageMonthlySpent = if (totalMonths > 0) {
            totalSpentAllTime / totalMonths
        } else {
            0.0
        }

        return HistoricalStats(
            averageMonthlySpent = averageMonthlySpent,
            totalMonths = totalMonths,
            totalSpentAllTime = totalSpentAllTime
        )
    }
}
