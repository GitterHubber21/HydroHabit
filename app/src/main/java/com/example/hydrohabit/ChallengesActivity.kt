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
import android.widget.FrameLayout
import android.content.SharedPreferences
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.updateLayoutParams
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.core.content.edit
import com.google.android.material.progressindicator.CircularProgressIndicator

class ChallengesActivity : AppCompatActivity() {
    private lateinit var gestureDetector: GestureDetector
    private lateinit var cards: List<FrameLayout>
    private val isFlipped = BooleanArray(4)
    private var dailyGoal = 0f
    data class Challenge(val text: String, val type: Int)
    enum class ChallengeStatus{
        COMPLETED,
        NOT_YET_COMPLETED,
        IMPOSSIBLE
    }
    private var dailyChallenges: Map<Int, Challenge> = mapOf()

    private lateinit var sharedPreferences: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        setContentView(R.layout.activity_challenges)
        dailyGoal = sharedPreferences.getFloat("daily_volume_goal", 3000f)
        dailyChallenges = mapOf(
            0 to Challenge("Hit ${(dailyGoal*0.6f).toInt()} ml before 6 PM.", 1),
            1 to Challenge("Drink ${(dailyGoal*0.5f).toInt()} ml before lunch.", 1),
            2 to Challenge("Pour into your glass three times today.", 2),
            3 to Challenge("Complete your daily hydration goal.", 2),
            4 to Challenge("Pour into your glass before 9 AM.", 2)
        )

        //1 means circularProgressIndicator measurement
        //2 means checked or not measurement

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
        updateMonthlyChallengeDisplay()
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
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
            val challengeId = todaysChallenges[i]
            val challenge = dailyChallenges[challengeId]
            val challengeType = challenge?.type ?: 1

            card.setOnClickListener { flipCard(card, i) }

            val front = card.findViewById<TextView>(frontIds[i])
            val backCircle = card.findViewById<FrameLayout>(R.id.card_back_circle)
            val backCheck = card.findViewById<FrameLayout>(R.id.card_back_check)
            backCircle.alpha = 0f
            backCheck.alpha = 0f

            val back = when (challengeType) {
                1 -> backCircle
                2 -> backCheck
                else -> backCircle
            }

            front.text = challenge?.text ?: "No challenge"
            front.alpha = 1f

            editor.putInt("card_back_id_$i", back.id)

