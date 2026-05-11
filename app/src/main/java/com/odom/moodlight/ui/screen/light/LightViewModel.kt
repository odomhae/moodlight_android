package com.odom.moodlight.ui.screen.light

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.data.model.VisualPattern
import kotlinx.coroutines.flow.combine
import com.odom.moodlight.data.repository.SettingsRepository
import com.odom.moodlight.ui.component.addToRecentColors
import com.odom.moodlight.ui.component.argbLongToColor
import com.odom.moodlight.ui.component.colorToArgbLong
import com.odom.moodlight.ui.component.encodeRecentColors
import com.odom.moodlight.ui.component.parseRecentColors
import com.odom.moodlight.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LightUiState(
    val lightColor: Color = AppColors.WarmYellow,
    val colorIndex: Int = 0,
    val isCustomColorSelected: Boolean = false,
    val selectedCustomColor: Color? = null,
    val recentCustomColors: List<Color> = emptyList(),
    val brightness: Float = 0.8f,
    val isCycleMode: Boolean = false,
    val visualPattern: VisualPattern = VisualPattern.NONE,
    val activeSound: SoundType? = null,
    val emoji: String = "🌙",
    val customIconPath: String? = null,
    val showClock: Boolean = true,
    val sleepMode: Boolean = false,
    val timerMinutes: Int = 0,
    val timerRemainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val soundButtonEmoji: String = "🔇",
    val isSoundActive: Boolean = false,
)

