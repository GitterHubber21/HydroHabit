package com.example.hydrohabit

import android.app.Activity
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        initViews()
        setupRainView()
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

        fillButton.setOnClickListener {
            // Handle fill button click
        }

        add250Button.setOnClickListener {
            // Handle +250ml button click
        }

        add500Button.setOnClickListener {
            // Handle +500ml button click
        }

        add750Button.setOnClickListener {
            // Handle +750ml button click
        }
    }
}
