package com.feridcetin.jumphero

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScoreboardActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var textViewHighScore: TextView
    private lateinit var btnBackToMenu: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        textViewHighScore = findViewById(R.id.textViewHighScore)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)

        loadHighScore()

        btnBackToMenu.setOnClickListener {
            finish()
        }
    }

    private fun loadHighScore() {
        val highScore = sharedPref.getInt("high_score", 0)
        textViewHighScore.text = getString(R.string.high_score, highScore)

    }
}