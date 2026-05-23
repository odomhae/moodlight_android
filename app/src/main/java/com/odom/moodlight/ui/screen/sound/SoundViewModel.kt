package com.odom.moodlight.ui.screen.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SoundTab { LULLABY, WHITE_NOISE }

data class SoundUiState(
    val selectedTab: SoundTab = SoundTab.LULLABY,
    val currentTrackIndex: Int? = null,
    val activeWhiteNoise: SoundType? = null,
)

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val tracks: List<LullabyTrack> = soundPlayer.lullabyTracks

    private val _selectedTab = MutableStateFlow(SoundTab.LULLABY)

    val state: StateFlow<SoundUiState> = combine(
        _selectedTab,
        soundPlayer.currentLullabyIndex,
        soundPlayer.activeWhiteNoise
    ) { tab, lullabyIndex, whiteNoise ->
        SoundUiState(
            selectedTab = tab,
            currentTrackIndex = lullabyIndex,
            activeWhiteNoise = whiteNoise
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SoundUiState())

    fun selectTab(tab: SoundTab) {
        _selectedTab.value = tab
    }

    fun toggleLullaby(index: Int) {
        val track = soundPlayer.lullabyTracks.getOrNull(index) ?: return
        val isStopping = state.value.currentTrackIndex == index
        soundPlayer.toggleLullaby(track, soundPlayer.lullabyTracks)
        viewModelScope.launch {
            // savedSoundName(백색소음 이름)은 덮어쓰지 않고 유지
            val savedName = settingsRepository.savedSoundName.first()
            if (isStopping) settingsRepository.setSavedSound("NONE", savedName)
            else settingsRepository.setSavedSound("LULLABY", savedName)
        }
    }

    fun toggleWhiteNoise(sound: SoundType) {
        val isStopping = state.value.activeWhiteNoise == sound
        soundPlayer.toggleWhiteNoise(sound)
        viewModelScope.launch {
            // 중지할 때도 sound.name을 유지해 조명탭에서 복원 가능하도록
            if (isStopping) settingsRepository.setSavedSound("NONE", sound.name)
            else settingsRepository.setSavedSound("WHITE_NOISE", sound.name)
        }
    }
}
