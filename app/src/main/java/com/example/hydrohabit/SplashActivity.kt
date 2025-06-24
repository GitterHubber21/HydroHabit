package com.example.hydrohabit

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import okhttp3.*
import java.io.IOException

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private val cookieStorage = mutableMapOf<String, String>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (url.host == "water.coolcoder.hackclub.app") {
                    cookies.forEach { cookieStorage[it.name] = it.value }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> =
                cookieStorage.map { (name, value) ->
                    Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(url.host)
                        .build()
                }
        })
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar: ProgressBar = findViewById(R.id.progressIndicator)
        val drawable = DrawableCompat.wrap(progressBar.indeterminateDrawable)
        DrawableCompat.setTint(drawable, getColor(android.R.color.white))
        progressBar.indeterminateDrawable = drawable

        initializePrefs()
        initializeCookies()
        checkAuthenticationStatus()
    }

    private fun initializePrefs() {
        sharedPrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)
    }

    private fun initializeCookies() {
        sharedPrefs.all.forEach { (k, v) ->
            if (v is String) cookieStorage[k] = v
        }
    }

    private fun checkAuthenticationStatus() {
        if (cookieStorage.isEmpty()) {
            handleExpiredSession()
        } else {
            validateStoredSession()
        }
    }

    private fun handleExpiredSession() {
        val onboardingDone = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)

        if (onboardingDone) {
            navigateToLogin()
        } else {
            navigateToOnboarding()
        }
    }

    private fun validateStoredSession() {
        val request = Request.Builder()
            .url("https://water.coolcoder.hackclub.app/api/current_user")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SplashActivity", "Session validation failed", e)
                runOnUiThread { handleExpiredSession() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    when (response.code) {
                        200 -> navigateToMain()
                        401 -> {
                            clearStoredSession()
                            handleExpiredSession()
                        }
                        else -> {
                            Log.w("SplashActivity", "Server error: ${response.code}")
                            handleExpiredSession()
                        }
                    }
                }
            }
        })
    }

    private fun clearStoredSession() {
        sharedPrefs.all.keys.forEach { key ->
            if (!key.startsWith("__androidx_security_crypto_encrypted_prefs__")) {
                sharedPrefs.edit { remove(key) }
            }
        }

        cookieStorage.clear()

        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit { putBoolean("login_completed", false) }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
        finish()
    }

    private fun navigateToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
        finish()
    }
}
