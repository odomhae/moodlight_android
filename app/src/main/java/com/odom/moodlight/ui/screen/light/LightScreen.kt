package com.odom.moodlight.ui.screen.light

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.ui.component.BrightnessSlider
import com.odom.moodlight.ui.component.ColorPickerRow
import com.odom.moodlight.ui.component.LightOrb
import com.odom.moodlight.ui.component.PaywallBottomSheet
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun LightScreen(viewModel: LightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    KeepScreenOn()

    LaunchedEffect(Unit) {
        viewModel.exitApp.collect {
            (context as? Activity)?.finishAndRemoveTask()
        }
    }

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

    var showTimerSheet by remember { mutableStateOf(false) }
    var showControlSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .alpha(animatedBrightness)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedColor.copy(alpha = 0.15f))
        )

        // 타이머 (상단)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            if (state.isTimerRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️ " + formatTimer(state.timerRemainingSeconds),
                        fontSize = 20.sp,
                        color = AppColors.WarmYellow,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = viewModel::cancelTimer) {
                        Text("취소", color = AppColors.TextDim, fontSize = 13.sp)
                    }
                }
            } else {
                TextButton(onClick = { showTimerSheet = true }) {
                    Text(
                        text = "⏱️ 타이머",
                        fontSize = 16.sp,
                        color = AppColors.TextDim
                    )
                }
            }
        }

        // 조명 (중앙)
        LightOrb(
            color = animatedColor,
            emoji = state.emoji,
            customIconPath = state.customIconPath,
            size = 240.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        // 컨트롤 열기 버튼 (하단)
        if (!state.sleepMode) {
            IconButton(
                onClick = { showControlSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "색상 및 밝기 조절",
                    tint = AppColors.TextDim,
                    modifier = Modifier.size(32.dp)
                )
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

    if (showControlSheet) {
        ControlBottomSheet(
            selectedIndex = state.colorIndex,
            isCycleMode = state.isCycleMode,
            brightness = state.brightness,
            onColorSelect = viewModel::selectColor,
            onCycleSelect = viewModel::toggleCycleMode,
            onBrightnessChange = viewModel::setBrightness,
            onDismiss = { showControlSheet = false }
        )
    }

    if (showTimerSheet) {
        TimerBottomSheet(
            onDismiss = { showTimerSheet = false },
            onStart = { minutes ->
                viewModel.startTimer(minutes)
                showTimerSheet = false
            }
        )
    }

    if (state.showPaywall) {
        PaywallBottomSheet(
            products = emptyList(),
            onDismiss = viewModel::dismissPaywall,
            onPurchase = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlBottomSheet(
    selectedIndex: Int,
    isCycleMode: Boolean,
    brightness: Float,
    onColorSelect: (Int) -> Unit,
    onCycleSelect: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "색상 및 밝기",
                fontSize = 16.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            ColorPickerRow(
                selectedIndex = selectedIndex,
                isCycleMode = isCycleMode,
                onColorSelect = onColorSelect,
                onCycleSelect = onCycleSelect
            )
            BrightnessSlider(
                brightness = brightness,
                onBrightnessChange = onBrightnessChange
            )
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerBottomSheet(onDismiss: () -> Unit, onStart: (Int) -> Unit) {
    val presets = listOf(
        1 to "1분",
        15 to "15분",
        30 to "30분",
        60 to "1시간",
        120 to "2시간",
        180 to "3시간",
        240 to "4시간",
        300 to "5시간",
        360 to "6시간"
    )
    var selectedMinutes by remember { mutableIntStateOf(30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "타이머 설정",
                fontSize = 18.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presets.take(3).forEach { (min, label) ->
                    FilterChip(
                        selected = selectedMinutes == min,
                        onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presets.drop(3).take(3).forEach { (min, label) ->
                    FilterChip(
                        selected = selectedMinutes == min,
                        onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                presets.drop(6).forEach { (min, label) ->
                    FilterChip(
                        selected = selectedMinutes == min,
                        onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onStart(selectedMinutes) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow)
            ) {
                Text("시작", color = AppColors.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
