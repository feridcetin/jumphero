package com.feridcetin.jumphero

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class SettingActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var sharedPref: SharedPreferences
    private var mRewardedAd: RewardedAd? = null
    private lateinit var billingClient: BillingClient

    private lateinit var btnLangEn: Button
    private lateinit var btnLangTr: Button
    private lateinit var switchPremiumCharacter: Switch
    private lateinit var switchAdvancedTheme: Switch
    private lateinit var btnSaveCharacter: Button
    private lateinit var btnMusicSettings: Button
    private lateinit var btnBackgroundSettings: Button

    private var selectedCharacterColor: Int = R.drawable.rounded_button_red
    private lateinit var characterButtons: List<ImageButton>

    private val colorDrawables = mapOf(
        R.id.btnRed to R.drawable.rounded_button_red,
        R.id.btnBlue to R.drawable.rounded_button_blue,
        R.id.btnGreen to R.drawable.rounded_button_green,
        R.id.btnYellow to R.drawable.rounded_button_yellow,
        R.id.btnOrange to R.drawable.rounded_button_orange,
        R.id.btnPink to R.drawable.rounded_button_pink,
        R.id.btnTurquoise to R.drawable.rounded_button_turquoise,
        R.id.btnWhite to R.drawable.rounded_button_white,
        R.id.btnBlack to R.drawable.rounded_button_black,
        R.id.btnBrown to R.drawable.rounded_button_brown,
        R.id.btnGray to R.drawable.rounded_button_gray,
        R.id.btnPurple to R.drawable.rounded_button_purple
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        MobileAds.initialize(this) {}
        loadRewardedAd()

        sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)

        initializeViews()
        loadSettings()
        setupListeners()

        // BillingClient'ı başlat
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        // Google Play'e bağlan
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchaseHistory()
                } else {
                    Log.e("Billing", "Faturalandırma hizmeti bağlantısı kurulamadı: ${billingResult.responseCode}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d("Billing", "Faturalandırma hizmeti bağlantısı kesildi.")
            }
        })
    }

    private fun initializeViews() {
        btnLangEn = findViewById(R.id.btn_lang_en)
        btnLangTr = findViewById(R.id.btn_lang_tr)
        switchPremiumCharacter = findViewById(R.id.switch_premium_character)
        switchAdvancedTheme = findViewById(R.id.switch_advanced_theme)
        btnSaveCharacter = findViewById(R.id.btnSaveCharacter)
        btnMusicSettings = findViewById(R.id.btnMusicSettings)
        btnBackgroundSettings = findViewById(R.id.btn_background_settings)

        characterButtons = listOf(
            findViewById(R.id.btnRed),
            findViewById(R.id.btnBlue),
            findViewById(R.id.btnGreen),
            findViewById(R.id.btnYellow),
            findViewById(R.id.btnOrange),
            findViewById(R.id.btnPink),
            findViewById(R.id.btnTurquoise),
            findViewById(R.id.btnWhite),
            findViewById(R.id.btnBlack),
            findViewById(R.id.btnBrown),
            findViewById(R.id.btnGray),
            findViewById(R.id.btnPurple)
        )
    }

    private fun loadSettings() {
        updateLanguageButtons()
        switchPremiumCharacter.isChecked = sharedPref.getBoolean("hasCharactersPack", false)
        switchAdvancedTheme.isChecked = sharedPref.getBoolean("hasAdvancedTheme", false)
        selectedCharacterColor = sharedPref.getInt("selected_character_color", R.drawable.rounded_button_red)
        updateCharacterSelectionUI()
    }

    private fun setupListeners() {
        btnLangEn.setOnClickListener {
            saveStringSetting("language", "en")
            LocaleHelper.setLocaleAndRestart(this, "en")
            updateLanguageButtons()
        }
        btnLangTr.setOnClickListener {
            saveStringSetting("language", "tr")
            LocaleHelper.setLocaleAndRestart(this, "tr")
            updateLanguageButtons()
        }

        switchPremiumCharacter.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!sharedPref.getBoolean("hasCharactersPack", false)) {
                    buyPremiumCharacter()
                }
            } else {
                switchPremiumCharacter.isChecked = true
            }
        }

        switchAdvancedTheme.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("hasAdvancedTheme", isChecked)
        }

        characterButtons.forEach { button ->
            button.setOnClickListener {
                selectedCharacterColor = colorDrawables[it.id] ?: R.drawable.rounded_button_red
                updateCharacterSelectionUI()
            }
        }

        btnSaveCharacter.setOnClickListener {
            showRewardedAd()
        }

        btnMusicSettings.setOnClickListener {
            val intent = Intent(this, MusicSettingActivity::class.java)
            startActivity(intent)
        }

        btnBackgroundSettings.setOnClickListener {
            val intent = Intent(this, BackgroundSettingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateLanguageButtons() {
        val savedLanguage = sharedPref.getString("language", "en")
        btnLangEn.setBackgroundColor(if (savedLanguage == "en") Color.parseColor("#4CAF50") else Color.LTGRAY)
        btnLangTr.setBackgroundColor(if (savedLanguage == "tr") Color.parseColor("#4CAF50") else Color.LTGRAY)
    }

    private fun updateCharacterSelectionUI() {
        val selectedBorderDrawable = ContextCompat.getDrawable(this, R.drawable.rounded_button_border)

        characterButtons.forEach { button ->
            val colorDrawableId = colorDrawables[button.id]
            if (colorDrawableId != null) {
                if (colorDrawableId == selectedCharacterColor) {
                    val colorDrawable = ContextCompat.getDrawable(this, colorDrawableId)
                    val layers = arrayOf(colorDrawable, selectedBorderDrawable)
                    val layerDrawable = LayerDrawable(layers)
                    button.background = layerDrawable
                } else {
                    button.background = ContextCompat.getDrawable(this, colorDrawableId)
                }
            }
        }
    }

    private fun saveBooleanSetting(key: String, value: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    private fun saveStringSetting(key: String, value: String) {
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-2120666198065087/7475733865", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    private fun showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) {
                saveCharacterSelection()
                Toast.makeText(this, "Karakteriniz başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Reklam henüz hazır değil. Lütfen biraz bekleyin.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCharacterSelection() {
        with(sharedPref.edit()) {
            putInt("selected_character_color", selectedCharacterColor)
            apply()
        }
    }

    // Google Play Billing Metotları
    private fun buyPremiumCharacter() {
        if (!billingClient.isReady) {
            Toast.makeText(this, "Faturalandırma hizmeti henüz hazır değil. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
            switchPremiumCharacter.isChecked = false
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_character_pack")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                billingClient.launchBillingFlow(this, flowParams)
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Ürün bilgileri yüklenemedi. Lütfen internet bağlantınızı kontrol edin.", Toast.LENGTH_LONG).show()
                    switchPremiumCharacter.isChecked = false
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            for (purchase in purchases) {
                if (purchase.products.contains("premium_character_pack")) {
                    with(sharedPref.edit()) {
                        putBoolean("hasCharactersPack", true)
                        apply()
                    }
                    switchPremiumCharacter.isChecked = true
                    Toast.makeText(this, "Premium karakter satın alındı!", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Satın alma işlemi iptal edildi veya başarısız oldu.", Toast.LENGTH_LONG).show()
            switchPremiumCharacter.isChecked = false
        }
    }

    private fun queryPurchaseHistory() {
        if (!billingClient.isReady) {
            Log.e("Billing", "queryPurchaseHistory çağrıldığında BillingClient henüz hazır değildi.")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        if (purchase.products.contains("premium_character_pack")) {
                            with(sharedPref.edit()) {
                                putBoolean("hasCharactersPack", true)
                                apply()
                            }
                            switchPremiumCharacter.isChecked = true
                        }
                    }
                }
            }
        }
    }
}