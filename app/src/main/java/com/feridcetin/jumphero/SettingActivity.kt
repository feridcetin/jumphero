package com.feridcetin.jumphero

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
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
            findViewById(R.id.btnGreen)/*,
            findViewById(R.id.btnYellow),
            findViewById(R.id.btnCyan),
            findViewById(R.id.btnMagenta),
            findViewById(R.id.btnOrange),
            findViewById(R.id.btnPurple),
            findViewById(R.id.btnLime),
            findViewById(R.id.btnPink),
            findViewById(R.id.btnBrown),
            findViewById(R.id.btnTeal)*/
        )

        loadSettings()

        // Dil butonu tıklama olayları
        btnLangEn.setOnClickListener {
            saveStringSetting("language", "en")
            updateLanguageButtons()
        }
        btnLangTr.setOnClickListener {
            saveStringSetting("language", "tr")
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
                  /*  R.id.btnYellow -> selectedCharacterColor = Color.YELLOW
                    R.id.btnCyan -> selectedCharacterColor = Color.CYAN
                    R.id.btnMagenta -> selectedCharacterColor = Color.MAGENTA
                    R.id.btnOrange -> selectedCharacterColor = Color.parseColor("#FFA500") // Orange
                    R.id.btnPurple -> selectedCharacterColor = Color.parseColor("#800080") // Purple
                    R.id.btnLime -> selectedCharacterColor = Color.parseColor("#00FF00") // Lime (Bright Green)
                    R.id.btnPink -> selectedCharacterColor = Color.parseColor("#FFC0CB") // Pink
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
             /*   R.id.btnYellow -> if (selectedCharacterColor == Color.YELLOW) button.elevation = 10f
                R.id.btnCyan -> if (selectedCharacterColor == Color.CYAN) button.elevation = 10f
                R.id.btnMagenta -> if (selectedCharacterColor == Color.MAGENTA) button.elevation = 10f
                R.id.btnOrange -> if (selectedCharacterColor == Color.parseColor("#FFA500")) button.elevation = 10f
                R.id.btnPurple -> if (selectedCharacterColor == Color.parseColor("#800080")) button.elevation = 10f
                R.id.btnLime -> if (selectedCharacterColor == Color.parseColor("#00FF00")) button.elevation = 10f
                R.id.btnPink -> if (selectedCharacterColor == Color.parseColor("#FFC0CB")) button.elevation = 10f
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
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
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
}