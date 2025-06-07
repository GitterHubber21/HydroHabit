package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.core.content.edit
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val goButton: Button = findViewById(R.id.goButton)
        val welcomeText: TextView = findViewById(R.id.welcomeText)



        welcomeText.alpha = 0f
        goButton.alpha = 0f


        animateWelcomeText(welcomeText)
        animateGoButton(goButton)

        goButton.setOnClickListener {

            sharedPreferences.edit {
                putBoolean("onboarding_completed", true)
            }
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
            finish()
        }
    }

    private fun animateWelcomeText(welcomeText: TextView) {
        welcomeText.translationY = -800f
        welcomeText.alpha = 0f

        welcomeText.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                welcomeText.animate()
                    .translationY(-20f)
                    .setDuration(300)
                    .withEndAction {
                        welcomeText.animate()
                            .translationY(0f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun animateGoButton(goButton: Button) {
        Handler(Looper.getMainLooper()).postDelayed({
            goButton.scaleX = 0f
            goButton.scaleY = 0f
            goButton.alpha = 0f

            goButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    startPulsingAnimation(goButton)
                }
                .start()
        }, 1500)
    }

    private fun startPulsingAnimation(button: Button) {
        val pulseAnimation = android.view.animation.ScaleAnimation(
            1f, 1.05f,
            1f, 1.05f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode = android.view.animation.Animation.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }

        button.startAnimation(pulseAnimation)
    }
}