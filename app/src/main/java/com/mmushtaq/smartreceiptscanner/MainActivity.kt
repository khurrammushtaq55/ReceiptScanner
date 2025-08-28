package com.mmushtaq.smartreceiptscanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmushtaq.smartreceiptscanner.screens.CaptureScreen
import com.mmushtaq.smartreceiptscanner.screens.HomeScreen
import com.mmushtaq.smartreceiptscanner.screens.ReviewScreen
import com.mmushtaq.smartreceiptscanner.screens.history.HistoryScreen
import com.mmushtaq.smartreceiptscanner.screens.utils.LoadingOverlay
import com.mmushtaq.smartreceiptscanner.screens.utils.rememberMainAppState
import com.mmushtaq.smartreceiptscanner.ui.theme.AppTheme
import com.mmushtaq.smartreceiptscanner.util.PdfUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // If you set up AppTheme earlier, use that; otherwise MaterialTheme is fine.
            AppTheme {
                val app = rememberMainAppState(activity = this)

                // Hardware back behaves like your top-bar back.
                BackHandler(enabled = app.canGoBack) { app.onBack() }

                Scaffold(
                    snackbarHost = { SnackbarHost(app.snackbarHostState) }
                ) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (app.current) {
                            Screen.Home -> HomeScreen(
                                onOpenCamera  = app::onOpenCamera,
                                onImagePicked = app::onImagePicked,
                                onPdfPicked   = app::onPdfPicked,
                                onOpenHistory = { app.navigate(Screen.History) }
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

                            Screen.History -> HistoryScreen(
                                onBack = { app.onBack() },
                                onOpenDetail = { /* TODO: detail */ }
                            )
                        }

                        if (app.isRendering) LoadingOverlay()
                    }
                }
            }
        }
    }
}

sealed class Screen {
    data object Home : Screen()
    data object Camera : Screen()
    data object Review : Screen()
    data object History : Screen()

}