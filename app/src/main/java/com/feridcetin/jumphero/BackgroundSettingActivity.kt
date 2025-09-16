package com.feridcetin.jumphero

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BackgroundSettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var rgBackgroundImages: RadioGroup
    private lateinit var btnSaveBackground: Button
    private lateinit var ivSelectedBackgroundPreview: ImageView // Önizleme için ImageView

    // Arka plan resim ID'lerini ve RadioButton ID'lerini eşleştiren bir Map
    private val backgroundIdMap = mapOf(
        R.id.rb_background1 to R.drawable.background_image1, // Kendi drawable kaynaklarınızı buraya ekleyin
        R.id.rb_background2 to R.drawable.background_image2,
        R.id.rb_background3 to R.drawable.background_image3,
        R.id.rb_background4 to R.drawable.background_image4,
        R.id.rb_background5 to R.drawable.background_image5
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_setting)

        sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)

        rgBackgroundImages = findViewById(R.id.rg_background_images)
        btnSaveBackground = findViewById(R.id.btn_save_background)
        ivSelectedBackgroundPreview = findViewById(R.id.iv_selected_background_preview)

        loadBackgroundSettings()

        rgBackgroundImages.setOnCheckedChangeListener { group, checkedId ->
            val selectedBackgroundResId = backgroundIdMap[checkedId]
            if (selectedBackgroundResId != null) {
                ivSelectedBackgroundPreview.setImageResource(selectedBackgroundResId)
            }
        }

        btnSaveBackground.setOnClickListener {
            saveBackgroundSettings()
            Toast.makeText(this, R.string.background_settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBackgroundSettings() {
        // SharedPreferences'tan kaydedilmiş arka plan resminin kaynak ID'sini al
        val selectedBackgroundResId = sharedPref.getInt("selected_background", R.drawable.background_image1) // Varsayılan bir arka plan resmi belirleyin

        // Kaydedilen kaynak ID'sine karşılık gelen RadioButton ID'sini bul
        val selectedRadioButtonId = backgroundIdMap.entries.firstOrNull { it.value == selectedBackgroundResId }?.key

        if (selectedRadioButtonId != null) {
            rgBackgroundImages.check(selectedRadioButtonId)
            ivSelectedBackgroundPreview.setImageResource(selectedBackgroundResId)
        } else {
            // Eğer kaydedilen resim bulunamazsa varsayılanı seç ve önizle
            rgBackgroundImages.check(R.id.rb_background1)
            ivSelectedBackgroundPreview.setImageResource(R.drawable.background_image1)
        }
    }

    private fun saveBackgroundSettings() {
        val checkedRadioButtonId = rgBackgroundImages.checkedRadioButtonId
        val selectedBackgroundResId = backgroundIdMap[checkedRadioButtonId]

        if (selectedBackgroundResId != null) {
            with(sharedPref.edit()) {
                putInt("selected_background", selectedBackgroundResId)
                apply()
            }
        }
        //Log.e("GameView", "BackgroundSettingActivity= ${selectedBackgroundResId}")
    }
}