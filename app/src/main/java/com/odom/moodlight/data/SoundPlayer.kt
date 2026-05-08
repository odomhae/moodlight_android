package com.odom.moodlight.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 백색소음 플레이어 (단일 재생으로 변경)
    private var whiteNoisePlayer: MediaPlayer? = null
    private val _activeWhiteNoise = MutableStateFlow<SoundType?>(null)
    val activeWhiteNoise: StateFlow<SoundType?> = _activeWhiteNoise.asStateFlow()

    // 자장가 플레이어
    private var lullabyPlayer: MediaPlayer? = null
    private var lullabyPlaylist: List<LullabyTrack> = emptyList()
    private val _currentLullabyIndex = MutableStateFlow<Int?>(null)
    val currentLullabyIndex: StateFlow<Int?> = _currentLullabyIndex.asStateFlow()

    // ── 백색소음 ──────────────────────────────────────────────

    fun playWhiteNoise(sound: SoundType) {
        stopAll() // 자장가 포함 모두 정지
        
        val resId = context.resources.getIdentifier(sound.resourceName, "raw", context.packageName)
        if (resId == 0) return
        
        whiteNoisePlayer = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            start()
        }
        _activeWhiteNoise.value = sound
    }

    fun stopWhiteNoise() {
        whiteNoisePlayer?.apply { stop(); release() }
        whiteNoisePlayer = null
        _activeWhiteNoise.value = null
    }

    fun toggleWhiteNoise(sound: SoundType) {
        if (_activeWhiteNoise.value == sound) {
            stopWhiteNoise()
        } else {
            playWhiteNoise(sound)
        }
    }

    fun stopAll() {
        stopWhiteNoise()
        stopLullaby()
    }

    // ── 자장가 플레이리스트 ──────────────────────────────────

    fun playLullaby(track: LullabyTrack, allTracks: List<LullabyTrack>) {
        stopAll() // 백색소음 포함 모두 정지
        lullabyPlaylist = allTracks
        val startIndex = allTracks.indexOf(track).coerceAtLeast(0)
        playLullabyAtIndex(startIndex, attemptsLeft = allTracks.size)
    }

    private fun playLullabyAtIndex(index: Int, attemptsLeft: Int) {
        if (attemptsLeft <= 0) return
        val track = lullabyPlaylist.getOrNull(index) ?: return
        val resId = context.resources.getIdentifier(track.resourceName, "raw", context.packageName)
        
        if (resId == 0) {
            val next = (index + 1) % lullabyPlaylist.size
            playLullabyAtIndex(next, attemptsLeft - 1)
            return
        }

        lullabyPlayer?.release()
        lullabyPlayer = MediaPlayer.create(context, resId)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener {
                val next = (index + 1) % lullabyPlaylist.size
                playLullabyAtIndex(next, lullabyPlaylist.size)
            }
            start()
        }
        _currentLullabyIndex.value = index
    }

    fun stopLullaby() {
        lullabyPlayer?.apply { stop(); release() }
        lullabyPlayer = null
        lullabyPlaylist = emptyList()
        _currentLullabyIndex.value = null
    }

    fun toggleLullaby(track: LullabyTrack, allTracks: List<LullabyTrack>) {
        val currentIndex = _currentLullabyIndex.value
        val targetIndex = allTracks.indexOf(track)
        
        if (currentIndex != null && allTracks.getOrNull(currentIndex) == track) {
            stopLullaby()
        } else {
            playLullaby(track, allTracks)
        }
    }
}
