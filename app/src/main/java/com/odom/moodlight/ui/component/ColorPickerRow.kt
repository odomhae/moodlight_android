package com.odom.moodlight.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun ColorPickerRow(
    selectedIndex: Int,
    isCycleMode: Boolean,
    onColorSelect: (Int) -> Unit,
    onCycleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppColors.cycleColors.forEachIndexed { index, color ->
            ColorDot(
                color = color,
                isSelected = !isCycleMode && selectedIndex == index,
                onClick = { onColorSelect(index) }
            )
        }
        RainbowDot(
            isSelected = isCycleMode,
            onClick = onCycleSelect
        )
    }
}

@Composable
private fun ColorDot(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RainbowDot(isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        AppColors.WarmYellow,
                        AppColors.MintGreen,
                        AppColors.SkyBlue,
                        AppColors.Lavender,
                        AppColors.SoftPink,
                        AppColors.WarmYellow,
                    )
                )
            )
            .then(
                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🌈", fontSize = 18.sp)
    }
}
