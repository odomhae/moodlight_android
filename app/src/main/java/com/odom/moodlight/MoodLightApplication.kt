package com.odom.moodlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.android.gms.ads.MobileAds
import com.odom.moodlight.R
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
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val AUDIO_CHANNEL_ID = "audio_service_channel"
    }
}
