package com.odom.moodlight.ui.screen.sound

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odom.moodlight.R
import com.odom.moodlight.data.model.SoundType
import com.odom.moodlight.ui.component.PaywallBottomSheet
import com.odom.moodlight.ui.component.SoundCard
import com.odom.moodlight.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundScreen(viewModel: SoundViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showVolumeSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.tab_sound),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showVolumeSheet = true }) {
                Text(stringResource(id = R.string.sound_screen_volume_btn), color = AppColors.TextPrimary, fontSize = 14.sp)
            }
        }

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

    }

    if (showVolumeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showVolumeSheet = false },
            sheetState = sheetState,
            containerColor = AppColors.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    stringResource(id = R.string.sound_screen_volume_settings_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))
                val accessibleSounds = SoundType.entries.filter { !it.isPro || state.isPro }
                accessibleSounds.forEach { sound ->
                    val isActive = state.activeSounds.contains(sound)
                    val volume = state.volumes[sound] ?: 1f
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sound.emoji} ${stringResource(id = sound.labelResId)}",
                            fontSize = 14.sp,
                            color = if (isActive) AppColors.WarmYellow else AppColors.TextDim,
                            modifier = Modifier.width(100.dp)
                        )
                        Slider(
                            value = volume,
                            onValueChange = { viewModel.setVolume(sound, it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = AppColors.WarmYellow,
                                activeTrackColor = AppColors.WarmYellow,
                            )
                        )
                    }
                }
            }
        }
    }

    if (state.showPaywall) {
        PaywallBottomSheet(
            products = products,
            onDismiss = viewModel::dismissPaywall,
            onPurchase = { product -> (context as? Activity)?.let { viewModel.purchase(it, product) } },
            onRetry = viewModel::retryBilling
        )
    }
}
