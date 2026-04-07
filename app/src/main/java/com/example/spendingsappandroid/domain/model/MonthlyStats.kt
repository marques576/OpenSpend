package com.example.spendingsappandroid.domain.model

data class MonthlyStats(
    val totalSpent: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransaction: Double = 0.0,
    val largestTransaction: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val medianTransaction: Double = 0.0,
    val topMerchant: String = "",
    val spendingByApp: Map<String, Double> = emptyMap(),
    val activeDays: Int = 0
)
