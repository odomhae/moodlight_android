package com.odom.moodlight.data.model

import androidx.annotation.StringRes
import com.odom.moodlight.R

enum class SoundType(
    val emoji: String,
    @StringRes val labelResId: Int,
    val isPro: Boolean,
    val fileName: String   // assets/whiteSound/ 안의 실제 파일명 (확장자 포함)
) {
    RAIN("🌧️", R.string.sound_rain, false, "rain.mp3"),
    WAVE("🌊", R.string.sound_wave, false, "wave.mp3"),
    FOREST("🌲", R.string.sound_forest, false, "forest.mp3"),
    FIRE("🔥", R.string.sound_fire, false, "fire.mp3"),
    PIANO("🎹", R.string.sound_piano, false, "piano.mp3"),
    WIND("🌬️", R.string.sound_wind, false, "wind.mp3"),
}
