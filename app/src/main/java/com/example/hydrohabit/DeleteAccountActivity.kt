package com.example.hydrohabit

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DeleteAccountActivity : AppCompatActivity() {

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
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delete_account)
        val backArrow: ImageView = findViewById(R.id.backIcon)
        val rootLayout = findViewById<RelativeLayout>(R.id.relativeLayout_account_delete)
        val passwordEditText = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordEditText = findViewById<EditText>(R.id.passwordInputAgain)
        val changePasswordButton = findViewById<Button>(R.id.enterButton)



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
            val newPassword = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword == confirmPassword) {
                deleteAccountAndLogout()
            } else {
                Toast.makeText(this, "The two passwords do not match.", Toast.LENGTH_SHORT).show()
                passwordEditText.text.clear()
                confirmPasswordEditText.text.clear()
            }
        }
        initializeCookies()
        initializePrefs()

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
    private fun deleteAccountAndLogout() {
        val url = "https://water.coolcoder.hackclub.app/api/delete_account"
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DeleteAccount", "Failed to delete account", e)
                runOnUiThread {
                    Toast.makeText(this@DeleteAccountActivity, "Failed to delete account", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@DeleteAccountActivity, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        CoroutineScope(Dispatchers.Main).launch {
                            clearCookiesAndLogout()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@DeleteAccountActivity, "Error deleting account", Toast.LENGTH_SHORT).show()
                        Log.e("server_response", "")
                    }
                }
            }
        })
    }
    private suspend fun clearCookiesAndLogout() {
        withContext(Dispatchers.Main) {
            sharedPrefs.all.keys.forEach { key ->
                if (!key.startsWith("__androidx_security_crypto_encrypted_prefs__")) {
                    sharedPrefs.edit { remove(key) }
                }
            }

            cookieStorage.clear()

            val intent = Intent(this@DeleteAccountActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top)
            finish()
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
}