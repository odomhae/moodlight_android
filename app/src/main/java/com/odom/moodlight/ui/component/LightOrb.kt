package com.odom.moodlight.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LightOrb(
    color: Color,
    emoji: String,
    size: Dp = 200.dp,
    onEmojiTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(breatheScale)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = this.size.minDimension / 2

            // 다층 글로우 효과
            for (i in 5 downTo 1) {
                drawCircle(
                    color = color.copy(alpha = 0.06f * i),
                    radius = baseRadius * (1f + (5 - i) * 0.18f),
                    center = center
                )
            }
            // 메인 원
            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = baseRadius * 0.88f,
                center = center
            )
            // 하이라이트
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = baseRadius * 0.4f,
                center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.2f)
            )
        }

        Text(
            text = emoji,
            fontSize = (size.value * 0.3f).sp,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures { onEmojiTap() }
                }
        )
    }
}