@HiltViewModel
class LightViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val soundPlayer: SoundPlayer,
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

    private var wasInBackground = false
    private var soundWasActiveBeforeBackground = false
    private var timerWasPausedByBackground = false

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> onAppBackground()
            Lifecycle.Event.ON_START -> if (wasInBackground) onAppForeground()
            else -> {}
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        viewModelScope.launch {
            combine(
                settingsRepository.colorIndex,
                settingsRepository.brightness,
                settingsRepository.selectedColorArgb,
                settingsRepository.recentColors
            ) { colorIdx, brightness, selectedArgb, recentRaw ->
                object {
                    val colorIdx = colorIdx
                    val brightness = brightness
                    val selectedArgb = selectedArgb
                    val recentRaw = recentRaw
                }
            }.collect { d ->
                val presetArgb = colorToArgbLong(AppColors.cycleColors[d.colorIdx])
                val isCustom = d.selectedArgb != presetArgb &&
                    AppColors.cycleColors.none { colorToArgbLong(it) == d.selectedArgb }
                val lightColor = argbLongToColor(d.selectedArgb)
                _state.update {
                    it.copy(
                        colorIndex = d.colorIdx,
                        lightColor = lightColor,
                        brightness = d.brightness,
                        isCustomColorSelected = isCustom,
                        selectedCustomColor = if (isCustom) lightColor else null,
                        recentCustomColors = parseRecentColors(d.recentRaw)
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
            settingsRepository.visualPattern.collect { id ->
                _state.update { it.copy(visualPattern = VisualPattern.fromId(id)) }
            }
        }
        viewModelScope.launch {
            val savedMinutes = settingsRepository.lastTimerMinutes.first()
            if (savedMinutes > 0) startTimer(savedMinutes)
        }
        viewModelScope.launch {
            combine(
                soundPlayer.activeWhiteNoise,
                soundPlayer.currentLullabyIndex,
                settingsRepository.savedSoundMode,
                settingsRepository.savedSoundName
            ) { activeWhiteNoise, lullabyIndex, savedMode, savedName ->
                val isActive = lullabyIndex != null || activeWhiteNoise != null
                val emoji = when {
                    lullabyIndex != null -> "🎵"
                    activeWhiteNoise != null -> activeWhiteNoise.emoji
                    savedMode == "LULLABY" -> "🎵"
                    savedMode == "WHITE_NOISE" -> SoundType.entries.find { it.name == savedName }?.emoji ?: "🔇"
                    else -> "🔇"
                }
                emoji to isActive
            }.collect { (emoji, isActive) ->
                _state.update { it.copy(soundButtonEmoji = emoji, isSoundActive = isActive) }
            }
        }
    }

    fun selectColor(index: Int) {
        cycleJob?.cancel()
        val color = AppColors.cycleColors[index]
        _state.update {
            it.copy(
                colorIndex = index,
                lightColor = color,
                isCycleMode = false,
                isCustomColorSelected = false,
                selectedCustomColor = null
            )
        }
        viewModelScope.launch {
            settingsRepository.setColorIndex(index)
            settingsRepository.setSelectedColorArgb(colorToArgbLong(color))
        }
    }

    fun selectCustomColor(color: Color) {
        cycleJob?.cancel()
        _state.update {
            it.copy(
                lightColor = color,
                isCycleMode = false,
                isCustomColorSelected = true,
                selectedCustomColor = color
            )
        }
        viewModelScope.launch {
            settingsRepository.setSelectedColorArgb(colorToArgbLong(color))
            val updated = addToRecentColors(_state.value.recentCustomColors, color)
            settingsRepository.setRecentColors(encodeRecentColors(updated))
        }
    }

    fun toggleCycleMode() {
        val entering = !_state.value.isCycleMode
        _state.update { it.copy(isCycleMode = entering, isCustomColorSelected = false) }
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

    fun setVisualPattern(pattern: VisualPattern) {
        _state.update { it.copy(visualPattern = pattern) }
        viewModelScope.launch { settingsRepository.setVisualPattern(pattern.id) }
    }

    fun nextEmoji() {
        currentEmojiIndex = (currentEmojiIndex + 1) % emojis.size
        _state.update { it.copy(emoji = emojis[currentEmojiIndex], customIconPath = null) }
        viewModelScope.launch {
            settingsRepository.setEmojiIndex(currentEmojiIndex)
            settingsRepository.setCustomIconPath("")
        }
    }

    fun cycleSoundMode() {
        val activeWhiteNoise = soundPlayer.activeWhiteNoise.value
        val activeLullabyIndex = soundPlayer.currentLullabyIndex.value
        viewModelScope.launch {
            when {
                activeLullabyIndex != null -> {
                    // 자장가 재생 중 → 소리 꺼짐
                    soundPlayer.stopAll()
                    settingsRepository.setSavedSound("NONE", "")
                }
                activeWhiteNoise != null -> {
                    // 백색소음 재생 중 → 자장가로 전환
                    soundPlayer.stopWhiteNoise()
                    val tracks = soundPlayer.lullabyTracks
                    if (tracks.isNotEmpty()) {
                        soundPlayer.playLullaby(tracks[0], tracks)
                        settingsRepository.setSavedSound("LULLABY", "")
                    }
                }
                else -> {
                    // 소리 꺼짐 → 백색소음 시작 (저장된 소리 사용, 없으면 RAIN)
                    val savedName = settingsRepository.savedSoundName.first()
                    val sound = SoundType.entries.find { it.name == savedName } ?: SoundType.RAIN
                    soundPlayer.playWhiteNoise(sound)
                    settingsRepository.setSavedSound("WHITE_NOISE", sound.name)
                }
            }
        }
    }

    fun toggleSound(sound: SoundType) {
        val current = _state.value.activeSound
        if (current == sound) {
            soundPlayer.stopWhiteNoise()
            _state.update { it.copy(activeSound = null) }
        } else {
            soundPlayer.playWhiteNoise(sound)
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
        viewModelScope.launch {
            val savedMode = settingsRepository.savedSoundMode.first()
            val savedName = settingsRepository.savedSoundName.first()
            when (savedMode) {
                "LULLABY" -> {
                    val tracks = soundPlayer.lullabyTracks
                    if (tracks.isNotEmpty()) soundPlayer.playLullaby(tracks[0], tracks)
                }
                "WHITE_NOISE" -> {
                    val sound = SoundType.entries.find { it.name == savedName } ?: SoundType.RAIN
                    soundPlayer.playWhiteNoise(sound)
                }
            }
        }
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
        soundPlayer.stopAll()
        _state.update { it.copy(isTimerRunning = false, timerRemainingSeconds = 0) }
    }

    private fun onAppBackground() {
        wasInBackground = true
        soundWasActiveBeforeBackground = soundPlayer.hasActiveSounds()
        timerWasPausedByBackground = _state.value.isTimerRunning
        timerJob?.cancel()
        soundPlayer.stopAll()
    }

    private fun onAppForeground() {
        wasInBackground = false
        if (timerWasPausedByBackground) {
            timerWasPausedByBackground = false
            val remaining = _state.value.timerRemainingSeconds
            if (remaining > 0) {
                timerJob = viewModelScope.launch {
                    var rem = remaining
                    while (rem > 0) {
                        delay(1000)
                        rem--
                        _state.update { it.copy(timerRemainingSeconds = rem) }
                    }
                    soundPlayer.stopAll()
                    _state.update { it.copy(isTimerRunning = false) }
                    _exitApp.tryEmit(Unit)
                }
            } else {
                _state.update { it.copy(isTimerRunning = false) }
            }
        }
        if (soundWasActiveBeforeBackground) {
            soundWasActiveBeforeBackground = false
            viewModelScope.launch {
                val savedMode = settingsRepository.savedSoundMode.first()
                val savedName = settingsRepository.savedSoundName.first()
                when (savedMode) {
                    "LULLABY" -> {
                        val tracks = soundPlayer.lullabyTracks
                        if (tracks.isNotEmpty()) soundPlayer.playLullaby(tracks[0], tracks)
                    }
                    "WHITE_NOISE" -> {
                        val sound = SoundType.entries.find { it.name == savedName } ?: SoundType.RAIN
                        soundPlayer.playWhiteNoise(sound)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        soundPlayer.stopAll()
    }
}
