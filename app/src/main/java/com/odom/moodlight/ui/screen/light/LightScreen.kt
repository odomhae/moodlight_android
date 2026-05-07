package com.odom.moodlight.ui.screen.light

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.play.core.review.ReviewManagerFactory
import com.odom.moodlight.MoodLightDeviceAdminReceiver
import com.odom.moodlight.R
import com.odom.moodlight.data.model.VisualPattern
import com.odom.moodlight.ui.component.BrightnessSlider
import com.odom.moodlight.ui.component.ColorPaletteSheet
import com.odom.moodlight.ui.component.ColorPickerRow
import com.odom.moodlight.ui.component.LightOrb
import com.odom.moodlight.ui.component.RewardedAdSheet
import com.odom.moodlight.ui.component.VisualPatternEffect
import com.odom.moodlight.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun LightScreen(viewModel: LightViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    KeepScreenOn()

    LaunchedEffect(Unit) {
        viewModel.exitApp.collect {
            val activity = context as? Activity
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val adminComp = ComponentName(context, MoodLightDeviceAdminReceiver::class.java)
            if (dpm?.isAdminActive(adminComp) == true) {
                dpm.lockNow()
                activity?.finishAndRemoveTask()
            } else {
                activity?.window?.let { window ->
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    val lp = window.attributes
                    lp.screenBrightness = 0f
                    window.attributes = lp
                }
                delay(600)
                activity?.finishAndRemoveTask()
            }
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

    val hintTransition = rememberInfiniteTransition(label = "swipeHint")
    val hintBounce by hintTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintBounce"
    )

    val isNonePattern = state.visualPattern == VisualPattern.NONE
    val isWavePattern = state.visualPattern == VisualPattern.WAVE

    // NONE 모드에서 배경색 밝기 + 현재 밝기를 합산해 텍스트 대비색 결정
    val perceivedLuminance = animatedColor.red * 0.299f + animatedColor.green * 0.587f + animatedColor.blue * 0.114f
    val effectiveLuminance = if (isNonePattern) perceivedLuminance * animatedBrightness else 0f
    val onBackground = if (isNonePattern && effectiveLuminance > 0.45f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isNonePattern) animatedColor else AppColors.Background)
            // alpha는 루트에 적용하지 않음 → 텍스트가 항상 선명하게 유지됨
            .pointerInput(state.sleepMode) {
                if (!state.sleepMode) {
                    var startY = 0f
                    var totalDy = 0f
                    detectVerticalDragGestures(
                        onDragStart = { offset -> startY = offset.y; totalDy = 0f },
                        onVerticalDrag = { change, dy -> totalDy += dy; change.consume() },
                        onDragEnd = {
                            if (startY > size.height * 0.35f && totalDy < -80f) showControlSheet = true
                            totalDy = 0f
                        },
                        onDragCancel = { totalDy = 0f }
                    )
                }
            }
    ) {
        // 패턴 효과 (NONE 이외)
        if (!isNonePattern) {
            Box(modifier = Modifier.fillMaxSize().background(animatedColor.copy(alpha = 0.15f)))
            VisualPatternEffect(
                pattern = state.visualPattern,
                color = animatedColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 조명 Orb — 딤 오버레이 아래에 배치해 밝기 조절에 함께 반응
        if (!isNonePattern && !isWavePattern) {
            LightOrb(
                color = animatedColor,
                emoji = state.emoji,
                customIconPath = state.customIconPath,
                size = 240.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 밝기 딤 오버레이 — 배경/Orb만 어둡게, 위의 텍스트는 영향받지 않음
        val dimAlpha = (1f - animatedBrightness).coerceIn(0f, 0.95f)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))

        // 타이머 (상단) — 오버레이 위에 배치 → 항상 선명
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
                        color = onBackground.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = viewModel::cancelTimer) {
                        Text(
                            stringResource(R.string.light_cancel),
                            color = onBackground.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                TextButton(onClick = { showTimerSheet = true }) {
                    Text(
                        text = stringResource(R.string.light_timer_title),
                        fontSize = 16.sp,
                        color = onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 스와이프 힌트 (하단) — 오버레이 위에 배치 → 항상 선명
        if (!state.sleepMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = onBackground.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(28.dp)
                        .offset(y = hintBounce.dp)
                )
                Text(
                    text = stringResource(R.string.light_swipe_hint),
                    fontSize = 13.sp,
                    color = onBackground.copy(alpha = 0.3f)
                )
            }
        }

        // 수면 모드
        if (state.sleepMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.toggleSleepMode() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.light_sleep_mode_hint),
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary.copy(alpha = 0.15f)
                )
            }
        }
    }

    if (showControlSheet) {
        ControlBottomSheet(
            selectedIndex = state.colorIndex,
            isCycleMode = state.isCycleMode,
            recentCustomColors = state.recentCustomColors,
            isCustomColorSelected = state.isCustomColorSelected,
            selectedCustomColor = state.selectedCustomColor,
            brightness = state.brightness,
            onColorSelect = viewModel::selectColor,
            onCycleSelect = viewModel::toggleCycleMode,
            onCustomColorSelect = viewModel::selectCustomColor,
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
                (context as? Activity)?.let { act ->
                    val manager = ReviewManagerFactory.create(act)
                    manager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                        manager.launchReviewFlow(act, reviewInfo)
                    }
                }
            }
        )
    }

    if (state.showPaywall) {
        RewardedAdSheet(
            isAdReady = state.isAdReady,
            onWatchAd = { (context as? Activity)?.let { viewModel.watchAd(it) } },
            onDismiss = viewModel::dismissPaywall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlBottomSheet(
    selectedIndex: Int,
    isCycleMode: Boolean,
    recentCustomColors: List<androidx.compose.ui.graphics.Color>,
    isCustomColorSelected: Boolean,
    selectedCustomColor: androidx.compose.ui.graphics.Color?,
    brightness: Float,
    onColorSelect: (Int) -> Unit,
    onCycleSelect: () -> Unit,
    onCustomColorSelect: (androidx.compose.ui.graphics.Color) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPalette by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                stringResource(R.string.light_control_title),
                fontSize = 16.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            ColorPickerRow(
                selectedIndex = selectedIndex,
                isCycleMode = isCycleMode,
                recentCustomColors = recentCustomColors,
                isCustomColorSelected = isCustomColorSelected,
                selectedCustomColor = selectedCustomColor,
                onColorSelect = onColorSelect,
                onCycleSelect = onCycleSelect,
                onCustomColorSelect = onCustomColorSelect,
                onOpenPalette = { showPalette = true }
            )
            BrightnessSlider(
                brightness = brightness,
                onBrightnessChange = onBrightnessChange
            )
        }
    }

    if (showPalette) {
        ColorPaletteSheet(
            onColorSelected = { color ->
                onCustomColorSelect(color)
                showPalette = false
            },
            onDismiss = { showPalette = false }
        )
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presets = listOf(
        1 to "1" + stringResource(R.string.light_unit_min),
        15 to "15" + stringResource(R.string.light_unit_min),
        30 to "30" + stringResource(R.string.light_unit_min),
        60 to "1" + stringResource(R.string.light_unit_hour),
        120 to "2" + stringResource(R.string.light_unit_hour),
        180 to "3" + stringResource(R.string.light_unit_hour),
        240 to "4" + stringResource(R.string.light_unit_hour),
        300 to "5" + stringResource(R.string.light_unit_hour),
        360 to "6" + stringResource(R.string.light_unit_hour)
    )
    var selectedMinutes by remember { mutableIntStateOf(30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                stringResource(R.string.light_timer_setting_title),
                fontSize = 18.sp,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                presets.take(3).forEach { (min, label) ->
                    FilterChip(selected = selectedMinutes == min, onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                presets.drop(3).take(3).forEach { (min, label) ->
                    FilterChip(selected = selectedMinutes == min, onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                presets.drop(6).forEach { (min, label) ->
                    FilterChip(selected = selectedMinutes == min, onClick = { selectedMinutes = min },
                        label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onStart(selectedMinutes) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow)
            ) {
                Text(stringResource(R.string.light_start), color = AppColors.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
