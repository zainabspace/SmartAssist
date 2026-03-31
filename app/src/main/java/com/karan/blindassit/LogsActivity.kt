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

        val cursor = db.rawQuery("SELECT * FROM sensor_data", null)

        val builder = StringBuilder()

        while (cursor.moveToNext()) {

            val time = cursor.getString(1)
            val distance = cursor.getInt(2)
            val light = cursor.getInt(3)
            val activity = cursor.getString(4)

            builder.append("Time: $time\n")
            builder.append("Distance: $distance\n")
            builder.append("Light: $light\n")
            builder.append("Activity: $activity\n")
            builder.append("----------------------\n\n")
        }

        logTextView.text = builder.toString()

        cursor.close()
    }
}