package com.odom.moodlight.ui.screen.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.R
import com.odom.moodlight.ui.component.TimerArcProgress
import com.odom.moodlight.ui.component.WheelPicker
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.timer_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(24.dp))

        // 원형 프로그레스 + 시간 표시
        Box(contentAlignment = Alignment.Center) {
            TimerArcProgress(progress = state.progress, size = 240.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 실행 중이면 남은 시간, 아니면 설정된 시간 표시
                val displaySeconds = if (state.isRunning) state.remainingSeconds
                                     else (state.hours * 3600 + state.minutes * 60)
                val h = displaySeconds / 3600
                val m = (displaySeconds % 3600) / 60
                val s = displaySeconds % 60
                Text(
                    text = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppColors.TextPrimary
                )
                if (state.isRunning) {
                    Text(stringResource(R.string.timer_remaining_time), fontSize = 13.sp, color = AppColors.TextDim)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!state.isRunning) {
            // 프리셋 버튼 - 클릭 시 시간 즉시 설정
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15" + stringResource(R.string.timer_unit_min), 
                       30 to "30" + stringResource(R.string.timer_unit_min), 
                       60 to "1" + stringResource(R.string.timer_unit_hour), 
                       120 to "2" + stringResource(R.string.timer_unit_hour)).forEach { (min, label) ->
                    FilterChip(
                        selected = state.hours == min / 60 && state.minutes == min % 60,
                        onClick = {
                            viewModel.setHours(min / 60)
                            viewModel.setMinutes(min % 60)
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // WheelPicker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = (0..23).map { "%02d".format(it) },
                    selectedIndex = state.hours,
                    onSelectedIndexChange = viewModel::setHours,
                    modifier = Modifier.width(80.dp)
                )
                Text(stringResource(R.string.timer_unit_hour), fontSize = 16.sp, color = AppColors.TextDim, modifier = Modifier.padding(horizontal = 4.dp))
                WheelPicker(
                    items = (0..59).map { "%02d".format(it) },
                    selectedIndex = state.minutes,
                    onSelectedIndexChange = viewModel::setMinutes,
                    modifier = Modifier.width(80.dp)
                )
                Text(stringResource(R.string.timer_unit_min), fontSize = 16.sp, color = AppColors.TextDim, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(24.dp))

            // 종료 동작 선택
            Text(stringResource(R.string.timer_end_action_title), fontSize = 14.sp, color = AppColors.TextDim)
            Spacer(Modifier.height(8.dp))
            TimerEndAction.entries.forEach { action ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.endAction == action,
                        onClick = { viewModel.setEndAction(action) }
                    )
                    Text(
                        text = when (action) {
                            TimerEndAction.CLOSE_APP -> stringResource(R.string.timer_action_close_app)
                            TimerEndAction.DIM_AND_CLOSE -> stringResource(R.string.timer_action_dim_and_close)
                            TimerEndAction.PLAY_ALARM -> stringResource(R.string.timer_action_play_alarm)
                        },
                        fontSize = 14.sp,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 시작 버튼
            Button(
                onClick = viewModel::startTimer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow),
                enabled = (state.hours * 3600 + state.minutes * 60) > 0
            ) {
                Text(stringResource(R.string.timer_start), color = AppColors.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            OutlinedButton(
                onClick = viewModel::cancelTimer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.timer_cancel), color = AppColors.TextPrimary)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
