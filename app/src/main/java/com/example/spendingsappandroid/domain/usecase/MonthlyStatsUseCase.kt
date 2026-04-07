package com.example.spendingsappandroid.domain.usecase

import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.MonthlyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class MonthlyStatsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(monthBounds: Pair<Long, Long>): Flow<MonthlyStats> {
        val (start, end) = monthBounds

        val basicStats = combine(
            repository.getTotalSpent(start, end),
            repository.getTransactionCount(start, end),
            repository.getLargestTransaction(start, end),
            repository.getActiveDays(start, end),
            repository.getTopMerchant(start, end)
        ) { totalSpent, count, largest, activeDays, topMerchant ->
            BasicStats(totalSpent, count, largest, activeDays, topMerchant)
        }

        return combine(
            basicStats,
            repository.getTransactionsForMonth(start, end)
        ) { basic, transactions ->
            val averageTransaction = if (basic.count > 0) basic.totalSpent / basic.count else 0.0
            val dailyAverage = calculateDailyAverage(basic.totalSpent, start)
            val medianTransaction = calculateMedian(transactions)
            val spendingByApp = calculateSpendingByApp(transactions)

            MonthlyStats(
                totalSpent = basic.totalSpent,
                transactionCount = basic.count,
                averageTransaction = averageTransaction,
                largestTransaction = basic.largest,
                dailyAverage = dailyAverage,
                medianTransaction = medianTransaction,
                topMerchant = basic.topMerchant,
                spendingByApp = spendingByApp,
                activeDays = basic.activeDays
            )
        }
    }

    private fun calculateDailyAverage(totalSpent: Double, start: Long): Double {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = start
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (daysInMonth > 0) totalSpent / daysInMonth else 0.0
    }

    private fun calculateMedian(transactions: List<TransactionEntity>): Double {
        if (transactions.isEmpty()) return 0.0
        val sortedAmounts = transactions.map { it.amount }.sorted()
        val mid = sortedAmounts.size / 2
        return if (sortedAmounts.size % 2 == 0) {
            (sortedAmounts[mid - 1] + sortedAmounts[mid]) / 2.0
        } else {
            sortedAmounts[mid]
        }
    }

    private fun calculateSpendingByApp(transactions: List<TransactionEntity>): Map<String, Double> {
        return transactions.groupBy { it.sourceApp }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    private data class BasicStats(
        val totalSpent: Double,
        val count: Int,
        val largest: Double,
        val activeDays: Int,
        val topMerchant: String
    )
}
