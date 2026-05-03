package com.odom.moodlight.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.ui.component.BrightnessSlider
import com.odom.moodlight.ui.component.ColorPickerRow
import com.odom.moodlight.ui.component.PaywallBottomSheet
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showOrientationMenu by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(16.dp))

        // PRO 배너 (비 PRO 사용자)
        if (!state.isPro) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showPaywall() },
                colors = CardDefaults.cardColors(containerColor = AppColors.WarmYellow.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("PRO로 업그레이드", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("모든 사운드 & 기능 잠금 해제", fontSize = 13.sp, color = AppColors.TextDim)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SettingSection(title = "기본 색상") {
            ColorPickerRow(
                selectedIndex = state.colorIndex,
                isCycleMode = false,
                onColorSelect = viewModel::setColorIndex,
                onCycleSelect = {}
            )
        }

        SettingSection(title = "기본 밝기") {
            BrightnessSlider(
                brightness = state.brightness,
                onBrightnessChange = viewModel::setBrightness
            )
        }

        SettingSection(title = "화면 방향") {
            Box {
                OutlinedButton(onClick = { showOrientationMenu = true }) {
                    Text(
                        text = when (state.orientation) {
                            "portrait" -> "세로"
                            "landscape" -> "가로"
                            else -> "자동"
                        },
                        color = AppColors.TextPrimary
                    )
                }
                DropdownMenu(
                    expanded = showOrientationMenu,
                    onDismissRequest = { showOrientationMenu = false }
                ) {
                    listOf("portrait" to "세로", "landscape" to "가로", "auto" to "자동").forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setOrientation(value)
                                showOrientationMenu = false
                            }
                        )
                    }
                }
            }
        }

        SettingRow(
            title = "마지막 설정 복원",
            subtitle = "앱 시작 시 이전 설정 불러오기"
        ) {
            Switch(
                checked = state.autoRestore,
                onCheckedChange = viewModel::setAutoRestore,
                colors = SwitchDefaults.colors(checkedThumbColor = AppColors.WarmYellow)
            )
        }

        SettingSection(title = "언어") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ko" to "한국어", "en" to "English", "ja" to "日本語").forEach { (code, label) ->
                    FilterChip(
                        selected = state.language == code,
                        onClick = { viewModel.setLanguage(code) },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }
        }

        HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 8.dp))

        SettingClickRow("개인정보 처리방침") {
            uriHandler.openUri("https://yourapp.com/privacy")
        }

        SettingClickRow("리뷰 남기기") {}

        SettingRow(title = "앱 버전", subtitle = state.appVersion) {}

        Spacer(Modifier.height(80.dp))
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
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontSize = 13.sp, color = AppColors.TextDim, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
    HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SettingRow(title: String, subtitle: String = "", action: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = AppColors.TextPrimary)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 13.sp, color = AppColors.TextDim)
            }
        }
        action()
    }
    HorizontalDivider(color = AppColors.Border)
}

@Composable
private fun SettingClickRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = AppColors.TextPrimary)
    }
    HorizontalDivider(color = AppColors.Border)
}
