package com.example.spendingsappandroid.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendingsappandroid.data.local.TransactionEntity
import com.example.spendingsappandroid.data.repository.MonitoredAppsRepository
import com.example.spendingsappandroid.data.repository.TransactionRepository
import com.example.spendingsappandroid.domain.model.HistoricalStats
import com.example.spendingsappandroid.domain.model.MonthlyStats
import com.example.spendingsappandroid.domain.usecase.HistoricalAverageUseCase
import com.example.spendingsappandroid.domain.usecase.MonthlyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    monthlyStatsUseCase: MonthlyStatsUseCase,
    historicalAverageUseCase: HistoricalAverageUseCase,
    private val repository: TransactionRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) : ViewModel() {

    val currency: StateFlow<String> = monitoredAppsRepository.currency

    val showDailyAverage: StateFlow<Boolean> = monitoredAppsRepository.showDailyAverage
    val showMedianTransaction: StateFlow<Boolean> = monitoredAppsRepository.showMedianTransaction
    val showTopMerchant: StateFlow<Boolean> = monitoredAppsRepository.showTopMerchant
    val showSpendingByApp: StateFlow<Boolean> = monitoredAppsRepository.showSpendingByApp
    val showActiveDays: StateFlow<Boolean> = monitoredAppsRepository.showActiveDays
    val showHistoricalAverage: StateFlow<Boolean> = monitoredAppsRepository.showHistoricalAverage

    val historicalStats: StateFlow<HistoricalStats> = historicalAverageUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoricalStats()
        )

    private val _monthOffset = MutableStateFlow(0)
    val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

    private val monthBounds: Pair<Long, Long>
        get() = getMonthBoundaries(_monthOffset.value)

    val monthlyStats: StateFlow<MonthlyStats> = monthlyStatsUseCase(monthBounds)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthlyStats()
        )

    val transactions: StateFlow<List<TransactionEntity>> =
        repository.getTransactionsForMonth(monthBounds.first, monthBounds.second)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun previousMonth() {
        _monthOffset.value = _monthOffset.value - 1
    }

    fun nextMonth() {
        if (_monthOffset.value < 0) {
            _monthOffset.value = _monthOffset.value + 1
        }
    }

    private fun getMonthBoundaries(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.MONTH, offset)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return start to end
    }
}
