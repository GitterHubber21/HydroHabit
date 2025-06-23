package com.example.hydrohabit

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
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

class SettingsActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var sharedPrefs: SharedPreferences
    private val cookieStorage = mutableMapOf<String, String>()

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backArrow: ImageView = findViewById(R.id.backIcon)
        backArrow.rotation = 90f

        backArrow.setOnClickListener {
            finishWithAnimation()
        }

        initializePrefs()
        initializeCookies()

        gestureDetector = GestureDetector(this, SwipeGestureListener())

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.warning_floating_window, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        dialogView.findViewById<Button>(R.id.button_no).setOnClickListener {
            alertDialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.button_yes).setOnClickListener {
            alertDialog.dismiss()
            CoroutineScope(Dispatchers.Main).launch {
                clearCookiesAndLogout()
            }
        }
        alertDialog.show()
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
        finish()
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
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

    @Deprecated(
        "This method has been deprecated in favor of using the\n      " +
                "{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      " +
                "The OnBackPressedDispatcher controls how back button events are dispatched\n      " +
                "to one or more {@link OnBackPressedCallback} objects."
    )
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }
}