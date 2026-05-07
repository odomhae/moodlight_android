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
        VisualPattern.WAVE -> WaveEffect(modifier = modifier)
        VisualPattern.SNOWFALL -> SnowfallEffect(modifier = modifier)
    }
}

@Composable
private fun StarfieldEffect(modifier: Modifier = Modifier) {
    val stars = remember {
        List(60) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 1.5f + 0.8f   // speed
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
        // 중앙 Orb 영역 반지름 — 화면 짧은 쪽의 30%
        val exclusionRadius = minOf(size.width, size.height) * 0.30f
        val cx = size.width / 2f
        val cy = size.height / 2f

        stars.forEach { (x, y, speed) ->
            val starX = x * size.width
            val starY = y * size.height
            val dx = starX - cx
            val dy = starY - cy
            // 중앙 원 안쪽은 그리지 않음
            if (dx * dx + dy * dy < exclusionRadius * exclusionRadius) return@forEach

            val alpha = ((sin(time * speed) * 0.5f + 0.5f) * 0.85f + 0.1f).coerceIn(0f, 1f)
            val radius = (sin(time * speed * 0.7f) * 4.0f + 2.5f).coerceAtLeast(1.2f)
            
            // 별 모양 그리기 (사방으로 뻗친 반짝이는 모양)
            val starPath = Path().apply {
                moveTo(starX, starY - radius * 2.5f) // 위로 뾰족
                quadraticTo(starX, starY, starX + radius * 2.5f, starY) // 오른쪽으로 뾰족
                quadraticTo(starX, starY, starX, starY + radius * 2.5f) // 아래로 뾰족
                quadraticTo(starX, starY, starX - radius * 2.5f, starY) // 왼쪽으로 뾰족
                quadraticTo(starX, starY, starX, starY - radius * 2.5f) // 다시 위로
                close()
            }

            drawPath(
                path = starPath,
                color = Color.White,
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

// WAVE는 선택 색상을 배경으로 쓰고, 그 위에 검정 파도 오버레이를 그림
@Composable
private fun WaveEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        drawWaves(phase = phase, width = size.width, height = size.height)
    }
}

private fun DrawScope.drawWaves(phase: Float, width: Float, height: Float) {
    val waveCount = 4
    repeat(waveCount) { i ->
        val waveAlpha = 0.22f - i * 0.05f   // 0.22, 0.17, 0.12, 0.07
        val amplitude = height * 0.09f + i * height * 0.02f
        val frequency = 1.2f + i * 0.35f
        val yBase = height * (0.25f + i * 0.18f)
        val phaseOffset = i * (PI / 2.5f).toFloat()
        val path = Path()
        val steps = 80
        for (step in 0..steps) {
            val x = width * step / steps
            val y = yBase + amplitude * sin(phase + phaseOffset + frequency * x / width * 2 * PI).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        drawPath(path = path, color = Color.Black, alpha = waveAlpha)
    }
}

@Composable
private fun SnowfallEffect(modifier: Modifier = Modifier) {
    val snowflakes = remember {
        List(70) {
            Snowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6f + 5f,
                speed = Random.nextFloat() * 0.8f + 0.4f,
                drift = Random.nextFloat() * 0.5f - 0.25f // 좌우 흔들림
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "snowfall")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val exclusionRadius = minOf(size.width, size.height) * 0.30f

        snowflakes.forEach { flake ->
            // 시간에 따른 위치 계산 (위에서 아래로) - 기존 20f에서 5f로 낮추어 속도를 절반으로 조절
            val currentY = (flake.y * size.height + time * flake.speed * 2f) % size.height
            // 좌우 흔들림 (Sin 함수 사용) - 흔들림 속도를 줄이기 위해 time에 0.5f를 곱함
            val currentX = (flake.x * size.width + sin(time * 0.2f + flake.y * 10f) * 3f) % size.width
            
            val dx = currentX - cx
            val dy = currentY - cy

            // 중앙 Orb 영역 제외
            if (dx * dx + dy * dy < exclusionRadius * exclusionRadius) return@forEach

            drawCircle(
                color = Color.White,
                radius = flake.size,
                center = Offset(currentX, currentY),
                alpha = 0.7f
            )
        }
    }
}

private data class Snowflake(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val drift: Float
)
