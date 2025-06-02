package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt

class InsightsActivity : AppCompatActivity() {
    private var isBellSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = "#292929".toColorInt()
        setContentView(R.layout.activity_insights)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_insights
        bottomNavigationView.itemIconTintList = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_insights -> true
                R.id.nav_home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_challenges -> {
                    startActivity(Intent(applicationContext, ChallengesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
        val bellIcon: ImageView = findViewById(R.id.bellIcon)
        val testButton: Button = findViewById(R.id.testButton)
        testButton.setBackgroundResource(R.drawable.rounded_transparent_square)

        bellIcon.setOnClickListener {
            if (isBellSelected) {
                bellIcon.setImageResource(R.drawable.ic_bell_unselected)
            } else {

                bellIcon.setImageResource(R.drawable.ic_bell)
            }


            isBellSelected = !isBellSelected
        }
        testButton.setOnClickListener {
            startActivity(Intent(applicationContext, TestActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }
}