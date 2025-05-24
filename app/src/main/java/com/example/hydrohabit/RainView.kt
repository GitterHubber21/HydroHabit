package com.example.hydrohabit

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import java.util.Random
import androidx.core.graphics.toColorInt
import kotlin.math.*

class RainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val random = Random()
    private val raindrops = mutableListOf<Raindrop>()
    private val ripples = mutableListOf<Ripple>()

    private val regularPaint = Paint().apply {
        color = "#3b86d6".toColorInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val blurPaint = Paint().apply {
        color = "#3b86d6".toColorInt()
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    private val ripplePaint = Paint().apply {
        color = "#3b86d6".toColorInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val uiElements = mutableListOf<RectF>()
    var onVolumeChanged: ((Float) -> Unit)? = null
    private var glassContainerView: View? = null
    private var glassContainerRect: RectF? = null
    private var waterLevel: Float = 0f
    private var isRaining = false
    private val glassVolumeMl = 1000f
    private var currentVolumeMl = 0f
    private var waterLevelRatio = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val animator = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        override fun run() {
            updateRaindrops()
            updateRipples()
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun start() {
        handler.post(animator)
    }

    fun stop() {
        handler.removeCallbacks(animator)
    }

    fun registerUIElement(view: View) {
        uiElements.add(getViewRect(view))
    }

    fun registerGlassContainer(view: View) {
        glassContainerView = view
    }

    fun startRain() {
        refreshGlassContainerRect()
        isRaining = true
    }

    fun stopRain() {
        isRaining = false
    }

    private fun refreshGlassContainerRect() {
        glassContainerView?.let {
            glassContainerRect = getViewRect(it)
        }
        glassContainerRect?.let { rect ->
            val width = rect.width()
            val height = rect.height()
            Log.d("RainView", "Glass rect - X: ${rect.left}, Y: ${rect.top}, Width: $width, Height: $height")
        }
    }

    private fun getViewRect(view: View): RectF {
        val location = IntArray(2)
        val parentLocation = IntArray(2)

        view.getLocationOnScreen(location)
        this.getLocationOnScreen(parentLocation)

        val left = (location[0] - parentLocation[0]).toFloat()
        val top = (location[1] - parentLocation[1]).toFloat()
        val right = left + view.width
        val bottom = top + view.height

        return RectF(left, top, right, bottom)
    }

    private fun isRaindropHittingWave(raindrop: Raindrop): Boolean {
        val glassRect = glassContainerRect ?: return false

        if (raindrop.x < glassRect.left || raindrop.x > glassRect.right) return false

        for (ripple in ripples) {
            if (ripple.isRaindropColliding(raindrop.x, raindrop.y)) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun updateRaindrops() {
        if (isRaining) {
            if (random.nextFloat() < 0.6f && raindrops.size < 300) {
                val screenCenter = width / 2f
                val x = screenCenter - 100f + random.nextFloat() * 200f
                raindrops.add(Raindrop(x, 0f, random.nextInt(15, 45).toFloat()))
            }
        }

        val iterator = raindrops.iterator()
        while (iterator.hasNext()) {
            val raindrop = iterator.next()
            raindrop.y += raindrop.speed

            refreshGlassContainerRect()
            val glassRect = glassContainerRect

            val hitsWave = isRaindropHittingWave(raindrop)

            val hitsWater = glassRect?.let {
                val waterSurfaceY = it.bottom - (it.height() * waterLevelRatio)
                val raindropBottom = raindrop.y
                val xInside = raindrop.x in it.left..it.right
                val yTouchingWater = raindropBottom >= waterSurfaceY && raindrop.y - raindrop.speed < waterSurfaceY
                xInside && yTouchingWater && waterLevelRatio > 0f
            } == true

            val hitsBottom = glassRect?.let {
                val raindropBottom = raindrop.y
                val bottomLine = it.bottom - 20f
                val xInside = raindrop.x in it.left..it.right
                val yTouchingBottom = raindropBottom in (bottomLine - raindrop.speed)..bottomLine
                xInside && yTouchingBottom && waterLevelRatio == 0f
            } == true

            val isBeyondGlassBottom = glassRect?.let { glass ->
                raindrop.x in glass.left..glass.right && raindrop.y > glass.bottom
            } == true

            if (hitsWave || hitsWater || hitsBottom) {
                if (hitsWater && !hitsWave) {
                    glassRect?.let { glass ->
                        val waterSurfaceY = glass.bottom - (glass.height() * waterLevelRatio)
                        createRipple(raindrop.x, waterSurfaceY, glass.left, glass.right)
                    }
                    Log.d("RainView", "Raindrop hit water surface at (${raindrop.x}, ${raindrop.y})")
                } else if (hitsBottom && !hitsWave) {
                    Log.d("RainView", "Raindrop touched the bottom of the glass at (${raindrop.x}, ${raindrop.y})")
                } else if (hitsWave) {
                    Log.d("RainView", "Raindrop hit wave at (${raindrop.x}, ${raindrop.y})")
                }

                val dropVolume = if (raindrop.size < 30f) 0.5f else 1.0f
                currentVolumeMl += dropVolume
                if (currentVolumeMl > glassVolumeMl) currentVolumeMl = glassVolumeMl
                waterLevelRatio = currentVolumeMl / glassVolumeMl
                onVolumeChanged?.invoke(currentVolumeMl)
                iterator.remove()
            } else if (isBeyondGlassBottom) {
                Log.d("RainView", "Raindrop disappeared beyond glass bottom at (${raindrop.x}, ${raindrop.y})")
                iterator.remove()
            } else if (raindrop.y > height) {
                Log.d("RainView", "Raindrop fell off screen at (${raindrop.x}, ${raindrop.y})")
                iterator.remove()
            }
        }
    }

    private fun createRipple(x: Float, y: Float, leftBound: Float, rightBound: Float) {
        ripples.add(Ripple(x, y, leftBound, rightBound, expanding = true, direction = -1))
        ripples.add(Ripple(x, y, leftBound, rightBound, expanding = true, direction = 1))
    }

    private fun updateRipples() {
        val iterator = ripples.iterator()
        while (iterator.hasNext()) {
            val ripple = iterator.next()
            ripple.update()

            if (ripple.isFinished()) {
                iterator.remove()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (raindrop in raindrops) {
            val isBlurred = isOverlappingUIElement(raindrop)
            val paint = if (isBlurred) blurPaint else regularPaint

            canvas.drawCircle(raindrop.x, raindrop.y - raindrop.size/2, raindrop.size/2, paint)
        }

        glassContainerRect?.let { glass ->
            val waterTop = glass.bottom - (glass.height() * waterLevelRatio)
            val waterRect = RectF(glass.left, waterTop, glass.right, glass.bottom)

            val waterPaint = Paint().apply {
                color = "#3b86d6".toColorInt()
                isAntiAlias = true
            }

            canvas.drawRect(waterRect, waterPaint)
        }

        for (ripple in ripples) {
            ripple.draw(canvas, ripplePaint)
        }
    }

    private fun isOverlappingUIElement(raindrop: Raindrop): Boolean {
        val raindropRect = RectF(
            raindrop.x - raindrop.size / 2,
            raindrop.y - raindrop.size,
            raindrop.x + raindrop.size / 2,
            raindrop.y
        )

        for (element in uiElements) {
            if (RectF.intersects(raindropRect, element)) {
                return true
            }
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    private data class Raindrop(
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float = size / 2f
    )

    private class Ripple(
        private val originX: Float,
        private val originY: Float,
        private val leftBound: Float,
        private val rightBound: Float,
        private var expanding: Boolean,
        private val direction: Int
    ) {
        private var currentX: Float = originX
        private var amplitude: Float = 8f
        private var speed: Float = 3f
        private var damping: Float = 0.95f
        private var phase: Float = 0f
        private var finished = false

        fun update() {
            if (finished) return

            if (expanding) {
                currentX += direction * speed
                phase += 0.3f

                amplitude *= damping

                if ((direction < 0 && currentX <= leftBound) || (direction > 0 && currentX >= rightBound)) {
                    expanding = false
                    amplitude *= 0.3f
                    speed *= 0.5f
                }

                if (amplitude < 0.5f) {
                    finished = true
                }
            } else {
                amplitude *= 0.85f
                phase += 0.2f

                if (amplitude < 0.2f) {
                    finished = true
                }
            }
        }

        fun isRaindropColliding(raindropX: Float, raindropY: Float): Boolean {
            if (finished || amplitude < 0.1f) return false

            val distance = abs(raindropX - currentX)
            if (distance > 15f) return false

            val waveOffset = sin(phase) * amplitude
            val waveY = originY + waveOffset

            return abs(raindropY - waveY) < 5f
        }

        fun draw(canvas: Canvas, paint: Paint) {
            if (finished || amplitude < 0.1f) return

            val alpha = (amplitude / 8f * 255).toInt().coerceIn(20, 150)
            paint.alpha = alpha

            val waveLength = 20f
            val numPoints = 15
            val points = mutableListOf<Float>()

            for (i in 0 until numPoints) {
                val x = currentX - waveLength/2 + (i * waveLength / numPoints)
                if (x < leftBound || x > rightBound) continue

                val waveOffset = sin(phase + i * 0.5f) * amplitude
                val y = originY + waveOffset

                points.add(x)
                points.add(y)
            }

            for (i in 0 until points.size step 2) {
                if (i + 1 < points.size) {
                    canvas.drawCircle(points[i], points[i + 1], 1.5f, paint)
                }
            }
        }

        fun isFinished(): Boolean = finished
    }
}