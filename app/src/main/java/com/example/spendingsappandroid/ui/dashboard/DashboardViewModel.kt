package com.example.spendingsappandroid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.data.repository.MonitoredAppsRepository
import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.MonthlyStats
import com.example.spendingsappandroid.domain.usecase.MonthlyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val monthlyStatsUseCase: MonthlyStatsUseCase,
    private val repository: TransactionRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) : ViewModel() {

    val currency: StateFlow<String> = monitoredAppsRepository.currency

    // Metric toggle states
    val showDailyAverage: StateFlow<Boolean> = monitoredAppsRepository.showDailyAverage
    val showMedianTransaction: StateFlow<Boolean> = monitoredAppsRepository.showMedianTransaction
    val showTopMerchant: StateFlow<Boolean> = monitoredAppsRepository.showTopMerchant
    val showSmallestPurchase: StateFlow<Boolean> = monitoredAppsRepository.showSmallestPurchase
    val showSpendingByApp: StateFlow<Boolean> = monitoredAppsRepository.showSpendingByApp
    val showActiveDays: StateFlow<Boolean> = monitoredAppsRepository.showActiveDays

    /** Offset from the current month (0 = this month, -1 = last month, etc.) */
    private val _monthOffset = MutableStateFlow(0)
    val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

    /** Whether the user is viewing the current month (disables forward arrow). */
    val isCurrentMonth: StateFlow<Boolean> = _monthOffset
        .map { it == 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Human-readable label for the selected month, e.g. "April 2026" or "This Month". */
    val monthLabel: StateFlow<String> = _monthOffset
        .map { offset -> formatMonthLabel(offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "This Month")

    val monthlyStats: StateFlow<MonthlyStats> = _monthOffset
        .flatMapLatest { offset ->
            val info = getMonthInfo(offset)
            monthlyStatsUseCase(info.start, info.end, info.daysElapsed)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyStats()
        )

    val transactions: StateFlow<List<TransactionEntity>> = _monthOffset
        .flatMapLatest { offset ->
            val info = getMonthInfo(offset)
            repository.getTransactionsForMonth(info.start, info.end)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun goToPreviousMonth() {
        _monthOffset.value -= 1
    }

    fun goToNextMonth() {
        if (_monthOffset.value < 0) {
            _monthOffset.value += 1
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun formatMonthLabel(offset: Int): String {
        if (offset == 0) return "This Month"
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.MONTH, offset)
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private data class MonthInfo(
        val start: Long,
        val end: Long,
        val daysElapsed: Int
    )

    private fun getMonthInfo(offset: Int): MonthInfo {
        val now = Calendar.getInstance(TimeZone.getDefault())
        val target = Calendar.getInstance(TimeZone.getDefault())
        target.add(Calendar.MONTH, offset)

        // Start of target month
        target.set(Calendar.DAY_OF_MONTH, 1)
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        val start = target.timeInMillis

        // End of target month
        target.add(Calendar.MONTH, 1)
        target.add(Calendar.MILLISECOND, -1)
        val end = target.timeInMillis

        // Days elapsed: for current month use today's day-of-month,
        // for past months use the total days in that month
        val daysElapsed = if (offset == 0) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            // Reset to start of month to get actual max
            target.add(Calendar.MILLISECOND, 1)    // back to start of next month
            target.add(Calendar.MONTH, -1)          // back to target month
            target.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        return MonthInfo(start, end, daysElapsed)
    }
}
