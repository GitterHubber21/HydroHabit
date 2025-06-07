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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit


class LoginActivity : AppCompatActivity() {
    private fun forceHideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val loginButton: Button = findViewById(R.id.loginButton)
        val signupText: TextView = findViewById(R.id.signupText)

        loginButton.setOnClickListener {

            sharedPreferences.edit {
                putBoolean("login_completed", true)
            }
            startActivity(Intent(applicationContext, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
            finish()
        }

        signupText.setOnClickListener {
            startActivity(Intent(applicationContext, SignupActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            finish()
        }
        val rootLayout = findViewById<RelativeLayout?>(R.id.relativeLayout_login)


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
    }
}