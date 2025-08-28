package com.mmushtaq.smartreceiptscanner.screens.utils

import android.app.Activity
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mmushtaq.smartreceiptscanner.Screen
import com.mmushtaq.smartreceiptscanner.util.PdfUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberMainAppState(
    activity: Activity,
): MainAppState {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentScreen = remember { mutableStateOf<Screen>(Screen.Home) }

    val backStack = remember { mutableStateListOf<Screen>() }
    val pendingUri = remember { mutableStateOf<Uri?>(null) }
    val isRendering = remember { mutableStateOf(false) }

    return remember {
        MainAppState(
            activity = activity,
            snackbarHostState = snackbarHostState,
            currentSc = currentScreen,
            backStack = backStack,
            pendingUri = pendingUri,
            isRenderingState = isRendering,
            scope = scope,
        )
    }
}

class MainAppState(
    private val activity: Activity,
    val snackbarHostState: SnackbarHostState,
    private val currentSc: MutableState<Screen>,
    private val backStack: SnapshotStateList<Screen>,
    val pendingUri: MutableState<Uri?>,
    private val isRenderingState: MutableState<Boolean>,
    private val scope: CoroutineScope
) {
    val isRendering: Boolean get() = isRenderingState.value
    val canGoBack: Boolean get() = currentSc.value != Screen.Home

    // Navigation
    val current get() = currentSc.value
    fun navigate(to: Screen) { backStack.add(currentSc.value); currentSc.value = to }
    fun navigateHome() { backStack.clear(); pendingUri.value = null; currentSc.value = Screen.Home }
    fun onBack(): Boolean {
        val prev = backStack.removeLastOrNull() ?: return false
        currentSc.value = prev
        if (prev != Screen.Review) pendingUri.value = null
        return true
    }

    // Home actions
    fun onOpenCamera() = navigate(Screen.Camera)
    fun onImagePicked(uri: Uri) { pendingUri.value = uri; navigate(Screen.Review) }
    fun onCaptured(uri: Uri) { pendingUri.value = uri; navigate(Screen.Review) }
    fun onReviewDone() { pendingUri.value = null; onBack() }

    fun onPdfPicked(uri: Uri) {
        isRenderingState.value = true
        scope.launch(Dispatchers.IO) {
            val imgUri = PdfUtil.renderFirstPageToCacheImage(activity, uri)
            withContext(Dispatchers.Main) {
                isRenderingState.value = false
                if (imgUri != null) {
                    pendingUri.value = imgUri
                    navigate(Screen.Review)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Couldn't read PDF") }
                }
            }
        }
    }
}
