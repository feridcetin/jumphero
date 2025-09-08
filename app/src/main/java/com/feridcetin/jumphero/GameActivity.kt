package com.feridcetin.jumphero


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        MobileAds.initialize(this) {}
        loadRewardedAd()

        val frameLayout = findViewById<FrameLayout>(R.id.game_container)
        gameView = GameView(this)
        frameLayout.addView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    // AdMob ödüllü reklamı yükler
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdMob", "Rewarded Ad failed to load: ${adError.message}")
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("AdMob", "Rewarded Ad was loaded.")
                rewardedAd = ad
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdMob", "Ad was dismissed.")
                        rewardedAd = null
                        loadRewardedAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("AdMob", "Ad failed to show.")
                        rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("AdMob", "Ad showed.")
                    }
                }
            }
        })
    }

    // `GameView` tarafından çağrılacak, reklamı gösterme metodu
    fun showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd?.show(this) { rewardItem ->
                // Reklam başarıyla izlendi, can hakkı ver ve yeni diyaloğu göster
                Log.d("AdMob", "User earned ${rewardItem.amount} ${rewardItem.type}")
                gameView.grantLifeAndShowResumeDialog()
            }
        } else {
            // Reklam yoksa, normal oyun bitiş diyaloğunu göster
            Log.d("AdMob", "Rewarded ad wasn't ready yet.")
            gameView.showGameOverDialog()
        }
    }
}