package com.nudge.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.Nc
import java.util.concurrent.Executors

@androidx.compose.runtime.Composable
fun ReceiptScanScreen(
    onResult: (amount: String, merchant: String, date: String) -> Unit,
    onCancel: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedAmount by remember { mutableStateOf("") }
    var extractedMerchant by remember { mutableStateOf("") }
    var extractedDate by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (capturedBitmap != null) {
        // Review extracted data
        Column(Modifier.fillMaxSize().background(Nc.background).padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Review Scanned Receipt", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
                TextButton(onClick = onCancel) { Text("Cancel", color = Nc.inkSoft) }
            }
            Spacer(Modifier.height(16.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = Nc.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Scanned ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Nc.accent)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(extractedAmount, { extractedAmount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(extractedMerchant, { extractedMerchant = it }, label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(extractedDate, { extractedDate = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onResult(extractedAmount, extractedMerchant, extractedDate) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Nc.accent)
            ) { Text("Confirm & Add", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    } else if (!hasCameraPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera access needed", fontSize = 16.sp, color = Nc.ink)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant Permission") }
            }
        }
    } else {
        // Camera preview
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val imageCapture = remember { ImageCapture.Builder().setTargetResolution(AndroidSize(1920, 1080)).build() }
            val executor = remember { Executors.newSingleThreadExecutor() }

            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Shutter button
            var isPressed by remember { mutableStateOf(false) }
            val shutterScale by animateFloatAsState(if (isPressed) 0.9f else 1f, spring(), label = "shutter")

            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp).scale(shutterScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White).clickable {
                            isPressed = true
                            imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                    isProcessing = true
                                    val bitmap = image.toBitmap()
                                    capturedBitmap = bitmap
                                    runOcr(bitmap) { amount, merchant, date ->
                                        extractedAmount = amount
                                        extractedMerchant = merchant
                                        extractedDate = date
                                        isProcessing = false
                                    }
                                    image.close()
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    isPressed = false
                                }
                            })
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }

            // Cancel
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) { Text("Cancel", color = Color.White) }

            // Processing overlay
            if (isProcessing) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Nc.accent)
                        Spacer(Modifier.height(12.dp))
                        Text("Extracting text...", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun runOcr(bitmap: Bitmap, callback: (String, String, String) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)
    recognizer.process(image)
        .addOnSuccessListener { result ->
            val lines = result.textBlocks.flatMap { it.lines }.map { it.text }
            val amount = lines.find { it.matches(Regex(".*?(total|amount|due|₹|\\$|USD|EUR).*?[\\d,.]+.*")) }
                ?.let { Regex("[\\d,]+[.]?\\d*").find(it)?.value } ?: ""
            val merchant = lines.firstOrNull() ?: ""
            val date = lines.find { it.matches(Regex(".*?\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*")) }
                ?.let { Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}").find(it)?.value } ?: ""
            callback(amount, merchant, date)
        }
        .addOnFailureListener { callback("", "", "") }
}

private fun androidx.camera.core.ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return Bitmap.createBitmap(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!)
}
