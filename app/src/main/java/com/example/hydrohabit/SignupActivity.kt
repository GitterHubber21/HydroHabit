package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        val backArrow: ImageView = findViewById(R.id.backIcon)

        backArrow.setOnClickListener {
             startActivity(Intent(applicationContext, LoginActivity::class.java))
             overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        }
    }
}