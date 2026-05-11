package com.odom.moodlight.ui.screen.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.model.VisualPattern
import com.odom.moodlight.data.repository.SettingsRepository
import com.odom.moodlight.ui.component.addToRecentColors
import com.odom.moodlight.ui.component.colorToArgbLong
import com.odom.moodlight.ui.component.encodeRecentColors
import com.odom.moodlight.ui.component.parseRecentColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val colorIndex: Int = 0,
    val brightness: Float = 0.8f,
    val orientation: String = "portrait",
    val autoRestore: Boolean = true,
    val language: String = "ko",
    val emojiIndex: Int = 0,
    val customIconPath: String? = null,
    val visualPattern: VisualPattern = VisualPattern.NONE,
    val recentCustomColors: List<Color> = emptyList(),
    val appVersion: String = "1.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialAd: SharedFlow<Unit> = _showInterstitialAd.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.colorIndex,
        settingsRepository.brightness,
        settingsRepository.language,
        settingsRepository.emojiIndex,
        combine(
            settingsRepository.customIconPath,
            settingsRepository.visualPattern,
            settingsRepository.recentColors
        ) { customPath, pattern, recentRaw -> Triple(customPath, pattern, recentRaw) }
    ) { colorIdx, brightness, lang, emojiIdx, (customPath, pattern, recentRaw) ->
        SettingsUiState(
            colorIndex = colorIdx,
            brightness = brightness,
            language = lang,
            emojiIndex = emojiIdx,
            customIconPath = customPath,
            visualPattern = VisualPattern.fromId(pattern),
            recentCustomColors = parseRecentColors(recentRaw),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private fun onIconChanged() {
        viewModelScope.launch {
            val count = settingsRepository.incrementAndGetIconChangeCount()
            if (count % 3 == 0) {
                _showInterstitialAd.tryEmit(Unit)
            }
        }
    }

    fun selectEmojiPreset(index: Int) {
        onIconChanged()
        viewModelScope.launch {
            settingsRepository.setCustomIconPath("")
            settingsRepository.setEmojiIndex(index)
        }
    }

    fun setColorIndex(v: Int) = viewModelScope.launch { settingsRepository.setColorIndex(v) }
    fun setBrightness(v: Float) = viewModelScope.launch { settingsRepository.setBrightness(v) }
    fun setOrientation(v: String) = viewModelScope.launch { settingsRepository.setOrientation(v) }
    fun setAutoRestore(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoRestore(v) }
    fun setLanguage(v: String) = viewModelScope.launch { settingsRepository.setLanguage(v) }
    fun setEmojiIndex(v: Int) = viewModelScope.launch { settingsRepository.setEmojiIndex(v) }

    fun setCustomIconPath(path: String) {
        onIconChanged()
        viewModelScope.launch { settingsRepository.setCustomIconPath(path) }
    }

    fun setVisualPattern(pattern: VisualPattern) {
        viewModelScope.launch { settingsRepository.setVisualPattern(pattern.id) }
    }

    fun selectCustomColor(color: Color) {
        viewModelScope.launch {
            settingsRepository.setSelectedColorArgb(colorToArgbLong(color))
            val updated = addToRecentColors(state.value.recentCustomColors, color)
            settingsRepository.setRecentColors(encodeRecentColors(updated))
        }
    }

    fun clearCustomIcon() = viewModelScope.launch { settingsRepository.setCustomIconPath("") }
}
