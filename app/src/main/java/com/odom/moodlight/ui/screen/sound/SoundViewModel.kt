package com.odom.moodlight.ui.screen.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SoundTab { LULLABY, WHITE_NOISE }

data class SoundUiState(
    val selectedTab: SoundTab = SoundTab.LULLABY,
    val currentTrackIndex: Int? = null,
    val activeWhiteNoise: SoundType? = null,
    val lullabyTracks: List<LullabyTrack> = LullabyTrack.entries
)

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer
) : ViewModel() {

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
        val track = LullabyTrack.entries.getOrNull(index) ?: return
        soundPlayer.toggleLullaby(track, LullabyTrack.entries)
    }

    fun toggleWhiteNoise(sound: SoundType) {
        soundPlayer.toggleWhiteNoise(sound)
    }
}
