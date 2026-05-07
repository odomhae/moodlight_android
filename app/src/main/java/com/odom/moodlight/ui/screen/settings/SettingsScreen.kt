package com.odom.moodlight.ui.screen.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.play.core.review.ReviewManagerFactory
import com.odom.moodlight.MoodLightDeviceAdminReceiver
import com.odom.moodlight.R
import com.odom.moodlight.data.model.VisualPattern
import com.odom.moodlight.ui.component.ColorPaletteSheet
import com.odom.moodlight.ui.component.INTERSTITIAL_AD_UNIT_ID
import com.odom.moodlight.ui.component.RewardedAdSheet
import com.odom.moodlight.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var showColorPalette by remember { mutableStateOf(false) }

    val dpm = remember { context.getSystemService(DevicePolicyManager::class.java) }
    val adminComponent = remember { ComponentName(context, MoodLightDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm?.isAdminActive(adminComponent) == true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = dpm?.isAdminActive(adminComponent) == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var customBitmap by remember(state.customIconPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(state.customIconPath) {
        customBitmap = if (state.customIconPath != null) {
            withContext(Dispatchers.IO) {
                try { BitmapFactory.decodeFile(state.customIconPath)?.asImageBitmap() } catch (e: Exception) { null }
            }
        } else null
    }

    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    fun loadInterstitialAd() {
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            })
    }
    LaunchedEffect(Unit) { loadInterstitialAd() }
    LaunchedEffect(Unit) {
        viewModel.showInterstitialAd.collect {
            val ad = interstitialAd
            if (ad != null && activity != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() { interstitialAd = null; loadInterstitialAd() }
                }
                ad.show(activity)
                interstitialAd = null
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val file = File(context.filesDir, "custom_icon.jpg")
                    val bitmap = context.contentResolver.openInputStream(selectedUri)?.use { BitmapFactory.decodeStream(it) } ?: return@launch
                    val orientation = context.contentResolver.openInputStream(selectedUri)?.use {
                        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    } ?: ExifInterface.ORIENTATION_NORMAL
                    val rotated = rotateBitmap(bitmap, orientation)
                    file.outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    if (rotated !== bitmap) bitmap.recycle()
                    withContext(Dispatchers.Main) { viewModel.setCustomIconPath(file.absolutePath) }
                } catch (_: Exception) { }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val path = cameraFile?.absolutePath ?: return@rememberLauncherForActivityResult
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path) ?: return@launch
                    val orientation = ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val rotated = rotateBitmap(bitmap, orientation)
                    File(path).outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    if (rotated !== bitmap) bitmap.recycle()
                    withContext(Dispatchers.Main) { viewModel.setCustomIconPath(path) }
                } catch (_: Exception) { }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(16.dp))

        if (!state.isPro) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.showPaywall() },
                colors = CardDefaults.cardColors(containerColor = AppColors.WarmYellow.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.settings_pro_upgrade), fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text(stringResource(R.string.settings_pro_desc), fontSize = 13.sp, color = AppColors.TextDim)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 시각적 패턴 섹션
        SettingSection(title = stringResource(R.string.settings_visual_pattern_section)) {
            val patterns = listOf(
                VisualPattern.NONE to stringResource(R.string.settings_pattern_none),
                VisualPattern.STARLIGHT to stringResource(R.string.settings_pattern_starlight),
                VisualPattern.CANDLE_FLICKER to stringResource(R.string.settings_pattern_candle),
                VisualPattern.WAVE to stringResource(R.string.settings_pattern_wave),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                patterns.forEach { (pattern, label) ->
                    FilterChip(
                        selected = state.visualPattern == pattern,
                        onClick = { viewModel.setVisualPattern(pattern) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 색상 섹션
        SettingSection(title = stringResource(R.string.settings_color_section)) {
            OutlinedButton(
                onClick = { showColorPalette = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_open_color_palette), color = AppColors.TextPrimary)
            }
            if (state.recentCustomColors.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.settings_recent_colors), fontSize = 12.sp, color = AppColors.TextDim)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recentCustomColors.forEach { color ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .then(
                                    Modifier.clickable { viewModel.selectCustomColor(color) }
                                )
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = color)
                            }
                        }
                    }
                }
            }
        }

        // 아이콘 섹션
        SettingSection(title = stringResource(R.string.settings_icon_section)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("🌙", "👶", "🌟", "🐑", "🦋").forEachIndexed { index, emoji ->
                    FilterChip(
                        selected = state.customIconPath == null && state.emojiIndex == index,
                        onClick = { viewModel.selectEmojiPreset(index) },
                        label = { Text(emoji, fontSize = 20.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val file = File(context.filesDir, "custom_icon.jpg")
                        cameraFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_camera), color = AppColors.TextPrimary)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_gallery), color = AppColors.TextPrimary)
                }
            }
            if (customBitmap != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(bitmap = customBitmap!!, contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Text(stringResource(R.string.settings_custom_image_active), fontSize = 13.sp, color = AppColors.TextDim)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearCustomIcon() }) {
                        Text(stringResource(R.string.settings_remove), color = AppColors.SoftPink, fontSize = 13.sp)
                    }
                }
            }
        }

        SettingRow(
            title = stringResource(R.string.settings_screen_off_title),
            subtitle = if (isAdminActive) stringResource(R.string.settings_enabled) else stringResource(R.string.settings_permission_required)
        ) {
            Switch(
                checked = isAdminActive,
                onCheckedChange = { enable ->
                    if (enable) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.settings_permission_explanation))
                        }
                        activity?.startActivity(intent)
                    } else {
                        dpm?.removeActiveAdmin(adminComponent)
                        isAdminActive = false
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AppColors.WarmYellow)
            )
        }

        HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 8.dp))

        SettingClickRow(stringResource(R.string.settings_leave_review)) {
            activity?.let { act ->
                val manager = ReviewManagerFactory.create(act)
                manager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                    manager.launchReviewFlow(act, reviewInfo)
                }
            }
        }

        SettingRow(title = stringResource(R.string.settings_app_version), subtitle = state.appVersion) {}

        Spacer(Modifier.height(80.dp))
    }

    if (state.showPaywall) {
        RewardedAdSheet(
            isAdReady = state.isAdReady,
            onWatchAd = { activity?.let { viewModel.watchAd(it) } },
            onDismiss = viewModel::dismissPaywall
        )
    }

    if (showColorPalette) {
        ColorPaletteSheet(
            onColorSelected = { color ->
                viewModel.selectCustomColor(color)
                showColorPalette = false
            },
            onDismiss = { showColorPalette = false }
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontSize = 13.sp, color = AppColors.TextDim, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
    HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SettingRow(title: String, subtitle: String = "", action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = AppColors.TextPrimary)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = AppColors.TextDim)
        }
        action()
    }
    HorizontalDivider(color = AppColors.Border)
}

@Composable
private fun SettingClickRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = AppColors.TextPrimary)
    }
    HorizontalDivider(color = AppColors.Border)
}

private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bitmap
    }
    return try {
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (_: OutOfMemoryError) {
        bitmap
    }
}
