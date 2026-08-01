package com.nudge.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nudge.android.ui.theme.*

data class CardScanResult(val last4: String, val network: String, val expiry: String?)

@Composable
fun CardScannerDialog(onDismiss: () -> Unit, onScanned: (CardScanResult) -> Unit) {
    val context = LocalContext.current
    var processing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val haptics = remember { NudgeHaptics(context) }

    fun process(bitmap: Bitmap) {
        processing = true
        error = null
        scanCardBitmap(bitmap) { result ->
            processing = false
            bitmap.recycle()
            if (result == null) {
                haptics.error()
                error = "No valid card number found. Keep the card flat, avoid glare, and try again."
            } else {
                haptics.success()
                onScanned(result)
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) process(bitmap) else error = "Capture cancelled."
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) camera.launch(null) else error = "Camera permission is required to scan a card."
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF0D100E)) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Lucide.X(size = 22.dp, color = Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Scan card", style = DSTypography.headlineMedium, color = Color.White)
                            Text("Only the last four digits are saved", style = DSTypography.bodySmall, color = Color.White.copy(alpha = .56f))
                        }
                    }
                    Spacer(Modifier.weight(.35f))
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.58f)
                            .border(2.dp, DS.Signal, RoundedCornerShape(26.dp))
                            .background(Color.White.copy(alpha = .035f), RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Lucide.CreditCard(size = 54.dp, color = DS.Signal)
                            Spacer(Modifier.height(14.dp))
                            Text("Align the full card inside the frame", color = Color.White, style = DSTypography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .07f)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Shield(size = 20.dp, color = DS.Signal)
                            Spacer(Modifier.width(10.dp))
                            Text("The image is processed on-device and discarded. Nudge never stores the full card number or CVV.", color = Color.White.copy(alpha = .68f), style = DSTypography.bodySmall)
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = DS.Negative, style = DSTypography.bodySmall)
                    }
                    Spacer(Modifier.weight(.65f))
                    Button(
                        onClick = { if (cameraGranted) camera.launch(null) else permission.launch(Manifest.permission.CAMERA) },
                        enabled = !processing,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(19.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DS.Signal, contentColor = DS.InkPrimary)
                    ) {
                        if (processing) CircularProgressIndicator(Modifier.size(22.dp), color = DS.InkPrimary, strokeWidth = 2.dp)
                        else { Lucide.Camera(size = 21.dp, color = DS.InkPrimary); Spacer(Modifier.width(9.dp)); Text("Open camera", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

private fun scanCardBitmap(bitmap: Bitmap, callback: (CardScanResult?) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { text ->
            val normalized = text.text.replace('\n', ' ')
            val candidates = Regex("(?:\\d[ -]?){13,19}").findAll(normalized)
                .map { it.value.filter(Char::isDigit) }
                .filter { it.length in 13..19 && passesLuhn(it) }
                .toList()
            val number = candidates.maxByOrNull { it.length }
            if (number == null) callback(null)
            else {
                val expiry = Regex("(?:0[1-9]|1[0-2])\\s*[/|-]\\s*(?:\\d{2}|\\d{4})").find(normalized)?.value?.replace(" ", "")
                callback(CardScanResult(number.takeLast(4), detectNetwork(number), expiry))
            }
            recognizer.close()
        }
        .addOnFailureListener { recognizer.close(); callback(null) }
}

internal fun passesLuhn(number: String): Boolean {
    var sum = 0
    var doubleDigit = false
    for (i in number.indices.reversed()) {
        var digit = number[i].digitToInt()
        if (doubleDigit) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
        doubleDigit = !doubleDigit
    }
    return number.length in 13..19 && sum % 10 == 0
}

private fun detectNetwork(number: String): String = when {
    number.startsWith("4") -> "Visa"
    number.take(2).toIntOrNull() in 51..55 || number.take(4).toIntOrNull() in 2221..2720 -> "Mastercard"
    number.startsWith("34") || number.startsWith("37") -> "American Express"
    number.startsWith("60") || number.startsWith("65") || number.take(2).toIntOrNull() in 81..89 -> "RuPay"
    else -> "Card"
}
