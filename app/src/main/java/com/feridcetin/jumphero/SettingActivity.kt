package com.feridcetin.jumphero

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class SettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var mRewardedAd: RewardedAd? = null

    private lateinit var btnLangEn: Button
    private lateinit var btnLangTr: Button
    private lateinit var switchPremiumCharacter: Switch
    private lateinit var switchAdvancedTheme: Switch
    private lateinit var btnSaveCharacter: Button

    private var selectedCharacterColor: Int = R.drawable.rounded_button_red
    private lateinit var characterButtons: List<ImageButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        MobileAds.initialize(this) {}
        loadRewardedAd()

        sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)

        btnLangEn = findViewById(R.id.btn_lang_en)
        btnLangTr = findViewById(R.id.btn_lang_tr)
        switchPremiumCharacter = findViewById(R.id.switch_premium_character)
        switchAdvancedTheme = findViewById(R.id.switch_advanced_theme)
        btnSaveCharacter = findViewById(R.id.btnSaveCharacter)

        characterButtons = listOf(
            findViewById(R.id.btnRed),
            findViewById(R.id.btnBlue),
            findViewById(R.id.btnGreen),
            findViewById(R.id.btnYellow),
            findViewById(R.id.btnOrange),
            findViewById(R.id.btnPink),
            findViewById(R.id.btnTurquoise),
            findViewById(R.id.btnWhite),
            findViewById(R.id.btnBlack)/*,
            findViewById(R.id.btnPink),
            findViewById(R.id.btnBrown),
            findViewById(R.id.btnTeal)*/
        )

        loadSettings()

        // Dil butonu tıklama olayları
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

        // Tema ve Karakter anahtarı tıklama olayları
        switchPremiumCharacter.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("hasCharactersPack", isChecked)
        }
        switchAdvancedTheme.setOnCheckedChangeListener { _, isChecked ->
            saveBooleanSetting("hasAdvancedTheme", isChecked)
        }

        // Karakter butonu tıklama olayları
        characterButtons.forEach { button ->
            button.setOnClickListener {
                when (it.id) {
                    R.id.btnRed -> selectedCharacterColor = R.drawable.rounded_button_red
                    R.id.btnBlue -> selectedCharacterColor = R.drawable.rounded_button_blue
                    R.id.btnGreen -> selectedCharacterColor = R.drawable.rounded_button_green
                    R.id.btnYellow -> selectedCharacterColor = R.drawable.rounded_button_yellow
                    R.id.btnOrange -> selectedCharacterColor = R.drawable.rounded_button_orange
                    R.id.btnPink -> selectedCharacterColor = R.drawable.rounded_button_pink
                    R.id.btnTurquoise -> selectedCharacterColor = R.drawable.rounded_button_turquoise
                    R.id.btnWhite -> selectedCharacterColor = R.drawable.rounded_button_white
                    R.id.btnBlack -> selectedCharacterColor = R.drawable.rounded_button_black
                 /*     R.id.btnPink -> selectedCharacterColor = Color.parseColor("#FFC0CB") // Pink
                    R.id.btnBrown -> selectedCharacterColor = Color.parseColor("#A52A2A") // Brown
                    R.id.btnTeal -> selectedCharacterColor = Color.parseColor("#008080") // Teal*/
                }
                updateCharacterSelectionUI()
            }
        }

        // Kaydet butonu tıklama olayı
        btnSaveCharacter.setOnClickListener {
            showRewardedAd()
        }
    }

    private fun loadSettings() {
        updateLanguageButtons()
        switchPremiumCharacter.isChecked = sharedPref.getBoolean("hasCharactersPack", false)
        switchAdvancedTheme.isChecked = sharedPref.getBoolean("hasAdvancedTheme", false)
        selectedCharacterColor = sharedPref.getInt("selected_character_color", R.drawable.rounded_button_red)
        updateCharacterSelectionUI()
    }

    private fun updateLanguageButtons() {
        val savedLanguage = sharedPref.getString("language", "en")
        btnLangEn.setBackgroundColor(if (savedLanguage == "en") Color.parseColor("#4CAF50") else Color.LTGRAY)
        btnLangTr.setBackgroundColor(if (savedLanguage == "tr") Color.parseColor("#4CAF50") else Color.LTGRAY)
    }

    private fun updateCharacterSelectionUI() {
        characterButtons.forEach { button ->
            button.elevation = 0f
            when (button.id) {
                R.id.btnRed -> if (selectedCharacterColor == R.drawable.rounded_button_red) button.elevation = 10f
                R.id.btnBlue -> if (selectedCharacterColor ==R.drawable.rounded_button_blue) button.elevation = 10f
                R.id.btnGreen -> if (selectedCharacterColor == R.drawable.rounded_button_green) button.elevation = 10f
                R.id.btnYellow -> if (selectedCharacterColor == R.drawable.rounded_button_yellow) button.elevation = 10f
                R.id.btnOrange -> if (selectedCharacterColor == R.drawable.rounded_button_orange) button.elevation = 10f
                R.id.btnPink -> if (selectedCharacterColor == R.drawable.rounded_button_pink) button.elevation = 10f
                R.id.btnTurquoise -> if (selectedCharacterColor ==  R.drawable.rounded_button_turquoise) button.elevation = 10f
                R.id.btnWhite -> if (selectedCharacterColor == R.drawable.rounded_button_white) button.elevation = 10f
                R.id.btnBlack -> if (selectedCharacterColor == R.drawable.rounded_button_black) button.elevation = 10f
              /*    R.id.btnPink -> if (selectedCharacterColor == Color.parseColor("#FFC0CB")) button.elevation = 10f
                R.id.btnBrown -> if (selectedCharacterColor == Color.parseColor("#A52A2A")) button.elevation = 10f
                R.id.btnTeal -> if (selectedCharacterColor == Color.parseColor("#008080")) button.elevation = 10f*/
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

        val selectedCharacterColor_Log = sharedPref.getInt("selected_character_color", R.drawable.character_default)

        //Log.e("SettingActivity", "selected_character_color_Log= ${selectedCharacterColor_Log}, selectedCharacterColor= ${selectedCharacterColor}")
    }
}