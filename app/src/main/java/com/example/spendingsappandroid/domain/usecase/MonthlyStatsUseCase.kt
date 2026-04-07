package com.example.spendingsappandroid.domain.usecase

import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.MonthlyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Calculates monthly spending statistics from stored transactions.
 * Automatically determines current month boundaries and emits
 * updated [MonthlyStats] whenever the underlying data changes.
 */
class MonthlyStatsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(): Flow<MonthlyStats> {
        val (start, end) = getCurrentMonthBoundaries()

        return combine(
            repository.getTotalSpent(start, end),
            repository.getTransactionCount(start, end),
            repository.getLargestTransaction(start, end)
        ) { totalSpent, count, largest ->
            MonthlyStats(
                totalSpent = totalSpent,
                transactionCount = count,
                averageTransaction = if (count > 0) totalSpent / count else 0.0,
                largestTransaction = largest
            )
        }
    }

    private fun getCurrentMonthBoundaries(): Pair<Long, Long> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())

        // Start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        // End of current month
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
}
