package com.odom.moodlight.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.odom.moodlight.R


@Composable
fun AdBannerView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.TEST_ADMOB_BANNER_ID)
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}
