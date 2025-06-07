package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt

class ChallengesActivity : AppCompatActivity() {
    private var isBellSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_challenges)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_challenges
        bottomNavigationView.itemIconTintList = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_challenges -> true
                R.id.nav_insights -> {
                    startActivity(Intent(applicationContext, InsightsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
        val bellIcon: ImageView = findViewById(R.id.bellIcon)

        bellIcon.setOnClickListener {
            if (isBellSelected) {
                bellIcon.setImageResource(R.drawable.ic_bell_unselected)
            } else {

                bellIcon.setImageResource(R.drawable.ic_bell)
            }


            isBellSelected = !isBellSelected
        }
    }
}