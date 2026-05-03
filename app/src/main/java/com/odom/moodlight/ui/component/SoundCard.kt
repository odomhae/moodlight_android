package com.odom.moodlight.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundCard(
    sound: SoundType,
    isActive: Boolean,
    isPro: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive)
                    AppColors.TextPrimary.copy(alpha = 0.15f)
                else
                    AppColors.Panel
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = sound.emoji, fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = sound.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                if (isActive) {
                    WaveformAnimation(
                        color = AppColors.WarmYellow,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "정지" else "재생",
                        tint = AppColors.TextPrimary
                    )
                }
            }
        }
        if (sound.isPro && !isPro) {
            ProBadgeOverlay(modifier = Modifier.matchParentSize())
        }
    }
}
