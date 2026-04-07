package com.example.spendingsappandroid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE hash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsForMonth(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun getTotalSpent(start: Long, end: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun getTransactionCount(start: Long, end: Long): Flow<Int>

    @Query("SELECT COALESCE(MAX(amount), 0.0) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun getLargestTransaction(start: Long, end: Long): Flow<Double>

    @Query("SELECT COALESCE(MIN(amount), 0.0) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun getSmallestTransaction(start: Long, end: Long): Flow<Double>

    @Query("SELECT amount FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY amount ASC")
    fun getAllAmounts(start: Long, end: Long): Flow<List<Double>>

    @Query("SELECT merchant FROM transactions WHERE timestamp BETWEEN :start AND :end GROUP BY merchant ORDER BY SUM(amount) DESC LIMIT 1")
    fun getTopMerchant(start: Long, end: Long): Flow<String?>

    @Query("SELECT sourceApp FROM transactions WHERE timestamp BETWEEN :start AND :end GROUP BY sourceApp ORDER BY SUM(amount) DESC LIMIT 1")
    fun getTopSourceApp(start: Long, end: Long): Flow<String?>

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime')) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    fun getActiveDays(start: Long, end: Long): Flow<Int>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
