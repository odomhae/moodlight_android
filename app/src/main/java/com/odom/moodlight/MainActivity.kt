package com.odom.moodlight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.odom.moodlight.ui.navigation.AppNavigation
import com.odom.moodlight.ui.theme.MoodLightTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.odom.moodlight.data.repository.BillingRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var billingRepository: BillingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        billingRepository.connect()
        setContent {
            MoodLightTheme {
                AppNavigation()
            }
        }
    }
}
