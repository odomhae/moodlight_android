package com.odom.moodlight.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundChip(
    sound: SoundType,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) AppColors.TextPrimary.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (isActive) AppColors.TextPrimary.copy(alpha = 0.6f) else AppColors.Border

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = sound.emoji, fontSize = 20.sp)
        Text(
            text = stringResource(sound.labelResId),
            fontSize = 11.sp,
            color = if (isActive) AppColors.TextPrimary else AppColors.TextDim
        )
    }
}
