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
import android.view.animation.DecelerateInterpolator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.widget.FrameLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.content.SharedPreferences
import android.util.TypedValue
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class ChallengesActivity : AppCompatActivity() {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var cards: List<FrameLayout>
    private val isFlipped = BooleanArray(4)
    private var dailyGoal = 0f
    private var dailyChallenges = mapOf<String, Any>()
    //1 means circularProgressIndicator measurement
    //2 means X times out of X times measurement
    //3 means checked or not measurement
    private lateinit var sharedPreferences: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedPreferences = getSharedPreferences("challenges_prefs", MODE_PRIVATE)
        setContentView(R.layout.activity_challenges)

        dailyGoal = sharedPreferences.getFloat("daily_volume_goal", 3000f)

        dailyChallenges = mapOf("Drink ${dailyGoal*0.8f} liter of water today." to 1,
            "Reach ${dailyGoal*0.75f} liters by the end of the day." to 1,
            "Hit ${dailyGoal*0.6f} liters before 6 PM." to 1,
            "Complete your daily hydration goal." to 3,
            "Drink ${dailyGoal*0.15f} mL every 3 hours." to 2,
            "Pour into your glass before 9 AM." to 3,
            "Drink ${dailyGoal*0.5f} mL before lunch." to 1,
            "Pour into your glass three times today." to 2)

        initializeViews()
        setupClickListeners()

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
        val pageTitle: TextView = findViewById(R.id.appTitle)
        val decorView = window.decorView
        val fitSystemWindows = decorView.fitsSystemWindows
        val marginTopDp = if(!fitSystemWindows) 36 else 24
        val marginTopPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginTopDp.toFloat(),
            resources.displayMetrics
        ).toInt()

        settingsIcon.updateLayoutParams<RelativeLayout.LayoutParams> {
            topMargin = marginTopPx
        }
        pageTitle.updateLayoutParams<RelativeLayout.LayoutParams> {
            topMargin = marginTopPx
        }
        settingsIcon.setOnClickListener {
            finishWithAnimation()
        }
        gestureDetector = GestureDetector(this, SwipeGestureListener())
        animateCardDealing()
    }

    private fun initializeViews() {
        cards = listOf(
            findViewById(R.id.card1),
            findViewById(R.id.card2),
            findViewById(R.id.card3),
            findViewById(R.id.card4)
        )
    }

    private fun setupClickListeners() {
        val sharedPrefs = getSharedPreferences("challenge_prefs", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val frontIds = listOf(
            R.id.card_front,
            R.id.card_front,
            R.id.card_front,
            R.id.card_front
        )
        val todaysChallenges = getTodaysChallenges()

        for (i in cards.indices) {
            val card = cards[i]
            val challengeText = todaysChallenges[i]
            val challengeType = dailyChallenges[challengeText] ?: 1

            card.setOnClickListener { flipCard(card, i) }

            val front = card.findViewById<TextView>(frontIds[i])
            val backCircle = card.findViewById<FrameLayout>(R.id.card_back_circle)
            val backText = card.findViewById<FrameLayout>(R.id.card_back_text)
            val backCheck = card.findViewById<FrameLayout>(R.id.card_back_check)
            backCircle.alpha = 0f
            backText.alpha = 0f
            backCheck.alpha = 0f

            val back = when (challengeType) {
                1 -> backCircle
                2 -> backText
                3 -> backCheck
                else -> backCircle
            }

            front.text = challengeText
            front.alpha = 1f

            editor.putInt("card_back_id_$i", back.id)
        }
        editor.apply()
    }



    private fun flipCard(card: FrameLayout, index: Int) {
        val sharedPref = getSharedPreferences("challenge_prefs", MODE_PRIVATE)
        val backId = sharedPref.getInt("card_back_id_$index", R.id.card_back_circle)

        val front = card.findViewById<TextView>(R.id.card_front)
        val back = card.findViewById<FrameLayout>(backId)

        val backLayouts = listOf(
            R.id.card_back_circle,
            R.id.card_back_text,
            R.id.card_back_check
        )

        card.isClickable = false

        card.animate()
            .rotationY(90f)
            .setDuration(150)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isFlipped[index] = !isFlipped[index]

                    if (isFlipped[index]) {

                        front.alpha = 0f
                        backLayouts.forEach {
                            val view = card.findViewById<FrameLayout>(it)
                            view?.alpha = if (it == backId) 1f else 0f
                        }
                    } else {

                        back.alpha = 0f
                        front.alpha = 1f
                    }

                    card.rotationY = -90f

                    card.animate()
                        .rotationY(0f)
                        .setDuration(150)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                card.isClickable = true
                            }
                        })
                }
            })
    }






    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
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

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    @Deprecated("Use OnBackPressedDispatcher instead.")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithoutAnimation()
    }

    private fun animateCardDealing() {
        val square1 = findViewById<FrameLayout>(R.id.card1_container)
        val square2 = findViewById<FrameLayout>(R.id.card2_container)
        val square3 = findViewById<FrameLayout>(R.id.card3_container)
        val square4 = findViewById<FrameLayout>(R.id.card4_container)
        val dailyTitle = findViewById<TextView>(R.id.dailyTitle)
        val monthlyTitle = findViewById<TextView>(R.id.monthlyTitle)
        val monthlyChallenge = findViewById<TextView>(R.id.monthlyChallengeDisplay)

        val animationDuration = 300L
        val dealingDelay = 200L

        listOf(square4, square3, square1, square2).forEachIndexed { i, square ->
            square.postDelayed({
                square.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .rotation(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(animationDuration)
                    .start()
            }, dealingDelay * (i + 1))
        }

        val finalDelay = dealingDelay * 5
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
    private fun getTodaysChallenges(): List<String> {
        val today = dateFormat.format(Date())
        val savedDate = sharedPreferences.getString("challenge_date", "")

        return if (savedDate == today) {
            (0..3).map { i ->
                sharedPreferences.getString("challenge_$i", dailyChallenges.keys.first()) ?: dailyChallenges.keys.first()
            }
        } else {
            val usedIndices = mutableSetOf<Int>()
            val newChallenges = mutableListOf<String>()
            val challengeList = dailyChallenges.keys.toList()

            repeat(4) {
                var challengeIndex: Int
                do {
                    challengeIndex = challengeList.indices.random()
                } while (challengeIndex in usedIndices)

                usedIndices.add(challengeIndex)
                newChallenges.add(challengeList[challengeIndex])
            }
            with(sharedPreferences.edit()) {
                putString("challenge_date", today)
                newChallenges.forEachIndexed { index, challenge ->
                    putString("challenge_$index", challenge)
                }
                apply()
            }
            newChallenges
        }
    }

}
