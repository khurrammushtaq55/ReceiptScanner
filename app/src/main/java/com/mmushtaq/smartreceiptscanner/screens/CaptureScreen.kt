package com.mmushtaq.smartreceiptscanner.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mmushtaq.smartreceiptscanner.util.MediaStoreUtil

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
        ) { Text(if (captureInProgress) "Capturing..." else "Capture") }

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
    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.addListener({
            val provider = providerFuture.get()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    ctx as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                onReady(imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(ctx))

        previewView
    })
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