package com.karan.blindassit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecentEventsActivity : AppCompatActivity() {

    lateinit var textView: TextView
    lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recent_events)

        textView = findViewById(R.id.recentText)
        dbHelper = DatabaseHelper(this)

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM sensor_data ORDER BY id DESC LIMIT 10",
            null
        )

        val builder = StringBuilder()

        while (cursor.moveToNext()) {

            builder.append("Time: ${cursor.getString(1)}\n")
            builder.append("Distance: ${cursor.getInt(2)}\n")
            builder.append("Light: ${cursor.getInt(3)}\n")
            builder.append("Activity: ${cursor.getString(4)}\n")
            builder.append("----------------------\n")
        }

        textView.text = builder.toString()

        cursor.close()
    }
}