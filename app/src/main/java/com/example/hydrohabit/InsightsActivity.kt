package com.example.hydrohabit

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.graphics.toColorInt
import androidx.core.content.ContextCompat
import java.util.*
import androidx.core.content.res.ResourcesCompat
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class InsightsActivity : AppCompatActivity() {
    private var isBellSelected = false
    private var cellSize = 0
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insights)

        calculateCellSize()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_insights
        bottomNavigationView.itemIconTintList = null
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_insights -> true
                R.id.nav_home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_challenges -> {
                    startActivity(Intent(applicationContext, ChallengesActivity::class.java))
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
        gestureDetector = GestureDetector(this, SwipeGestureListener())
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
        private fun finishWithAnimation() {
            startActivity(Intent(applicationContext, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun calculateCellSize() {
        val calendarGrid = findViewById<GridLayout>(R.id.calendarGrid)
        calendarGrid.post {
            val gridWidth = calendarGrid.width
            val gridHeight = calendarGrid.height

            val marginWidth = (gridWidth * 0.02).toInt()
            val marginHeight = (gridHeight * 0.02).toInt()

            val totalMarginsWidth = marginWidth * 2 * 7
            val usableWidth = gridWidth - totalMarginsWidth
            val cellWidth = usableWidth / 7

            val totalRows = 7
            val totalMarginsHeight = marginHeight * 2 * totalRows
            val usableHeight = gridHeight - totalMarginsHeight
            val cellHeight = usableHeight / totalRows

            cellSize = minOf(cellWidth, cellHeight)

            if (cellSize > 0) {
                setupCalendar()
            }
        }
    }

    private fun setupCalendar() {
        val calendarGrid = findViewById<GridLayout>(R.id.calendarGrid)
        val monthTitle = findViewById<TextView>(R.id.monthTitle)

        calendarGrid.removeAllViews()

        val gridWidth = calendarGrid.width
        val gridHeight = calendarGrid.height
        val marginWidth = (gridWidth * 0.02).toInt()
        val marginHeight = (gridHeight * 0.02).toInt()

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val englishMonths = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val monthName = englishMonths[currentMonth]
        monthTitle.text = monthName

        val dayHeaders = arrayOf("M", "T", "W", "T", "F", "S", "S")
        val headerTextSize = calculateTextSize(0.5f)

        for (dayHeader in dayHeaders) {
            val headerView = TextView(this).apply {
                text = dayHeader
                textSize = headerTextSize
                setTextColor("#3b86d6".toColorInt())
                gravity = android.view.Gravity.CENTER
                typeface = ResourcesCompat.getFont(this@InsightsActivity, R.font.dosis_bold)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = (cellSize * 0.8).toInt()
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(marginWidth, marginHeight, marginWidth, marginHeight)
                }
            }
            calendarGrid.addView(headerView)
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val adjustedFirstDay = if (firstDayOfWeek == 1) 6 else firstDayOfWeek - 2
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until adjustedFirstDay) {
            val emptyView = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(marginWidth, marginHeight, marginWidth, marginHeight)
                }
            }
            calendarGrid.addView(emptyView)
        }

        val dayTextSize = calculateTextSize(0.4f)

        for (day in 1..daysInMonth) {
            val dayView = TextView(this).apply {
                text = day.toString()
                textSize = dayTextSize
                gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(marginWidth, marginHeight, marginWidth, marginHeight)
                }

                if (day == today) {
                    setBackgroundResource(R.drawable.rounded_day_current_background)
                    setTextColor(ContextCompat.getColor(this@InsightsActivity, android.R.color.black))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    setTextColor(ContextCompat.getColor(this@InsightsActivity, android.R.color.white))
                    setBackgroundResource(R.drawable.rounded_day_background)
                }

                setOnClickListener {
                }
            }
            calendarGrid.addView(dayView)
        }

        val totalCells = adjustedFirstDay + daysInMonth
        val remainingCells = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)

        for (i in 0 until remainingCells) {
            val emptyView = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(marginWidth, marginHeight, marginWidth, marginHeight)
                }
            }
            calendarGrid.addView(emptyView)
        }
    }

    private fun calculateTextSize(ratio: Float): Float {
        val baseTextSize = (cellSize * ratio) / resources.displayMetrics.density

        return when {
            baseTextSize < 12f -> 12f
            baseTextSize > 24f -> 24f
            else -> baseTextSize
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        calculateCellSize()
        setupCalendar()
    }
}