package com.example.spendingsappandroid.data.repository

import com.example.spendingsappandroid.data.local.TransactionDao
import com.example.spendingsappandroid.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun existsByHash(hash: String): Boolean {
        return transactionDao.existsByHash(hash)
    }

    fun getTransactionsForMonth(start: Long, end: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForMonth(start, end)
    }

    fun getTotalSpent(start: Long, end: Long): Flow<Double> {
        return transactionDao.getTotalSpent(start, end)
    }

    fun getTransactionCount(start: Long, end: Long): Flow<Int> {
        return transactionDao.getTransactionCount(start, end)
    }

    fun getLargestTransaction(start: Long, end: Long): Flow<Double> {
        return transactionDao.getLargestTransaction(start, end)
    }

    fun getSmallestTransaction(start: Long, end: Long): Flow<Double> {
        return transactionDao.getSmallestTransaction(start, end)
    }

    fun getAllAmounts(start: Long, end: Long): Flow<List<Double>> {
        return transactionDao.getAllAmounts(start, end)
    }

    fun getTopMerchant(start: Long, end: Long): Flow<String?> {
        return transactionDao.getTopMerchant(start, end)
    }

    fun getTopSourceApp(start: Long, end: Long): Flow<String?> {
        return transactionDao.getTopSourceApp(start, end)
    }

    fun getActiveDays(start: Long, end: Long): Flow<Int> {
        return transactionDao.getActiveDays(start, end)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteById(id)
    }
}
