package com.karan.blindassit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LogsActivity : AppCompatActivity() {

    lateinit var logTextView: TextView
    lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        logTextView = findViewById(R.id.logTextView)
        dbHelper = DatabaseHelper(this)

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM sensor_data ORDER BY id DESC LIMIT 50", null
        )

        val builder = StringBuilder()
        var count = 0

        while (cursor.moveToNext()) {
            count++
            val time     = cursor.getString(1)
            val distance = cursor.getInt(2)
            val light    = cursor.getInt(3)
            val activity = cursor.getString(4)

            // Determine status badge
            val (badge, label) = when {
                distance < 20 -> Pair("🔴", "DANGER")
                distance < 50 -> Pair("🟡", "WARNING")
                else          -> Pair("🟢", "SAFE")
            }

            // Human-readable distance description
            val distDesc = when {
                distance < 20 -> "Very close obstacle ($distance cm)"
                distance < 50 -> "Obstacle ahead ($distance cm)"
                else          -> "Path clear ($distance cm)"
            }

            // Human-readable light description
            val lightDesc = when {
                light < 1  -> "Completely dark"
                light < 8  -> "Dark environment"
                light < 50 -> "Low light"
                else       -> "Well lit"
            }

            builder.append("$badge [$time]  $label\n")
            builder.append("   📏 $distDesc\n")
            builder.append("   💡 $lightDesc (value: $light)\n")
            builder.append("   🚶 $activity\n")
            builder.append("──────────────────────────────\n\n")
        }

        if (count == 0) {
            logTextView.text = "No logs yet. Start monitoring to record sensor data."
        } else {
            logTextView.text = "Showing last $count events (newest first)\n\n" + builder.toString()
        }

        cursor.close()
    }
}