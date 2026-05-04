package com.odom.moodlight.ui.screen.light

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.data.repository.BillingRepository
import com.odom.moodlight.data.repository.SettingsRepository
import com.odom.moodlight.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import javax.inject.Inject

data class LightUiState(
    val lightColor: Color = AppColors.WarmYellow,
    val colorIndex: Int = 0,
    val brightness: Float = 0.8f,
    val isCycleMode: Boolean = false,
    val activeSound: SoundType? = null,
    val emoji: String = "🌙",
    val customIconPath: String? = null,
    val showClock: Boolean = true,
    val sleepMode: Boolean = false,
    val timerMinutes: Int = 0,
    val timerRemainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isPro: Boolean = false,
    val showPaywall: Boolean = false,
)

@HiltViewModel
class LightViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val billingRepository: BillingRepository,
    private val soundPlayer: SoundPlayer
) : ViewModel() {

    private val _state = MutableStateFlow(LightUiState())
    val state: StateFlow<LightUiState> = _state.asStateFlow()

    private val _exitApp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exitApp: SharedFlow<Unit> = _exitApp.asSharedFlow()

    private val emojis = listOf("🌙", "👶", "🌟", "🐑", "🦋")
    private var currentEmojiIndex = 0
    private var cycleIndex = 0
    private var cycleJob: Job? = null
    private var timerJob: Job? = null
    private var sleepJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.colorIndex,
                settingsRepository.brightness,
                billingRepository.isPro
            ) { colorIdx, brightness, isPro ->
                Triple(colorIdx, brightness, isPro)
            }.collect { (colorIdx, brightness, isPro) ->
                _state.update {
                    it.copy(
                        colorIndex = colorIdx,
                        lightColor = AppColors.cycleColors[colorIdx],
                        brightness = brightness,
                        isPro = isPro
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.emojiIndex.collect { idx ->
                currentEmojiIndex = idx
                _state.update { it.copy(emoji = emojis[idx]) }
            }
        }
        viewModelScope.launch {
            settingsRepository.customIconPath.collect { path ->
                _state.update { it.copy(customIconPath = path) }
            }
        }
        viewModelScope.launch {
            val saved = settingsRepository.lastTimerMinutes.first()
            if (saved > 0) startTimer(saved)
        }
    }

    fun selectColor(index: Int) {
        cycleJob?.cancel()
        _state.update { it.copy(colorIndex = index, lightColor = AppColors.cycleColors[index], isCycleMode = false) }
        viewModelScope.launch { settingsRepository.setColorIndex(index) }
    }

    fun toggleCycleMode() {
        val entering = !_state.value.isCycleMode
        _state.update { it.copy(isCycleMode = entering) }
        if (entering) {
            cycleJob = viewModelScope.launch {
                while (true) {
                    delay(2000)
                    cycleIndex = (cycleIndex + 1) % AppColors.cycleColors.size
                    _state.update { it.copy(lightColor = AppColors.cycleColors[cycleIndex]) }
                }
            }
        } else {
            cycleJob?.cancel()
        }
    }

    fun setBrightness(value: Float) {
        _state.update { it.copy(brightness = value) }
        viewModelScope.launch { settingsRepository.setBrightness(value) }
    }

    fun nextEmoji() {
        currentEmojiIndex = (currentEmojiIndex + 1) % emojis.size
        _state.update { it.copy(emoji = emojis[currentEmojiIndex], customIconPath = null) }
        viewModelScope.launch {
            settingsRepository.setEmojiIndex(currentEmojiIndex)
            settingsRepository.setCustomIconPath("")
        }
    }

    fun toggleSound(sound: SoundType) {
        if (sound.isPro && !_state.value.isPro) {
            _state.update { it.copy(showPaywall = true) }
            return
        }
        val current = _state.value.activeSound
        if (current == sound) {
            soundPlayer.stop(sound)
            _state.update { it.copy(activeSound = null) }
        } else {
            current?.let { soundPlayer.stop(it) }
            soundPlayer.play(sound)
            _state.update { it.copy(activeSound = sound) }
        }
    }

    fun toggleClock() = _state.update { it.copy(showClock = !it.showClock) }

    fun toggleSleepMode() {
        val entering = !_state.value.sleepMode
        _state.update { it.copy(sleepMode = entering) }
        if (entering) {
            sleepJob = viewModelScope.launch {
                delay(5 * 60 * 1000L)
                _state.update { it.copy(brightness = 0.05f, sleepMode = false) }
            }
        } else {
            sleepJob?.cancel()
        }
    }

    fun startTimer(minutes: Int) {
        timerJob?.cancel()
        _state.update { it.copy(timerMinutes = minutes, timerRemainingSeconds = minutes * 60, isTimerRunning = true) }
        viewModelScope.launch { settingsRepository.setLastTimerMinutes(minutes) }
        timerJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.update { it.copy(timerRemainingSeconds = remaining) }
            }
            soundPlayer.stopAll()
            _state.update { it.copy(isTimerRunning = false) }
            _exitApp.tryEmit(Unit)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isTimerRunning = false, timerRemainingSeconds = 0) }
    }

    fun dismissPaywall() = _state.update { it.copy(showPaywall = false) }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stopAll()
    }
}
