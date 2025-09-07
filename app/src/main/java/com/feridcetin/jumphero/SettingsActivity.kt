package com.feridcetin.jumphero

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.content.ContextWrapper

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("appLanguage", "en") ?: "en"
        val localeToSwitch = Locale(languageCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(localeToSwitch)
        val newContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(newContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val buttonEnglish: Button = findViewById(R.id.buttonEnglish)
        val buttonTurkish: Button = findViewById(R.id.buttonTurkish)

        buttonEnglish.setOnClickListener {
            setLocale("en")
        }

        buttonTurkish.setOnClickListener {
            setLocale("tr")
        }
    }

    private fun setLocale(languageCode: String) {
        val sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("appLanguage", languageCode)
            apply()
        }
        recreate() // Activity'yi yeniden oluşturarak dilin hemen uygulanmasını sağlar.
    }
}