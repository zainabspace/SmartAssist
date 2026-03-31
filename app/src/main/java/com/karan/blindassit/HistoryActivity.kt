package com.karan.blindassit

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    lateinit var textView: TextView
    lateinit var deleteButton: Button
    lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_history)

        textView = findViewById(R.id.historyText)
        deleteButton = findViewById(R.id.deleteButton)

        dbHelper = DatabaseHelper(this)

        loadHistory()

        deleteButton.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete History")
                .setMessage("Are you sure you want to delete all sensor history?")
                .setPositiveButton("Yes") { _, _ ->

                    val db = dbHelper.writableDatabase
                    db.execSQL("DELETE FROM sensor_data")

                    Toast.makeText(
                        this,
                        "History deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun loadHistory() {

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM sensor_data", null)

        val builder = StringBuilder()

        while (cursor.moveToNext()) {

            builder.append("Time: ${cursor.getString(1)}\n")
            builder.append("Distance: ${cursor.getInt(2)} cm\n")
            builder.append("Light: ${cursor.getInt(3)}\n")
            builder.append("Activity: ${cursor.getString(4)}\n")
            builder.append("--------------------------\n")
        }

        textView.text = builder.toString()

        cursor.close()
    }
}