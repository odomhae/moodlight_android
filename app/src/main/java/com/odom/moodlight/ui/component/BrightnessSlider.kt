package com.odom.moodlight.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun BrightnessSlider(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🌑", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.05f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.TextPrimary,
                activeTrackColor = AppColors.TextPrimary.copy(alpha = 0.8f),
                inactiveTrackColor = AppColors.TextDim,
            )
        )
        Text(text = "☀️", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
