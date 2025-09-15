package com.feridcetin.jumphero

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MusicSettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var rgBgMusic: RadioGroup
    private lateinit var rgWinMusic: RadioGroup
    private lateinit var rgLoseMusic: RadioGroup
    private lateinit var switchBgMusic: Switch
    private lateinit var switchWinMusic: Switch
    private lateinit var switchLoseMusic: Switch
    private lateinit var btnSaveMusic: Button

    private val loseMusicIdMap = mapOf(
        R.id.rb_lose_music1 to R.raw.lose1,
        R.id.rb_lose_music2 to R.raw.lose2
    )

    private val winMusicIdMap = mapOf(
        R.id.rb_win_music1 to R.raw.win1,
        R.id.rb_win_music2 to R.raw.win2
    )

    private val bgMusicIdMap = mapOf(
        R.id.rb_bg_music1 to R.raw.bg1,
        R.id.rb_bg_music2 to R.raw.bg2
    )

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

        rgWinMusic.setOnCheckedChangeListener { group, checkedId ->
            val winMusicResourceId = winMusicIdMap[checkedId]
            if (winMusicResourceId != null) {
                playMusic(winMusicResourceId)
            }
        }

        rgLoseMusic.setOnCheckedChangeListener { group, checkedId ->
            val loseMusicResourceId = loseMusicIdMap[checkedId]
            if (loseMusicResourceId != null) {
                playMusic(loseMusicResourceId)
            }
        }

        rgBgMusic.setOnCheckedChangeListener { group, checkedId ->
            val bgMusicResourceId = bgMusicIdMap[checkedId]
            if (bgMusicResourceId != null) {
                playMusic(bgMusicResourceId)
            }
        }

        btnSaveMusic.setOnClickListener {
            saveMusicSettings()
            Toast.makeText(this, R.string.music_settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun playMusic(resourceId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer?.start()
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

        val selectedBgMusicId = sharedPref.getInt("selected_bg_music", R.raw.bg1)
        val selectedWinMusicId = sharedPref.getInt("selected_win_music", R.raw.win1)
        val selectedLoseMusicId = sharedPref.getInt("selected_lose_music", R.raw.lose1)

        val selectedBgRadioId = bgMusicIdMap.entries.firstOrNull { it.value == selectedBgMusicId }?.key
        if (selectedBgRadioId != null) {
            rgBgMusic.check(selectedBgRadioId)
        }

        val selectedWinRadioId = winMusicIdMap.entries.firstOrNull { it.value == selectedWinMusicId }?.key
        if (selectedWinRadioId != null) {
            rgWinMusic.check(selectedWinRadioId)
        }

        val selectedLoseRadioId = loseMusicIdMap.entries.firstOrNull { it.value == selectedLoseMusicId }?.key
        if (selectedLoseRadioId != null) {
            rgLoseMusic.check(selectedLoseRadioId)
        }
    }

    private fun saveMusicSettings() {
        with(sharedPref.edit()) {
            val selectedBgMusicId = bgMusicIdMap[rgBgMusic.checkedRadioButtonId]
            if (selectedBgMusicId != null) {
                putInt("selected_bg_music", selectedBgMusicId)
            }

            val selectedWinMusicId = winMusicIdMap[rgWinMusic.checkedRadioButtonId]
            if (selectedWinMusicId != null) {
                putInt("selected_win_music", selectedWinMusicId)
            }

            val selectedLoseMusicId = loseMusicIdMap[rgLoseMusic.checkedRadioButtonId]
            if (selectedLoseMusicId != null) {
                putInt("selected_lose_music", selectedLoseMusicId)
            }

            putBoolean("bg_music_enabled", switchBgMusic.isChecked)
            putBoolean("win_music_enabled", switchWinMusic.isChecked)
            putBoolean("lose_music_enabled", switchLoseMusic.isChecked)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}