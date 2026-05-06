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
import com.odom.moodlight.ui.component.INTERSTITIAL_AD_UNIT_ID
import com.odom.moodlight.ui.component.PaywallBottomSheet
import com.odom.moodlight.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var cameraFile by remember { mutableStateOf<File?>(null) }

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

    // 전면 광고 관리
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }

    fun loadInterstitialAd() {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            }
        )
    }

    LaunchedEffect(Unit) { loadInterstitialAd() }

    LaunchedEffect(Unit) {
        viewModel.showInterstitialAd.collect {
            val ad = interstitialAd
            if (ad != null && activity != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitialAd()
                    }
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
                    val bitmap = context.contentResolver.openInputStream(selectedUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    } ?: return@launch
                    val orientation = context.contentResolver.openInputStream(selectedUri)?.use { input ->
                        ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    } ?: ExifInterface.ORIENTATION_NORMAL
                    val rotated = rotateBitmap(bitmap, orientation)
                    file.outputStream().use { out -> rotated.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    if (rotated !== bitmap) bitmap.recycle()
                    withContext(Dispatchers.Main) { viewModel.setCustomIconPath(file.absolutePath) }
                } catch (e: Exception) { }
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
                } catch (e: Exception) { }
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
        Text("설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Spacer(Modifier.height(16.dp))

        if (!state.isPro) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showPaywall() },
                colors = CardDefaults.cardColors(containerColor = AppColors.WarmYellow.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("PRO로 업그레이드", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("모든 사운드 & 기능 잠금 해제", fontSize = 13.sp, color = AppColors.TextDim)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SettingSection(title = "중앙 아이콘") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🌙", "👶", "🌟", "🐑", "🦋").forEachIndexed { index, emoji ->
                    FilterChip(
                        selected = state.customIconPath == null && state.emojiIndex == index,
                        onClick = { viewModel.selectEmojiPreset(index) },
                        label = { Text(emoji, fontSize = 20.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val file = File(context.filesDir, "custom_icon.jpg")
                        cameraFile = file
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 카메라", color = AppColors.TextPrimary)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🖼️ 앨범", color = AppColors.TextPrimary)
                }
            }
            if (customBitmap != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = customBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text("커스텀 이미지 사용 중", fontSize = 13.sp, color = AppColors.TextDim)
                }
            }
        }

        SettingRow(
            title = "타이머 종료 시 화면 끄기",
            subtitle = if (isAdminActive) "활성화됨" else "권한 허용 필요"
        ) {
            Switch(
                checked = isAdminActive,
                onCheckedChange = { enable ->
                    if (enable) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "타이머 종료 시 화면을 자동으로 끕니다.")
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

      //  SettingClickRow("개인정보 처리방침") {}

        SettingClickRow("리뷰 남기기") {
            activity?.let { act ->
                val manager = ReviewManagerFactory.create(act)
                manager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                    manager.launchReviewFlow(act, reviewInfo)
                }
            }
        }

        SettingRow(title = "앱 버전", subtitle = state.appVersion) {}

        Spacer(Modifier.height(80.dp))
    }

    if (state.showPaywall) {
        PaywallBottomSheet(
            products = products,
            onDismiss = viewModel::dismissPaywall,
            onPurchase = { product -> activity?.let { viewModel.purchase(it, product) } },
            onRetry = viewModel::retryBilling
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 15.sp, color = AppColors.TextPrimary)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 13.sp, color = AppColors.TextDim)
            }
        }
        action()
    }
    HorizontalDivider(color = AppColors.Border)
}

@Composable
private fun SettingClickRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
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
