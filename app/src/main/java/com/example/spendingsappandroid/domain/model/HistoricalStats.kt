package com.example.spendingsappandroid.domain.model

data class HistoricalStats(
    val averageMonthlySpent: Double = 0.0,
    val totalMonths: Int = 0,
    val totalSpentAllTime: Double = 0.0
)
