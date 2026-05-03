package com.odom.moodlight.ui.screen.light

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.component.*
import com.odom.moodlight.ui.theme.AppColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun LightScreen(viewModel: LightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    KeepScreenOn()

    val animatedColor by animateColorAsState(
        targetValue = state.lightColor,
        animationSpec = tween(1000),
        label = "color"
    )

    val animatedBrightness by animateFloatAsState(
        targetValue = if (state.sleepMode) 0.1f else state.brightness,
        animationSpec = if (state.sleepMode) tween(5 * 60 * 1000) else tween(300),
        label = "brightness"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .alpha(animatedBrightness)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 조명 영역 (70%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .background(animatedColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LightOrb(
                        color = animatedColor,
                        emoji = state.emoji,
                        size = 200.dp,
                        onEmojiTap = viewModel::nextEmoji
                    )
                    if (state.showClock) {
                        Spacer(Modifier.height(24.dp))
                        ClockDisplay()
                    }
                    if (state.isCycleMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "🌈 색상 사이클 모드 중",
                            fontSize = 13.sp,
                            color = AppColors.TextDim
                        )
                    }
                }
            }

            // 컨트롤 패널 (30%)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f),
                color = AppColors.Panel.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorPickerRow(
                        selectedIndex = state.colorIndex,
                        isCycleMode = state.isCycleMode,
                        onColorSelect = viewModel::selectColor,
                        onCycleSelect = viewModel::toggleCycleMode
                    )

                    BrightnessSlider(
                        brightness = state.brightness,
                        onBrightnessChange = viewModel::setBrightness
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(SoundType.entries) { sound ->
                            SoundChip(
                                sound = sound,
                                isActive = state.activeSound == sound,
                                isPro = state.isPro,
                                onClick = { viewModel.toggleSound(sound) }
                            )
                        }
                    }

                    ActionButtonRow(
                        sleepMode = state.sleepMode,
                        showClock = state.showClock,
                        isTimerRunning = state.isTimerRunning,
                        timerRemainingSeconds = state.timerRemainingSeconds,
                        onToggleSleep = viewModel::toggleSleepMode,
                        onToggleClock = viewModel::toggleClock,
                        onStartTimer = viewModel::startTimer,
                        onCancelTimer = viewModel::cancelTimer
                    )
                }
            }
        }

        // 수면 모드 터치 차단
        if (state.sleepMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.toggleSleepMode() })
                    }
            )
        }
    }

    if (state.showPaywall) {
        PaywallBottomSheet(
            products = emptyList(),
            onDismiss = viewModel::dismissPaywall,
            onPurchase = {}
        )
    }
}

@Composable
private fun ClockDisplay() {
    val time = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            time.value = LocalTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }
    Text(
        text = time.value.format(DateTimeFormatter.ofPattern("HH:mm")),
        fontSize = 48.sp,
        fontWeight = FontWeight.Light,
        color = AppColors.TextPrimary
    )
    Text(
        text = LocalDate.now().format(DateTimeFormatter.ofPattern("E, MMM d")),
        fontSize = 14.sp,
        color = AppColors.TextDim
    )
}

@Composable
private fun ActionButtonRow(
    sleepMode: Boolean,
    showClock: Boolean,
    isTimerRunning: Boolean,
    timerRemainingSeconds: Int,
    onToggleSleep: () -> Unit,
    onToggleClock: () -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    var showTimerSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            emoji = "⏱️",
            label = if (isTimerRunning) formatTimer(timerRemainingSeconds) else "타이머",
            isActive = isTimerRunning,
            onClick = { if (isTimerRunning) onCancelTimer() else showTimerSheet = true }
        )
        ActionButton(
            emoji = "🕐",
            label = "시계",
            isActive = showClock,
            onClick = onToggleClock
        )
        ActionButton(
            emoji = "💤",
            label = "수면",
            isActive = sleepMode,
            onClick = onToggleSleep
        )
    }

    if (showTimerSheet) {
        TimerBottomSheet(
            onDismiss = { showTimerSheet = false },
            onStart = { minutes ->
                onStartTimer(minutes)
                showTimerSheet = false
            }
        )
    }
}

private fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun ActionButton(
    emoji: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = emoji,
                fontSize = 22.sp,
                color = if (isActive) AppColors.WarmYellow else AppColors.TextDim
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = if (isActive) AppColors.WarmYellow else AppColors.TextDim
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerBottomSheet(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    val presets = listOf(15, 30, 60, 120)
    var selectedMinutes by remember { mutableIntStateOf(30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("타이머 설정", fontSize = 18.sp, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { min ->
                    FilterChip(
                        selected = selectedMinutes == min,
                        onClick = { selectedMinutes = min },
                        label = { Text(if (min < 60) "${min}분" else "${min / 60}시간") }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onStart(selectedMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow)
            ) {
                Text("시작", color = AppColors.Background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
