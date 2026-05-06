package com.odom.moodlight.ui.screen.sound

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.odom.moodlight.data.SoundPlayer
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoundUiState(
    val activeSounds: Set<SoundType> = emptySet(),
    val volumes: Map<SoundType, Float> = emptyMap(),
    val isPro: Boolean = false,
    val showPaywall: Boolean = false,
    val selectedSoundForVolume: SoundType? = null,
)

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val _showPaywall = MutableStateFlow(false)
    private val _selectedForVolume = MutableStateFlow<SoundType?>(null)

    val products: StateFlow<List<ProductDetails>> = billingRepository.products

    val state: StateFlow<SoundUiState> = combine(
        soundPlayer.activeSounds,
        soundPlayer.volumes,
        billingRepository.isPro,
        _showPaywall,
        _selectedForVolume
    ) { active, volumes, isPro, showPaywall, selectedForVolume ->
        SoundUiState(
            activeSounds = active,
            volumes = volumes,
            isPro = isPro,
            showPaywall = showPaywall,
            selectedSoundForVolume = selectedForVolume
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SoundUiState())

    fun toggle(sound: SoundType) {
        val isPro = state.value.isPro
        if (sound.isPro && !isPro) {
            _showPaywall.value = true
            return
        }
        soundPlayer.toggle(sound)
    }

    fun setVolume(sound: SoundType, volume: Float) {
        soundPlayer.setVolume(sound, volume)
    }

    fun selectForVolume(sound: SoundType?) {
        _selectedForVolume.value = sound
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    fun purchase(activity: Activity, product: ProductDetails) {
        billingRepository.launchPurchaseFlow(activity, product)
    }

    fun retryBilling() = billingRepository.connect()
}
