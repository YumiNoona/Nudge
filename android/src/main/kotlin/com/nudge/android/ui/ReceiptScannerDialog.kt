package com.nudge.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nudge.android.importer.FinancialDocumentImporter
import com.nudge.android.importer.ReceiptDraft
import com.nudge.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReceiptScannerDialog(onDismiss: () -> Unit, onScanned: (ReceiptDraft) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = remember { NudgeHaptics(context) }
    var visible by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var warningDraft by remember { mutableStateOf<ReceiptDraft?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var usingFrontCamera by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    fun close() {
        visible = false
        scope.launch {
            delay(190)
            onDismiss()
        }
    }

    fun useDraft(draft: ReceiptDraft) {
        haptics.success()
        onScanned(draft)
    }

    fun process(uri: Uri, cleanup: (() -> Unit)? = null) {
        processing = true
        message = null
        warningDraft = null
        scope.launch {
            val outcome = runCatching {
                val document = FinancialDocumentImporter.readDocument(context, uri)
                val draft = FinancialDocumentImporter.parseReceipt(document.text)
                    ?: error("No clear total was found. Keep the merchant name and complete total visible.")
                document.warning to draft
            }
            cleanup?.invoke()
            processing = false
            outcome.onSuccess { (warning, draft) ->
                if (warning == null) useDraft(draft) else {
                    haptics.warning()
                    message = warning
                    warningDraft = draft
                }
            }.onFailure {
                haptics.error()
                message = it.message ?: "Nudge could not read this receipt. Retake it in brighter light."
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (!granted) message = "Camera access is off. Allow it here or choose an existing image."
    }
    LaunchedEffect(Unit) {
        visible = true
        if (!cameraGranted) permission.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(cameraGranted, lifecycleOwner) {
        if (cameraGranted) runCatching { controller.bindToLifecycle(lifecycleOwner) }
        onDispose { controller.unbind() }
    }

    fun capture() {
        if (!cameraGranted || processing) return
        processing = true
        message = null
        warningDraft = null
        val file = File.createTempFile("nudge_receipt_", ".jpg", context.cacheDir)
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    process(Uri.fromFile(file)) { file.delete() }
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    processing = false
                    message = "The photo was not captured. Hold steady and try again."
                    haptics.error()
                }
            },
        )
    }

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Transparent).statusBarsPadding().navigationBarsPadding(),
        ) {
            message?.let { status ->
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 24.dp, vertical = 30.dp).widthIn(max = 360.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1C1C1E),
                ) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(status, color = Color.White.copy(alpha = .88f), fontSize = 10.sp, lineHeight = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        warningDraft?.let { draft ->
                            TextButton(onClick = { useDraft(draft) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("Use detected result", color = DS.Signal, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = visible,
                enter = slideInVertically(tween(260)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(210)) { it } + fadeOut(tween(150)),
            ) {
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(.64f).padding(horizontal = 8.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(34.dp)).background(Color.Black),
                ) {
                            if (cameraGranted) {
                                AndroidView(
                                    factory = { cameraContext ->
                                        PreviewView(cameraContext).apply {
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                            this.controller = controller
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(Color.Black))
                            }

                    if (optionsOpen) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = .72f),
                        ) {
                            Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CameraControl(onClick = {
                                    val nextFront = !usingFrontCamera
                                    runCatching {
                                        controller.cameraSelector = if (nextFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                        usingFrontCamera = nextFront
                                        torchEnabled = false
                                    }.onFailure { message = "This camera is not available on your device." }
                                    optionsOpen = false
                                }) { Lucide.RotateCamera(size = 21.dp, color = Color.White) }
                                CameraControl(onClick = {
                                    runCatching {
                                        torchEnabled = !torchEnabled
                                        controller.enableTorch(torchEnabled)
                                    }.onFailure {
                                        torchEnabled = false
                                        message = "Flash is not available for this camera."
                                    }
                                    optionsOpen = false
                                }) { Lucide.Flashlight(size = 21.dp, color = if (torchEnabled) DS.Signal else Color.White) }
                            }
                        }
                    }

                    Row(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CameraControl(onClick = ::close) { Lucide.ChevronLeft(size = 26.dp, color = Color.White) }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Surface(
                                onClick = {
                                    if (cameraGranted) capture() else permission.launch(Manifest.permission.CAMERA)
                                },
                                enabled = !processing,
                                modifier = Modifier.size(76.dp),
                                shape = CircleShape,
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(4.dp, Color.Black.copy(alpha = .45f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (processing) CircularProgressIndicator(Modifier.size(27.dp), strokeWidth = 2.dp, color = DS.AccentDeep)
                                    else Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                        }
                        CameraControl(onClick = { optionsOpen = !optionsOpen }) {
                            Lucide.MoreVertical(size = 25.dp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraControl(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(50.dp), shape = CircleShape, color = Color.Black.copy(alpha = .55f)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
