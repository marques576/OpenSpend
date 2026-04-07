package com.example.spendingsappandroid.domain.usecase

import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.MonthlyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Calculates monthly spending statistics from stored transactions.
 * Accepts explicit month boundaries so the caller can navigate
 * between months. Emits updated [MonthlyStats] whenever the
 * underlying data changes.
 */
class MonthlyStatsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * @param start     start of the target month in epoch millis
     * @param end       end of the target month in epoch millis
     * @param daysElapsed number of days to use for daily-average calculation
     *                    (current day-of-month for the current month, or total
     *                     days in the month for past months)
     */
    operator fun invoke(start: Long, end: Long, daysElapsed: Int): Flow<MonthlyStats> {

        // combine supports up to 5 typed flows — split into two groups
        val coreFlow: Flow<CoreStats> = combine(
            repository.getTotalSpent(start, end),
            repository.getTransactionCount(start, end),
            repository.getLargestTransaction(start, end),
            repository.getSmallestTransaction(start, end),
            repository.getAllAmounts(start, end)
        ) { totalSpent, count, largest, smallest, allAmounts ->
            CoreStats(totalSpent, count, largest, smallest, allAmounts)
        }

        val extraFlow: Flow<ExtraStats> = combine(
            repository.getTopMerchant(start, end),
            repository.getTopSourceApp(start, end),
            repository.getActiveDays(start, end)
        ) { topMerchant, topSourceApp, activeDays ->
            ExtraStats(topMerchant, topSourceApp, activeDays)
        }

        return combine(coreFlow, extraFlow) { core, extra ->
            MonthlyStats(
                totalSpent = core.totalSpent,
                transactionCount = core.count,
                averageTransaction = if (core.count > 0) core.totalSpent / core.count else 0.0,
                largestTransaction = core.largest,
                smallestTransaction = core.smallest,
                dailyAverage = if (daysElapsed > 0) core.totalSpent / daysElapsed else 0.0,
                medianTransaction = computeMedian(core.allAmounts),
                topMerchant = extra.topMerchant ?: "",
                topSourceApp = extra.topSourceApp ?: "",
                activeDays = extra.activeDays
            )
        }
    }

    private data class CoreStats(
        val totalSpent: Double,
        val count: Int,
        val largest: Double,
        val smallest: Double,
        val allAmounts: List<Double>
    )

    private data class ExtraStats(
        val topMerchant: String?,
        val topSourceApp: String?,
        val activeDays: Int
    )

    private fun computeMedian(sortedAmounts: List<Double>): Double {
        if (sortedAmounts.isEmpty()) return 0.0
        val size = sortedAmounts.size
        return if (size % 2 == 1) {
            sortedAmounts[size / 2]
        } else {
            (sortedAmounts[size / 2 - 1] + sortedAmounts[size / 2]) / 2.0
        }
    }
}
