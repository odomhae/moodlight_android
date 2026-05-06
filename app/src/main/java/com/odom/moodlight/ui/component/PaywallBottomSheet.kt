package com.odom.moodlight.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.odom.moodlight.ui.theme.AppColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    products: List<ProductDetails>,
    onDismiss: () -> Unit,
    onPurchase: (ProductDetails) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var retryKey by remember { mutableIntStateOf(0) }
    var isTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(retryKey) {
        isTimedOut = false
        delay(10_000L)
        if (products.isEmpty()) isTimedOut = true
    }

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
            Text(text = "✨ PRO 업그레이드", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "모든 사운드, 색상 사이클, 믹서 기능을 잠금 해제하세요",
                fontSize = 14.sp,
                color = AppColors.TextDim
            )
            Spacer(Modifier.height(24.dp))

            listOf(
                "🔥 모닥불",
                "🎵 자장가",
                "🎹 피아노",
                "🌬️ 바람 소리",
                "🎛️ 사운드 믹서",
                "🌈 색상 사이클 모드"
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

            when {
                products.isNotEmpty() -> {
                    products.forEach { product ->
                        val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                            ?: product.subscriptionOfferDetails?.firstOrNull()
                                ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                            ?: ""
                        val title = if (product.productId.contains("lifetime")) "평생 이용권 $price" else "월 구독 $price"
                        Button(
                            onClick = { onPurchase(product) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow)
                        ) {
                            Text(text = title, color = AppColors.Background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                isTimedOut -> {
                    Text(
                        text = "상품 정보를 불러올 수 없습니다.\nPlay Store 연결을 확인해주세요.",
                        fontSize = 14.sp,
                        color = AppColors.TextDim,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            retryKey++
                            onRetry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarmYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("다시 시도", color = AppColors.Background, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    CircularProgressIndicator(color = AppColors.WarmYellow)
                    Spacer(Modifier.height(8.dp))
                    Text("상품 정보 불러오는 중...", fontSize = 13.sp, color = AppColors.TextDim)
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text(text = "나중에", color = AppColors.TextDim)
            }
        }
    }
}
