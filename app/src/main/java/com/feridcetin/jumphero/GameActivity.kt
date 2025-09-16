package com.feridcetin.jumphero

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private var mRewardedAd: RewardedAd? = null

    private lateinit var adViewTop: AdView
    private lateinit var adViewBottom: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        MobileAds.initialize(this) {}

        adViewTop = findViewById(R.id.adViewTop)
        val adRequestTop = AdRequest.Builder().build()
        adViewTop.loadAd(adRequestTop)

        adViewBottom = findViewById(R.id.adViewBottom)
        val adRequestBottom = AdRequest.Builder().build()
        adViewBottom.loadAd(adRequestBottom)

        gameView = GameView(this)
        findViewById<FrameLayout>(R.id.gameContainer).addView(gameView)

        loadRewardedAd()

        // Geri tuşu hareketleri için OnBackPressedCallback ekleme
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (gameView.getPaused()) {
                    // Oyun duraklatılmışsa, geri tuşuna basıldığında ana menüye dön
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    // Oyunu duraklat
                    gameView.setPaused(true)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    fun showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) { rewardItem ->
                gameView.grantLifeAndShowResumeDialog()
                loadRewardedAd()
            }
        } else {
            gameView.grantLifeAndShowResumeDialog()
            loadRewardedAd()
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        adViewTop.pause()
        adViewBottom.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        adViewTop.resume()
        adViewBottom.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        adViewTop.destroy()
        adViewBottom.destroy()
    }

    fun saveHighScore(score: Int) {
        val sharedPref = getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        val currentHighScore = sharedPref.getInt("high_score", 0)

        if (score > currentHighScore) {
            with(sharedPref.edit()) {
                putInt("high_score", score)
                apply()
            }
        }
    }
}