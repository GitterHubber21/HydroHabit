package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator
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
            finishWithAnimation()
        }
        gestureDetector= GestureDetector(this, SwipeGestureListener())
        animateCardDealing()
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
        val settingsIntent = Intent(applicationContext, SettingsActivity::class.java).apply {
            putExtra("caller_activity", "ChallengesActivity")
        }
        startActivity(settingsIntent)
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_challenges
    }
    private fun finishWithoutAnimation(){
        finish()
        overridePendingTransition(0, 0)
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithoutAnimation()
    }
    private fun animateCardDealing(){

        val square1 = findViewById<TextView>(R.id.square1)
        val square2 = findViewById<TextView>(R.id.square2)
        val square3 = findViewById<TextView>(R.id.square3)
        val square4 = findViewById<TextView>(R.id.square4)
        val dailyTitle = findViewById<TextView>(R.id.dailyTitle)
        val monthlyTitle = findViewById<TextView>(R.id.monthlyTitle)
        val monthlyChallenge = findViewById<TextView>(R.id.monthlyChallengeDisplay)

        val animationDuration = 600L
        val dealingDelay = 200L

        square4.postDelayed({
            square4.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animationDuration)
                .start()
        }, dealingDelay)
        square2.postDelayed({
            square2.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animationDuration)
                .start()
        }, dealingDelay*2)
        square1.postDelayed({
            square1.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animationDuration)
                .start()
        }, dealingDelay*3)
        square3.postDelayed({
            square3.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(animationDuration)
                .start()
        }, dealingDelay*4)

        val finalDelay = dealingDelay*5
        monthlyTitle.postDelayed({
            monthlyTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, finalDelay)
        dailyTitle.postDelayed({
            dailyTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, finalDelay)

        monthlyChallenge.postDelayed({
            monthlyChallenge.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }, finalDelay + 200)
    }
}