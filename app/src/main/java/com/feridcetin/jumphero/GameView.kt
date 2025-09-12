package com.feridcetin.jumphero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AlertDialog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.Random

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
    private val scoreToLevelUp: Int = 20
    private var previousScoreForLevel: Int = 0
    private var obstacleSpeed: Float = 10f

    // Bonus can için yeni değişken
    private var isBonusLifeGiven: Boolean = false

    // Engel yüksekliği değişkenleri
    private val baseObstacleHeight: Float = 200f // Temel engel boyu
    private val heightIncreasePerLevel: Float = 20f // Her seviyede artacak boy

    private val obstaclePaint = Paint()
    private val scorePaint = Paint()
    private val bottomBoundaryPaint = Paint()
    private val levelPaint = Paint()

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

    init {
        holder.addCallback(this)

        val sharedPref = context.getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        val hasCharactersPack = sharedPref.getBoolean("hasCharactersPack", false)
        val hasAdvancedTheme = sharedPref.getBoolean("hasAdvancedTheme", false)

        val characterResId = if (hasCharactersPack) R.drawable.character_premium else R.drawable.character_default
        characterBitmap = BitmapFactory.decodeResource(resources, characterResId)
        characterBitmap = Bitmap.createScaledBitmap(characterBitmap, characterSize.toInt(), characterSize.toInt(), true)

        backgroundBitmapOriginal = BitmapFactory.decodeResource(resources, R.drawable.background)

        scorePaint.color = Color.parseColor("#FFD700")
        bottomBoundaryPaint.color = Color.parseColor("#FFD700")

        scorePaint.textSize = 80f
        scorePaint.isFakeBoldText = true

        levelPaint.color = Color.WHITE
        levelPaint.textSize = 80f
        levelPaint.isFakeBoldText = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = Thread(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        characterY = (screenHeight / 2).toFloat()
        createInitialObstacles()

        val aspectRatio = backgroundBitmapOriginal.width.toFloat() / backgroundBitmapOriginal.height.toFloat()
        val scaledWidth = (screenHeight * aspectRatio).toInt()
        backgroundBitmapScaled = Bitmap.createScaledBitmap(backgroundBitmapOriginal, scaledWidth, screenHeight, true)

        backgroundX2 = backgroundBitmapScaled.width.toFloat()

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
            update()
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

        if (backgroundX1 < -backgroundBitmapScaled.width) {
            backgroundX1 = backgroundX2 + backgroundBitmapScaled.width
        }
        if (backgroundX2 < -backgroundBitmapScaled.width) {
            backgroundX2 = backgroundX1 + backgroundBitmapScaled.width
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

                // Seviye atlama kontrolü
                if (score > previousScoreForLevel && score % scoreToLevelUp == 0) {
                    level++
                    previousScoreForLevel = score
                    obstacleSpeed += 1f
                    isBonusLifeGiven = false // Yeni seviyede bonus can hakkını sıfırla
                }
            }
        }

        // Seviye 3 ve katlarında can ekleme
        if (level > 0 && level % 3 == 0 && !isBonusLifeGiven) {
            lives++
            isBonusLifeGiven = true // Canı verdikten sonra true yap ki tekrar verilmesin
            //showBonusLifeDialog()
        }
    }

    private fun draw() {
        if (!isReady) return

        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()

            canvas.drawBitmap(backgroundBitmapScaled, backgroundX1, 0f, null)
            canvas.drawBitmap(backgroundBitmapScaled, backgroundX2, 0f, null)

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

            val scoreText = "Skor: $score"
            val scoreTextWidth = scorePaint.measureText(scoreText)
            canvas.drawText(scoreText, screenWidth.toFloat() - scoreTextWidth - 50f, 100f, scorePaint)

            val levelText = "Seviye: $level"
            val levelTextWidth = levelPaint.measureText(levelText)
            canvas.drawText(levelText, screenWidth.toFloat() - levelTextWidth - 50f, 200f, levelPaint)

            canvas.drawRect(0f, screenHeight.toFloat() - 20f, screenWidth.toFloat(), screenHeight.toFloat(), bottomBoundaryPaint)
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), 20f, bottomBoundaryPaint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !isGameOver) {
            characterVelocity = jumpPower
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
            if (lives <= 0) {
                AlertDialog.Builder(context)
                    .setTitle("Oyun Bitti!")
                    .setMessage("Skorunuz: $score\n\nÖdüllü reklam izleyerek ekstra bir can kazanmak ister misin?")
                    .setPositiveButton("Evet") { dialog, which ->
                        dialog.dismiss()
                        (context as GameActivity).showRewardedAd()
                    }
                    .setNegativeButton("Hayır") { dialog, which ->
                        dialog.dismiss()
                        (context as GameActivity).finish()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                AlertDialog.Builder(context)
                    .setTitle("Oyun Bitti!")
                    .setMessage("Skorunuz: $score")
                    .setPositiveButton("Yeniden Başla") { dialog, which ->
                        dialog.dismiss()
                        resetGame()
                    }
                    .setNegativeButton("Ana Menü") { dialog, which ->
                        dialog.dismiss()
                        (context as GameActivity).finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    fun grantLifeAndShowResumeDialog() {
        Log.d("GameView", "Reklam izlendi, can hakkı verildi.")
        lives++
        showResumeDialog()
    }

    private fun showResumeDialog() {
        (context as GameActivity).runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Tebrikler!")
                .setMessage("Reklam izlendi, can hakkınız arttı. Oyuna devam etmek için Tamam'a basın.")
                .setPositiveButton("Tamam") { dialog, which ->
                    dialog.dismiss()
                    resumeGame()
                }
                .setCancelable(false)
                .show()
        }
    }

    // Yeni bonus can diyalog metodu
    private fun showBonusLifeDialog() {
        (context as GameActivity).runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Tebrikler!")
                .setMessage("Seviye ${level}'e ulaştığınız için bir bonus can kazandınız!")
                .setPositiveButton("Tamam", null)
                .setCancelable(false)
                .show()
        }
    }

    private fun resumeGame() {
        isGameOver = false
        isPlaying = true
        resetCharacterAndObstacles()
        thread = Thread(this)
        thread?.start()
        isReady = true
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

    data class Obstacle(var x: Float, var y: Float, var color: Int, var height: Float) {
        val width = 100f
        val top = y - height / 2
        val bottom = y + height / 2
    }
}