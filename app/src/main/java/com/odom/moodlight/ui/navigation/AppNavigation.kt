package com.odom.moodlight.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odom.moodlight.ui.screen.light.LightScreen
import com.odom.moodlight.ui.screen.settings.SettingsScreen
import com.odom.moodlight.ui.screen.sound.SoundScreen
import com.odom.moodlight.ui.screen.timer.TimerScreen
import com.odom.moodlight.ui.theme.AppColors

sealed class Screen(val route: String, val emoji: String, val label: String) {
    data object Light : Screen("light", "💡", "조명")
    data object Sound : Screen("sound", "🎵", "사운드")
    data object Timer : Screen("timer", "⏱️", "타이머")
    data object Settings : Screen("settings", "⚙️", "설정")
}

private val screens = listOf(Screen.Light, Screen.Sound, Screen.Timer, Screen.Settings)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            NavigationBar(containerColor = AppColors.Panel) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(text = screen.emoji, fontSize = 20.sp) },
                        label = { Text(screen.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = AppColors.WarmYellow,
                            unselectedTextColor = AppColors.TextDim,
                            indicatorColor = AppColors.WarmYellow.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Light.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Light.route) { LightScreen() }
            composable(Screen.Sound.route) { SoundScreen() }
            composable(Screen.Timer.route) { TimerScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
