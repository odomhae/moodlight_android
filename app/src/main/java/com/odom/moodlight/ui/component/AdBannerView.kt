package com.odom.moodlight.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// 테스트 광고 ID — 실제 배포 전 교체 필요
const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

@Composable
fun AdBannerView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(BANNER_AD_UNIT_ID)
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}
