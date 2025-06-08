package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    private fun forceHideKeyboard(){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        val backArrow: ImageView = findViewById(R.id.backIcon)

        backArrow.setOnClickListener {
             startActivity(Intent(applicationContext, LoginActivity::class.java))
             overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        }

        val rootLayout= findViewById<RelativeLayout>(R.id.relativeLayout_signup)

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