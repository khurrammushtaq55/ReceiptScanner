package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Projection for spend-by-category summaries (e.g. Home screen monthly breakdown). */
data class CategoryTotal(
    val category: String?,
    val totalMinor: Long,
    val count: Int
)

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<ReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ReceiptEntity>)

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun get(id: String): ReceiptEntity?

    @Delete
    suspend fun delete(entity: ReceiptEntity)

    @Query(
        """
        SELECT category, SUM(COALESCE(totalMinor, 0)) AS totalMinor, COUNT(*) AS count
        FROM receipts
        WHERE createdAt >= :from AND createdAt < :to
        GROUP BY category
        """
    )
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>
}
