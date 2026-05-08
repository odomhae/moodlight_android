package com.odom.moodlight.data.model

import androidx.annotation.StringRes
import com.odom.moodlight.R

enum class SoundType(
    val emoji: String,
    @StringRes val labelResId: Int,
    val isPro: Boolean,
    val resourceName: String
) {
    RAIN("🌧️", R.string.sound_rain, false, "rain"),
    WAVE("🌊", R.string.sound_wave, false, "wave"),
    FOREST("🌲", R.string.sound_forest, false, "forest"),
    FIRE("🔥", R.string.sound_fire, false, "fire"),
    PIANO("🎹", R.string.sound_piano, false, "piano"),
    WIND("🌬️", R.string.sound_wind, false, "wind"),
}
