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

    private fun refreshGlassContainerRect() {
        glassContainerView?.let {
            glassContainerRect = getViewRect(it)
        }
    }

    private fun getViewRect(view: View): RectF {
        val rect = RectF()
        val location = IntArray(2)
        this.getLocationOnScreen(location)

        view.getGlobalVisibleRect(Rect().apply {
            rect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            rect.offset(-location[0].toFloat(), -location[1].toFloat())
        })
        return rect
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun updateRaindrops() {
        if (random.nextFloat() < 0.6f && raindrops.size < 300) {
            val x = random.nextInt(width).toFloat()
            raindrops.add(Raindrop(x, 0f, random.nextInt(15, 45).toFloat()))
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
            val hitsGlass = glassContainerRect?.let { RectF.intersects(raindropRect, it) } == true

            if (hitsGlass) {
                Log.d("RainView", "Raindrop touched glass at (${raindrop.x}, ${raindrop.y})")
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

            val path = Path()
            val dropWidth = raindrop.size / 2

            path.moveTo(raindrop.x, raindrop.y - raindrop.size)
            path.quadTo(raindrop.x + dropWidth, raindrop.y - raindrop.size * 0.5f, raindrop.x, raindrop.y)
            path.quadTo(raindrop.x - dropWidth, raindrop.y - raindrop.size * 0.5f, raindrop.x, raindrop.y - raindrop.size)

            canvas.drawPath(path, paint)
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
