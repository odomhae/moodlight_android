package com.odom.moodlight.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odom.moodlight.R
import com.odom.moodlight.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardedAdSheet(
    isAdReady: Boolean,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.paywall_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.paywall_subtitle),
                fontSize = 14.sp,
                color = AppColors.TextDim,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            listOf(
                "🔥 " + stringResource(id = R.string.sound_fire),
                "🎵 " + stringResource(id = R.string.sound_lullaby),
                "🎹 " + stringResource(id = R.string.sound_piano),
                "🌬️ " + stringResource(id = R.string.sound_wind),
                stringResource(id = R.string.paywall_feature_mixer),
                stringResource(id = R.string.paywall_feature_cycle)
            ).forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✅", fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(text = feature, fontSize = 15.sp, color = AppColors.TextPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (isAdReady) {
                Button(
                    onClick = onWatchAd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow)
                ) {
                    Text(
                        text = stringResource(id = R.string.paywall_watch_ad),
                        color = AppColors.Background,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                CircularProgressIndicator(color = AppColors.WarmYellow)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.paywall_ad_loading),
                    fontSize = 13.sp,
                    color = AppColors.TextDim,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.paywall_later), color = AppColors.TextDim)
            }
        }
    }
}
