package com.feridcetin.jumphero

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 3000 // 3 saniye
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoaded = false
    private var isSplashTimeFinished = false
    private val handler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        MobileAds.initialize(this) {} // AdMob'u başlat

        loadInterstitialAd() // Reklamı yüklemeye başla

        // 3 saniyelik gecikmeyi başlat
        handler.postDelayed({
            isSplashTimeFinished = true
            checkAndNavigate()
        }, SPLASH_DELAY)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdMob", "Splash Interstitial Ad failed to load: ${adError.message}")
                interstitialAd = null
                isAdLoaded = true // Reklam yüklenemese bile zamanı dolmuş say
                checkAndNavigate()
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("AdMob", "Splash Interstitial Ad was loaded.")
                interstitialAd = ad
                isAdLoaded = true
                checkAndNavigate()

                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdMob", "Splash Interstitial Ad was dismissed.")
                        interstitialAd = null
                        navigateToMain()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("AdMob", "Splash Interstitial Ad failed to show.")
                        interstitialAd = null
                        navigateToMain()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("AdMob", "Splash Interstitial Ad showed.")
                    }
                }
            }
        })
    }

    private fun checkAndNavigate() {
        // Eğer hem süre dolduysa hem de reklam yüklendiyse (veya yüklenemediği anlaşıldıysa)
        if (isSplashTimeFinished && isAdLoaded) {
            if (interstitialAd != null) {
                // Reklam varsa göster
                interstitialAd?.show(this)
            } else {
                // Reklam yoksa doğrudan ana menüye git
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // SplashActivity'yi kapat
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Handler'ı temizle
    }
}