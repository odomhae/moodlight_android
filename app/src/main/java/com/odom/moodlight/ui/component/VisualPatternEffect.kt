package com.odom.moodlight.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.odom.moodlight.data.model.VisualPattern
import kotlin.math.*
import kotlin.random.Random

@Composable
fun VisualPatternEffect(
    pattern: VisualPattern,
    color: Color,
    modifier: Modifier = Modifier
) {
    when (pattern) {
        VisualPattern.NONE -> Unit
        VisualPattern.STARLIGHT -> StarfieldEffect(modifier = modifier)
        VisualPattern.CANDLE_FLICKER -> CandleFlickerEffect(color = color, modifier = modifier)
        VisualPattern.WAVE -> WaveEffect(color = color, modifier = modifier)
    }
}

@Composable
private fun StarfieldEffect(modifier: Modifier = Modifier) {
    val stars = remember {
        List(60) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 1.5f + 0.8f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "time"
    )
    Canvas(modifier = modifier) {
        stars.forEach { (x, y, speed) ->
            val alpha = ((sin(time * speed) * 0.5f + 0.5f) * 0.85f + 0.1f).coerceIn(0f, 1f)
            val radius = (sin(time * speed * 0.7f) * 0.8f + 1.5f).coerceAtLeast(0.5f)
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(x * size.width, y * size.height),
                alpha = alpha
            )
        }
    }
}

@Composable
private fun CandleFlickerEffect(color: Color, modifier: Modifier = Modifier) {
    var targetAlpha by remember { mutableFloatStateOf(0.15f) }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = Random.nextInt(80, 250), easing = FastOutSlowInEasing),
        label = "flicker",
        finishedListener = {
            targetAlpha = Random.nextFloat() * 0.22f + 0.04f
        }
    )
    LaunchedEffect(Unit) {
        targetAlpha = Random.nextFloat() * 0.22f + 0.04f
    }
    Canvas(modifier = modifier) {
        drawRect(color = color, alpha = alpha)
    }
}

@Composable
private fun WaveEffect(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        drawWaves(color = color, phase = phase, width = size.width, height = size.height)
    }
}

private fun DrawScope.drawWaves(color: Color, phase: Float, width: Float, height: Float) {
    val waveCount = 3
    repeat(waveCount) { i ->
        val waveAlpha = 0.12f - i * 0.03f
        val amplitude = height * 0.06f - i * height * 0.01f
        val frequency = 1.5f + i * 0.5f
        val yBase = height * (0.5f + i * 0.15f)
        val phaseOffset = i * (PI / 3).toFloat()
        val path = Path()
        path.moveTo(0f, yBase)
        val steps = 60
        for (step in 0..steps) {
            val x = width * step / steps
            val y = yBase + amplitude * sin(phase + phaseOffset + frequency * x / width * 2 * PI).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        drawPath(path = path, color = color, alpha = waveAlpha)
    }
}
