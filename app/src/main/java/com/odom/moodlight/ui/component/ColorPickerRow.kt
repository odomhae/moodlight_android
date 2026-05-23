package com.odom.moodlight.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.odom.moodlight.R
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
    recentCustomColors: List<Color> = emptyList(),
    isCustomColorSelected: Boolean = false,
    selectedCustomColor: Color? = null,
    onColorSelect: (Int) -> Unit,
    onCycleSelect: () -> Unit,
    onCustomColorSelect: (Color) -> Unit = {},
    onOpenPalette: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Preset colors + rainbow + add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppColors.cycleColors.forEachIndexed { index, color ->
                ColorDot(
                    color = color,
                    isSelected = !isCycleMode && !isCustomColorSelected && selectedIndex == index,
                    onClick = { onColorSelect(index) }
                )
            }
            RainbowDot(isSelected = isCycleMode, onClick = onCycleSelect)
            AddColorDot(onClick = onOpenPalette)
        }

        // Recent custom colors row (only shown if there are any)
        if (recentCustomColors.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                recentCustomColors.forEach { color ->
                    ColorDot(
                        color = color,
                        isSelected = isCustomColorSelected && selectedCustomColor?.let {
                            colorToArgbLong(it) == colorToArgbLong(color)
                        } == true,
                        onClick = { onCustomColorSelect(color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
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
                        AppColors.WarmYellow, AppColors.MintGreen, AppColors.SkyBlue,
                        AppColors.Lavender, AppColors.SoftPink, AppColors.WarmYellow,
                    )
                )
            )
            .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        } else {
            Text(text = "🌈", fontSize = 18.sp)
        }
    }
}

@Composable
private fun AddColorDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppColors.Panel)
            .border(1.5.dp, AppColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(R.string.color_add_description),
            tint = AppColors.TextDim,
            modifier = Modifier.size(20.dp)
        )
    }
}
