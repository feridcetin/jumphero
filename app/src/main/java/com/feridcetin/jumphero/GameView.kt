package com.feridcetin.jumphero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private var isGameOver = false
    private var isReady = false
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var score: Int = 0
    private var lives: Int = 3

    private var level: Int = 1
    private val scoreToLevelUp: Int = 15
    private var previousScoreForLevel: Int = 0
    private var obstacleSpeed: Float = 10f

    private var isPaused = false
    private var isBonusLifeGiven: Boolean = false

    private val baseObstacleHeight: Float = 200f
    private val heightIncreasePerLevel: Float = 20f

    private val obstaclePaint = Paint()
    private val scoreTextPaint = Paint()
    private val levelTextPaint = Paint()
    private val bottomBoundaryPaint = Paint()

    private val pauseOverlayPaint = Paint().apply { color = Color.BLACK; alpha = 150 }

    private var characterY: Float = 0f
    private var characterVelocity: Float = 0f
    private val gravity: Float = 1f
    private val jumpPower: Float = -15f
    private val obstacles = CopyOnWriteArrayList<Obstacle>()
    private val obstacleGap = 600f
    private val obstacleSpacing = 600f
    private val initialObstacleCount = 5
    private val random = Random()

    private val characterRect = RectF()
    private val characterSize = 100f
    private val characterCollisionRadius = characterSize / 2

    private lateinit var characterBitmap: Bitmap
    private val lifeIconSize = 80
    private val lifeIconMargin = 10f

    private lateinit var backgroundBitmapOriginal: Bitmap
    private lateinit var backgroundBitmapScaled: Bitmap
    private var backgroundX1: Float = 0f
    private var backgroundX2: Float = 0f

    private lateinit var pauseIconBitmap: Bitmap
    private lateinit var playIconBitmap: Bitmap
    private val iconSize = 150

    private lateinit var scoreIconBitmap: Bitmap
    private lateinit var levelIconBitmap: Bitmap
    private val uiIconSize = 80f

    init {
        holder.addCallback(this)

        val sharedPref = context.getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        val hasCharactersPack = sharedPref.getBoolean("hasCharactersPack", false)
        val hasAdvancedTheme = sharedPref.getBoolean("hasAdvancedTheme", false)

        val characterResId = if (hasCharactersPack) R.drawable.character_premium else R.drawable.character_default
        characterBitmap = BitmapFactory.decodeResource(resources, characterResId)
        characterBitmap = Bitmap.createScaledBitmap(characterBitmap, characterSize.toInt(), characterSize.toInt(), true)

        try {
            backgroundBitmapOriginal = BitmapFactory.decodeResource(resources, R.drawable.background)
        } catch (e: Exception) {
            Log.e("GameView", "Arka plan resmi yüklenemedi: background.png dosyasını kontrol edin.", e)
        }

        scoreTextPaint.color = Color.parseColor("#FFD700")
        scoreTextPaint.textSize = 80f
        scoreTextPaint.isFakeBoldText = true

        levelTextPaint.color = Color.WHITE
        levelTextPaint.textSize = 80f
        levelTextPaint.isFakeBoldText = true

        bottomBoundaryPaint.color = Color.parseColor("#FFD700")

        val originalPauseIcon = BitmapFactory.decodeResource(resources, R.drawable.pause_icon24x24)
        pauseIconBitmap = Bitmap.createScaledBitmap(originalPauseIcon, iconSize, iconSize, true)

        val originalPlayIcon = BitmapFactory.decodeResource(resources, R.drawable.play_icon256x256)
        playIconBitmap = Bitmap.createScaledBitmap(originalPlayIcon, iconSize, iconSize, true)

        val originalScoreIcon = BitmapFactory.decodeResource(resources, R.drawable.scor_icon24x24)
        scoreIconBitmap = Bitmap.createScaledBitmap(originalScoreIcon, uiIconSize.toInt(), uiIconSize.toInt(), true)

        val originalLevelIcon = BitmapFactory.decodeResource(resources, R.drawable.level_icon24x24)
        levelIconBitmap = Bitmap.createScaledBitmap(originalLevelIcon, uiIconSize.toInt(), uiIconSize.toInt(), true)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = Thread(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        characterY = (screenHeight / 2).toFloat()
        createInitialObstacles()

        if (::backgroundBitmapOriginal.isInitialized) {
            val aspectRatio = backgroundBitmapOriginal.width.toFloat() / backgroundBitmapOriginal.height.toFloat()
            val scaledWidth = (screenHeight * aspectRatio).toInt()
            backgroundBitmapScaled = Bitmap.createScaledBitmap(backgroundBitmapOriginal, scaledWidth, screenHeight, true)
            backgroundX2 = backgroundBitmapScaled.width.toFloat()
        }

        isReady = true
        if (!isPlaying) {
            isPlaying = true
            thread?.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        while (isPlaying) {
            if (!isPaused) {
                update()
            }
            draw()
            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun update() {
        if (!isReady || isGameOver) return

        val backgroundScrollSpeed = 2f
        backgroundX1 -= backgroundScrollSpeed
        backgroundX2 -= backgroundScrollSpeed

        if (::backgroundBitmapScaled.isInitialized) {
            if (backgroundX1 < -backgroundBitmapScaled.width) {
                backgroundX1 = backgroundX2 + backgroundBitmapScaled.width
            }
            if (backgroundX2 < -backgroundBitmapScaled.width) {
                backgroundX2 = backgroundX1 + backgroundBitmapScaled.width
            }
        }

        characterVelocity += gravity
        characterY += characterVelocity

        val characterX = (screenWidth / 4).toFloat()
        characterRect.set(
            characterX - characterCollisionRadius,
            characterY - characterCollisionRadius,
            characterX + characterCollisionRadius,
            characterY + characterCollisionRadius
        )

        var collisionOccurred = false
        if (characterY + characterCollisionRadius >= screenHeight || characterY - characterCollisionRadius <= 0) {
            collisionOccurred = true
        } else {
            for (obstacle in obstacles) {
                val obstacleRect = RectF(obstacle.x, obstacle.top, obstacle.x + obstacle.width, obstacle.bottom)
                if (characterRect.intersect(obstacleRect)) {
                    collisionOccurred = true
                    break
                }
            }
        }

        if (collisionOccurred) {
            lives--
            if (lives <= 0) {
                isGameOver = true
                isPlaying = false
                showGameOverDialog()
            } else {
                resetCharacterAndObstacles()
            }
            return
        }

        for (obstacle in obstacles) {
            obstacle.x -= obstacleSpeed
            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(obstacle)
                addNewObstacle()
                score++

                if (score > previousScoreForLevel && score % scoreToLevelUp == 0) {
                    level++
                    previousScoreForLevel = score
                    obstacleSpeed += 1f
                    isBonusLifeGiven = false
                }
            }
        }

        if (level > 0 && level % 3 == 0 && !isBonusLifeGiven) {
            lives++
            isBonusLifeGiven = true
        }
    }

    private fun draw() {
        if (!isReady) return

        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()

            if (::backgroundBitmapScaled.isInitialized) {
                canvas.drawBitmap(backgroundBitmapScaled, backgroundX1, 0f, null)
                canvas.drawBitmap(backgroundBitmapScaled, backgroundX2, 0f, null)
            } else {
                canvas.drawColor(Color.BLUE)
            }

            for (obstacle in obstacles) {
                obstaclePaint.color = obstacle.color
                canvas.drawRect(obstacle.x, obstacle.top, obstacle.x + obstacle.width, obstacle.bottom, obstaclePaint)
            }

            for (i in 0 until lives) {
                val left = 20f + i * (lifeIconSize + lifeIconMargin)
                val top = 20f
                val right = left + lifeIconSize
                val bottom = top + lifeIconSize
                val destRect = RectF(left, top, right, bottom)
                canvas.drawBitmap(characterBitmap, null, destRect, null)
            }

            val charDrawX = (screenWidth / 4).toFloat() - characterSize / 2
            val charDrawY = characterY - characterSize / 2
            canvas.drawBitmap(characterBitmap, charDrawX, charDrawY, null)

            val scoreIconRight = screenWidth.toFloat() - 20f
            val scoreIconLeft = scoreIconRight - uiIconSize
            val scoreIconTop = 30f
            canvas.drawBitmap(scoreIconBitmap, scoreIconLeft, scoreIconTop, null)

            val scoreText = "$score"
            val scoreTextWidth = scoreTextPaint.measureText(scoreText)
            canvas.drawText(scoreText, scoreIconLeft - scoreTextWidth - 10f, scoreIconTop + uiIconSize / 2 + scoreTextPaint.textSize / 3, scoreTextPaint)

            val levelIconRight = screenWidth.toFloat() - 20f
            val levelIconLeft = levelIconRight - uiIconSize
            val levelIconTop = 130f
            canvas.drawBitmap(levelIconBitmap, levelIconLeft, levelIconTop, null)

            val levelText = "$level"
            val levelTextWidth = levelTextPaint.measureText(levelText)
            canvas.drawText(levelText, levelIconLeft - levelTextWidth - 10f, levelIconTop + uiIconSize / 2 + levelTextPaint.textSize / 3, levelTextPaint)

            canvas.drawRect(0f, screenHeight.toFloat() - 20f, screenWidth.toFloat(), screenHeight.toFloat(), bottomBoundaryPaint)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), 20f, bottomBoundaryPaint)

            if (isPaused) {
                canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), pauseOverlayPaint)

                val iconDrawX = screenWidth / 2f - iconSize / 2f
                val iconDrawY = screenHeight / 2f - iconSize / 2f
                canvas.drawBitmap(playIconBitmap, iconDrawX, iconDrawY, null)
            }

            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isPaused) {
                setPaused(false)
                return true
            }
            else if (!isGameOver) {
                characterVelocity = jumpPower
            }
        }
        return true
    }

    private fun createInitialObstacles() {
        obstacles.clear()
        for (i in 0 until initialObstacleCount) {
            val randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            val currentObstacleHeight = baseObstacleHeight + (level - 1) * heightIncreasePerLevel
            val obstacleY = random.nextFloat() * (screenHeight - 400) + 200
            val obstacle = Obstacle(screenWidth.toFloat() + obstacleSpacing * i, obstacleY, randomColor, currentObstacleHeight)
            obstacles.add(obstacle)
        }
    }

    private fun addNewObstacle() {
        val lastObstacle = obstacles.last()
        val randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        val currentObstacleHeight = baseObstacleHeight + (level - 1) * heightIncreasePerLevel
        val newObstacleY = random.nextFloat() * (screenHeight - 400) + 200
        val newObstacle = Obstacle(lastObstacle.x + obstacleSpacing, newObstacleY, randomColor, currentObstacleHeight)
        obstacles.add(newObstacle)
    }

    private fun resetCharacterAndObstacles() {
        characterY = (screenHeight / 2).toFloat()
        characterVelocity = 0f
        obstacles.clear()
        createInitialObstacles()
    }

    fun pause() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }

    fun showGameOverDialog() {
        (context as GameActivity).runOnUiThread {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
            val positiveButton = dialogView.findViewById<Button>(R.id.positive_button)
            val negativeButton = dialogView.findViewById<Button>(R.id.negative_button)

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            if (lives <= 0) {
                dialogTitle.text = context.getString(R.string.game_over_title)
                dialogMessage.text = context.getString(R.string.game_over_message, score)
                positiveButton.text = context.getString(R.string.yes_button)
                negativeButton.text = context.getString(R.string.no_button)

                positiveButton.setOnClickListener {
                    dialog.dismiss()
                    (context as GameActivity).showRewardedAd()
                }
                negativeButton.setOnClickListener {
                    dialog.dismiss()
                    (context as GameActivity).finish()
                }
            } else {
                dialogTitle.text = context.getString(R.string.game_over_title)
                dialogMessage.text = context.getString(R.string.game_over_message, score)
                positiveButton.text = context.getString(R.string.restart_button)
                negativeButton.text = context.getString(R.string.main_menu_button)

                positiveButton.setOnClickListener {
                    dialog.dismiss()
                    resetGame()
                }
                negativeButton.setOnClickListener {
                    dialog.dismiss()
                    (context as GameActivity).finish()
                }
            }
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    fun grantLifeAndShowResumeDialog() {
        Log.d("GameView", "Reklam izlendi, can hakkı verildi.")
        lives++
        showResumeDialog()
    }

    private fun showResumeDialog() {
        (context as GameActivity).runOnUiThread {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
            val positiveButton = dialogView.findViewById<Button>(R.id.positive_button)
            val negativeButton = dialogView.findViewById<Button>(R.id.negative_button)

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogTitle.text = context.getString(R.string.congratulations_title)
            dialogMessage.text = context.getString(R.string.congratulations_message)
            positiveButton.text = context.getString(R.string.ok_button)
            negativeButton.visibility = View.GONE

            positiveButton.setOnClickListener {
                dialog.dismiss()
                resetCharacterAndObstacles() // BU SATIR EKLENDİ!
                isGameOver = false
                setPaused(false)
                resume()
            }

            dialog.setCancelable(false)
            dialog.show()
        }
    }

    private fun showBonusLifeDialog() {
        (context as GameActivity).runOnUiThread {
            val builder = AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialog_message)
            val positiveButton = dialogView.findViewById<Button>(R.id.positive_button)
            val negativeButton = dialogView.findViewById<Button>(R.id.negative_button)

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogTitle.text = context.getString(R.string.congratulations_title)
            dialogMessage.text = context.getString(R.string.bonus_life_message, level)
            positiveButton.text = context.getString(R.string.ok_button)
            negativeButton.visibility = View.GONE

            positiveButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setCancelable(false)
            dialog.show()
        }
    }

    private fun resetGame() {
        score = 0
        lives = 3
        level = 1
        previousScoreForLevel = 0
        obstacleSpeed = 10f
        characterY = (screenHeight / 2).toFloat()
        characterVelocity = 0f
        obstacles.clear()
        createInitialObstacles()
        isGameOver = false
        isPlaying = true
        thread = Thread(this)
        thread?.start()
        isReady = true
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun getPaused(): Boolean {
        return isPaused
    }

    data class Obstacle(var x: Float, var y: Float, var color: Int, var height: Float) {
        val width = 100f
        val top = y - height / 2
        val bottom = y + height / 2
    }
}