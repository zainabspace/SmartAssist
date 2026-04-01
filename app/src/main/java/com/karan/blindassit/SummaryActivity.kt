package com.karan.blindassit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val total     = db.rawQuery("SELECT COUNT(*) FROM sensor_data", null)
        val danger    = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE distance < 20", null)
        val warning   = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE distance >= 20 AND distance < 50", null)
        val safe      = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE distance >= 50", null)
        val dark      = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE light < 8", null)
        val riskCombo = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE distance < 50 AND light < 8", null)
        val running   = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE activity LIKE '%Running%'", null)
        val walking   = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE activity LIKE '%Walking%'", null)
        val dangerHour = db.rawQuery(
            "SELECT substr(time,1,2) as hr, COUNT(*) as cnt FROM sensor_data WHERE distance < 50 GROUP BY hr ORDER BY cnt DESC LIMIT 1", null
        )

        total.moveToFirst(); danger.moveToFirst(); warning.moveToFirst(); safe.moveToFirst()
        dark.moveToFirst(); riskCombo.moveToFirst(); running.moveToFirst(); walking.moveToFirst()

        val totalN   = total.getInt(0)
        val dangerN  = danger.getInt(0)
        val warningN = warning.getInt(0)
        val safeN    = safe.getInt(0)
        val darkN    = dark.getInt(0)
        val riskN    = riskCombo.getInt(0)
        val runN     = running.getInt(0)
        val walkN    = walking.getInt(0)

        val safePct = if (totalN > 0) (safeN * 100 / totalN) else 0

        // Safety score card
        val scoreView = findViewById<TextView>(R.id.safetyScoreText)
        val labelView = findViewById<TextView>(R.id.safetyLabelText)
        if (totalN > 0) {
            scoreView.text = "$safePct%"
            val (label, color) = when {
                safePct >= 80 -> Pair("✅ Excellent — you're navigating safely", 0xFF66BB6A.toInt())
                safePct >= 60 -> Pair("⚠️ Moderate — a few risky moments", 0xFFFFB300.toInt())
                else          -> Pair("❗ Needs Attention — many obstacles detected", 0xFFEF5350.toInt())
            }
            labelView.text = label
            labelView.setTextColor(color)
        } else {
            scoreView.text = "—"
            labelView.text = "No monitoring data yet"
        }

        // Obstacle counts
        findViewById<TextView>(R.id.dangerCount).text = dangerN.toString()
        findViewById<TextView>(R.id.warningCount).text = warningN.toString()
        findViewById<TextView>(R.id.safeCount).text = safeN.toString()

        // Environment
        findViewById<TextView>(R.id.darkCount).text = darkN.toString()
        findViewById<TextView>(R.id.riskCount).text = riskN.toString()

        // Activity
        findViewById<TextView>(R.id.runCount).text = "$runN events"
        findViewById<TextView>(R.id.walkCount).text = "$walkN events"

        // Riskiest hour
        val worstHour = if (dangerHour.moveToFirst()) {
            val hr = dangerHour.getString(0)
            val cnt = dangerHour.getInt(1)
            "$hr:00 – $hr:59  ($cnt obstacle events)"
        } else "No obstacle data recorded"
        findViewById<TextView>(R.id.worstHourText).text = worstHour

        total.close(); danger.close(); warning.close(); safe.close()
        dark.close(); riskCombo.close(); running.close(); walking.close(); dangerHour.close()
    }
}