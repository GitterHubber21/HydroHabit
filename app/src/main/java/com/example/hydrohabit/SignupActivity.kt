package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class SignupActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private fun forceHideKeyboard(){
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private fun sendSignupRequest(username: String, password: String) {
        val url = "https://water.coolcoder.hackclub.app/api/signup"
        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)

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
                    Toast.makeText(this@SignupActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val msg = when {
                    response.isSuccessful -> "Signup successful!"
                    response.code == 400 -> "Enter both username and password."
                    response.code == 409 -> "Username already exists."
                    else -> "Signup failed. Please try again.$response"

                }
                Log.d("server_response", "$response")

                runOnUiThread {
                    Toast.makeText(this@SignupActivity, msg, Toast.LENGTH_SHORT).show()

                    if (response.isSuccessful) {
                        startActivity(Intent(applicationContext, LoginActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
                    } else {
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
        setContentView(R.layout.activity_signup)

        val backArrow: ImageView = findViewById(R.id.backIcon)
        val signupButton: TextView = findViewById(R.id.signupButton)
        val usernameInput: EditText = findViewById(R.id.usernameInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val rootLayout = findViewById<RelativeLayout>(R.id.relativeLayout_signup)

        val passwordToggleFirst = findViewById<ImageView>(R.id.passwordToggleFirst)
        passwordToggleFirst.setOnClickListener {
            passwordToggleFirst.isSelected = !passwordToggleFirst.isSelected
            if (passwordToggleFirst.isSelected) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            passwordInput.setSelection(passwordInput.text?.length ?: 0)
        }

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

        signupButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please enter both fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 5){
                Toast.makeText(this, "Password must be at least 5 characters long.", Toast.LENGTH_SHORT).show()
                passwordInput.setText("")
                return@setOnClickListener
            }
            if(!password.any{ it.isUpperCase()}) {
                Toast.makeText(this, "Password must contain at least one uppercase letter.", Toast.LENGTH_SHORT).show()
                passwordInput.setText("")
                return@setOnClickListener
            }
            if(!password.any {!it.isLetterOrDigit()}) {
                Toast.makeText(this, "Password must contain at least one special character.", Toast.LENGTH_SHORT).show()
                passwordInput.setText("")
                return@setOnClickListener

            }

            sendSignupRequest(username, password)
        }
    }
    @Deprecated("This method has been deprecated in favor of using the\n      " +
            "{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n     " +
            " The OnBackPressedDispatcher controls how back button events are dispatched\n      " +
            "to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        finish()
    }
}