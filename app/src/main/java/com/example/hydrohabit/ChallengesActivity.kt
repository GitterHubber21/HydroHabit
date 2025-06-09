package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class ChallengesActivity : AppCompatActivity() {
    private var isBellSelected = false
    private lateinit var gestureDetector: GestureDetector
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
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
        val settingsIcon: ImageView = findViewById(R.id.settingsIcon)

        settingsIcon.setOnClickListener {
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
        }
        gestureDetector= GestureDetector(this, SwipeGestureListener())
    }
    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffY) > abs(diffX)) {

                if (diffY > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    finishWithAnimation()
                    return true
                }
            }

            return false
        }

    }
    private fun finishWithAnimation() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}