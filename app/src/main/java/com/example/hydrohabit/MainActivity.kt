package com.example.hydrohabit

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.drawable.Drawable
import androidx.core.content.getSystemService

class MainActivity : Activity() {

    private lateinit var rainView: RainView
    private lateinit var appTitle: TextView
    private lateinit var glassContainer: FrameLayout
    private lateinit var fillButton: Button
    private lateinit var add250Button: Button
    private lateinit var add500Button: Button
    private lateinit var add750Button: Button

    private val FILL_AMOUNT = 1000
    private val AMOUNT_250 = 250
    private val AMOUNT_500 = 500
    private val AMOUNT_750 = 750

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        initViews()
        setupRainView()
        setupButtons()
    }

    private fun initViews() {
        rainView = findViewById(R.id.rainView)
        appTitle = findViewById(R.id.appTitle)
        glassContainer = findViewById(R.id.glassContainer)
        fillButton = findViewById(R.id.fillButton)
        add250Button = findViewById(R.id.add250Button)
        add500Button = findViewById(R.id.add500Button)
        add750Button = findViewById(R.id.add750Button)
    }

    private fun setupRainView() {
        val otherElements = listOf(
            appTitle,
            fillButton,
            add250Button,
            add500Button,
            add750Button
        )

        rainView.post {
            rainView.registerGlassContainer(glassContainer)
            otherElements.forEach { element ->
                rainView.registerUIElement(element)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
        val vibrator = getSystemService<Vibrator>()

        setupPressable(
            fillButton,
            vibrator,
            pressedDrawableRes = R.drawable.pressed_button_rectangle,
            onPress = { rainView.startRain() },
            onRelease = {
                rainView.stopRain()
                addWaterToTracker(FILL_AMOUNT)
            }
        )

        setupPressable(
            add250Button,
            vibrator,
            pressedDrawableRes = R.drawable.pressed_button_rectangle_bottom
        ) {
            addWaterToTracker(AMOUNT_250)
        }

        setupPressable(
            add500Button,
            vibrator,
            pressedDrawableRes = R.drawable.pressed_button_rectangle_bottom
        ) {
            addWaterToTracker(AMOUNT_500)
        }

        setupPressable(
            add750Button,
            vibrator,
            pressedDrawableRes = R.drawable.pressed_button_rectangle_bottom
        ) {
            addWaterToTracker(AMOUNT_750)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPressable(
        button: Button,
        vibrator: Vibrator?,
        pressedDrawableRes: Int,
        onPress: (() -> Unit)? = null,
        onRelease: (() -> Unit)? = null
    ) {
        val originalBackground: Drawable = button.background
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.setBackgroundResource(pressedDrawableRes)
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    onPress?.invoke()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.background = originalBackground
                    onRelease?.invoke()
                    true
                }
                else -> false
            }
        }
    }


    private fun addWaterToTracker(amountMl: Int) {
        // Implement water tracking logic
    }
}
