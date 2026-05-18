package com.odom.moodlight.ui.screen.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.model.VisualPattern
import com.odom.moodlight.data.repository.SettingsRepository
import com.odom.moodlight.ui.component.addToRecentColors
import com.odom.moodlight.ui.component.argbLongToColor
import com.odom.moodlight.ui.component.colorToArgbLong
import com.odom.moodlight.ui.component.encodeRecentColors
import com.odom.moodlight.ui.component.parseRecentColors
import com.odom.moodlight.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val colorIndex: Int = 0,
    val isCycleMode: Boolean = false,
    val isCustomColorSelected: Boolean = false,
    val selectedCustomColor: Color? = null,
    val brightness: Float = 0.8f,
    val orientation: String = "portrait",
    val autoRestore: Boolean = true,
    val language: String = "ko",
    val emojiIndex: Int = 0,
    val customIconPath: String? = null,
    val visualPattern: VisualPattern = VisualPattern.CANDLE_FLICKER,
    val recentCustomColors: List<Color> = emptyList(),
    val appVersion: String = "1.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialAd: SharedFlow<Unit> = _showInterstitialAd.asSharedFlow()

    private val _isCycleMode = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.colorIndex,
        settingsRepository.brightness,
        settingsRepository.language,
        settingsRepository.emojiIndex,
        combine(
            settingsRepository.customIconPath,
            settingsRepository.visualPattern,
            settingsRepository.recentColors,
            settingsRepository.selectedColorArgb
        ) { customPath, pattern, recentRaw, selectedArgb ->
            listOf<Any?>(customPath, pattern, recentRaw, selectedArgb)
        }
    ) { colorIdx, brightness, lang, emojiIdx, inner ->
        val customPath = inner[0] as String?
        val pattern = inner[1] as String
        val recentRaw = inner[2] as String
        val selectedArgb = inner[3] as Long
        val presetArgb = colorToArgbLong(AppColors.cycleColors[colorIdx])
        val isCustom = selectedArgb != presetArgb && AppColors.cycleColors.none { colorToArgbLong(it) == selectedArgb }
        val selectedColor = argbLongToColor(selectedArgb)
        SettingsUiState(
            colorIndex = colorIdx,
            isCustomColorSelected = isCustom,
            selectedCustomColor = if (isCustom) selectedColor else null,
            brightness = brightness,
            language = lang,
            emojiIndex = emojiIdx,
            customIconPath = customPath,
            visualPattern = VisualPattern.fromId(pattern),
            recentCustomColors = parseRecentColors(recentRaw),
        )
    }.combine(_isCycleMode) { s, cycleMode ->
        s.copy(isCycleMode = cycleMode)
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

    fun selectColor(index: Int) {
        _isCycleMode.value = false
        val color = AppColors.cycleColors[index]
        viewModelScope.launch {
            settingsRepository.setColorIndex(index)
            settingsRepository.setSelectedColorArgb(colorToArgbLong(color))
        }
    }

    fun toggleCycleMode() {
        _isCycleMode.value = !_isCycleMode.value
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
