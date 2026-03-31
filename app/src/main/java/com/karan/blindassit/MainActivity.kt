package com.karan.blindassit

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.text.SimpleDateFormat
import android.graphics.Color
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var distanceText: TextView
    private lateinit var lightText: TextView
    private lateinit var obstacleText: TextView
    private lateinit var activityText: TextView

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var recentButton: Button
    private lateinit var summaryButton: Button
    private lateinit var historyButton: Button

    private lateinit var vibrator: Vibrator
    private lateinit var tts: TextToSpeech

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var socket: BluetoothSocket
    private lateinit var inputStream: InputStream

    private lateinit var dbHelper: DatabaseHelper

    private var monitoring = false
    private var lastDistance = -1
    private var lastSpeechTime = 0L

    private lateinit var statusText: TextView

    private lateinit var suggestionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        distanceText = findViewById(R.id.distanceText)
        lightText = findViewById(R.id.lightText)
        obstacleText = findViewById(R.id.obstacleText)
        activityText = findViewById(R.id.activityText)
        suggestionText = findViewById(R.id.suggestionText)

        statusText = findViewById(R.id.statusText)

        startButton = findViewById(R.id.startServiceButton)
        stopButton = findViewById(R.id.stopServiceButton)

        recentButton = findViewById(R.id.recentButton)
        summaryButton = findViewById(R.id.summaryButton)
        historyButton = findViewById(R.id.historyButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        dbHelper = DatabaseHelper(this)

        // ---------- TTS ----------
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // ---------- START BUTTON ----------
        startButton.setOnClickListener {

            if (!monitoring) {

                monitoring = true
                statusText.text = "🟢 Monitoring: ACTIVE"

                connectBluetooth()

                Toast.makeText(this, "Monitoring Started", Toast.LENGTH_SHORT).show()
            }
        }

        // ---------- STOP BUTTON ----------
        stopButton.setOnClickListener {

            monitoring = false

            statusText.text = "🔴 Monitoring: INACTIVE"

            try {
                socket.close()
            } catch (_: Exception) {
            }

            Toast.makeText(this, "Monitoring Stopped", Toast.LENGTH_SHORT).show()
        }
        // ---------- NAVIGATION ----------
        recentButton.setOnClickListener {
            startActivity(Intent(this, RecentEventsActivity::class.java))
        }

        summaryButton.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun connectBluetooth() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        pairedDevices?.forEach { device ->

            if (device.name == "HC-05") {

                try {

                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                    socket = device.createRfcommSocketToServiceRecord(uuid)

                    socket.connect()

                    inputStream = socket.inputStream

                    startReading()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startReading() {

        Thread {

            val buffer = ByteArray(1024)

            while (monitoring) {

                try {

                    val bytes = inputStream.read(buffer)

                    val data = String(buffer, 0, bytes)

                    processData(data)

                } catch (e: Exception) {
                    break
                }
            }

        }.start()
    }

    private fun processData(data: String) {

        try {

            val values = data.trim().split(",")

            if (values.size == 2) {

                val distance = values[0].toInt()
                val light = values[1].toInt()

                var suggestion = ""

                runOnUiThread {

                    // 📏 DISTANCE + 💡 LIGHT
                    distanceText.text = "📏 Distance: $distance cm"
                    lightText.text = "💡 Light Level: $light"

                    // 🚨 STATUS
                    if (distance < 20) {

                        obstacleText.text = "🚨 DANGER"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_danger)

                        obstacleText.scaleX = 1f
                        obstacleText.scaleY = 1f
                        obstacleText.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                android.os.VibrationEffect.createOneShot(
                                    800,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(800)
                        }

                        suggestion = "Stop immediately!"
                        suggestionText.text = "💬 $suggestion"

                    } else if (distance < 50) {

                        obstacleText.text = "⚠️ WARNING"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_warning)

                        obstacleText.scaleX = 1f
                        obstacleText.scaleY = 1f
                        obstacleText.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                android.os.VibrationEffect.createOneShot(
                                    400,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(400)
                        }

                        suggestion = "Move carefully"
                        suggestionText.text = "💬 $suggestion"

                    } else {

                        obstacleText.text = "✅ SAFE"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_safe)

                        obstacleText.scaleX = 1f
                        obstacleText.scaleY = 1f
                        obstacleText.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()

                        suggestion = if (light < 100) {
                            "Turn on light"
                        } else {
                            "All good"
                        }

                        suggestionText.text = if (light < 100) {
                            "💡 $suggestion"
                        } else {
                            "💬 $suggestion"
                        }
                    }

                    // 🚶 ACTIVITY
                    if (lastDistance != -1) {
                        if (Math.abs(distance - lastDistance) < 2)
                            activityText.text = "🧍 Standing"
                        else if (distance < lastDistance)
                            activityText.text = "🚶 Approaching"
                        else
                            activityText.text = "🏃 Moving away"
                    }

                    lastDistance = distance
                }

                // 🔊 SMART TTS (WITH DELAY)
                if (System.currentTimeMillis() - lastSpeechTime > 2000) {
                    tts.speak(
                        suggestion,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    lastSpeechTime = System.currentTimeMillis()
                }

                // 📊 DATABASE
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                dbHelper.insertData(time, distance, light, "Monitoring")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }}