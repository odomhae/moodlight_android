package com.odom.moodlight.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.odom.moodlight.data.model.SoundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val players = mutableMapOf<SoundType, MediaPlayer>()

    private val _activeSounds = MutableStateFlow<Set<SoundType>>(emptySet())
    val activeSounds: StateFlow<Set<SoundType>> = _activeSounds

    private val _volumes = MutableStateFlow<Map<SoundType, Float>>(
        SoundType.entries.associateWith { 1f }
    )
    val volumes: StateFlow<Map<SoundType, Float>> = _volumes

    fun toggle(sound: SoundType) {
        if (_activeSounds.value.contains(sound)) stop(sound) else play(sound)
    }

    fun play(sound: SoundType) {
        if (players.containsKey(sound)) return
        val resId = context.resources.getIdentifier(
            sound.resourceName, "raw", context.packageName
        )
        if (resId == 0) return
        val player = MediaPlayer.create(context, resId) ?: return
        val storedVolume = _volumes.value[sound] ?: 1f
        player.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setVolume(storedVolume, storedVolume)
            start()
        }
        players[sound] = player
        _activeSounds.value = _activeSounds.value + sound
    }

    fun stop(sound: SoundType) {
        players[sound]?.apply {
            stop()
            release()
        }
        players.remove(sound)
        _activeSounds.value = _activeSounds.value - sound
    }

    fun stopAll() {
        players.values.forEach { it.stop(); it.release() }
        players.clear()
        _activeSounds.value = emptySet()
    }

    fun setVolume(sound: SoundType, volume: Float) {
        players[sound]?.setVolume(volume, volume)
        _volumes.value = _volumes.value + (sound to volume)
    }

    fun hasActiveSounds() = players.isNotEmpty()
}
