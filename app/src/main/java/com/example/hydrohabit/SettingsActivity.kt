package com.example.hydrohabit

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.abs
import androidx.core.content.edit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import android.widget.Toast
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.coroutineScope
import okhttp3.internal.cache.DiskLruCache

class SettingsActivity : AppCompatActivity() {


    private lateinit var rainView: RainView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var sharedPrefs: SharedPreferences
    private val cookieStorage = mutableMapOf<String, String>()
    private val ANIMATION_DELAY = 100L

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (url.host == "water.coolcoder.hackclub.app") {
                    for (cookie in cookies) {
                        cookieStorage[cookie.name] = cookie.value
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = mutableListOf<Cookie>()
                for ((name, value) in cookieStorage) {
                    cookies.add(
                        Cookie.Builder()
                            .name(name)
                            .value(value)
                            .domain(url.host)
                            .build()
                    )
                }
                return cookies
            }
        })
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        val logoutButton: TextView = findViewById(R.id.logoutButton)
        val resetPasswordButton: TextView = findViewById(R.id.resetPasswordButton)
        val resetWaterButton: TextView = findViewById(R.id.resetButton)
        val profileButton: TextView = findViewById(R.id.profileButton)
        val notificationButton: TextView = findViewById(R.id.notificationButton)
        val deleteAccountButton: TextView = findViewById(R.id.deleteAccountButton)
        val changeGoalButton: TextView = findViewById(R.id.changeGoalButton)


        val backArrow: ImageView = findViewById(R.id.backIcon)
        backArrow.rotation = 90f

        backArrow.setOnClickListener {
            finishWithAnimation()
        }
        val pageTitle: TextView = findViewById(R.id.appTitle)
        val decorView = window.decorView
        val fitSystemWindows = decorView.fitsSystemWindows
        val marginTopDp = if(!fitSystemWindows) 36 else 24
        val marginTopPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginTopDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        backArrow.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = marginTopPx
        }
        pageTitle.updateLayoutParams<LinearLayout.LayoutParams> {
            topMargin = marginTopPx
        }

        initializePrefs()
        initializeCookies()

        gestureDetector = GestureDetector(this, SwipeGestureListener())

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        resetPasswordButton.setOnClickListener {
            showPasswordConfirmationDialog()
        }
        resetWaterButton.setOnClickListener {
            showWaterConfirmationDialog()
        }
        profileButton.setOnClickListener {
            showProfilePopup()
        }
        deleteAccountButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        changeGoalButton.setOnClickListener {
            showGoalChangePopup()
        }

        val isNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", false)
        notificationButton.isSelected = isNotificationsEnabled

        notificationButton.setOnClickListener {
            val newState = !notificationButton.isSelected
            notificationButton.isSelected = newState

            sharedPrefs.edit {
                putBoolean("notifications_enabled", newState)
            }

            if (newState) {
                NotificationScheduler.forceScheduleNotifications(this)
                Toast.makeText(this, "Reminders enabled", Toast.LENGTH_SHORT).show()
            } else {
                NotificationScheduler.cancelNotifications(this)
                Toast.makeText(this, "Reminders disabled", Toast.LENGTH_SHORT).show()
            }

            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        animateLayoutElements()

    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.logout_warning, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        dialogView.findViewById<TextView>(R.id.button_no).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.button_yes).setOnClickListener {
            alertDialog.dismiss()
            CoroutineScope(Dispatchers.Main).launch {
                clearCookiesAndLogout()
            }
        }
        alertDialog.show()
    }

    private fun showPasswordConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.password_warning, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        dialogView.findViewById<TextView>(R.id.button_no).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.button_yes).setOnClickListener {
            startActivity(Intent(applicationContext, PasswordChangeActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun showWaterConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.water_warning, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        dialogView.findViewById<TextView>(R.id.button_no).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.button_yes).setOnClickListener {
            resetQuantity()
            Toast.makeText(this@SettingsActivity, "Volume successfully reset", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun showProfilePopup() {
        val dialogView = layoutInflater.inflate(R.layout.profile_popup, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
        CoroutineScope(Dispatchers.Main).launch {
            val username = fetchUsernameFromApi()
            val usernameTextView = dialogView.findViewById<TextView>(R.id.usernameDisplay)
            usernameTextView.text = "Username: $username"
        }

        dialogView.findViewById<TextView>(R.id.button_cancel).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.button_logout).setOnClickListener {
            alertDialog.dismiss()
            CoroutineScope(Dispatchers.Main).launch {
                clearCookiesAndLogout()
            }
        }
    }
    private fun showDeleteConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.delete_warning, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        dialogView.findViewById<TextView>(R.id.button_no).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.button_yes).setOnClickListener {
            startActivity(Intent(applicationContext, DeleteAccountActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    private fun showGoalChangePopup() {
        val dialogView = layoutInflater.inflate(R.layout.goal_change_popup, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        dialogView.findViewById<TextView>(R.id.button_cancel).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.button_set).setOnClickListener {
            val goalInputArea = dialogView.findViewById<EditText>(R.id.goal_input)
            val newGoal = goalInputArea.text.toString().trim()
            if(!newGoal.isEmpty()) {
                postDailyGoal(newGoal.toFloat())
                alertDialog.dismiss()
            }else {
                alertDialog.dismiss()
            }
        }
    }


    private fun initializePrefs() {
        sharedPrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)
    }

    private fun initializeCookies() {
        val allCookies = sharedPrefs.all
        for ((key, value) in allCookies) {
            if (value is String) {
                cookieStorage[key] = value
            }
        }
    }

    private suspend fun fetchUsernameFromApi(): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://water.coolcoder.hackclub.app/api/current_user")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            jsonResponse.getString("username")
                        } else {
                            "No response body"
                        }
                    } else {
                        "Failed to load user (${response.code})"
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Network error", e)
                "Network error"
            }
        }
    }

    private fun finishWithAnimation() {
        val callerActivity = intent.getStringExtra("caller_activity")

        try {
            val activityClass = Class.forName("com.example.hydrohabit.$callerActivity")
            val intent = Intent(this, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
            finish()

        }catch (e: ClassNotFoundException) {
            Log.e("startError", "$e")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
            finish()

        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
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
                if (diffY < swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    finishWithAnimation()
                    return true
                }
            }

            return false
        }
    }

    private suspend fun clearCookiesAndLogout() {
        withContext(Dispatchers.Main) {
            sharedPrefs.all.keys.forEach { key ->
                if (!key.startsWith("__androidx_security_crypto_encrypted_prefs__")) {
                    sharedPrefs.edit { remove(key) }
                }
            }

            cookieStorage.clear()

            val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
            finish()
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }
    private fun resetQuantity() {
        val url = "https://water.coolcoder.hackclub.app/api/log"
        val json = JSONObject().apply { put("volume_ml", 0.0) }.toString()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val serverReply = response.body?.use { it.string() } ?: "Empty response"
                Log.d("server_response", serverReply)
            }
        })
    }
    private fun postDailyGoal(newGoal: Float) {

        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://water.coolcoder.hackclub.app/api/daily-goal"
            try {
                val json = JSONObject().apply {
                    put("daily_volume_goal", newGoal)
                }.toString()
                val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("server_response", "Successfully posted daily goal = $newGoal ml")
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity,"Goal updated successfully", Toast.LENGTH_SHORT).show()
                            sharedPrefs.edit{putFloat("daily_volume_goal", newGoal)}
                        }
                    } else {
                        Log.w("server_response", "POST daily_goal failed: ${response.code}")
                        Toast.makeText(this@SettingsActivity,"Goal update failed, ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("server_response", "Error posting goal", e)
                Toast.makeText(this@SettingsActivity,"Error during goal update", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun animateLayoutElements(){
        val titleGeneral = findViewById<TextView>(R.id.titleGeneral)
        val layoutGeneral = findViewById<LinearLayout>(R.id.layout_general)

        val titleSecurity = findViewById<TextView>(R.id.titleSecurity)
        val layoutSecurity = findViewById<LinearLayout>(R.id.layout_security)

        val titleLicenses = findViewById<TextView>(R.id.titleLicenses)
        val layoutLicenses = findViewById<LinearLayout>(R.id.layout_licenses)

        val titleLogout = findViewById<TextView>(R.id.titleLogout)
        val layoutLogout = findViewById<LinearLayout>(R.id.layout_logout)

        titleGeneral.postDelayed({
            titleGeneral.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY)
        layoutGeneral.postDelayed({
            layoutGeneral.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY)


        titleSecurity.postDelayed({
            titleSecurity.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*2)
        layoutSecurity.postDelayed({
            layoutSecurity.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*2)


        titleLicenses.postDelayed({
            titleLicenses.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*3)
        layoutLicenses.postDelayed({
            layoutLicenses.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*3)


        titleLogout.postDelayed({
            titleLogout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*4)
        layoutLogout.postDelayed({
            layoutLogout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, ANIMATION_DELAY*4)

    }



}