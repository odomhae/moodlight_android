package com.odom.moodlight.data.model

enum class SoundType(
    val emoji: String,
    val label: String,
    val isPro: Boolean,
    val resourceName: String
) {
    RAIN("🌧️", "빗소리", false, "rain"),
    WAVE("🌊", "파도 소리", false, "wave"),
    FOREST("🌲", "숲 소리", false, "forest"),
    FIRE("🔥", "모닥불", true, "fire"),
    LULLABY("🎵", "자장가", true, "lullaby"),
    PIANO("🎹", "피아노", true, "piano"),
    WIND("🌬️", "바람 소리", true, "wind"),
}
