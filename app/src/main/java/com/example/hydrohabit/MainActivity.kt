package com.example.hydrohabit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.getSystemService
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt


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
    private var isBellSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = "#292929".toColorInt()

        initViews()
        setupRainView()
        setupButtons()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.itemIconTintList = null
        val bellIcon: ImageView = findViewById(R.id.bellIcon)

        bellIcon.setOnClickListener {
            if (isBellSelected) {
                bellIcon.setImageResource(R.drawable.ic_bell)
            } else {

                bellIcon.setImageResource(R.drawable.ic_bell_unselected)
            }


            isBellSelected = !isBellSelected
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_insights -> {
                    startActivity(Intent(applicationContext, InsightsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_challenges -> {
                    startActivity(Intent(applicationContext, ChallengesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }


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
