package com.odom.moodlight.data.repository

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePrefs: SharedPreferences
) : PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(securePrefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val KEY_IS_PRO = "is_pro"
        const val PRODUCT_LIFETIME = "babylight_pro_lifetime"
        const val PRODUCT_MONTHLY = "babylight_pro_monthly"
    }

    fun connect(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                    queryProducts()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProducts() {
        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(inappParams) { _, inappDetails ->
            billingClient.queryProductDetailsAsync(subsParams) { _, subsDetails ->
                _products.value = inappDetails + subsDetails
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        val productDetailsParams = if (offerToken != null) {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offerToken)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .build()
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    fun restorePurchases() {
        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(type).build()
            ) { _, purchases -> handlePurchases(purchases) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(params) {}
                }
                setProStatus(true)
            }
        }
    }

    private fun setProStatus(value: Boolean) {
        securePrefs.edit().putBoolean(KEY_IS_PRO, value).apply()
        _isPro.value = value
    }
}
