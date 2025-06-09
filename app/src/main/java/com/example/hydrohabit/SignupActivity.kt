package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private fun forceHideKeyboard(){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun sendSignupRequest(username: String, password: String) {
        val url = "https://water.coolcoder.hackclub.app/api/signup"

        val json = """
            {
                "username": "$username",
                "password": "$password"
            }
        """.trimIndent()

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SignupActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val msg = if (response.isSuccessful) {
                    "Signup successful!"

                } else {
                    "Signup failed: ${response.code}"
                }
                if(response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
                }
                else{
                    runOnUiThread {
                        Toast.makeText(this@SignupActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }


            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        val backArrow: ImageView = findViewById(R.id.backIcon)
        val loginButton: Button = findViewById(R.id.loginButton)
        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val rootLayout = findViewById<RelativeLayout>(R.id.relativeLayout_signup)

        backArrow.setOnClickListener {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        }

        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.let { focusedView ->
                    if (focusedView is EditText) {
                        val rect = Rect()
                        focusedView.getGlobalVisibleRect(rect)
                        if (!rect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            focusedView.clearFocus()
                            forceHideKeyboard()
                        }
                    }
                }
            }
            false
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                sendSignupRequest(username, password)
            } else {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
