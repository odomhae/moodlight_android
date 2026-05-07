package com.odom.moodlight.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.ui.theme.AppColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteSheet(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hue by remember { mutableFloatStateOf(30f) }
    var saturation by remember { mutableFloatStateOf(0.8f) }
    var lightness by remember { mutableFloatStateOf(0.7f) }

    val selectedColor = remember(hue, saturation, lightness) {
        hslToColor(hue, saturation, lightness)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Panel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "색상 선택",
                fontSize = 16.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedColor)
            )

            // Hue slider
            Text("색조", fontSize = 13.sp, color = AppColors.TextDim)
            GradientSlider(
                value = hue / 360f,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red, Color(0xFFFF7F00), Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color(0xFF8B00FF), Color.Red
                    )
                ),
                onValueChange = { hue = it * 360f }
            )

            // Saturation slider
            Text("채도", fontSize = 13.sp, color = AppColors.TextDim)
            GradientSlider(
                value = saturation,
                brush = Brush.horizontalGradient(
                    listOf(
                        hslToColor(hue, 0f, lightness),
                        hslToColor(hue, 1f, lightness)
                    )
                ),
                onValueChange = { saturation = it }
            )

            // Lightness slider
            Text("밝기", fontSize = 13.sp, color = AppColors.TextDim)
            GradientSlider(
                value = lightness,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        hslToColor(hue, saturation, 0.5f),
                        Color.White
                    )
                ),
                onValueChange = { lightness = it }
            )

            Button(
                onClick = { onColorSelected(selectedColor); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
            ) {
                Text(
                    "이 색상 선택",
                    color = if (lightness > 0.6f) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    brush: Brush,
    onValueChange: (Float) -> Unit
) {
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .onGloballyPositioned { trackSize = it.size }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (trackSize.width > 0)
                        onValueChange((offset.x / trackSize.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val thumbX = value * trackSize.width.toFloat()
        val thumbOffsetDp = with(density) { (thumbX - 14f).coerceAtLeast(0f).toDp() }
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetDp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .border(2.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                .align(Alignment.CenterStart)
        )
    }
}

fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - kotlin.math.abs(2 * lightness - 1f)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2 - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

fun colorToArgbLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL

fun argbLongToColor(argb: Long): Color = Color(argb.toInt())

fun parseRecentColors(raw: String): List<Color> {
    if (raw.isBlank()) return emptyList()
    return raw.split(",").mapNotNull { it.trim().toLongOrNull()?.let { l -> argbLongToColor(l) } }
}

fun encodeRecentColors(colors: List<Color>): String =
    colors.joinToString(",") { colorToArgbLong(it).toString() }

fun addToRecentColors(existing: List<Color>, newColor: Color, max: Int = 5): List<Color> {
    val filtered = existing.filter {
        kotlin.math.abs(colorToArgbLong(it).toLong() - colorToArgbLong(newColor).toLong()) > 100_000L
    }
    return (listOf(newColor) + filtered).take(max)
}
