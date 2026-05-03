package com.odom.moodlight.ui.screen.sound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.component.PaywallBottomSheet
import com.odom.moodlight.ui.component.SoundCard
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundScreen(viewModel: SoundViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "사운드",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SoundType.entries) { sound ->
                SoundCard(
                    sound = sound,
                    isActive = state.activeSounds.contains(sound),
                    isPro = state.isPro,
                    onToggle = { viewModel.toggle(sound) }
                )
            }
        }

        // 볼륨 슬라이더 (재생 중인 사운드가 있을 때)
        val activeList = state.activeSounds.toList()
        if (activeList.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = AppColors.Panel,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("볼륨", fontSize = 14.sp, color = AppColors.TextDim)
                    activeList.forEach { sound ->
                        val volume = state.volumes[sound] ?: 1f
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sound.emoji, fontSize = 16.sp)
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(sound, it) },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = AppColors.WarmYellow,
                                    activeTrackColor = AppColors.WarmYellow,
                                )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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
