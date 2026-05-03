package com.odom.moodlight.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
}
