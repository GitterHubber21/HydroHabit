package com.example.hydrohabit

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.*
import java.io.IOException
import androidx.core.content.edit

class SplashActivity : AppCompatActivity() {

    private lateinit var encryptedPrefs: SharedPreferences
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
        setContentView(R.layout.activity_splash)

        initializeEncryptedPrefs()
        initializeCookies()

        checkAuthenticationStatus()
    }

    private fun initializeEncryptedPrefs() {
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
    }

    private fun initializeCookies() {
        val allCookies = encryptedPrefs.all
        for ((key, value) in allCookies) {
            if (value is String) {
                cookieStorage[key] = value
            }
        }
    }

    private fun checkAuthenticationStatus() {
        val loginCompleted = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("login_completed", false)
        val onboardingCompleted = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("onboarding_complete", false)
        if (!loginCompleted && onboardingCompleted || cookieStorage.isEmpty() ) {
            validateStoredSession()
            navigateToLogin()

        }
        if(!onboardingCompleted){
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
                runOnUiThread {
                    navigateToLogin()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    when (response.code) {
                        200 -> {
                            Log.d("SplashActivity", "Session valid, navigating to MainActivity")
                            navigateToMain()
                        }
                        401 -> {
                            Log.d("SplashActivity", "Session expired, clearing data")
                            clearStoredSession()
                            navigateToLogin()
                        }
                        else -> {
                            Log.w("SplashActivity", "Server error: ${response.code}")
                            navigateToLogin()
                        }
                    }
                }
            }
        })
    }

    private fun clearStoredSession() {

        encryptedPrefs.edit { clear() }
        cookieStorage.clear()

        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit {
                putBoolean("login_completed", false)
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
        finish()
    }
    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
        finish()
    }
}