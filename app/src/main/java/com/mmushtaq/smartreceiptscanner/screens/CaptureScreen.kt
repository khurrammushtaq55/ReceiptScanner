package com.mmushtaq.smartreceiptscanner.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mmushtaq.smartreceiptscanner.R
import com.mmushtaq.smartreceiptscanner.util.MediaStoreUtil
import androidx.camera.core.Preview as CameraXPreview

@Composable
fun CaptureScreen(
    onCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var bindingInProgress by remember { mutableStateOf(true) }
    var captureInProgress by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        CameraPreview(
            onReady = { ic -> imageCapture = ic; bindingInProgress = false }
        )

        Button(
            onClick = {
                imageCapture?.let { ic ->
                    captureInProgress = true
                    capturePhoto(context, ic) { uri, err ->
                        captureInProgress = false
                        if (uri != null) onCaptured(uri) else Log.e("Capture", "Failed", err)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) { Text(if (captureInProgress) stringResource(R.string.capturing) else stringResource(R.string.capture)) }

        if (bindingInProgress || captureInProgress) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraXPreview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                onReady(imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider.unbindAll()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    cb: (Uri?, Throwable?) -> Unit
) {
    val output = MediaStoreUtil.buildImageOutputOptions(context)
    imageCapture.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                cb(results.savedUri, null) // <- final MediaStore item URI
            }

            override fun onError(exception: ImageCaptureException) {
                cb(null, exception)
            }
        }
    )
}