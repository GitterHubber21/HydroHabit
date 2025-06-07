package com.example.hydrohabit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
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

class InsightsActivity : AppCompatActivity() {
    private var isBellSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insights)

        setupCalendar()

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

        bellIcon.setOnClickListener {
            if (isBellSelected) {
                bellIcon.setImageResource(R.drawable.ic_bell_unselected)
            } else {
                bellIcon.setImageResource(R.drawable.ic_bell)
            }
            isBellSelected = !isBellSelected
        }

    }

    private fun setupCalendar() {
        val calendarGrid = findViewById<GridLayout>(R.id.calendarGrid)
        val monthTitle = findViewById<TextView>(R.id.monthTitle)

        calendarGrid.removeAllViews()

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val today = calendar.get(Calendar.DAY_OF_MONTH)

        val englishMonths = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val monthName = englishMonths[currentMonth]
        monthTitle.text = monthName

        val dayHeaders = arrayOf("M", "T", "W", "T", "F", "S", "S")
        for (dayHeader in dayHeaders) {
            val headerView = TextView(this).apply {
                text = dayHeader
                textSize = 20f
                setTextColor("#3b86d6".toColorInt())
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
                typeface = ResourcesCompat.getFont(this@InsightsActivity, R.font.dosis_bold)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
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
                    width = 120
                    height = 120
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            calendarGrid.addView(emptyView)
        }

        for (day in 1..daysInMonth) {
            val dayView = TextView(this).apply {
                text = day.toString()
                textSize = 20f
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(12, 14, 12, 14)
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
                    width = 120
                    height = 120
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            calendarGrid.addView(emptyView)
        }
    }
}