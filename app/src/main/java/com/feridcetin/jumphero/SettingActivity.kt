package com.feridcetin.jumphero

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {

    private lateinit var langEnButton: Button
    private lateinit var langTrButton: Button
    private lateinit var premiumCharacterSwitch: Switch
    private lateinit var advancedThemeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // UI bileşenlerini bağlama
        langEnButton = findViewById(R.id.btn_lang_en)
        langTrButton = findViewById(R.id.btn_lang_tr)
        premiumCharacterSwitch = findViewById(R.id.switch_premium_character)
        advancedThemeSwitch = findViewById(R.id.switch_advanced_theme)

        val sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)

        // Premium karakter ayarını yükleme ve dinleyici ekleme
        val hasCharactersPack = sharedPref.getBoolean("hasCharactersPack", false)
        premiumCharacterSwitch.isChecked = hasCharactersPack
        premiumCharacterSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("hasCharactersPack", isChecked).apply()
        }

        // Gelişmiş tema ayarını yükleme ve dinleyici ekleme
        val hasAdvancedTheme = sharedPref.getBoolean("hasAdvancedTheme", false)
        advancedThemeSwitch.isChecked = hasAdvancedTheme
        advancedThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("hasAdvancedTheme", isChecked).apply()
        }

        // Dil değiştirme butonları
        langEnButton.setOnClickListener {
            // Dil ayarını yapar ve uygulamayı yeniden başlatır
            LocaleHelper.setLocaleAndRestart(this, "en")
        }

        langTrButton.setOnClickListener {
            // Dil ayarını yapar ve uygulamayı yeniden başlatır
            LocaleHelper.setLocaleAndRestart(this, "tr")
        }
    }

    // Uygulama açılışında dil ayarlarını uygulamak için
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }
}