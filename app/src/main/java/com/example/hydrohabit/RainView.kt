package com.example.hydrohabit

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private val random = Random()
    private val raindrops = mutableListOf<Raindrop>()

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var waterSurfacePoints = mutableListOf<WaterPoint>()
    private var waterTiltX = 0f
    private var waterTiltY = 0f
    private val dampingFactor = 0.95f
    private val springConstant = 0.4f


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

    private val waterPaint = Paint().apply {
        color = "#3b86d6".toColorInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val uiElements = mutableListOf<RectF>()
    var onVolumeChanged: ((Float) -> Unit)? = null
    var onTimedRainStateChanged: ((Boolean) -> Unit)? = null
    private var glassContainerView: View? = null
    private var glassContainerRect: RectF? = null
    private var isRaining = false
    private var glassVolumeMl = 3000f
    private var currentVolumeMl = 0f
    private var waterLevelRatio = 0f
    private var isTimedRain = false

    private var plannedDrops = mutableListOf<PlannedDrop>()
    private var currentDropIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private val animator = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        override fun run() {
            updateRaindrops()
            updateWaterPhysics()
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    data class PlannedDrop(
        val spawnTime: Long,
        val volume: Float,
        val size: Float
    )

    data class WaterPoint(
        var x: Float,
        var y: Float,
        var restY: Float,
        var velocity: Float = 0f
    )

    init {

        setLayerType(LAYER_TYPE_HARDWARE, null)
        loadGlassVolume()
        initializeWaterSurface()
    }

    private fun initializeWaterSurface() {
        waterSurfacePoints.clear()
    }
    private fun loadGlassVolume(){
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        glassVolumeMl = sharedPrefs.getFloat("daily_volume_goal", 3000f)
    }


    private fun updateWaterSurfacePoints() {
        val glassRect = glassContainerRect ?: return
        if (waterLevelRatio <= 0f) return

        val numPoints = 40
        val width = glassRect.width()
        val waterSurfaceY = glassRect.bottom - (glassRect.height() * waterLevelRatio)

        if (waterSurfacePoints.size != numPoints) {
            waterSurfacePoints.clear()
            for (i in 0 until numPoints) {
                val x = glassRect.left + (i * width / (numPoints - 1))
                waterSurfacePoints.add(WaterPoint(x, waterSurfaceY, waterSurfaceY))
            }
        } else {
            for (i in waterSurfacePoints.indices) {
                waterSurfacePoints[i].restY = waterSurfaceY
            }
        }
    }
    private fun updateWaterSurfacePointsDirect() {
        val glassRect = glassContainerRect ?: return
        if (waterLevelRatio <= 0f) return

        val numPoints = 40
        val width = glassRect.width()
        val waterSurfaceY = glassRect.bottom - (glassRect.height() * waterLevelRatio)


        waterSurfacePoints.clear()
        for (i in 0 until numPoints) {
            val x = glassRect.left + (i * width / (numPoints - 1))
            waterSurfacePoints.add(WaterPoint(x, waterSurfaceY, waterSurfaceY))

        }
    }

    private fun updateWaterPhysics() {
        if (waterLevelRatio <= 0f || waterSurfacePoints.isEmpty()) return

        val glassRect = glassContainerRect ?: return

        val tiltEffect = if (currentVolumeMl >= glassVolumeMl) {
            0f
        } else {
            -sin(waterTiltX * PI / 50f).toFloat()
        }
        val baseWaterY = glassRect.bottom - (glassRect.height() * waterLevelRatio)
        val maxWaterDisplacement = glassRect.height() * 0.2f

        for (i in waterSurfacePoints.indices) {
            val point = waterSurfacePoints[i]
            val normalizedX = (point.x - glassRect.left) / glassRect.width() - 0.5f

            val tiltDisplacement = tiltEffect * maxWaterDisplacement * normalizedX * 2f
            val targetY = baseWaterY + tiltDisplacement

            val force = (targetY - point.y) * springConstant
            point.velocity += force
            point.velocity *= dampingFactor
            point.y += point.velocity

            if (i > 0 && i < waterSurfacePoints.size - 1) {
                val leftPoint = waterSurfacePoints[i - 1]
                val rightPoint = waterSurfacePoints[i + 1]
                val avgNeighborY = (leftPoint.y + rightPoint.y) / 2f
                val waveForce = (avgNeighborY - point.y) * 0.05f
                point.velocity += waveForce
            }
        }

        for (point in waterSurfacePoints) {
            if (point.y < glassRect.top + 10f) {
                point.y = glassRect.top + 10f
                point.velocity *= -0.5f
            }
            if (point.y > glassRect.bottom - 10f) {
                point.y = glassRect.bottom - 10f
                point.velocity *= -0.5f
            }
        }
    }

    fun start() {
        handler.post(animator)
        val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        handler.removeCallbacks(animator)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val remappedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedRotationMatrix
        )

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientation)

        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()


        val alpha = 0.1f
        waterTiltX = waterTiltX * (1f - alpha) + roll * alpha


        waterTiltX = waterTiltX.coerceIn(-60f, 60f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
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
        isTimedRain = false
    }

    fun stopRain() {
        isRaining = false
    }

    fun addWaterDirectly(volumeMl: Float) {
        refreshGlassContainerRect()
        currentVolumeMl += volumeMl
        if (currentVolumeMl > glassVolumeMl) currentVolumeMl = glassVolumeMl
        waterLevelRatio = currentVolumeMl / glassVolumeMl

        updateWaterSurfacePointsDirect()

        invalidate()
    }

    private fun refreshGlassContainerRect() {
        glassContainerView?.let {
            glassContainerRect = getViewRect(it)
            updateWaterSurfacePoints()
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

        if (waterSurfacePoints.isNotEmpty()) {
            for (i in 0 until waterSurfacePoints.size - 1) {
                val point1 = waterSurfacePoints[i]
                val point2 = waterSurfacePoints[i + 1]

                if (raindrop.x >= point1.x && raindrop.x <= point2.x) {
                    val ratio = (raindrop.x - point1.x) / (point2.x - point1.x)
                    val waterY = point1.y + (point2.y - point1.y) * ratio

                    if (abs(raindrop.y - waterY) < 5f) {
                        return true
                    }
                }
            }
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun updateRaindrops() {
        if (isRaining) {
            if (isTimedRain) {
                val currentTime = System.currentTimeMillis()

                while (currentDropIndex < plannedDrops.size &&
                    currentTime >= plannedDrops[currentDropIndex].spawnTime) {
                    val drop = plannedDrops[currentDropIndex]
                    val screenCenter = width / 2f
                    val x = screenCenter - 100f + random.nextFloat() * 200f
                    raindrops.add(Raindrop(x, 0f, drop.size, volume = drop.volume))
                    currentDropIndex++
                }

                if (currentDropIndex >= plannedDrops.size && raindrops.isEmpty()) {
                    isRaining = false
                    isTimedRain = false
                    onTimedRainStateChanged?.invoke(false)
                    Log.d("RainView", "Timed rain completed. All drops spawned and landed.")
                }
            } else {
                if (random.nextFloat() < 0.6f && raindrops.size < 300) {
                    val screenCenter = width / 2f
                    val x = screenCenter - 100f + random.nextFloat() * 200f
                    raindrops.add(Raindrop(x, 0f, random.nextInt(15, 45).toFloat(), volume = 0.5f))
                }
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
                val waterSurfaceY = if (waterSurfacePoints.isNotEmpty()) {
                    var surfaceY = it.bottom - (it.height() * waterLevelRatio)
                    for (i in 0 until waterSurfacePoints.size - 1) {
                        val point1 = waterSurfacePoints[i]
                        val point2 = waterSurfacePoints[i + 1]
                        if (raindrop.x >= point1.x && raindrop.x <= point2.x) {
                            val ratio = (raindrop.x - point1.x) / (point2.x - point1.x)
                            surfaceY = point1.y + (point2.y - point1.y) * ratio
                            break
                        }
                    }
                    surfaceY
                } else {
                    it.bottom - (it.height() * waterLevelRatio)
                }

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
                    for (point in waterSurfacePoints) {
                        val distance = abs(point.x - raindrop.x)
                        if (distance < 30f) {
                            val impact = (1f - distance / 30f) * raindrop.size * 0.3f
                            point.velocity -= impact
                        }
                    }
                }

                currentVolumeMl += raindrop.volume
                if (currentVolumeMl > glassVolumeMl) currentVolumeMl = glassVolumeMl
                waterLevelRatio = currentVolumeMl / glassVolumeMl
                updateWaterSurfacePoints()
                onVolumeChanged?.invoke(raindrop.volume)
                iterator.remove()

            } else if (isBeyondGlassBottom) {
                iterator.remove()
            } else if (raindrop.y > height) {
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
            if (waterLevelRatio > 0f) {
                if (waterSurfacePoints.isNotEmpty()) {
                    val path = Path()

                    path.moveTo(glass.left, glass.bottom)

                    path.lineTo(glass.right, glass.bottom)

                    val rightWaterPoint = waterSurfacePoints.last()
                    path.lineTo(glass.right, rightWaterPoint.y)

                    for (i in waterSurfacePoints.size - 1 downTo 0) {
                        val point = waterSurfacePoints[i]
                        path.lineTo(point.x, point.y)
                    }

                    path.lineTo(glass.left, waterSurfacePoints.first().y)

                    path.close()

                    canvas.drawPath(path, waterPaint)
                } else {
                    val waterTop = glass.bottom - (glass.height() * waterLevelRatio)
                    val waterRect = RectF(glass.left, waterTop, glass.right, glass.bottom)
                    canvas.drawRect(waterRect, waterPaint)
                }
            }
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
        val speed: Float = size / 2f,
        val volume: Float = 0.5f
    )
}