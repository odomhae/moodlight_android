package com.odom.moodlight.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformAnimation(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val delays = listOf(0, 150, 300)
    val heights = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 6f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$delay"
        )
    }

    Row(
        modifier = modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightAnim ->
            val h by heightAnim
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}
