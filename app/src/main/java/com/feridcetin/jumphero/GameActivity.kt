package com.feridcetin.jumphero

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // TEST REKLAM BİRİMİ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // GameView'i oluştur ve içeriğin kökü olarak ayarla
        gameView = GameView(this)
        setContentView(gameView)

        // AdMob'u başlat ve reklamı yükle
        MobileAds.initialize(this) {}
        loadRewardedAd()

        // Yeni OnBackPressedDispatcher yapısı
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Eğer oyun duraklatılmamışsa, duraklat
                if (!gameView.getPaused()) {
                    gameView.setPaused(true)
                } else {
                    // Oyun duraklatılmışsa, varsayılan geri tuşu davranışını etkinleştir ve çıkış yap
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    // Ödüllü reklamı yükleme metodu
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    // Reklamı gösterme metodu
    fun showRewardedAd() {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Kullanıcı reklamı kapattığında yeni reklamı yükle
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    // Oyun duraklatılmışsa devam etmesini sağla
                    if (gameView.getPaused()) {
                        gameView.setPaused(false)
                    }
                }
            }
            // Reklamı göster ve ödülü ver
            ad.show(this) { rewardItem ->
                // Ödül verildiğinde GameView içindeki metodu çağır
                gameView.grantLifeAndShowResumeDialog()
            }
        }
    }
}