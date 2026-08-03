package com.nudge.android.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stable icon facade used throughout the app.
 *
 * The previous implementation attempted to parse SVG path strings at runtime.
 * Compact SVG commands were skipped, so many icons rendered as empty boxes on
 * real devices. These are compiled AndroidX vectors and therefore render
 * consistently across supported Android versions.
 */
object Lucide {
    @Composable
    private fun Render(
        image: ImageVector,
        modifier: Modifier,
        color: Color,
        size: Dp
    ) {
        Icon(
            imageVector = image,
            contentDescription = null,
            tint = if (color == Color.Unspecified) LocalContentColor.current else color,
            modifier = modifier.then(Modifier.size(if (size == Dp.Unspecified) 24.dp else size))
        )
    }

    @Composable fun Wallet(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.AccountBalanceWallet, modifier, color, size)
    @Composable fun Home(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Home, modifier, color, size)
    @Composable fun Plus(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Add, modifier, color, size)
    @Composable fun ChartBar(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.BarChart, modifier, color, size)
    @Composable fun ChartPie(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.PieChart, modifier, color, size)
    @Composable fun Flame(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.LocalFireDepartment, modifier, color, size)
    @Composable fun Trophy(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.EmojiEvents, modifier, color, size)
    @Composable fun Target(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.TrackChanges, modifier, color, size)
    @Composable fun PiggyBank(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Savings, modifier, color, size)
    @Composable fun Check(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Check, modifier, color, size)
    @Composable fun X(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Close, modifier, color, size)
    @Composable fun ArrowLeft(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ChevronLeft, modifier, color, size)
    @Composable fun ChevronLeft(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ChevronLeft, modifier, color, size)
    @Composable fun ChevronRight(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ChevronRight, modifier, color, size)
    @Composable fun Settings(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Settings, modifier, color, size)
    @Composable fun MoreHorizontal(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.MoreHoriz, modifier, color, size)
    @Composable fun FileText(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Description, modifier, color, size)
    @Composable fun Database(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Storage, modifier, color, size)
    @Composable fun Tag(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.LocalOffer, modifier, color, size)
    @Composable fun Shield(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Shield, modifier, color, size)
    @Composable fun RefreshCw(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Refresh, modifier, color, size)
    @Composable fun Trash2(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Delete, modifier, color, size)
    @Composable fun TrendingUp(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.TrendingUp, modifier, color, size)
    @Composable fun Clock(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Schedule, modifier, color, size)
    @Composable fun LayoutDashboard(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Dashboard, modifier, color, size)
    @Composable fun Download(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Download, modifier, color, size)
    @Composable fun Star(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Star, modifier, color, size)
    @Composable fun Heart(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Favorite, modifier, color, size)
    @Composable fun ShoppingCart(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ShoppingCart, modifier, color, size)
    @Composable fun Bell(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Notifications, modifier, color, size)
    @Composable fun User(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Person, modifier, color, size)
    @Composable fun Menu(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.GridView, modifier, color, size)
    @Composable fun Sun(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.LightMode, modifier, color, size)
    @Composable fun Moon(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.DarkMode, modifier, color, size)
    @Composable fun Sparkles(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.AutoAwesome, modifier, color, size)
    @Composable fun CreditCard(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.CreditCard, modifier, color, size)
    @Composable fun ListTodo(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ReceiptLong, modifier, color, size)
    @Composable fun LayoutList(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ViewList, modifier, color, size)
    @Composable fun Calendar(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.CalendarMonth, modifier, color, size)
    @Composable fun Filter(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.FilterList, modifier, color, size)
    @Composable fun Backspace(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Backspace, modifier, color, size)
    @Composable fun Search(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Search, modifier, color, size)
    @Composable fun Edit(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Edit, modifier, color, size)
    @Composable fun Camera(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.PhotoCamera, modifier, color, size)
    @Composable fun Image(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Image, modifier, color, size)
    @Composable fun QrCode(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.QrCodeScanner, modifier, color, size)
    @Composable fun Info(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Info, modifier, color, size)
    @Composable fun Message(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.Message, modifier, color, size)
    @Composable fun OpenInNew(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.OpenInNew, modifier, color, size)
    @Composable fun Copy(modifier: Modifier = Modifier, color: Color = Color.Unspecified, size: Dp = Dp.Unspecified, strokeWidth: Dp = Dp.Unspecified) = Render(Icons.Rounded.ContentCopy, modifier, color, size)
}
