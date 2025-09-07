package com.feridcetin.jumphero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix // Bitmap'i ölçeklemek için eklendi
import android.graphics.Paint
import android.graphics.RectF
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

    private val obstaclePaint = Paint()
    private val scorePaint = Paint()
    private val bottomBoundaryPaint = Paint()

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

    private lateinit var backgroundBitmapOriginal: Bitmap // Orijinal arka plan resmini tutacak
    private lateinit var backgroundBitmapScaled: Bitmap  // Ölçeklenmiş arka plan resmini tutacak
    private var backgroundX1: Float = 0f
    private var backgroundX2: Float = 0f // İkinci arka plan için

    init {
        holder.addCallback(this)

        val sharedPref = context.getSharedPreferences("JumpHeroPrefs", Context.MODE_PRIVATE)
        val hasCharactersPack = sharedPref.getBoolean("hasCharactersPack", false)
        val hasAdvancedTheme = sharedPref.getBoolean("hasAdvancedTheme", false)

        val characterResId = if (hasCharactersPack) R.drawable.character_premium else R.drawable.character_default
        characterBitmap = BitmapFactory.decodeResource(resources, characterResId)
        characterBitmap = Bitmap.createScaledBitmap(characterBitmap, characterSize.toInt(), characterSize.toInt(), true)

        backgroundBitmapOriginal = BitmapFactory.decodeResource(resources, R.drawable.background) // Orijinal resmi yüklüyoruz

        if (hasAdvancedTheme) {
            scorePaint.color = Color.WHITE
            bottomBoundaryPaint.color = Color.WHITE
        } else {
            scorePaint.color = Color.BLACK
            bottomBoundaryPaint.color = Color.BLACK
        }

        scorePaint.textSize = 80f
        scorePaint.isFakeBoldText = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread = Thread(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        characterY = (screenHeight / 2).toFloat()
        createInitialObstacles()

        // Arka plan resmini ekran yüksekliğine göre ölçekle
        val aspectRatio = backgroundBitmapOriginal.width.toFloat() / backgroundBitmapOriginal.height.toFloat()
        val scaledWidth = (screenHeight * aspectRatio).toInt()
        backgroundBitmapScaled = Bitmap.createScaledBitmap(backgroundBitmapOriginal, scaledWidth, screenHeight, true)

        // İkinci arka plan resminin başlangıç pozisyonunu ayarla
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

        // Arka planı kaydır
        val backgroundScrollSpeed = 2f // Hızı ayarla, engellerden yavaş olsun
        backgroundX1 -= backgroundScrollSpeed
        backgroundX2 -= backgroundScrollSpeed

        // İlk resim ekranın solundan tamamen çıktıysa, ikincinin sağına konumlandır
        if (backgroundX1 < -backgroundBitmapScaled.width) {
            backgroundX1 = backgroundX2 + backgroundBitmapScaled.width
        }
        // İkinci resim ekranın solundan tamamen çıktıysa, ilkinin sağına konumlandır
        if (backgroundX2 < -backgroundBitmapScaled.width) {
            backgroundX2 = backgroundX1 + backgroundBitmapScaled.width
        }

        characterVelocity += gravity
        characterY += characterVelocity

        if (characterY + characterCollisionRadius >= screenHeight || characterY - characterCollisionRadius <= 0) {
            isGameOver = true
            isPlaying = false
            showGameOverDialog()
            return
        }

        val characterX = (screenWidth / 4).toFloat()
        characterRect.set(
            characterX - characterCollisionRadius,
            characterY - characterCollisionRadius,
            characterX + characterCollisionRadius,
            characterY + characterCollisionRadius
        )

        for (obstacle in obstacles) {
            val obstacleRect = RectF(obstacle.x, obstacle.top, obstacle.x + obstacle.width, obstacle.bottom)

            if (characterRect.intersect(obstacleRect)) {

                lives--

                if (lives <= 0) {
                    isGameOver = true
                    isPlaying = false
                    showGameOverDialog()
                    return
                } else {
                    resetCharacterAndObstacles()
                    return
                }
            }

            obstacle.x -= 10f
            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(obstacle)
                addNewObstacle()
                score++
            }
        }
    }

    private fun draw() {
        if (!isReady) return

        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()

            // Arka plan resimlerini çiz
            // Ölçeklenmiş bitmap'i kullanıyoruz
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

            canvas.drawText(score.toString(), (screenWidth - 100).toFloat(), 100f, scorePaint)

            canvas.drawRect(0f, screenHeight.toFloat() - 20f, screenWidth.toFloat(), screenHeight.toFloat(), bottomBoundaryPaint)

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
            val obstacleY = random.nextFloat() * (screenHeight - 400) + 200
            val obstacle = Obstacle(screenWidth.toFloat() + obstacleSpacing * i, obstacleY, randomColor)
            obstacles.add(obstacle)
        }
    }

    private fun addNewObstacle() {
        val lastObstacle = obstacles.last()
        val randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        val newObstacleY = random.nextFloat() * (screenHeight - 400) + 200
        val newObstacle = Obstacle(lastObstacle.x + obstacleSpacing, newObstacleY, randomColor)
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

    private fun showGameOverDialog() {
        (context as GameActivity).runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Oyun Bitti!")
                .setMessage("Skorunuz: $score")
                .setPositiveButton("Yeniden Başla") { dialog, which ->
                    resetGame()
                }
                .setNegativeButton("Ana Menü") { dialog, which ->
                    (context as GameActivity).finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun resetGame() {
        score = 0
        lives = 3
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

    data class Obstacle(var x: Float, var y: Float, var color: Int) {
        val width = 200f
        val height = 400f
        val top = y - height / 2
        val bottom = y + height / 2
    }
}