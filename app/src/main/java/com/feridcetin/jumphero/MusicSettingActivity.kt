package com.feridcetin.jumphero

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MusicSettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    private lateinit var rgBgMusic: RadioGroup
    private lateinit var rgWinMusic: RadioGroup
    private lateinit var rgLoseMusic: RadioGroup
    private lateinit var switchBgMusic: Switch
    private lateinit var switchWinMusic: Switch
    private lateinit var switchLoseMusic: Switch
    private lateinit var btnSaveMusic: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_setting)

        sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)

        rgBgMusic = findViewById(R.id.rg_bg_music)
        rgWinMusic = findViewById(R.id.rg_win_music)
        rgLoseMusic = findViewById(R.id.rg_lose_music)
        switchBgMusic = findViewById(R.id.switch_bg_music)
        switchWinMusic = findViewById(R.id.switch_win_music)
        switchLoseMusic = findViewById(R.id.switch_lose_music)
        btnSaveMusic = findViewById(R.id.btn_save_music)

        loadMusicSettings()

        // Switch durumuna göre RadioGroup'ları etkinleştirme/devre dışı bırakma
        switchBgMusic.setOnCheckedChangeListener { _, isChecked ->
            rgBgMusic.isEnabled = isChecked
            for (i in 0 until rgBgMusic.childCount) {
                rgBgMusic.getChildAt(i).isEnabled = isChecked
            }
        }

        switchWinMusic.setOnCheckedChangeListener { _, isChecked ->
            rgWinMusic.isEnabled = isChecked
            for (i in 0 until rgWinMusic.childCount) {
                rgWinMusic.getChildAt(i).isEnabled = isChecked
            }
        }

        switchLoseMusic.setOnCheckedChangeListener { _, isChecked ->
            rgLoseMusic.isEnabled = isChecked
            for (i in 0 until rgLoseMusic.childCount) {
                rgLoseMusic.getChildAt(i).isEnabled = isChecked
            }
        }

        btnSaveMusic.setOnClickListener {
            saveMusicSettings()
            Toast.makeText(this, "Müzik ayarları kaydedildi!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadMusicSettings() {
        val bgMusicEnabled = sharedPref.getBoolean("bg_music_enabled", true)
        val winMusicEnabled = sharedPref.getBoolean("win_music_enabled", true)
        val loseMusicEnabled = sharedPref.getBoolean("lose_music_enabled", true)

        switchBgMusic.isChecked = bgMusicEnabled
        switchWinMusic.isChecked = winMusicEnabled
        switchLoseMusic.isChecked = loseMusicEnabled

        rgBgMusic.isEnabled = bgMusicEnabled
        for (i in 0 until rgBgMusic.childCount) {
            rgBgMusic.getChildAt(i).isEnabled = bgMusicEnabled
        }

        rgWinMusic.isEnabled = winMusicEnabled
        for (i in 0 until rgWinMusic.childCount) {
            rgWinMusic.getChildAt(i).isEnabled = winMusicEnabled
        }

        rgLoseMusic.isEnabled = loseMusicEnabled
        for (i in 0 until rgLoseMusic.childCount) {
            rgLoseMusic.getChildAt(i).isEnabled = loseMusicEnabled
        }

        val selectedBgMusicId = sharedPref.getInt("selected_bg_music", R.id.rb_bg_music1)
        val selectedWinMusicId = sharedPref.getInt("selected_win_music", R.id.rb_win_music1)
        val selectedLoseMusicId = sharedPref.getInt("selected_lose_music", R.id.rb_lose_music1)

        rgBgMusic.check(selectedBgMusicId)
        rgWinMusic.check(selectedWinMusicId)
        rgLoseMusic.check(selectedLoseMusicId)
    }

    private fun saveMusicSettings() {
        with(sharedPref.edit()) {
            putInt("selected_bg_music", rgBgMusic.checkedRadioButtonId)
            putInt("selected_win_music", rgWinMusic.checkedRadioButtonId)
            putInt("selected_lose_music", rgLoseMusic.checkedRadioButtonId)
            putBoolean("bg_music_enabled", switchBgMusic.isChecked)
            putBoolean("win_music_enabled", switchWinMusic.isChecked)
            putBoolean("lose_music_enabled", switchLoseMusic.isChecked)
            apply()
        }
    }
}