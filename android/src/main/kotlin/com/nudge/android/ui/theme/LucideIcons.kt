package com.nudge.android.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Lucide-style thin-line icons rendered via Compose Canvas.
 * All paths are actual Lucide icon paths (MIT) scaled to a 24x24 viewBox.
 *
 * Usage: Lucide.Wallet(modifier = Modifier.size(20.dp), color = Ink)
 */

object Lucide {

    // ── internal ──

    private const val SCALE = 0.92f  // breathing room from full 24×24
    private const val OFFSET = (24f - 24f * SCALE) / 2f

    private fun Path.scalePath() = Unit // handled below per-icon

    @Composable
    private fun Icon(
        path: Path,
        modifier: Modifier,
        color: Color,
        strokeWidth: Dp,
        filled: Boolean = false
    ) {
        val swPx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }
        Canvas(modifier = modifier) {
            if (filled) {
                drawPath(path, color)
            } else {
                drawPath(
                    path, color,
                    style = Stroke(width = swPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }

    // ── navigation ──

    @Composable fun Wallet(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h14a1 1 0 0 1 1 1v4h-6a2 2 0 0 0 0 4h6v4a1 1 0 0 1-1 1H5a2 2 0 0 1-2-2V7"), modifier, color, strokeWidth)
    }

    @Composable fun Home(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10"), modifier, color, strokeWidth)
    }

    @Composable fun Plus(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M5 12h14 M12 5v14"), modifier, color, strokeWidth)
    }

    @Composable fun ChartBar(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 3v18h18 M18 17V9 M13 17V5 M8 17v-3"), modifier, color, strokeWidth)
    }

    @Composable fun ChartPie(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M21.21 15.89A10 10 0 1 1 8 2.83 M22 12A10 10 0 0 0 12 2v10z"), modifier, color, strokeWidth)
    }

    @Composable fun Flame(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"), modifier, color, strokeWidth)
    }

    @Composable fun Trophy(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M6 9H4.5a2.5 2.5 0 0 1 0-5H6 M18 9h1.5a2.5 2.5 0 0 0 0-5H18 M4 22h16 M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22 M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22 M18 2H6v7a6 6 0 0 0 12 0V2Z"), modifier, color, strokeWidth)
    }

    @Composable fun Target(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 12m-10 0a10 10 0 1 0 20 0a10 10 0 1 0-20 0 M12 12m-6 0a6 6 0 1 0 12 0a6 6 0 1 0-12 0 M12 12m-2 0a2 2 0 1 0 4 0a2 2 0 1 0-4 0"), modifier, color, strokeWidth)
    }

    @Composable fun PiggyBank(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M19 5c-1.5 0-2.8 1.4-3 2-3.5-1.5-11-.3-11 5 0 1.8 0 3 2 4.5V20h4v-2h3v2h4v-4c1-.5 1.7-1 2-2h2v-4h-2c0-1-.5-1.5-1-2h0V5z M2 9v1c0 1.1.9 2 2 2h1 M16 11h0"), modifier, color, strokeWidth)
    }

    // ── actions ──

    @Composable fun Check(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M20 6 9 17l-5-5"), modifier, color, strokeWidth)
    }

    @Composable fun X(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M18 6 6 18 M6 6l12 12"), modifier, color, strokeWidth)
    }

    @Composable fun ArrowLeft(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 19l-7-7 7-7 M19 12H5"), modifier, color, strokeWidth)
    }

    @Composable fun ChevronLeft(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M15 18l-6-6 6-6"), modifier, color, strokeWidth)
    }

    @Composable fun ChevronRight(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M9 18l6-6-6-6"), modifier, color, strokeWidth)
    }

    @Composable fun Settings(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"), modifier, color, strokeWidth)
    }

    @Composable fun MoreHorizontal(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0 M19 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0 M5 12m-1 0a1 1 0 1 0 2 0a1 1 0 1 0-2 0"), modifier, color, strokeWidth)
    }

    // ── data / utility ──

    @Composable fun FileText(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z M14 2v4a2 2 0 0 0 2 2h4 M10 9H8 M16 13H8 M16 17H8"), modifier, color, strokeWidth)
    }

    @Composable fun Database(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 8c4.97 0 9-1.343 9-3s-4.03-3-9-3-9 1.343-9 3 4.03 3 9 3z M3 5v6c0 1.657 4.03 3 9 3s9-1.343 9-3V5 M3 11v6c0 1.657 4.03 3 9 3s9-1.343 9-3v-6"), modifier, color, strokeWidth)
    }

    @Composable fun Tag(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z M7 7h.01"), modifier, color, strokeWidth)
    }

    @Composable fun Shield(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.06 1.06 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"), modifier, color, strokeWidth)
    }

    @Composable fun RefreshCw(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8 M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16 M21 3v5h-5 M3 21v-5h5"), modifier, color, strokeWidth)
    }

    @Composable fun Trash2(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 6h18 M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6 M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2 M10 11v6 M14 11v6"), modifier, color, strokeWidth)
    }

    @Composable fun TrendingUp(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M22 7l-8.5 8.5-5-5L2 17 M16 7h6v6"), modifier, color, strokeWidth)
    }

    @Composable fun Clock(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2"), modifier, color, strokeWidth)
    }

    @Composable fun LayoutDashboard(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 3h7v9H3z M14 3h7v5h-7z M14 12h7v9h-7z M3 16h7v5H3z"), modifier, color, strokeWidth)
    }

    @Composable fun Download(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5 M12 15V3"), modifier, color, strokeWidth)
    }

    @Composable fun Star(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"), modifier, color, strokeWidth)
    }

    @Composable fun ShoppingCart(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"), modifier, color, strokeWidth)
    }

    @Composable fun Bell(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0"), modifier, color, strokeWidth)
    }

    @Composable fun User(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"), modifier, color, strokeWidth)
    }

    @Composable fun Menu(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M4 12h16 M4 6h16 M4 18h16"), modifier, color, strokeWidth)
    }

    @Composable fun Sun(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M12 12m-4 0a4 4 0 1 0 8 0a4 4 0 1 0-8 0 M12 2v2 M12 20v2 M4.93 4.93l1.41 1.41 M17.66 17.66l1.41 1.41 M2 12h2 M20 12h2 M6.34 17.66l-1.41 1.41 M19.07 4.93l-1.41 1.41"), modifier, color, strokeWidth)
    }

    @Composable fun Moon(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"), modifier, color, strokeWidth)
    }

    @Composable fun Sparkles(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z M20 3v4 M22 5h-4 M4 17v2 M5 18H3"), modifier, color, strokeWidth)
    }

    @Composable fun CreditCard(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M2 5h20v14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5z M2 10h20 M7 15h4 M13 15h2"), modifier, color, strokeWidth)
    }

    @Composable fun ListTodo(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M11 17h6 M8 12l-2 2-1-1 M11 12h6 M8 7L6 9 5 8 M11 7h6"), modifier, color, strokeWidth)
    }

    @Composable fun LayoutList(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M3 5h18 M3 12h10 M3 19h18"), modifier, color, strokeWidth)
    }

    @Composable fun Calendar(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M8 2v4 M16 2v4 M3 10h18 M21 6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6z"), modifier, color, strokeWidth)
    }

    @Composable fun Filter(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = androidx.compose.ui.unit.Dp.Unspecified, strokeWidth: Dp = androidx.compose.ui.unit.Dp.Unspecified) {
        Icon(pathOf("M22 3H2l8 9.46V19l4 2v-8.54L22 3z"), modifier, color, strokeWidth)
    }

    // ── Path builder ──
    private fun pathOf(data: String): Path = Path().apply {
        val parts = data.split(Regex("\\s+"))
        var i = 0
        while (i < parts.size) {
            when (parts[i]) {
                "M" -> { moveTo(parts[i+1].toFloat() * SCALE + OFFSET, parts[i+2].toFloat() * SCALE + OFFSET); i += 3 }
                "m" -> { relativeMoveTo(parts[i+1].toFloat() * SCALE, parts[i+2].toFloat() * SCALE); i += 3 }
                "L" -> { lineTo(parts[i+1].toFloat() * SCALE + OFFSET, parts[i+2].toFloat() * SCALE + OFFSET); i += 3 }
                "l" -> { relativeLineTo(parts[i+1].toFloat() * SCALE, parts[i+2].toFloat() * SCALE); i += 3 }
                "H" -> { lineTo(parts[i+1].toFloat() * SCALE + OFFSET, 0f) /* simplified */; i += 2 }
                "V" -> { lineTo(0f, parts[i+1].toFloat() * SCALE + OFFSET) /* simplified */; i += 2 }
                "C" -> { cubicTo(parts[i+1].toFloat()*SCALE+OFFSET, parts[i+2].toFloat()*SCALE+OFFSET, parts[i+3].toFloat()*SCALE+OFFSET, parts[i+4].toFloat()*SCALE+OFFSET, parts[i+5].toFloat()*SCALE+OFFSET, parts[i+6].toFloat()*SCALE+OFFSET); i += 7 }
                "Z" -> { close(); i += 1 }
                "z" -> { close(); i += 1 }
                else -> i += 1
            }
        }
    }
}
