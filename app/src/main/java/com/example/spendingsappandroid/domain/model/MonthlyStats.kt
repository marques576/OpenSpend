package com.example.spendingsappandroid.domain.model

data class MonthlyStats(
    val totalSpent: Double = 0.0,
    val transactionCount: Int = 0,
    val averageTransaction: Double = 0.0,
    val largestTransaction: Double = 0.0
)
