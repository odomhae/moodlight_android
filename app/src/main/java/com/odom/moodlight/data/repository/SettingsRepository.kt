package com.odom.moodlight.data.repository

import com.odom.moodlight.data.datastore.AppPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val prefs: AppPreferences
) {
    val colorIndex: Flow<Int> = prefs.colorIndex
    val brightness: Flow<Float> = prefs.brightness
    val orientation: Flow<String> = prefs.orientation
    val autoRestore: Flow<Boolean> = prefs.autoRestore
    val language: Flow<String> = prefs.language

    suspend fun setColorIndex(v: Int) = prefs.setColorIndex(v)
    suspend fun setBrightness(v: Float) = prefs.setBrightness(v)
    suspend fun setOrientation(v: String) = prefs.setOrientation(v)
    suspend fun setAutoRestore(v: Boolean) = prefs.setAutoRestore(v)
    suspend fun setLanguage(v: String) = prefs.setLanguage(v)
}
