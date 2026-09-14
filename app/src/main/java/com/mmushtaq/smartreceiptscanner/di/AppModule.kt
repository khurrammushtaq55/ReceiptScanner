package com.mmushtaq.smartreceiptscanner.di

import android.app.Application
import androidx.room.Room
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.AppDb
import com.mmushtaq.smartreceiptscanner.core.data.db.ALL_MIGRATIONS
import com.mmushtaq.smartreceiptscanner.core.ocr.OcrClient
import com.mmushtaq.smartreceiptscanner.core.parser.MerchantPatternStore
import com.mmushtaq.smartreceiptscanner.scan.OcrViewModel
import com.mmushtaq.smartreceiptscanner.screens.HomeViewModel
import com.mmushtaq.smartreceiptscanner.screens.history.HistoryViewModel
import com.mmushtaq.smartreceiptscanner.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // DB
    single {
        Room.databaseBuilder(
            androidContext() as Application,
            AppDb::class.java,
            "receipts.db"
        ).addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDb>().receiptDao() }
    single { get<AppDb>().merchantPatternDao() }
    single { ReceiptRepository(get()) }
    single { MerchantPatternStore(get()) }

    // OCR
    single { OcrClient(androidContext()) }

    // ViewModels
    viewModel { OcrViewModel(get(), get(), get()) }  // ocr, repo, merchant pattern store
    viewModel { HistoryViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
