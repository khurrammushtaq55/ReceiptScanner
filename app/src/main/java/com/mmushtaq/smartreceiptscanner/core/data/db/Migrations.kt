package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: added `category` column to `receipts` (Phase 1 — Categories & Budgeting). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE receipts ADD COLUMN category TEXT")
    }
}

/** v2 -> v3: added `merchant_patterns` table (Phase 3 — per-merchant correction memory). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchant_patterns (
                merchantKey TEXT NOT NULL PRIMARY KEY,
                category TEXT,
                currency TEXT,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
