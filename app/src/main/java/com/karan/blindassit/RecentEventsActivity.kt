package com.karan.blindassit

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecentEventsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent_events)

        dbHelper = DatabaseHelper(this)
        loadRecentEvents()
    }

    private fun loadRecentEvents() {
        val container = findViewById<LinearLayout>(R.id.eventsContainer)
        container.removeAllViews()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM sensor_data ORDER BY id DESC LIMIT 10", null
        )

        var dangerCount  = 0
        var warningCount = 0
        var safeCount    = 0
        var index        = 1

        if (!cursor.moveToFirst()) {
            // Empty state card
            val empty = buildEmptyCard()
            container.addView(empty)
            cursor.close()
            updateMiniStats(0, 0, 0)
            return
        }

        do {
            val time     = cursor.getString(1)
            val distance = cursor.getInt(2)
            val light    = cursor.getInt(3)
            val activity = cursor.getString(4)

            when {
                distance < 20 -> dangerCount++
                distance < 50 -> warningCount++
                else          -> safeCount++
            }

            val card = buildEventCard(index, time, distance, light, activity)
            container.addView(card)
            index++

        } while (cursor.moveToNext())

        cursor.close()
        updateMiniStats(dangerCount, warningCount, safeCount)
    }

    private fun updateMiniStats(danger: Int, warning: Int, safe: Int) {
        findViewById<TextView>(R.id.miniDanger).text  = danger.toString()
        findViewById<TextView>(R.id.miniWarning).text = warning.toString()
        findViewById<TextView>(R.id.miniSafe).text    = safe.toString()
    }

    private fun buildEventCard(
        index: Int,
        time: String,
        distance: Int,
        light: Int,
        activity: String
    ): LinearLayout {

        val (statusEmoji, statusLabel, accentColor, leftBorderColor) = when {
            distance < 20 -> Quad("🔴", "DANGER",  0xFFEF5350.toInt(), 0xFFEF5350.toInt())
            distance < 50 -> Quad("🟡", "WARNING", 0xFFFFB300.toInt(), 0xFFFFB300.toInt())
            else          -> Quad("🟢", "SAFE",    0xFF66BB6A.toInt(), 0xFF66BB6A.toInt())
        }

        val distDesc = when {
            distance < 20 -> "Very close obstacle — $distance cm"
            distance < 50 -> "Obstacle ahead — $distance cm"
            else          -> "Path clear — $distance cm"
        }

        val lightDesc = when {
            light < 1  -> "⬛ Pitch dark"
            light < 8  -> "🌑 Dark environment"
            light < 50 -> "🌤 Low light"
            else       -> "☀️ Well lit"
        }

        // Outer row: left color strip + card
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(12)
            layoutParams = params
        }

        // Left color strip (timeline indicator)
        val strip = View(this).apply {
            setBackgroundColor(leftBorderColor)
            layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT).also {
                it.marginEnd = dp(10)
            }
        }

        // Card body
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_bg)
            elevation = dp(6).toFloat()
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        // Top row: index + status badge + time
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(10) }
        }

        val indexView = TextView(this).apply {
            text = "#$index"
            textSize = 12f
            setTextColor(0xFF546E7A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dp(8) }
        }

        val badgeView = TextView(this).apply {
            text = "  $statusEmoji $statusLabel  "
            textSize = 11f
            setTextColor(accentColor)
            setBackgroundColor(0x22FFFFFF)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dp(8) }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val timeView = TextView(this).apply {
            text = "🕐 $time"
            textSize = 12f
            setTextColor(0xFF78909C.toInt())
        }

        topRow.addView(indexView)
        topRow.addView(badgeView)
        topRow.addView(spacer)
        topRow.addView(timeView)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(0xFF1E3A5A.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ).also { it.bottomMargin = dp(10) }
        }

        // Distance row
        val distView = makeDetailRow("📏", distDesc, 0xFFECEFF1.toInt())

        // Light row
        val lightView = makeDetailRow("💡", "$lightDesc  (raw: $light)", 0xFFECEFF1.toInt())

        // Activity row
        val actView = makeDetailRow("🚶", activity, accentColor)

        card.addView(topRow)
        card.addView(divider)
        card.addView(distView)
        card.addView(lightView)
        card.addView(actView)

        row.addView(strip)
        row.addView(card)

        return row
    }

    private fun makeDetailRow(emoji: String, text: String, color: Int): TextView {
        return TextView(this).apply {
            this.text = "$emoji  $text"
            textSize = 14f
            setTextColor(color)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(6) }
        }
    }

    private fun buildEmptyCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.card_bg)
            setPadding(dp(20), dp(40), dp(20), dp(40))
            gravity = android.view.Gravity.CENTER
            elevation = dp(6).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dp(20) }

            val icon = TextView(this@RecentEventsActivity).apply {
                text = "📡"
                textSize = 48f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(12) }
            }

            val msg = TextView(this@RecentEventsActivity).apply {
                text = "No events recorded yet"
                textSize = 16f
                setTextColor(0xFFECEFF1.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(6) }
            }

            val sub = TextView(this@RecentEventsActivity).apply {
                text = "Start monitoring from the home screen"
                textSize = 13f
                setTextColor(0xFF546E7A.toInt())
                gravity = android.view.Gravity.CENTER
            }

            addView(icon); addView(msg); addView(sub)
        }
    }

    // Helper data class to avoid Pair<Pair<>> nesting
    data class Quad(val a: String, val b: String, val c: Int, val d: Int)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}