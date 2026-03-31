package com.karan.blindassit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {

    lateinit var textView: TextView
    lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_summary)

        textView = findViewById(R.id.summaryText)

        dbHelper = DatabaseHelper(this)

        val db = dbHelper.readableDatabase

        val total = db.rawQuery("SELECT COUNT(*) FROM sensor_data", null)
        val obstacle = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE distance < 50", null)
        val dark = db.rawQuery("SELECT COUNT(*) FROM sensor_data WHERE light < 1", null)

        total.moveToFirst()
        obstacle.moveToFirst()
        dark.moveToFirst()

        val summary = """
            Total Records: ${total.getInt(0)}
            
            Obstacle Events: ${obstacle.getInt(0)}
            
            Dark Environment Events: ${dark.getInt(0)}
        """.trimIndent()

        textView.text = summary

        total.close()
        obstacle.close()
        dark.close()
    }
}