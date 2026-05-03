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

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.colorIndex,
        settingsRepository.brightness,
        settingsRepository.orientation,
        settingsRepository.autoRestore,
        combine(settingsRepository.language, billingRepository.isPro, _showPaywall) { lang, isPro, paywall ->
            Triple(lang, isPro, paywall)
        }
    ) { colorIdx, brightness, orientation, autoRestore, (language, isPro, showPaywall) ->
        SettingsUiState(
            colorIndex = colorIdx,
            brightness = brightness,
            orientation = orientation,
            autoRestore = autoRestore,
            language = language,
            isPro = isPro,
            showPaywall = showPaywall
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setColorIndex(v: Int) = viewModelScope.launch { settingsRepository.setColorIndex(v) }
    fun setBrightness(v: Float) = viewModelScope.launch { settingsRepository.setBrightness(v) }
    fun setOrientation(v: String) = viewModelScope.launch { settingsRepository.setOrientation(v) }
    fun setAutoRestore(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoRestore(v) }
    fun setLanguage(v: String) = viewModelScope.launch { settingsRepository.setLanguage(v) }
    fun showPaywall() = _showPaywall.update { true }
    fun dismissPaywall() = _showPaywall.update { false }
}
