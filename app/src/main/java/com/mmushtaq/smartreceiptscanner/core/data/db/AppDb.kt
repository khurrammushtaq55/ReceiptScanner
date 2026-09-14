package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReceiptEntity::class, MerchantPatternEntity::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun merchantPatternDao(): MerchantPatternDao
}
