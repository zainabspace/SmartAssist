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

        logsButton = findViewById(R.id.recentButton)
        summaryButton = findViewById(R.id.summaryButton)
        historyButton = findViewById(R.id.historyButton)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        dbHelper = DatabaseHelper(this)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        startButton.setOnClickListener {

            if (!monitoring) {

                monitoring = true
                statusText.text = "🟢 Monitoring ACTIVE"

                connectBluetooth()

                Toast.makeText(this, "Monitoring Started", Toast.LENGTH_SHORT).show()
            }
        }

        stopButton.setOnClickListener {

            monitoring = false
            statusText.text = "🔴 Monitoring STOPPED"

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
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

        pairedDevices?.forEach { device ->

            if (device.name == "HC-05") {

                try {

                    val uuid = UUID.fromString(
                        "00001101-0000-1000-8000-00805F9B34FB"
                    )

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
                val light = values[1].toInt()

                runOnUiThread {

                    distanceText.text = "📏 Distance: $distance cm"
                    lightText.text = "💡 Light Level: $light"

                    var message = ""

                    // ---------- OBSTACLE LOGIC ----------

                    if (distance < 20) {

                        obstacleText.text = "DANGER"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_danger)

                        vibrate(800)

                        suggestionText.text = "Stop immediately"

                        message = "Danger. Obstacle very close."

                    }
                    else if (distance < 50) {

                        obstacleText.text = "WARNING"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_warning)

                        vibrate(400)

                        suggestionText.text = "Move carefully"

                        message = "Obstacle ahead."

                    }
                    else {

                        obstacleText.text = "SAFE"
                        obstacleText.setBackgroundResource(R.drawable.bg_circle_safe)

                        suggestionText.text = "Path clear"
                    }


                    // ---------- DARK ENVIRONMENT LOGIC ----------

                    if (light < 8) {

                        suggestionText.text = "Environment is dark"

                        // dark + obstacle = risk
                        if (distance < 50) {

                            message = "Risk"
                            vibrate(1000)

                        }
                        else {

                            message = "Environment is dark"
                            vibrate(300)

                        }
                    }

                    speak(message)
                }

                val time = SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

                dbHelper.insertData(time, distance, light, "Monitoring")
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
                VibrationEffect.createOneShot(
                    duration,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            vibrator.vibrate(duration)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt(x*x + y*y + z*z)

            runOnUiThread {

                when {
                    magnitude > 15 -> activityText.text = "🏃 Running"
                    magnitude > 11 -> activityText.text = "🚶 Walking"
                    else -> activityText.text = "🧍 Standing"
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