package com.mmushtaq.smartreceiptscanner.di

import android.app.Application
import androidx.room.Room
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.AppDb
import com.mmushtaq.smartreceiptscanner.core.ocr.OcrClient
import com.mmushtaq.smartreceiptscanner.scan.OcrViewModel
import com.mmushtaq.smartreceiptscanner.screens.history.HistoryViewModel
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
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDb>().receiptDao() }
    single { ReceiptRepository(get()) }

    // OCR
    single { OcrClient(androidContext()) }

    // ViewModels
    viewModel { OcrViewModel(get(), get()) }        // now also takes repo
    viewModel { HistoryViewModel(get()) }
}