            if(challenge != null){
                updateCardBack(card, challenge, i)
            }
        }
        editor.apply()
    }



    private fun flipCard(card: FrameLayout, index: Int) {
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val backId = sharedPrefs.getInt("card_back_id_$index", R.id.card_back_circle)

        val front = card.findViewById<TextView>(R.id.card_front)
        val back = card.findViewById<FrameLayout>(backId)

        val backLayouts = listOf(
            R.id.card_back_circle,
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
        val monthlyChallenge = findViewById<LinearLayout>(R.id.monthlyChallengeDisplay)

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
    private fun getTodaysChallenges(): List<Int> {
        val today = dateFormat.format(Date())
        val savedDate = sharedPreferences.getString("challenge_date", "")

        return if (savedDate == today) {
            (0..3).map { i ->
                sharedPreferences.getInt("challenge_$i", i)
            }
        } else {
            val availableChallengeIds = dailyChallenges.keys.toList()
            val usedIndices = mutableSetOf<Int>()
            val newChallenges = mutableListOf<Int>()

            while (newChallenges.size < 4 && usedIndices.size < availableChallengeIds.size) {
                val random = availableChallengeIds.random()
                if (random !in usedIndices) {
                    usedIndices.add(random)
                    newChallenges.add(random)
                }
            }

            sharedPreferences.edit {
                putString("challenge_date", today)
                newChallenges.forEachIndexed { index, value ->
                    putInt("challenge_$index", value)
                }
                putBoolean("challenge_generation_since_goal_change", true)
            }
            newChallenges
        }
    }

    private fun evaluateChallengeProgress(challenge: Challenge): Triple<Float, Boolean, ChallengeStatus> {
        val currentVolume = sharedPreferences.getFloat("current_volume", 0f)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val pourNumber = sharedPreferences.getInt("pour_number", 0)

        return when (challenge.type) {
            1 -> {
                val goalVolume = Regex("\\d+").find(challenge.text)?.value?.toFloatOrNull() ?: return Triple(0f, false, ChallengeStatus.NOT_YET_COMPLETED)

                val timeLimit = when {
                    challenge.text.contains("before lunch", true) -> 12
                    challenge.text.contains("before 6 PM", true) -> 18
                    else -> null
                }

                val progress = (currentVolume / goalVolume).coerceAtMost(1f)
                return when {
                    progress >= 1f && (timeLimit == null || hour < timeLimit) ->
                        Triple(progress, true, ChallengeStatus.COMPLETED)

                    timeLimit != null && hour >= timeLimit ->
                        Triple(progress, false, ChallengeStatus.IMPOSSIBLE)

                    else ->
                        Triple(progress, false, ChallengeStatus.NOT_YET_COMPLETED)
                }
            }

            2 -> {
                val status = when {
                    challenge.text.contains("before 9 AM", true) -> {
                        if (hour < 9 && currentVolume > 0f) ChallengeStatus.COMPLETED
                        else if (hour >= 9 && currentVolume >= 0f) ChallengeStatus.IMPOSSIBLE
                        else ChallengeStatus.NOT_YET_COMPLETED
                    }

                    challenge.text.contains("complete your daily hydration goal", true) ->
                        if (currentVolume >= dailyGoal) ChallengeStatus.COMPLETED else ChallengeStatus.NOT_YET_COMPLETED

                    challenge.text.contains("glass three times today", true) ->
                        if (pourNumber >= 3) ChallengeStatus.COMPLETED else ChallengeStatus.NOT_YET_COMPLETED

                    else -> ChallengeStatus.NOT_YET_COMPLETED
                }

                return Triple(if (status == ChallengeStatus.COMPLETED) 1f else 0f, status == ChallengeStatus.COMPLETED, status)
            }

            else -> Triple(0f, false, ChallengeStatus.NOT_YET_COMPLETED)
        }
    }
    private fun updateCardBack(card: FrameLayout, challenge: Challenge, index: Int) {
        val (progress, isCompleted, status) = evaluateChallengeProgress(challenge)

        val circleProgress = card.findViewById<CircularProgressIndicator>(R.id.circle_progress_indicator)
        val percentText = card.findViewById<TextView>(R.id.progress_text_percent)
        val checkIcon = card.findViewById<ImageView>(R.id.check_icon)
        val notYetCompleted = card.findViewById<TextView>(R.id.progress_text_not_yet_completed)

        when (challenge.type) {
            1 -> {
                val percent = (progress * 100).toInt()
                circleProgress.progress = percent
                percentText.text = "$percent%"

                when (status) {
                    ChallengeStatus.COMPLETED -> {
                        card.setBackgroundResource(R.drawable.rounded_transparent_square_glow_outline)
                    }
                    ChallengeStatus.IMPOSSIBLE -> {
                        percentText.text = "Missed"
                        circleProgress.alpha=0f
                        card.setBackgroundResource(R.drawable.rounded_transparent_square_glow_outline_fail)}
                    ChallengeStatus.NOT_YET_COMPLETED -> {percentText.text = "$percent%"}
                }
            }

            2 -> {
                when (status) {
                    ChallengeStatus.COMPLETED -> {
                        checkIcon.alpha = 1f
                        notYetCompleted.alpha = 0f
                        card.setBackgroundResource(R.drawable.rounded_transparent_square_glow_outline)
                    }

                    ChallengeStatus.NOT_YET_COMPLETED -> {
                        checkIcon.alpha = 0f
                        notYetCompleted.alpha = 1f
                    }

                    ChallengeStatus.IMPOSSIBLE -> {
                        checkIcon.alpha = 0f
                        notYetCompleted.text = "Too Late"
                        notYetCompleted.alpha = 1f
                        card.setBackgroundResource(R.drawable.rounded_transparent_square_glow_outline_fail)
                    }
                }
            }
        }
    }
    private fun updateMonthlyChallengeDisplay() {
        val daysInMonth = sharedPreferences.getInt("days_in_current_month", 0)
        val completedDays = sharedPreferences.getInt("number_of_completed_days_in_current_month", 0)
        val monthlyChallengeDisplay = findViewById<TextView>(R.id.monthlyChallengeText)
        val monthDisplayTextProgress = findViewById<TextView>(R.id.monthlyChallengeProgress)
        val monthlyChallengeLayout = findViewById<LinearLayout>(R.id.monthlyChallengeDisplay)

        if (daysInMonth > 0) {
            val calendar = Calendar.getInstance()
            val currentMonthIndex = calendar.get(Calendar.MONTH)
            val englishMonths = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val currentMonth = englishMonths[currentMonthIndex]

            monthlyChallengeDisplay.text =
                "Complete your daily goal every day in $currentMonth."

            monthDisplayTextProgress.text =
                "$completedDays/$daysInMonth"

            if (completedDays == daysInMonth) {
                monthlyChallengeLayout.setBackgroundResource(R.drawable.rounded_transparent_square_glow_outline)
            }
        } else {
            monthDisplayTextProgress.text = "Monthly challenge data not available"
        }
    }


}
