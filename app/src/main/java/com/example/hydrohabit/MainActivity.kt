package com.example.hydrohabit

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

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

    private fun setupButtons() {
        fillButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    rainView.startRain()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    rainView.stopRain()
                    addWaterToTracker(FILL_AMOUNT)
                    true
                }
                else -> false
            }
        }

        add250Button.setOnClickListener {
            addWaterToTracker(AMOUNT_250)
        }

        add500Button.setOnClickListener {
            addWaterToTracker(AMOUNT_500)
        }

        add750Button.setOnClickListener {
            addWaterToTracker(AMOUNT_750)
        }
    }

    private fun addWaterToTracker(amountMl: Int) {
        // Implement water tracking logic
    }
}