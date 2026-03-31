package com.karan.blindassit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "sensor.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        val createTable = """
            CREATE TABLE sensor_data(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time TEXT,
                distance INTEGER,
                light INTEGER,
                activity TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sensor_data")
        onCreate(db)
    }

    fun insertData(time: String, distance: Int, light: Int, activity: String) {

        val db = writableDatabase

        val values = ContentValues()
        values.put("time", time)
        values.put("distance", distance)
        values.put("light", light)
        values.put("activity", activity)

        db.insert("sensor_data", null, values)
    }
}