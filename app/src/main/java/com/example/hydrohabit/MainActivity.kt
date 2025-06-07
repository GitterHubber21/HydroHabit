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
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.coroutines.*
import androidx.core.content.edit


class MainActivity : Activity() {

    private lateinit var rainView: RainView
    private lateinit var appTitle: TextView
    private lateinit var glassContainer: FrameLayout
    private lateinit var fillButton: Button
    private lateinit var add250Button: Button
    private lateinit var add500Button: Button
    private lateinit var add750Button: Button
    private lateinit var waterVolumeText: TextView

    private val FILL_AMOUNT = 1000
    private val AMOUNT_250 = 250
    private val AMOUNT_500 = 500
    private val AMOUNT_750 = 750
    private var isBellSelected = false
    private var isTimedRainActive = false
    private var displayedVolume = 0f
    private val client = OkHttpClient()
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isOnboardingCompleted = sharedPreferences.getBoolean("onboarding_completed", false)
        val isLoginCompleted = sharedPreferences.getBoolean("login_completed", false)
        sharedPreferences.edit { clear() }
        setContentView(R.layout.activity_main)
        window.statusBarColor = "#292929".toColorInt()

        initViews()
        setupRainView()
        setupButtons()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.itemIconTintList = null
        val bellIcon: ImageView = findViewById(R.id.bellIcon)
        rainView = findViewById(R.id.rainView)
        waterVolumeText = findViewById(R.id.waterVolume)

        rainView.onVolumeChanged = { dropletVolume ->
            runOnUiThread {
                if (!isTimedRainActive) {
                    displayedVolume += dropletVolume
                    if (displayedVolume > 2000f) displayedVolume = 2000f
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                }
            }
            Log.d("RainVolume", "Displayed volume: $displayedVolume")
        }
        coroutineScope.launch {
            while (isActive) {
                updateQuantity("quantity", displayedVolume.toString())
                delay(3000L)
            }
        }
        rainView.onTimedRainStateChanged = { isActive ->
            runOnUiThread {
                isTimedRainActive = isActive
                updateButtonStates()
            }
        }


        bellIcon.setOnClickListener {
            if (isBellSelected) {
                bellIcon.setImageResource(R.drawable.ic_bell_unselected)
            } else {
                bellIcon.setImageResource(R.drawable.ic_bell)
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
        if(!isOnboardingCompleted){
            startActivity(Intent(applicationContext, OnboardingActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
        if(!isLoginCompleted&&isOnboardingCompleted){
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
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
    private fun updateQuantity(key: String, value: String) {
        val url = "https://water.coolcoder.hackclub.app/api/quantity"

        val json = """
            {
                "key": "$key",
                "value": "$value"
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    //Toast.makeText(this@MainActivity, "Request failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    //Toast.makeText(this@MainActivity, "Response: $responseBody", Toast.LENGTH_SHORT).show()
                }
            }
        })
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

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
        val vibrator = getSystemService<Vibrator>()

        setupPressable(
            fillButton,
            vibrator,
            pressedDrawableRes = R.drawable.pressed_button_circle,
            onPress = {
                if (!isTimedRainActive) {
                    rainView.startRain()
                }
            },
            onRelease = {
                if (!isTimedRainActive) {
                    rainView.stopRain()
                }
            }
        )

        setupPressable(
            add250Button,
            vibrator,
            pressedDrawableRes = R.drawable.rounded_transparent_square,
            onPress = {
                if (!isTimedRainActive) {
                    displayedVolume += AMOUNT_250
                    if (displayedVolume > 2000f) displayedVolume = 2000f
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                    rainView.startTimedRain(AMOUNT_250.toFloat(), 3.0f)
                    addWaterToTracker(AMOUNT_250)
                }
            },
            onRelease = {
            }
        )

        setupPressable(
            add500Button,
            vibrator,
            pressedDrawableRes = R.drawable.rounded_transparent_square,
            onPress = {
                if (!isTimedRainActive) {
                    displayedVolume += AMOUNT_500
                    if (displayedVolume > 2000f) displayedVolume = 2000f
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                    rainView.startTimedRain(AMOUNT_500.toFloat(), 4.0f)
                    addWaterToTracker(AMOUNT_500)
                }
            },
            onRelease = {
            }
        )

        setupPressable(
            add750Button,
            vibrator,
            pressedDrawableRes = R.drawable.rounded_transparent_square,
            onPress = {
                if (!isTimedRainActive) {
                    displayedVolume += AMOUNT_750
                    if (displayedVolume > 2000f) displayedVolume = 2000f
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                    rainView.startTimedRain(AMOUNT_750.toFloat(), 5.0f)
                    addWaterToTracker(AMOUNT_750)
                }
            },
            onRelease = {
            }
        )

        updateButtonStates()
    }

    private fun updateButtonStates() {
        val alpha = if (isTimedRainActive) 0.5f else 1.0f
        fillButton.alpha = alpha
        add250Button.alpha = alpha
        add500Button.alpha = alpha
        add750Button.alpha = alpha

        fillButton.isEnabled = !isTimedRainActive
        add250Button.isEnabled = !isTimedRainActive
        add500Button.isEnabled = !isTimedRainActive
        add750Button.isEnabled = !isTimedRainActive
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
            if (!button.isEnabled) return@setOnTouchListener false
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
    }
}