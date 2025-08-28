package com.mmushtaq.smartreceiptscanner

import android.app.Application
import com.mmushtaq.smartreceiptscanner.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class ReceiptApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReceiptApp)
            modules(appModule)
        }
    }
}