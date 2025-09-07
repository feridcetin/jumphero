package com.feridcetin.jumphero

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Locale
import android.content.res.Configuration
import android.content.ContextWrapper

class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null

    // Dil ayarını uygulamadan önce ayarlar.
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
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        if (!isAdFree()) {
            loadInterstitialAd()
        }

        val playButton: Button = findViewById(R.id.playButton)
        val shopButton: Button = findViewById(R.id.shopButton)
        val settingsButton: Button = findViewById(R.id.settingsButton)

        playButton.setOnClickListener {
            if (!isAdFree()) {
                showInterstitialAd()
            } else {
                val intent = Intent(this, GameActivity::class.java)
                startActivity(intent)
            }
        }

        shopButton.setOnClickListener {
            val intent = Intent(this, ShopActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            mInterstitialAd = null
                        }
                    }
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isAdFree(): Boolean {
        val sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("isAdFree", false)
    }
}