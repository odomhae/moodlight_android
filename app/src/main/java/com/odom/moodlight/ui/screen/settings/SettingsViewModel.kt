package com.odom.moodlight.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.repository.BillingRepository
import com.odom.moodlight.data.repository.SettingsRepository
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
    val isPro: Boolean = false,
    val showPaywall: Boolean = false,
    val appVersion: String = "1.0",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _showPaywall = MutableStateFlow(false)
    private var iconChangeCount = 0

    private val _showInterstitialAd = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialAd: SharedFlow<Unit> = _showInterstitialAd.asSharedFlow()

    private data class ExtraState(
        val language: String,
        val emojiIndex: Int,
        val customIconPath: String?,
        val isPro: Boolean,
        val showPaywall: Boolean
    )

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.colorIndex,
        settingsRepository.brightness,
        settingsRepository.orientation,
        settingsRepository.autoRestore,
        combine(
            settingsRepository.language,
            settingsRepository.emojiIndex,
            settingsRepository.customIconPath,
            billingRepository.isPro,
            _showPaywall
        ) { lang, emojiIdx, customPath, isPro, paywall ->
            ExtraState(lang, emojiIdx, customPath, isPro, paywall)
        }
    ) { colorIdx, brightness, orientation, autoRestore, extra ->
        SettingsUiState(
            colorIndex = colorIdx,
            brightness = brightness,
            orientation = orientation,
            autoRestore = autoRestore,
            language = extra.language,
            emojiIndex = extra.emojiIndex,
            customIconPath = extra.customIconPath,
            isPro = extra.isPro,
            showPaywall = extra.showPaywall
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private fun onIconChanged() {
        iconChangeCount++
        if (iconChangeCount % 3 == 0) {
            _showInterstitialAd.tryEmit(Unit)
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

    fun clearCustomIcon() = viewModelScope.launch { settingsRepository.setCustomIconPath("") }
    fun showPaywall() = _showPaywall.update { true }
    fun dismissPaywall() = _showPaywall.update { false }
}
