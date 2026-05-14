package com.odom.moodlight.data.model

enum class VisualPattern(val id: String) {
    NONE("none"),
    STARLIGHT("starlight"),
    CANDLE_FLICKER("candle_flicker"),
    WAVE("wave"),
    SNOWFALL("snowfall"),
    HEARTBEAT("heartbeat"),
    BUBBLE_FLOAT("bubble_float");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: NONE
    }
}
