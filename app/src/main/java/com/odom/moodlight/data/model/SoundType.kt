package com.odom.moodlight.data.model

import androidx.annotation.StringRes
import com.odom.moodlight.R

enum class SoundType(
    val emoji: String,
    @StringRes val labelResId: Int,
    val fileName: String
) {
    RAIN("🌧️", R.string.sound_rain, "rain.mp3"),
    WAVE("🌊", R.string.sound_wave, "wave.mp3"),
    FOREST("🌲", R.string.sound_forest, "birds.mp3"),
    SHHH("💋", R.string.sound_shhh, "Shhh.m4a"),
}
