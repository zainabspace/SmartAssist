package com.karan.blindassit

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DatabaseHelper(this)

        loadHistory()

        findViewById<Button>(R.id.deleteButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete History")
                .setMessage("Are you sure you want to delete all sensor history?")
                .setPositiveButton("Yes") { _, _ ->
                    dbHelper.writableDatabase.execSQL("DELETE FROM sensor_data")
                    Toast.makeText(this, "History deleted", Toast.LENGTH_SHORT).show()
                    loadHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadHistory() {
        val dangerContainer  = findViewById<LinearLayout>(R.id.dangerContainer)
        val warningContainer = findViewById<LinearLayout>(R.id.warningContainer)
        val safeContainer    = findViewById<LinearLayout>(R.id.safeContainer)
        val dangerHeader     = findViewById<TextView>(R.id.dangerHeader)
        val warningHeader    = findViewById<TextView>(R.id.warningHeader)
        val safeHeader       = findViewById<TextView>(R.id.safeHeader)
        val emptyText        = findViewById<TextView>(R.id.emptyText)

        dangerContainer.removeAllViews()
        warningContainer.removeAllViews()
        safeContainer.removeAllViews()

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM sensor_data ORDER BY id DESC", null)

        var totalRows = 0

        while (cursor.moveToNext()) {
            totalRows++
            val time     = cursor.getString(1)
            val distance = cursor.getInt(2)
            val light    = cursor.getInt(3)
            val activity = cursor.getString(4)

            val lightLabel = when {
                light < 1  -> "⬛ Pitch dark"
                light < 8  -> "🌑 Dark"
                light < 50 -> "🌤 Dim"
                else       -> "☀️ Bright"
            }

            val distLabel = when {
                distance < 20 -> "Very close  ($distance cm)"
                distance < 50 -> "Obstacle ahead  ($distance cm)"
                else          -> "Clear  ($distance cm)"
            }

            // Build card view programmatically
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                elevation = dp(6).toFloat()
                setBackgroundResource(R.drawable.card_bg)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dp(10)
                layoutParams = params
            }

            // Time row
            val timeView = TextView(this).apply {
                text = "🕐 $time"
                textSize = 12f
                setTextColor(0xFF90A4AE.toInt())
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                p.bottomMargin = dp(6)
                layoutParams = p
            }

            // Distance row
            val distView = TextView(this).apply {
                text = "📏 $distLabel"
                textSize = 15f
                setTextColor(0xFFECEFF1.toInt())
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                p.bottomMargin = dp(4)
                layoutParams = p
            }

            // Light row
            val lightView = TextView(this).apply {
                text = "💡 $lightLabel  (value: $light)"
                textSize = 15f
                setTextColor(0xFFECEFF1.toInt())
                val p = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                p.bottomMargin = dp(4)
                layoutParams = p
            }

            // Activity row
            val actView = TextView(this).apply {
                text = "🚶 $activity"
                textSize = 14f
                setTextColor(0xFFFFD54F.toInt())
            }

            card.addView(timeView)
            card.addView(distView)
            card.addView(lightView)
            card.addView(actView)

            when {
                distance < 20 -> dangerContainer.addView(card)
                distance < 50 -> warningContainer.addView(card)
                else          -> safeContainer.addView(card)
            }
        }
        cursor.close()

        if (totalRows == 0) {
            emptyText.visibility = View.VISIBLE
            dangerHeader.visibility  = View.GONE
            warningHeader.visibility = View.GONE
            safeHeader.visibility    = View.GONE
        } else {
            emptyText.visibility = View.GONE
            dangerHeader.visibility  = if (dangerContainer.childCount  > 0) View.VISIBLE else View.GONE
            warningHeader.visibility = if (warningContainer.childCount > 0) View.VISIBLE else View.GONE
            safeHeader.visibility    = if (safeContainer.childCount    > 0) View.VISIBLE else View.GONE
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}