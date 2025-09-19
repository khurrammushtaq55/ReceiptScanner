package com.mmushtaq.smartreceiptscanner

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmushtaq.smartreceiptscanner.ads.AdsInitializer
import com.mmushtaq.smartreceiptscanner.ads.InterstitialAdManager
import com.mmushtaq.smartreceiptscanner.screens.CaptureScreen
import com.mmushtaq.smartreceiptscanner.screens.HomeScreen
import com.mmushtaq.smartreceiptscanner.screens.ReviewScreen
import com.mmushtaq.smartreceiptscanner.screens.history.HistoryScreen
import com.mmushtaq.smartreceiptscanner.screens.utils.LoadingOverlay
import com.mmushtaq.smartreceiptscanner.screens.utils.rememberMainAppState
import com.mmushtaq.smartreceiptscanner.ui.theme.AppTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdsInitializer.init(this /*, testDeviceIds = listOf("85A8F6E80A77E80AEC833CA3993A7071") */)

        setContent {
            // If you set up AppTheme earlier, use that; otherwise MaterialTheme is fine.
            AppTheme {
                val app = rememberMainAppState(activity = this)

                // Hardware back behaves like your top-bar back.
                BackHandler(enabled = app.canGoBack) { app.onBack() }

                Scaffold(
                    snackbarHost = { SnackbarHost(app.snackbarHostState) }
                ) { pad ->
                    Box(
                        Modifier
                            .padding(pad)
                            .fillMaxSize()
                    ) {
                        when (app.current) {
                            Screen.Home -> HomeScreen(
                                onOpenCamera = app::onOpenCamera,
                                onImagePicked = app::onImagePicked,
                                onPdfPicked = app::onPdfPicked,
                                onOpenHistory = {
                                    app.navigate(Screen.History)
                                }
                            )

                            Screen.Camera -> CaptureScreen(
                                onCaptured = app::onCaptured,
                                modifier = Modifier.fillMaxSize()
                            )

                            Screen.Review -> {
                                val uri = app.pendingUri.value
                                if (uri != null) {
                                    ReviewScreen(
                                        imageUri = uri,
                                        contentResolver = contentResolver,
                                        onDone = app::onReviewDone
                                    )
                                } else {
                                    app.navigateHome()
                                }
                            }

                            Screen.History -> {
                                HistoryScreenWithAdOnBack(onBack = { app.onBack() })
                            }
                        }

                        if (app.isRendering) LoadingOverlay()
                    }
                }
            }
        }
    }


}

@Composable
fun HistoryScreenWithAdOnBack(onBack: () -> Unit) {
    val activity = LocalActivity.current as Activity
    val interstitial = remember { InterstitialAdManager(activity) }
    LaunchedEffect(Unit) { interstitial.load() }

    BackHandler {
        if (interstitial.isReady) {
            interstitial.show {
                interstitial.load()   // preload next
                onBack()              // navigate after ad closes
            }
        } else {
            onBack()
        }
    }

    HistoryScreen(onBack = onBack, onOpenDetail = { /* ... */ })
}

sealed class Screen {
    data object Home : Screen()
    data object Camera : Screen()
    data object Review : Screen()
    data object History : Screen()

}