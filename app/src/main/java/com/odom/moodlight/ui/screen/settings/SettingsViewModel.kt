package com.odom.moodlight.ui.screen.settings

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.RewardedAdManager
import com.odom.moodlight.data.model.VisualPattern
import com.odom.moodlight.data.repository.BillingRepository
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
    val isPro: Boolean = false,
    val showPaywall: Boolean = false,
    val appVersion: String = "1.0",
    val isAdReady: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
) : ViewModel() {

    private val _showPaywall = MutableStateFlow(false)

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialAd: SharedFlow<Unit> = _showInterstitialAd.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.colorIndex,
        settingsRepository.brightness,
        settingsRepository.language,
        settingsRepository.emojiIndex,
        combine(
            settingsRepository.customIconPath,
            billingRepository.isPro,
            _showPaywall,
            settingsRepository.visualPattern,
            combine(
                settingsRepository.recentColors,
                rewardedAdManager.isAdReady
            ) { recentRaw, adReady -> Pair(recentRaw, adReady) }
        ) { customPath, isPro, paywall, pattern, (recentRaw, adReady) ->
            object {
                val customPath = customPath
                val isPro = isPro
                val paywall = paywall
                val pattern = pattern
                val recentRaw = recentRaw
                val adReady = adReady
            }
        }
    ) { colorIdx, brightness, lang, emojiIdx, extra ->
        SettingsUiState(
            colorIndex = colorIdx,
            brightness = brightness,
            language = lang,
            emojiIndex = emojiIdx,
            customIconPath = extra.customPath,
            isPro = extra.isPro,
            showPaywall = extra.paywall,
            visualPattern = VisualPattern.fromId(extra.pattern),
            recentCustomColors = parseRecentColors(extra.recentRaw),
            isAdReady = extra.adReady
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
    fun showPaywall() = _showPaywall.update { true }
    fun dismissPaywall() = _showPaywall.update { false }

    fun watchAd(activity: Activity) {
        rewardedAdManager.show(activity) {
            billingRepository.setProStatus(true)
            _showPaywall.update { false }
        }
    }
}
