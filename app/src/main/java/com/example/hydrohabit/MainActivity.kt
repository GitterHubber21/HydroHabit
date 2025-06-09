package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.coroutines.*
import androidx.activity.enableEdgeToEdge
import android.view.GestureDetector
import android.widget.Toast
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import kotlin.math.abs


class MainActivity : ComponentActivity() {

    private lateinit var rainView: RainView
    private lateinit var appTitle: TextView
    private lateinit var glassContainer: FrameLayout
    private lateinit var fillButton: Button
    private lateinit var add250Button: Button
    private lateinit var add500Button: Button
    private lateinit var add750Button: Button
    private lateinit var waterVolumeText: TextView
    private lateinit var gestureDetector: GestureDetector

    private val AMOUNT_250 = 250
    private val AMOUNT_500 = 500
    private val AMOUNT_750 = 750
    private var isBellSelected = false
    private var isTimedRainActive = false
    private var displayedVolume = 0f
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)
    private lateinit var encryptedPrefs: SharedPreferences

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (url.host == "water.coolcoder.hackclub.app") {
                    for (cookie in cookies) {
                        encryptedPrefs.edit {
                            putString(cookie.name, cookie.value)
                        }
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = mutableListOf<Cookie>()
                val allCookies = encryptedPrefs.all
                for ((name, value) in allCookies) {
                    if (value is String) {
                        cookies.add(
                            Cookie.Builder()
                                .name(name)
                                .value(value)
                                .domain(url.host)
                                .build()
                        )
                    }
                }
                return cookies
            }
        })
        .build()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "secure_cookies",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        initViews()
        setupRainView()
        setupButtons()

        coroutineScope.launch {
            val initialVolume = fetchTotalVolume()

            withContext(Dispatchers.Main) {
                displayedVolume = initialVolume
                waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                rainView.addWaterDirectly(initialVolume)

                Log.d("MainActivity", "Initial server volume = $initialVolume ml")
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.itemIconTintList = null
        val settingsIcon: ImageView = findViewById(R.id.settingsIcon)
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
                updateQuantity(displayedVolume.toFloat())
                delay(1000L)
            }
        }
        rainView.onTimedRainStateChanged = { isActive ->
            runOnUiThread {
                isTimedRainActive = isActive
                updateButtonStates()
            }
        }


        settingsIcon.setOnClickListener {
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_insights -> {
                    startActivity(Intent(applicationContext, InsightsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_challenges -> {
                    startActivity(Intent(applicationContext, ChallengesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        gestureDetector = GestureDetector(this, SwipeGestureListener())

    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
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
    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffY) > abs(diffX)) {

                if (diffY > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    finishWithAnimation()
                    return true
                }
            }

            return false
        }
    }
    private fun finishWithAnimation() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
    }
    private fun updateQuantity(volume: Float) {
        val url = "https://water.coolcoder.hackclub.app/api/log"
        val json = JSONObject().apply { put("volume_ml", volume) }.toString()

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), json
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //pass
            }

            override fun onResponse(call: Call, response: Response) {
                val serverReply = response.body?.use { it.string() } ?: "Empty response"
                Log.d("server_response", serverReply)
            }
        })
    }
    private suspend fun fetchTotalVolume(): Float = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://water.coolcoder.hackclub.app/api/stats")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val total = JSONObject(body).optDouble("total_volume_ml", 0.0)
                        total.toFloat()
                    } else 0f
                } else {
                    Log.w("MainActivity", "stats request failed: ${response.code}")
                    0f
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "stats request error", e)
            0f
        }
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
                    rainView.addWaterDirectly(AMOUNT_250.toFloat())
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
                    rainView.addWaterDirectly(AMOUNT_500.toFloat())
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
                    rainView.addWaterDirectly(AMOUNT_750.toFloat())
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

}