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
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nudge.android.importer.*
import com.nudge.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun ReceiptScannerDialog(onDismiss: () -> Unit, onScanned: (DetailedReceiptDraft) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = remember { NudgeHaptics(context) }
    val pages = remember { mutableStateListOf<ReceiptPageDraft>() }
    var visible by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var readyForNext by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingWarningPage by remember { mutableStateOf<ReceiptPageDraft?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var usingFrontCamera by remember { mutableStateOf(false) }
    var handedOff by remember { mutableStateOf(false) }
    val cameraGranted = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val receiptFolder = remember { File(context.filesDir, "receipts").apply { mkdirs() } }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    fun cleanSession() {
        if (!handedOff) pages.forEach { runCatching { File(Uri.parse(it.localUri).path.orEmpty()).delete() } }
    }
    fun close() {
        visible = false
        scope.launch { delay(190); cleanSession(); onDismiss() }
    }
    fun acceptPage(page: ReceiptPageDraft) {
        pages += page
        pendingWarningPage = null
        readyForNext = false
        message = "Page ${pages.size} captured · overlap the last few lines if more remains"
        haptics.success()
    }
    fun process(uri: Uri) {
        processing = true
        message = null
        pendingWarningPage = null
        scope.launch {
            runCatching {
                val document = FinancialDocumentImporter.readDocument(context, uri)
                if (document.text.isBlank()) error("No readable receipt text found. Hold steady and retake this section.")
                ReceiptPageDraft(uri.toString(), document.text, document.warning)
            }.onSuccess { page ->
                if (page.warning == null) acceptPage(page) else {
                    pendingWarningPage = page
                    message = page.warning
                    haptics.warning()
                }
            }.onFailure {
                message = it.message ?: "Nudge could not read this section. Retake it in brighter light."
                haptics.error()
            }
            processing = false
        }
    }
    fun copyAndProcess(sources: List<Uri>) {
        if (sources.isEmpty()) return
        scope.launch {
            processing = true
            message = null
            var warningCount = 0
            var importedCount = 0
            for (source in sources) {
                val destination = File(receiptFolder, "receipt_${UUID.randomUUID()}.jpg")
                runCatching {
                    context.contentResolver.openInputStream(source)?.use { input -> destination.outputStream().use(input::copyTo) }
                        ?: error("Unable to open this image")
                    val localUri = Uri.fromFile(destination)
                    val document = FinancialDocumentImporter.readDocument(context, localUri)
                    if (document.text.isBlank()) error("No readable receipt text found in one of the selected images.")
                    ReceiptPageDraft(localUri.toString(), document.text, document.warning)
                }.onSuccess { page ->
                    pages += page
                    importedCount++
                    if (page.warning != null) warningCount++
                }.onFailure {
                    destination.delete()
                    message = it.message ?: "One selected image could not be read."
                }
            }
            processing = false
            if (importedCount > 0) {
                readyForNext = false
                message = buildString {
                    append("$importedCount ${if (importedCount == 1) "section" else "sections"} imported in order")
                    if (warningCount > 0) append(" · check $warningCount low-quality ${if (warningCount == 1) "scan" else "scans"}")
                }
                haptics.success()
            } else haptics.error()
        }
    }
    fun finish() {
        if (pages.isEmpty() || processing) return
        processing = true
        scope.launch {
            val draft = ReceiptIntelligence.parse(pages.map { it.ocrText }, pages)
            processing = false
            if (draft == null) {
                message = "No reliable grand total was found. Capture the bottom section containing the printed total."
                haptics.error()
            } else {
                handedOff = true
                haptics.success()
                onScanned(draft)
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        copyAndProcess(uris)
    }
    LaunchedEffect(Unit) {
        visible = true
        if (!cameraGranted) message = "Camera access is off. Close the scanner and allow it before starting again."
    }
    DisposableEffect(cameraGranted, lifecycleOwner) {
        if (cameraGranted) runCatching { controller.bindToLifecycle(lifecycleOwner) }
        onDispose { controller.unbind() }
    }

    fun capture() {
        if (!cameraGranted || processing) return
        processing = true
        message = null
        val file = File(receiptFolder, "receipt_${UUID.randomUUID()}.jpg")
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = process(Uri.fromFile(file))
                override fun onError(exception: ImageCaptureException) {
                    file.delete(); processing = false
                    message = "The photo was not captured. Hold steady and try again."
                    haptics.error()
                }
            },
        )
    }

    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Transparent).statusBarsPadding().navigationBarsPadding()) {
            val compactHeight = maxHeight < 620.dp
            val tabletWidth = maxWidth >= 600.dp
            val desiredPreviewHeight = maxHeight * when {
                compactHeight -> .52f
                tabletWidth -> .66f
                else -> .56f
            }
            // Gesture navigation can report a zero-height navigation inset on some OEM builds.
            // Keep a physical clearance as well, so the rounded viewport and shutter never sit
            // under the system gesture target.
            val bottomClearance = if (compactHeight) 20.dp else 28.dp
            val previewHeight = desiredPreviewHeight.coerceAtMost(680.dp).coerceAtMost(maxHeight - bottomClearance - 12.dp)
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (pages.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(15.dp), color = Color.Black.copy(alpha = .72f)) {
                        Text(
                            "${pages.size} ${if (pages.size == 1) "section" else "sections"} ready",
                            Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = Color.White,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                message?.let { status ->
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1C1C1E), modifier = Modifier.padding(horizontal = 24.dp)) {
                        Column(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(status, color = Color.White.copy(.86f), fontSize = 10.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                            pendingWarningPage?.let { page ->
                                Row {
                                    TextButton(onClick = {
                                        File(Uri.parse(page.localUri).path.orEmpty()).delete(); pendingWarningPage = null; message = "Retake this section"
                                    }) { Text("Retake", color = DS.Signal) }
                                    TextButton(onClick = { acceptPage(page) }) { Text("Use anyway", color = DS.Positive) }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter), visible = visible,
                enter = slideInVertically(tween(300)) { it / 3 } + fadeIn(tween(180)) + scaleIn(tween(280), initialScale = .97f),
                exit = slideOutVertically(tween(230)) { it / 3 } + fadeOut(tween(170)) + scaleOut(tween(220), targetScale = .97f),
            ) {
                Box(
                    Modifier.fillMaxWidth().widthIn(max = 760.dp).height(previewHeight)
                        .padding(horizontal = 8.dp).offset(y = -bottomClearance)
                        .clip(RoundedCornerShape(34.dp)).background(Color.Black),
                ) {
                    if (cameraGranted) AndroidView(
                        factory = { cameraContext -> PreviewView(cameraContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            this.controller = controller
                        } }, modifier = Modifier.fillMaxSize(),
                    ) else Box(Modifier.fillMaxSize().background(Color.Black))

                    if (optionsOpen) Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 92.dp),
                        shape = RoundedCornerShape(24.dp), color = Color.Black.copy(.76f),
                    ) {
                        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CameraControl({
                                val nextFront = !usingFrontCamera
                                runCatching { controller.cameraSelector = if (nextFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA; usingFrontCamera = nextFront; torchEnabled = false }
                                    .onFailure { message = "This camera is not available." }
                                optionsOpen = false
                            }) { Lucide.RotateCamera(size = 21.dp, color = Color.White) }
                            CameraControl({
                                runCatching { torchEnabled = !torchEnabled; controller.enableTorch(torchEnabled) }
                                    .onFailure { torchEnabled = false; message = "Flash is unavailable." }
                                optionsOpen = false
                            }) { Lucide.Flashlight(size = 21.dp, color = if (torchEnabled) DS.Signal else Color.White) }
                            CameraControl({ optionsOpen = false; gallery.launch("image/*") }) { Lucide.Image(size = 21.dp, color = Color.White) }
                        }
                    }

                    if (!readyForNext && pages.isNotEmpty()) {
                        Row(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CameraControl({
                                pages.removeLastOrNull()?.let { File(Uri.parse(it.localUri).path.orEmpty()).delete() }
                                readyForNext = true; message = "Retake this section"
                            }) { Lucide.RefreshCw(size = 21.dp, color = Color.White) }
                            Button(
                                onClick = { readyForNext = true; message = "Capture section ${pages.size + 1} with a few overlapping lines" },
                                modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(.92f), contentColor = Color.Black),
                            ) { Text("Continue", fontFamily = MonoFamily, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = ::finish, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DS.Signal, contentColor = Color.Black),
                            ) { Text("Finish", fontFamily = MonoFamily, fontWeight = FontWeight.Bold) }
                        }
                    } else Row(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CameraControl(::close) { Lucide.ChevronLeft(size = 26.dp, color = Color.White) }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Surface(
                                onClick = ::capture, enabled = cameraGranted && !processing,
                                modifier = Modifier.size(76.dp), shape = CircleShape, color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(4.dp, Color.Black.copy(.45f)),
                            ) { Box(contentAlignment = Alignment.Center) {
                                if (processing) CircularProgressIndicator(Modifier.size(27.dp), strokeWidth = 2.dp, color = DS.AccentDeep)
                                else Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White))
                            } }
                        }
                        CameraControl({ optionsOpen = !optionsOpen }) { Lucide.MoreVertical(size = 25.dp, color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraControl(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(50.dp), shape = CircleShape, color = Color.Black.copy(.55f)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
