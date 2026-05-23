package com.odom.moodlight.ui.navigation

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.odom.moodlight.R
import com.odom.moodlight.ui.component.AdBannerView
import com.odom.moodlight.ui.screen.light.LightScreen
import com.odom.moodlight.ui.screen.settings.SettingsScreen
import com.odom.moodlight.ui.screen.sound.SoundScreen
import com.odom.moodlight.ui.theme.AppColors

sealed class Screen(val route: String, val emoji: String, @StringRes val labelRes: Int) {
    data object Light : Screen("light", "💡", R.string.tab_light)
    data object Sound : Screen("sound", "🎵", R.string.tab_sound)
    data object Settings : Screen("settings", "⚙️", R.string.tab_settings)
}

private val screens = listOf(Screen.Light, Screen.Sound, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val context = LocalContext.current
    val activity = context as? Activity

    var showExitSheet by remember { mutableStateOf(false) }
    var navInterstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    val prefs = remember { context.getSharedPreferences("moodlight_nav", Context.MODE_PRIVATE) }
    val tabSwitchCount = remember { intArrayOf(prefs.getInt("tab_switch_count", 0)) }
    val isFirstRouteLoad = remember { booleanArrayOf(true) }
    val ADMOB_INTERSTITIAL_ID = stringResource(R.string.TEST_ADMOB_INTERSTITIAL_ID)

    fun loadNavInterstitial() {
        activity?.let { act ->
            InterstitialAd.load(act, ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) { navInterstitialAd = ad }
                    override fun onAdFailedToLoad(e: LoadAdError) { navInterstitialAd = null }
                }
            )
        }
    }

    LaunchedEffect(Unit) { loadNavInterstitial() }

    LaunchedEffect(currentRoute) {
        if (currentRoute == null) return@LaunchedEffect
        if (isFirstRouteLoad[0]) { isFirstRouteLoad[0] = false; return@LaunchedEffect }
        tabSwitchCount[0]++
        prefs.edit().putInt("tab_switch_count", tabSwitchCount[0]).apply()
        if (tabSwitchCount[0] % 5 == 0) {
            val ad = navInterstitialAd
            if (ad != null && activity != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        navInterstitialAd = null
                        loadNavInterstitial()
                    }
                }
                ad.show(activity)
                navInterstitialAd = null
            }
        }
    }

    BackHandler {
        showExitSheet = true
    }

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
                        label = { Text(stringResource(screen.labelRes), fontSize = 11.sp) },
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
            composable(Screen.Light.route) {
                LightScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Sound.route) { SoundScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }

    if (showExitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExitSheet = false },
            containerColor = AppColors.Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.exit_sheet_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.exit_sheet_subtitle),
                    fontSize = 14.sp,
                    color = AppColors.TextDim
                )
                Spacer(Modifier.height(20.dp))
                AdBannerView(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { activity?.finish() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SoftPink.copy(alpha = 0.8f))
                ) {
                    Text(stringResource(R.string.exit_button), color = AppColors.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showExitSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.exit_continue_button), color = AppColors.TextPrimary, fontSize = 16.sp)
                }
            }
        }
    }
}
