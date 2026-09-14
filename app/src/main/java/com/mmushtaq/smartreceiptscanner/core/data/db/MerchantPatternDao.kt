package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Remembers the last user correction for a given merchant (by normalized name),
 * so the next receipt from the same merchant can be pre-filled instead of re-guessed.
 */
@Entity(tableName = "merchant_patterns")
data class MerchantPatternEntity(
    @PrimaryKey val merchantKey: String, // normalized: trimmed + lowercased merchant name
    val category: String?,
    val currency: String?,
    val updatedAt: Long
)

@Dao
interface MerchantPatternDao {
    @Query("SELECT * FROM merchant_patterns WHERE merchantKey = :merchantKey")
    suspend fun get(merchantKey: String): MerchantPatternEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MerchantPatternEntity)
}
