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

class RainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val random = Random()
    private val raindrops = mutableListOf<Raindrop>()

    private val regularPaint = Paint().apply {
        color = Color.parseColor("#7fb3d5")
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val blurPaint = Paint().apply {
        color = Color.parseColor("#7fb3d5")
        isAntiAlias = true
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }

    private val uiElements = mutableListOf<RectF>()
    private var glassContainerView: View? = null
    private var glassContainerRect: RectF? = null

    private var isRaining = false

    private val handler = Handler(Looper.getMainLooper())
    private val animator = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        override fun run() {
            updateRaindrops()
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

            val raindropRect = RectF(
                raindrop.x - raindrop.size / 2,
                raindrop.y - raindrop.size,
                raindrop.x + raindrop.size / 2,
                raindrop.y
            )
            refreshGlassContainerRect()
            val hitsGlass = glassContainerRect?.let {
                val raindropBottom = raindrop.y
                val bottomLine = it.bottom - 20f
                val xInside = raindrop.x in it.left..it.right
                val yTouchingBottom = raindropBottom in (bottomLine - raindrop.speed)..bottomLine
                xInside && yTouchingBottom
            } == true


            if (hitsGlass) {
                Log.d("RainView", "Raindrop touched the bottom of the glass at (${raindrop.x}, ${raindrop.y})")
                iterator.remove()
            } else if (raindrop.y > height) {
                Log.d("RainView", "Raindrop fell off screen at (${raindrop.x}, ${raindrop.y})")
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
}