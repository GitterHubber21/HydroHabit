package com.example.hydrohabit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import java.io.IOException
import kotlinx.coroutines.*
import androidx.activity.enableEdgeToEdge
import android.view.GestureDetector
import androidx.core.content.edit
import org.json.JSONObject
import kotlin.math.abs
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams

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
    private var isTimedRainActive = false
    private var displayedVolume = 0f
    private var dailyGoal = 3000f
    private var isVolumeInitialized = false
    private var isAnimationActive = false

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + job)

    private lateinit var sharedPrefs: SharedPreferences
    private enum class MotivationLevel(val priority: Int) {
        LEVEL_50(1),
        LEVEL_90(2),
        LEVEL_100(3)
    }
    @Volatile
    private var isMotivationalAnimating = false
    private val motivationalQueue: MutableSet<MotivationLevel> = mutableSetOf()
    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (url.host == "water.coolcoder.hackclub.app") {
                    for (cookie in cookies) {
                        sharedPrefs.edit {
                            putString(cookie.name, cookie.value)
                        }
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = mutableListOf<Cookie>()
                val allCookies = sharedPrefs.all
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

        sharedPrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)

        initViews()
        setupRainView()
        setupButtons()
        initializeNotifications()

        animateLayoutElements {
            coroutineScope.launch {
                val initialVolume = fetchTotalVolume()
                withContext(Dispatchers.Main) {
                    displayedVolume = initialVolume
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                    rainView.addWaterDirectly(initialVolume)
                    isVolumeInitialized = true
                    Log.d("server_response", "Initial server volume = $initialVolume ml")
                    isAnimationActive = false
                    updateButtonStates()
                }
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.itemIconTintList = null

        val settingsIcon: ImageView = findViewById(R.id.settingsIcon)
        val pageTitle: TextView = findViewById(R.id.appTitle)
        val decorView = window.decorView
        val fitSystemWindows = decorView.fitsSystemWindows
        val marginTopDp = if(!fitSystemWindows) 36 else 24
        val marginTopPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginTopDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        settingsIcon.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = marginTopPx
        }
        pageTitle.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = marginTopPx
        }


        rainView = findViewById(R.id.rainView)
        waterVolumeText = findViewById(R.id.waterVolume)
        dailyGoal = sharedPrefs.getFloat("daily_volume_goal", 3000f)
        rainView.onVolumeChanged = { dropletVolume ->
            runOnUiThread {
                if (!isTimedRainActive) {
                    displayedVolume += dropletVolume
                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                }
                val motivationalTextDisplayed50 = sharedPrefs.getBoolean("motivational_text_displayed_50", false)
                val motivationalTextDisplayed90 = sharedPrefs.getBoolean("motivational_text_displayed_90", false)
                val motivationalTextDisplayed100 = sharedPrefs.getBoolean("motivational_text_displayed_100", false)
                val vibrator = getSystemService<Vibrator>()

                if(displayedVolume/dailyGoal >= 0.5 && !motivationalTextDisplayed50){
                    animateMotivationalText50()
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                    sharedPrefs.edit{
                        putBoolean("motivational_text_displayed_50", true)
                    }
                }
                if(displayedVolume/dailyGoal >= 0.9 && !motivationalTextDisplayed90){
                    animateMotivationalText90()
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                    sharedPrefs.edit{
                        putBoolean("motivational_text_displayed_90", true)
                    }
                }
                if(displayedVolume/dailyGoal >= 1 && !motivationalTextDisplayed100){
                    animateMotivationalText100()
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                    sharedPrefs.edit{
                        putBoolean("motivational_text_displayed_100", true)
                    }
                }
            }
            Log.d("RainVolume", "Displayed volume: $displayedVolume")
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

    override fun onPause() {
        super.onPause()
        if (isVolumeInitialized) {
            updateQuantity(displayedVolume)
        }
        isVolumeInitialized = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isVolumeInitialized) {
            updateQuantity(displayedVolume)
        }
        isVolumeInitialized = false
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
            e1: MotionEvent?, e2: MotionEvent,
            velocityX: Float, velocityY: Float
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
        val settingsIntent = Intent(applicationContext, SettingsActivity::class.java).apply {
            putExtra("caller_activity", "MainActivity")
        }
        startActivity(settingsIntent)
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
    }

    private fun updateQuantity(volume: Float) {
        val url = "https://water.coolcoder.hackclub.app/api/log"
        val json = JSONObject().apply { put("volume_ml", volume) }.toString()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        sharedPrefs.edit{
            putFloat("current_volume", volume)
        }

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val serverReply = response.body?.use { it.string() } ?: "Empty response"
                Log.d("server_response", "$serverReply, line 233 Main")
            }
        })
    }

    private suspend fun fetchTotalVolume(): Float = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://water.coolcoder.hackclub.app/api/detailed-stats")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val total = JSONObject(body).optDouble("today_volume_ml", 0.0)
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
        val otherElements = listOf(appTitle, fillButton, add250Button, add500Button, add750Button)
        rainView.post {
            rainView.registerGlassContainer(glassContainer)
            otherElements.forEach { rainView.registerUIElement(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
        val vibrator = getSystemService<Vibrator>()
        setupPressable(fillButton, vibrator, R.drawable.pressed_button_circle,
            onPress = {
                if (!isTimedRainActive) rainView.startRain()
            },
            onRelease = {
                if (!isTimedRainActive) rainView.stopRain()
            }
        )
        setupVolumeButton(add250Button, AMOUNT_250)
        setupVolumeButton(add500Button, AMOUNT_500)
        setupVolumeButton(add750Button, AMOUNT_750)
    }

    private fun setupVolumeButton(button: Button, amount: Int) {
        val vibrator = getSystemService<Vibrator>()
        val dailyGoal = sharedPrefs.getFloat("daily_volume_goal", 3000f)
        setupPressable(button, vibrator, R.drawable.rounded_transparent_square,
            onPress = {
                if (!isTimedRainActive) {
                    displayedVolume += amount
                    val progress = displayedVolume / dailyGoal
                    val motivationalTextDisplayed50 = sharedPrefs.getBoolean("motivational_text_displayed_50", false)
                    val motivationalTextDisplayed90 = sharedPrefs.getBoolean("motivational_text_displayed_90", false)
                    val motivationalTextDisplayed100 = sharedPrefs.getBoolean("motivational_text_displayed_100", false)

                    when{
                        progress >= 1f && !motivationalTextDisplayed100 ->{
                            animateMotivationalText100()
                            sharedPrefs.edit{
                                putBoolean("motivational_text_displayed_100", true)
                            }
                        }
                        progress >= 0.9f && !motivationalTextDisplayed90 ->{
                            animateMotivationalText90()
                            sharedPrefs.edit{
                                putBoolean("motivational_text_displayed_90", true)
                            }
                        }
                        progress >= 0.5f && !motivationalTextDisplayed50 ->{
                            animateMotivationalText50()
                            sharedPrefs.edit{
                                putBoolean("motivational_text_displayed_50", true)
                            }
                        }
                    }


                    waterVolumeText.text = String.format("%.1f ml", displayedVolume)
                    rainView.addWaterDirectly(amount.toFloat())
                }
            },
            onRelease = {}
        )
    }

    private fun updateButtonStates() {
        val alpha = if (isAnimationActive) 0.5f else 1.0f
        fillButton.alpha = alpha
        add250Button.alpha = alpha
        add500Button.alpha = alpha
        add750Button.alpha = alpha

        fillButton.isEnabled = !isAnimationActive
        add250Button.isEnabled = !isAnimationActive
        add500Button.isEnabled = !isAnimationActive
        add750Button.isEnabled = !isAnimationActive
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
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
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

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    @Deprecated("Use OnBackPressedDispatcher instead.")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithoutAnimation()
    }
    private fun initializeNotifications() {
        val sharedPrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)
        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)

        if (isNotificationsEnabled) {
            NotificationScheduler.scheduleNotifications(this)
        }
    }
    private fun animateLayoutElements(onAnimationComplete: () -> Unit) {
        isAnimationActive = true
        updateButtonStates()

        val glassContainer = findViewById<FrameLayout>(R.id.glassContainer)
        val waterVolume = findViewById<TextView>(R.id.waterVolume)
        val circleButton = findViewById<Button>(R.id.fillButton)
        val quickAddContainer = findViewById<LinearLayout>(R.id.quickAddContainer)

        glassContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()


        waterVolume.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
        circleButton.alpha = 0f
        circleButton.animate()
            .alpha(0.5f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        quickAddContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationComplete()
                }

                @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                    onAnimationComplete()
                }
            })
            .start()
    }
    private fun animateMotivationalText50() {
        enqueueMotivationalAnimation(MotivationLevel.LEVEL_50) {}
    }
    private fun animateMotivationalText90() {
        enqueueMotivationalAnimation(MotivationLevel.LEVEL_90) {}
    }
    private fun animateMotivationalText100() {
        enqueueMotivationalAnimation(MotivationLevel.LEVEL_100) {}
    }
    private fun enqueueMotivationalAnimation(level: MotivationLevel, animation: () -> Unit){
        synchronized(motivationalQueue) {
            motivationalQueue.add(level)

            val highest = motivationalQueue.maxByOrNull {it.priority}
            motivationalQueue.retainAll(setOfNotNull(highest))

            if(!isMotivationalAnimating && highest != null){
                runMotivationalAnimation(highest)
            }
        }
    }
    private fun runMotivationalAnimation(level: MotivationLevel){
        val motivationalText: TextView = when(level){
            MotivationLevel.LEVEL_50 -> findViewById(R.id.motivation_50)
            MotivationLevel.LEVEL_90 -> findViewById(R.id.motivation_90)
            MotivationLevel.LEVEL_100 -> findViewById(R.id.motivation_100)
        }

        isMotivationalAnimating = true

        motivationalText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction{
                motivationalText.postDelayed({
                    motivationalText.animate()
                        .alpha(0f)
                        .translationY(-20f)
                        .setDuration(400)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction{
                            synchronized(motivationalQueue){
                                motivationalQueue.clear()
                                isMotivationalAnimating = false
                            }
                        }
                        .start()
                }, 1000)
            }
            .start()
    }
}