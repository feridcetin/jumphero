package com.feridcetin.jumphero


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var playButton: Button
    private lateinit var settingsButton: Button
    private lateinit var exitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playButton = findViewById(R.id.btn_play)
        settingsButton = findViewById(R.id.btn_settings)
        exitButton = findViewById(R.id.btn_exit)

        // Buton tıklama dinleyicilerini ayarlama
        playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            // Yeni bir oyun seansı başlattığımızı GameActivity'ye bildiriyoruz
            intent.putExtra("isFirstGameSession", true)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        exitButton.setOnClickListener {
            finishAffinity()
        }
    }
}