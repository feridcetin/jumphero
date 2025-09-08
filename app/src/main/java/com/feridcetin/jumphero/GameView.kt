package com.feridcetin.jumphero


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
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

            // Yeni: Skoru dinamik olarak konumlandır
            val scoreText = score.toString()
            val scoreTextWidth = scorePaint.measureText(scoreText)
            canvas.drawText(scoreText, screenWidth.toFloat() - scoreTextWidth - 50f, 100f, scorePaint)

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