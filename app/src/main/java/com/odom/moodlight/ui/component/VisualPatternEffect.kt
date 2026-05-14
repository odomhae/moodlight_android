package com.odom.moodlight.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.odom.moodlight.data.model.VisualPattern
import kotlinx.coroutines.delay
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
        VisualPattern.HEARTBEAT -> HeartbeatEffect(color = color, modifier = modifier)
        VisualPattern.BUBBLE_FLOAT -> BubbleFloatEffect(color = color, modifier = modifier)
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

@Composable
private fun HeartbeatEffect(color: Color, modifier: Modifier = Modifier) {
    val rippleCount = 5
    val ripples = remember { List(rippleCount) { Animatable(0f) } }

    ripples.forEachIndexed { index, ripple ->
        LaunchedEffect(index) {
            delay(index * 1000L)
            while (true) {
                ripple.snapTo(0f)
                ripple.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // 하트의 너비 ≈ 4.4r, 높이 ≈ 1.8r 이므로 화면을 채우는 최대 r 계산
        val maxR = maxOf(size.width / 4.4f, size.height / 1.8f) * 1.15f

        ripples.forEach { ripple ->
            val progress = ripple.value
            val r = progress * maxR
            val alpha = when {
                progress < 0.15f -> (progress / 0.15f) * 0.5f
                else -> ((1f - progress) / 0.85f) * 0.5f
            }.coerceIn(0f, 0.5f)
            val strokeWidth = (1f - progress) * 3f + 0.5f

            drawPath(
                path = heartPath(cx, cy, r),
                color = color.copy(alpha = alpha),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

private fun heartPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    // 아래 꼭짓점에서 시작
    path.moveTo(cx, cy + r)
    // 왼쪽 곡선: 아래 꼭짓점 → 위 중앙 홈
    path.cubicTo(
        cx - r * 2.2f, cy + r * 0.5f,
        cx - r * 2.2f, cy - r * 0.6f,
        cx,            cy - r * 0.3f
    )
    // 오른쪽 곡선: 위 중앙 홈 → 아래 꼭짓점
    path.cubicTo(
        cx + r * 2.2f, cy - r * 0.6f,
        cx + r * 2.2f, cy + r * 0.5f,
        cx,            cy + r
    )
    path.close()
    return path
}

private data class Bubble(
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val speed: Float,
    val wobbleAmp: Float,
    val wobbleFreq: Float,
    val wobblePhase: Float
)

@Composable
private fun BubbleFloatEffect(color: Color, modifier: Modifier = Modifier) {
    val bubbles = remember {
        List(15) {
            Bubble(
                baseX = Random.nextFloat(),
                baseY = Random.nextFloat(),
                radius = Random.nextFloat() * 30f + 18f,
                speed = Random.nextFloat() * 0.008f + 0.004f,
                wobbleAmp = Random.nextFloat() * 18f + 12f,
                wobbleFreq = Random.nextFloat() * 0.5f + 0.4f,
                wobblePhase = Random.nextFloat() * (2 * PI).toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)),
        label = "bubbleTime"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val exclusionRadius = minOf(size.width, size.height) * 0.30f

        bubbles.forEach { bubble ->
            val currentY = (1f - ((bubble.baseY + time * bubble.speed) % 1f)) * size.height
            val wobble = sin(time * bubble.wobbleFreq + bubble.wobblePhase) * bubble.wobbleAmp +
                cos(time * bubble.wobbleFreq * 0.61f + bubble.wobblePhase * 1.3f) * bubble.wobbleAmp * 0.3f
            val currentX = bubble.baseX * size.width + wobble

            val dx = currentX - cx
            val dy = currentY - cy
            if (dx * dx + dy * dy < exclusionRadius * exclusionRadius) return@forEach

            val r = bubble.radius
            val center = Offset(currentX, currentY)

            val brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.04f),
                    color.copy(alpha = 0.08f),
                    color.copy(alpha = 0.22f),
                    color.copy(alpha = 0.05f)
                ),
                center = center,
                radius = r
            )
            drawCircle(brush = brush, radius = r, center = center)

            drawCircle(
                color = color.copy(alpha = 0.35f),
                radius = r,
                center = center,
                style = Stroke(width = 1.2f)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = r * 0.22f,
                center = Offset(currentX - r * 0.28f, currentY - r * 0.32f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = r * 0.10f,
                center = Offset(currentX - r * 0.14f, currentY - r * 0.18f)
            )
        }
    }
}
