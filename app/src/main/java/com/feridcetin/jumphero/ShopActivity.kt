package com.feridcetin.jumphero

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopActivity : AppCompatActivity() {

    private lateinit var billingClient: BillingClient

    private val removeAdsProductId = "remove_ads_product"
    private val unlockCharactersProductId = "unlock_characters_product"
    private val buyCoinsProductId = "coins_pack_100"
    private val advancedThemeProductId = "advanced_theme_pack"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        val removeAdsButton: Button = findViewById(R.id.removeAdsButton)
        val unlockCharactersButton: Button = findViewById(R.id.unlockCharactersButton)
        val buyCoinsButton: Button = findViewById(R.id.buyCoinsButton)
        val advancedThemeButton: Button = findViewById(R.id.advancedThemeButton)

        setupBillingClient()

        removeAdsButton.setOnClickListener {
            queryProductAndLaunchPurchase(removeAdsProductId)
        }

        unlockCharactersButton.setOnClickListener {
            queryProductAndLaunchPurchase(unlockCharactersProductId)
        }

        buyCoinsButton.setOnClickListener {
            queryProductAndLaunchPurchase(buyCoinsProductId)
        }

        advancedThemeButton.setOnClickListener {
            queryProductAndLaunchPurchase(advancedThemeProductId)
        }
    }

    private fun setupBillingClient() {
        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    Toast.makeText(this, "Satın alma işlemi iptal edildi.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Hata oluştu: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                }
            }

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(this@ShopActivity, "Billing bağlantısı başarılı.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ShopActivity, "Billing bağlantısı başarısız: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onBillingServiceDisconnected() {
                Toast.makeText(this@ShopActivity, "Billing bağlantısı koptu.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun queryProductAndLaunchPurchase(productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(this, billingFlowParams)
            } else {
                Toast.makeText(this, "Ürün bilgileri sorgulanamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.first()

            if (productId == buyCoinsProductId) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
                    if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        grantCoinsFeature()
                    }
                }
            } else {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            grantFeature(purchase.products)
                        }
                    }
                } else {
                    grantFeature(purchase.products)
                }
            }
        }
    }

    private fun grantFeature(productIds: List<String>) {
        val sharedPref = getSharedPreferences("JumpHeroPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        for (productId in productIds) {
            when (productId) {
                removeAdsProductId -> {
                    editor.putBoolean("isAdFree", true)
                    Toast.makeText(this, "Reklamlar kaldırıldı! Teşekkür ederiz.", Toast.LENGTH_LONG).show()
                }
                unlockCharactersProductId -> {
                    editor.putBoolean("hasCharactersPack", true)
                    Toast.makeText(this, "Yeni karakterlerin kilidi açıldı! Teşekkür ederiz.", Toast.LENGTH_LONG).show()
                }
                advancedThemeProductId -> {
                    editor.putBoolean("hasAdvancedTheme", true)
                    Toast.makeText(this, "Yeni tema paketi aktif edildi! Teşekkür ederiz.", Toast.LENGTH_LONG).show()
                }
            }
        }
        editor.apply()
    }

    private fun grantCoinsFeature() {
        val sharedPref = getSharedPreferences("JumpHeroPrefs", MODE_PRIVATE)
        val currentCoins = sharedPref.getInt("playerCoins", 0)
        val newCoins = currentCoins + 100

        with(sharedPref.edit()) {
            putInt("playerCoins", newCoins)
            apply()
        }

        Toast.makeText(this, "100 Coin hesabınıza eklendi! Yeni bakiyeniz: $newCoins", Toast.LENGTH_LONG).show()
    }
}