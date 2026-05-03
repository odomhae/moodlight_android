package com.odom.moodlight.ui.screen.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimerEndAction { CLOSE_APP, DIM_AND_CLOSE, PLAY_ALARM }

data class TimerUiState(
    val hours: Int = 0,
    val minutes: Int = 30,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val endAction: TimerEndAction = TimerEndAction.DIM_AND_CLOSE,
    val progress: Float = 1f,
)

@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun setHours(h: Int) = _state.update { it.copy(hours = h) }
    fun setMinutes(m: Int) = _state.update { it.copy(minutes = m) }
    fun setEndAction(action: TimerEndAction) = _state.update { it.copy(endAction = action) }

    fun startTimer() {
        val total = (_state.value.hours * 3600) + (_state.value.minutes * 60)
        if (total == 0) return
        _state.update { it.copy(totalSeconds = total, remainingSeconds = total, isRunning = true, progress = 1f) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.update {
                    it.copy(
                        remainingSeconds = remaining,
                        progress = remaining.toFloat() / total.toFloat()
                    )
                }
            }
            _state.update { it.copy(isRunning = false) }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _state.update { it.copy(isRunning = false, remainingSeconds = 0, progress = 1f) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
