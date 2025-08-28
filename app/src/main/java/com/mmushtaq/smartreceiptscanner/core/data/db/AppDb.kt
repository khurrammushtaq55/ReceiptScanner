package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}
