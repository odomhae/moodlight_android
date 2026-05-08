package com.odom.moodlight.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    companion object {
        val KEY_COLOR_INDEX = intPreferencesKey("color_index")
        val KEY_BRIGHTNESS = floatPreferencesKey("brightness")
        val KEY_ORIENTATION = stringPreferencesKey("orientation")
        val KEY_AUTO_RESTORE = booleanPreferencesKey("auto_restore")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_EMOJI_INDEX = intPreferencesKey("emoji_index")
        val KEY_CUSTOM_ICON_PATH = stringPreferencesKey("custom_icon_path")
        val KEY_ICON_CHANGE_COUNT = intPreferencesKey("icon_change_count")
        val KEY_LAST_TIMER_MINUTES = intPreferencesKey("last_timer_minutes")
        val KEY_VISUAL_PATTERN = stringPreferencesKey("visual_pattern")
        val KEY_SELECTED_COLOR_ARGB = longPreferencesKey("selected_color_argb")
        val KEY_RECENT_COLORS = stringPreferencesKey("recent_colors")
        val KEY_SAVED_SOUND_MODE = stringPreferencesKey("saved_sound_mode")  // "NONE" | "LULLABY" | "WHITE_NOISE"
        val KEY_SAVED_SOUND_NAME = stringPreferencesKey("saved_sound_name")  // WHITE_NOISE 선택 시 SoundType.name
    }

    val colorIndex: Flow<Int> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_COLOR_INDEX] ?: 0 }

    val brightness: Flow<Float> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_BRIGHTNESS] ?: 0.8f }

    val orientation: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ORIENTATION] ?: "portrait" }

    val autoRestore: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_AUTO_RESTORE] ?: true }

    val language: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LANGUAGE] ?: "ko" }

    val emojiIndex: Flow<Int> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_EMOJI_INDEX] ?: 0 }

    val customIconPath: Flow<String?> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_CUSTOM_ICON_PATH]?.takeIf { path -> path.isNotEmpty() } }

    suspend fun setColorIndex(v: Int) = store.edit { it[KEY_COLOR_INDEX] = v }
    suspend fun setBrightness(v: Float) = store.edit { it[KEY_BRIGHTNESS] = v }
    suspend fun setOrientation(v: String) = store.edit { it[KEY_ORIENTATION] = v }
    suspend fun setAutoRestore(v: Boolean) = store.edit { it[KEY_AUTO_RESTORE] = v }
    suspend fun setLanguage(v: String) = store.edit { it[KEY_LANGUAGE] = v }
    suspend fun setEmojiIndex(v: Int) = store.edit { it[KEY_EMOJI_INDEX] = v }
    suspend fun setCustomIconPath(v: String) = store.edit { it[KEY_CUSTOM_ICON_PATH] = v }
    suspend fun getIconChangeCount(): Int = store.data.first()[KEY_ICON_CHANGE_COUNT] ?: 0
    suspend fun setIconChangeCount(v: Int) = store.edit { it[KEY_ICON_CHANGE_COUNT] = v }

    val lastTimerMinutes: Flow<Int> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LAST_TIMER_MINUTES] ?: 0 }

    suspend fun setLastTimerMinutes(v: Int) = store.edit { it[KEY_LAST_TIMER_MINUTES] = v }

    val visualPattern: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_VISUAL_PATTERN] ?: "none" }

    val selectedColorArgb: Flow<Long> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SELECTED_COLOR_ARGB] ?: 0xFFFFD6A0L }

    val recentColors: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_RECENT_COLORS] ?: "" }

    suspend fun setVisualPattern(v: String) = store.edit { it[KEY_VISUAL_PATTERN] = v }
    suspend fun setSelectedColorArgb(v: Long) = store.edit { it[KEY_SELECTED_COLOR_ARGB] = v }
    suspend fun setRecentColors(v: String) = store.edit { it[KEY_RECENT_COLORS] = v }

    val savedSoundMode: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SAVED_SOUND_MODE] ?: "NONE" }

    val savedSoundName: Flow<String> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SAVED_SOUND_NAME] ?: "" }

    suspend fun setSavedSound(mode: String, name: String) = store.edit {
        it[KEY_SAVED_SOUND_MODE] = mode
        it[KEY_SAVED_SOUND_NAME] = name
    }
}
