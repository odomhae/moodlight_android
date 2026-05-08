package com.odom.moodlight.ui.screen.sound

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.odom.moodlight.data.model.LullabyTrack
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.component.AdBannerView
import com.odom.moodlight.ui.component.SoundCard
import com.odom.moodlight.ui.theme.AppColors

@Composable
fun SoundScreen(viewModel: SoundViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = AppColors.Background,
            contentColor = AppColors.WarmYellow,
            divider = {}
        ) {
            SoundTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = if (tab == SoundTab.LULLABY) "🎵 자장가" else "🌊 백색소음",
                            fontSize = 15.sp,
                            fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            when (state.selectedTab) {
                SoundTab.LULLABY -> LullabyList(
                    tracks = viewModel.tracks,
                    currentIndex = state.currentTrackIndex,
                    onTrackClick = viewModel::toggleLullaby
                )
                SoundTab.WHITE_NOISE -> WhiteNoiseGrid(
                    activeSound = state.activeWhiteNoise,
                    onSoundClick = viewModel::toggleWhiteNoise
                )
            }
        }

        AdBannerView(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun LullabyList(
    tracks: List<LullabyTrack>,
    currentIndex: Int?,
    onTrackClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tracks) { index, track ->
            val isPlaying = currentIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlaying) AppColors.WarmYellow.copy(alpha = 0.15f) else AppColors.Panel
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 16.sp,
                            color = if (isPlaying) AppColors.WarmYellow else AppColors.TextPrimary,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow, // 임시로 플레이 아이콘 사용 (ic_waveform 부재)
                            contentDescription = null,
                            tint = AppColors.WarmYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AppColors.TextDim,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteNoiseGrid(
    activeSound: SoundType?,
    onSoundClick: (SoundType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(SoundType.entries) { sound ->
            SoundCard(
                sound = sound,
                isActive = activeSound == sound,
                onToggle = { onSoundClick(sound) }
            )
        }
    }
}
