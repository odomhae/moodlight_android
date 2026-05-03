package com.odom.moodlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoodLightApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        MobileAds.initialize(this) {}
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AUDIO_CHANNEL_ID,
            "야간등 오디오",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "백그라운드 사운드 재생"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val AUDIO_CHANNEL_ID = "audio_service_channel"
    }
}
