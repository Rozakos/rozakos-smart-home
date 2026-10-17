package com.rozakos.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.ui.theme.BrandPalette

fun deviceAccent(type: DeviceTypeId): Color = when (type) {
    DeviceTypeId.CydStockTicker -> BrandPalette.Crimson
    DeviceTypeId.NucleoStockTicker -> BrandPalette.Navy
    DeviceTypeId.DotMatrixStockTicker -> BrandPalette.Navy
    DeviceTypeId.OledStockTicker -> BrandPalette.Teal
    DeviceTypeId.CydFollowerTracker -> BrandPalette.Crimson
}

fun deviceCaption(type: DeviceTypeId): String = when (type) {
    DeviceTypeId.CydStockTicker -> "Touchscreen ticker"
    DeviceTypeId.NucleoStockTicker -> "STM32 display"
    DeviceTypeId.DotMatrixStockTicker -> "LED matrix display"
    DeviceTypeId.OledStockTicker -> "Pocket-sized OLED"
    DeviceTypeId.CydFollowerTracker -> "Creator stats display"
}

/** Small, scalable hardware illustrations; the chart shapes are decorative icons. */
@Composable
fun DeviceArtwork(type: DeviceTypeId, modifier: Modifier = Modifier) {
    val accent = deviceAccent(type)
    Canvas(modifier.background(
        Brush.linearGradient(listOf(accent.copy(alpha = .15f), accent.copy(alpha = .04f))),
        RoundedCornerShape(22.dp),
    ).padding(5.dp)) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            drawOval(accent.copy(alpha = .12f), Offset(14f, 78f), Size(72f, 9f))
            when (type) {
                DeviceTypeId.DotMatrixStockTicker -> matrix()
                DeviceTypeId.OledStockTicker -> oled()
                DeviceTypeId.NucleoStockTicker -> nucleo(accent)
                DeviceTypeId.CydStockTicker -> touchDisplay(accent, followers = false)
                DeviceTypeId.CydFollowerTracker -> touchDisplay(accent, followers = true)
            }
        }
    }
}

private val Ink = BrandPalette.Navy
private val Screen = Color(0xFF1D1D2B)

private fun DrawScope.panel(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 4f) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))

private fun DrawScope.chart(color: Color, x: Float, y: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(x, y + h * .8f); lineTo(x + w * .23f, y + h * .48f)
        lineTo(x + w * .43f, y + h * .65f); lineTo(x + w * .7f, y + h * .2f)
        lineTo(x + w, y + h * .32f)
    }
    drawPath(path, color, style = Stroke(width = 2.8f, cap = StrokeCap.Round))
}

private fun DrawScope.oled() {
    panel(BrandPalette.SuccessInk, 21f, 18f, 58f, 60f, 7f)
    for (x in listOf(27f, 73f)) for (y in listOf(24f, 72f)) {
        drawCircle(Color(0xFF9AD2CB), 2.2f, Offset(x, y))
    }
    panel(Ink, 26f, 28f, 48f, 37f, 4f)
    panel(Screen, 30f, 32f, 40f, 29f, 2f)
    panel(Color(0xFF66E1E4), 34f, 36f, 13f, 3f, 1f)
    chart(Color(0xFF66E1E4), 34f, 42f, 31f, 14f)
    for (i in 0..3) panel(Color(0xFFE4BD72), 36f + i * 8f, 77f, 4f, 9f, 1f)
}

private fun DrawScope.touchDisplay(accent: Color, followers: Boolean) {
    panel(accent, 10f, 22f, 80f, 54f, 8f)
    panel(Ink, 15f, 27f, 69f, 42f, 5f)
    panel(Screen, 20f, 32f, 59f, 32f, 3f)
    drawCircle(Color.White.copy(alpha = .7f), 1.7f, Offset(87f, 49f))
    if (followers) {
        drawCircle(Color(0xFFFFBDCF), 5f, Offset(33f, 44f))
        panel(Color(0xFFFFBDCF), 25f, 51f, 16f, 7f, 4f)
        for (i in 0..2) panel(Color.White, 47f + i * 9f, 39f, 6f, 13f, 2f)
        panel(Color(0xFF86E8C6), 47f, 56f, 22f, 2f, 1f)
    } else {
        panel(Color.White.copy(alpha = .9f), 25f, 37f, 17f, 3f, 1f)
        chart(Color(0xFF7DE5CC), 25f, 42f, 47f, 17f)
    }
    panel(Ink, 41f, 76f, 18f, 4f, 1f)
    panel(accent.copy(alpha = .7f), 31f, 80f, 38f, 3f, 1f)
}

private fun DrawScope.matrix() {
    panel(Ink, 6f, 30f, 88f, 41f, 6f)
    panel(Screen, 10f, 34f, 80f, 33f, 3f)
    for (row in 0..4) for (col in 0..15) {
        val lit = (row == 1 && col % 5 != 4) || (row == 3 && col % 4 != 3) ||
            (row == 2 && col % 5 == 0)
        drawCircle(if (lit) Color(0xFFFFBC58) else Color(0xFF344640),
            1.65f, Offset(15f + col * 4.6f, 41f + row * 4.8f))
    }
    panel(Color(0xFF435F63), 16f, 71f, 9f, 5f, 1f)
    panel(Color(0xFF435F63), 75f, 71f, 9f, 5f, 1f)
}

private fun DrawScope.nucleo(accent: Color) {
    panel(Color(0xFF438D76), 13f, 14f, 74f, 70f, 5f)
    for (i in 0..7) {
        panel(Color(0xFFE7C98E), 16f + i * 9f, 17f, 3f, 5f, .5f)
        panel(Color(0xFFE7C98E), 16f + i * 9f, 76f, 3f, 5f, .5f)
    }
    panel(accent, 21f, 27f, 58f, 43f, 5f)
    panel(Screen, 25f, 31f, 50f, 35f, 3f)
    panel(Color.White, 30f, 36f, 15f, 3f, 1f)
    chart(Color(0xFFC1B6FF), 30f, 42f, 39f, 17f)
    drawCircle(Color(0xFFACF1BD), 2f, Offset(81f, 70f))
}

@Composable
fun DeviceStatusBadge(status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.Online -> BrandPalette.SuccessInk
        ConnectionStatus.Offline -> BrandPalette.Crimson
        ConnectionStatus.Unknown -> BrandPalette.Muted
    }
    val accent = when (status) {
        ConnectionStatus.Online -> BrandPalette.Teal
        ConnectionStatus.Offline -> BrandPalette.Alert
        ConnectionStatus.Unknown -> BrandPalette.Muted
    }
    Row(
        Modifier.background(accent.copy(alpha = .1f), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).background(accent, CircleShape))
        Text(when (status) {
            ConnectionStatus.Online -> "Online"
            ConnectionStatus.Offline -> "Offline"
            ConnectionStatus.Unknown -> "Checking"
        }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}
