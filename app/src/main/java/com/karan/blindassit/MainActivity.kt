package com.karan.blindassit

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var distanceText: TextView
    private lateinit var lightText: TextView
    private lateinit var obstacleText: TextView
    private lateinit var activityText: TextView
    private lateinit var activityEmoji: TextView
    private lateinit var suggestionText: TextView
    private lateinit var statusText: TextView

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var logsButton: Button
    private lateinit var summaryButton: Button
    private lateinit var historyButton: Button

    private lateinit var vibrator: Vibrator
    private lateinit var tts: TextToSpeech

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var socket: BluetoothSocket
    private lateinit var inputStream: InputStream

    private lateinit var dbHelper: DatabaseHelper

    private var monitoring = false
    private var lastSpeechTime = 0L

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Track current activity label for DB logging
    private var currentActivityLabel = "Standing"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        distanceText  = findViewById(R.id.distanceText)
        lightText     = findViewById(R.id.lightText)
        obstacleText  = findViewById(R.id.obstacleText)
        activityText  = findViewById(R.id.activityText)
        activityEmoji = findViewById(R.id.activityEmoji)
        suggestionText = findViewById(R.id.suggestionText)
        statusText    = findViewById(R.id.statusText)

        startButton   = findViewById(R.id.startServiceButton)
        stopButton    = findViewById(R.id.stopServiceButton)
        logsButton    = findViewById(R.id.recentButton)
        summaryButton = findViewById(R.id.summaryButton)
        historyButton = findViewById(R.id.historyButton)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        dbHelper = DatabaseHelper(this)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) tts.language = Locale.US
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        startButton.setOnClickListener {
            if (!monitoring) {
                monitoring = true
                statusText.text = "🟢 ACTIVE"
                suggestionText.text = "Connecting..."
                connectBluetooth()
                Toast.makeText(this, "Monitoring Started", Toast.LENGTH_SHORT).show()
            }
        }

        stopButton.setOnClickListener {
            monitoring = false
            statusText.text = "⏸ INACTIVE"
            obstacleText.text = "SAFE"
            obstacleText.setBackgroundResource(R.drawable.bg_circle_safe)
            suggestionText.text = "Tap START to begin"
            distanceText.text = "--"
            lightText.text = "--"
            if (::socket.isInitialized) {
                try { socket.close() } catch (_: Exception) {}
            }
            Toast.makeText(this, "Monitoring Stopped", Toast.LENGTH_SHORT).show()
        }

        logsButton.setOnClickListener {
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
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

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
                    val data = String(buffer, 0, bytes).trim()
                    processData(data)
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }

    private fun processData(data: String) {
        try {
            val values = data.split(",")
            if (values.size == 2) {
                val distance = values[0].toInt()
                val light    = values[1].toInt()

                runOnUiThread {
                    // ── Distance display ──
                    val distLabel = when {
                        distance < 20 -> "$distance cm"
                        distance < 50 -> "$distance cm"
                        else          -> "$distance cm"
                    }
                    distanceText.text = distLabel

                    // ── Light display: human word, not raw number ──
                    lightText.text = when {
                        light < 1  -> "Dark"
                        light < 8  -> "Dim"
                        light < 50 -> "Low"
                        else       -> "Bright"
                    }

                    var message = ""

                    // ── Obstacle status ──
                    when {
                        distance < 20 -> {
                            obstacleText.text = "DANGER"
                            obstacleText.setBackgroundResource(R.drawable.bg_circle_danger)
                            suggestionText.text = "STOP NOW"
                            vibrate(800)
                            message = "Danger! Obstacle very close. Stop immediately."
                        }
                        distance < 50 -> {
                            obstacleText.text = "WARNING"
                            obstacleText.setBackgroundResource(R.drawable.bg_circle_warning)
                            suggestionText.text = "Slow down"
                            vibrate(400)
                            message = "Obstacle ahead. Move carefully."
                        }
                        else -> {
                            obstacleText.text = "SAFE"
                            obstacleText.setBackgroundResource(R.drawable.bg_circle_safe)
                            suggestionText.text = "Path is clear"
                        }
                    }

                    // ── Dark environment override ──
                    if (light < 8) {
                        if (distance < 50) {
                            suggestionText.text = "Dark + Obstacle!"
                            message = "Warning! Dark environment with obstacle ahead."
                            vibrate(1000)
                        } else {
                            suggestionText.text = "Environment is dark"
                            if (message.isEmpty()) message = "Environment is dark. Be careful."
                            vibrate(300)
                        }
                    }

                    speak(message)
                }

                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val status = when {
                    distance < 20 -> "DANGER"
                    distance < 50 -> "WARNING"
                    else          -> "SAFE"
                }
                dbHelper.insertData(time, distance, light, "$currentActivityLabel | $status")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speak(text: String) {
        if (text.isEmpty()) return
        if (System.currentTimeMillis() - lastSpeechTime > 2000) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpeechTime = System.currentTimeMillis()
        }
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)

            runOnUiThread {
                when {
                    magnitude > 15 -> {
                        activityEmoji.text = "🏃"
                        activityText.text = "Running"
                        currentActivityLabel = "Running"
                    }
                    magnitude > 11 -> {
                        activityEmoji.text = "🚶"
                        activityText.text = "Walking"
                        currentActivityLabel = "Walking"
                    }
                    else -> {
                        activityEmoji.text = "🧍"
                        activityText.text = "Still"
                        currentActivityLabel = "Standing"
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        monitoring = false
        if (::socket.isInitialized) {
            try { socket.close() } catch (_: Exception) {}
        }
        sensorManager.unregisterListener(this)
        tts.shutdown()
    }
}