package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import android.widget.Button
import android.widget.Toast


class PasswordChangeActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences

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
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_password_reset)
        val backArrow: ImageView = findViewById(R.id.backIcon)
        val rootLayout = findViewById<RelativeLayout>(R.id.relativeLayout_password_reset)
        val oldPasswordEditText = findViewById<EditText>(R.id.oldPasswordInput)
        val newPasswordEditText = findViewById<EditText>(R.id.newPasswordInput)
        val changePasswordButton = findViewById<Button>(R.id.changeButton)

        sharedPrefs = getSharedPreferences("secure_cookies", MODE_PRIVATE)

        backArrow.setOnClickListener {
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
            finish()
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
        changePasswordButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            if(!oldPassword.isEmpty()&&!newPassword.isEmpty())
                changePassword(oldPassword, newPassword)
            else{
                Toast.makeText(this@PasswordChangeActivity, "Both old and new passwords are required.", Toast.LENGTH_SHORT).show()
            }

        }

    }

    @Deprecated("This method has been deprecated in favor of using the\n      " +
            "{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n     " +
            " The OnBackPressedDispatcher controls how back button events are dispatched\n      " +
            "to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        finish()
    }
    private fun forceHideKeyboard(){
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
    private fun changePassword(oldPassword: String, newPassword: String) {
        val oldPasswordEditText = findViewById<EditText>(R.id.oldPasswordInput)
        val newPasswordEditText = findViewById<EditText>(R.id.newPasswordInput)

        val url = "https://water.coolcoder.hackclub.app/api/change_password"
        val json = JSONObject().apply {
            put("old_password", oldPassword)
            put("new_password", newPassword)
        }.toString()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    oldPasswordEditText.text.clear()
                    newPasswordEditText.text.clear()
                    Toast.makeText(this@PasswordChangeActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    when (response.code) {
                        200 -> {
                            oldPasswordEditText.text.clear()
                            newPasswordEditText.text.clear()
                            Toast.makeText(this@PasswordChangeActivity, "Password changed successfully.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(applicationContext, SettingsActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
                            finish()
                        }
                        401 -> {
                            oldPasswordEditText.text.clear()
                            Toast.makeText(this@PasswordChangeActivity, "Current password is incorrect.", Toast.LENGTH_SHORT).show()
                            oldPasswordEditText.setText("")
                        }
                        else -> {
                            try {
                                val jsonResponse = JSONObject(responseBody ?: "{}")
                                val errorMessage = jsonResponse.optString("error", "An error occurred")
                                Toast.makeText(this@PasswordChangeActivity, errorMessage, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@PasswordChangeActivity, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        })
    }
}