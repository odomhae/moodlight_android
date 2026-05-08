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
    // 백색소음 플레이어
    private var whiteNoisePlayer: MediaPlayer? = null
    private val _activeWhiteNoise = MutableStateFlow<SoundType?>(null)
    val activeWhiteNoise: StateFlow<SoundType?> = _activeWhiteNoise.asStateFlow()

    // 자장가 플레이어
    private var lullabyPlayer: MediaPlayer? = null
    private var lullabyPlaylist: List<LullabyTrack> = emptyList()
    private val _currentLullabyIndex = MutableStateFlow<Int?>(null)
    val currentLullabyIndex: StateFlow<Int?> = _currentLullabyIndex.asStateFlow()

    // assets/lullaby/ 폴더의 오디오 파일을 앱 시작 시 한 번 스캔
    val lullabyTracks: List<LullabyTrack> by lazy {
        context.assets.list("lullaby")
            ?.filter { it.endsWith(".mp3", ignoreCase = true) || it.endsWith(".m4a", ignoreCase = true) }
            ?.sorted()
            ?.map { LullabyTrack(title = it.substringBeforeLast("."), fileName = it) }
            ?: emptyList()
    }

    // ── 공통 유틸 ────────────────────────────────────────────

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun createPlayerFromAsset(path: String): MediaPlayer? {
        return try {
            val afd = context.assets.openFd(path)
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
            }
        } catch (_: Exception) { null }
    }

    // ── 백색소음 ─────────────────────────────────────────────

    fun playWhiteNoise(sound: SoundType) {
        stopWhiteNoise()
        val player = createPlayerFromAsset("whiteSound/${sound.fileName}") ?: return
        player.isLooping = true
        player.start()
        whiteNoisePlayer = player
        _activeWhiteNoise.value = sound
    }

    fun stopWhiteNoise() {
        whiteNoisePlayer?.apply { stop(); release() }
        whiteNoisePlayer = null
        _activeWhiteNoise.value = null
    }

    fun toggleWhiteNoise(sound: SoundType) {
        if (_activeWhiteNoise.value == sound) stopWhiteNoise() else playWhiteNoise(sound)
    }

    // ── 자장가 플레이리스트 ──────────────────────────────────

    fun playLullaby(track: LullabyTrack, allTracks: List<LullabyTrack>) {
        stopLullaby()
        lullabyPlaylist = allTracks
        val startIndex = allTracks.indexOf(track).coerceAtLeast(0)
        playLullabyAtIndex(startIndex, attemptsLeft = allTracks.size)
    }

    private fun playLullabyAtIndex(index: Int, attemptsLeft: Int) {
        if (attemptsLeft <= 0) return
        val track = lullabyPlaylist.getOrNull(index) ?: return
        val player = createPlayerFromAsset("lullaby/${track.fileName}")
        if (player == null) {
            // 파일 없으면 다음 곡으로 건너뜀
            playLullabyAtIndex((index + 1) % lullabyPlaylist.size, attemptsLeft - 1)
            return
        }
        player.setOnCompletionListener {
            playLullabyAtIndex((index + 1) % lullabyPlaylist.size, lullabyPlaylist.size)
        }
        player.start()
        lullabyPlayer = player
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
        if (currentIndex != null && allTracks.getOrNull(currentIndex) == track) {
            stopLullaby()
        } else {
            stopWhiteNoise()
            playLullaby(track, allTracks)
        }
    }

    fun stopAll() {
        stopWhiteNoise()
        stopLullaby()
    }

    fun hasActiveSounds() = whiteNoisePlayer != null || lullabyPlayer != null
}
