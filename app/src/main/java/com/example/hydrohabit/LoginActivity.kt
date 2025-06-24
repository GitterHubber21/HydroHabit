package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var cookiePrefs: SharedPreferences

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                if (url.host == "water.coolcoder.hackclub.app") {
                    for (cookie in cookies) {
                        cookiePrefs.edit {
                            putString(cookie.name, cookie.value)
                        }
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = mutableListOf<Cookie>()
                val allCookies = cookiePrefs.all
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

    private fun forceHideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun attemptLogin(username: String, password: String) {
        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val url = "https://water.coolcoder.hackclub.app/api/login"
        val jsonObject = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val json = jsonObject.toString()
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), json
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    usernameInput.text.clear()
                    passwordInput.text.clear()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        getSharedPreferences("app_prefs", MODE_PRIVATE).edit {
                            putBoolean("login_completed", true)
                            apply()
                        }
                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(
                            Intent(this@LoginActivity, MainActivity::class.java)
                        )
                        overridePendingTransition(
                            R.anim.slide_in_from_top,
                            R.anim.slide_out_to_bottom
                        )
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid credentials",
                            Toast.LENGTH_SHORT
                        ).show()
                        usernameInput.text.clear()
                        passwordInput.text.clear()
                    }
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        cookiePrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)

        val loginButton: Button = findViewById(R.id.loginButton)
        val signupText: TextView = findViewById(R.id.signupText)
        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val rootLayout: RelativeLayout = findViewById(R.id.relativeLayout_login)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Enter both username and password", Toast.LENGTH_SHORT).show()
            } else {
                attemptLogin(username, password)
            }
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(
                R.anim.slide_in_from_right,
                R.anim.slide_out_to_left
            )
        }

        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.let { focused ->
                    if (focused is EditText) {
                        val r = Rect()
                        focused.getGlobalVisibleRect(r)
                        if (!r.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            focused.clearFocus()
                            forceHideKeyboard()
                        }
                    }
                }
            }
            false
        }
    }
}
