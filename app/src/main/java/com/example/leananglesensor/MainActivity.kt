package com.example.leananglesensor

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var orientationSensor: Sensor? = null
    private val estimator = LeanEstimator()

    private lateinit var gauge: LeanGaugeView
    private lateinit var maxLeftText: TextView
    private lateinit var maxRightText: TextView
    private lateinit var statusText: TextView
    private lateinit var sensorText: TextView
    private lateinit var lastRideText: TextView
    private lateinit var calibrateButton: Button
    private lateinit var rideButton: Button

    private var recording = false
    private var currentLeanDegrees = 0f

    private val preferences by lazy {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gauge = findViewById(R.id.leanGauge)
        maxLeftText = findViewById(R.id.maxLeftText)
        maxRightText = findViewById(R.id.maxRightText)
        statusText = findViewById(R.id.statusText)
        sensorText = findViewById(R.id.sensorText)
        lastRideText = findViewById(R.id.lastRideText)
        calibrateButton = findViewById(R.id.calibrateButton)
        rideButton = findViewById(R.id.rideButton)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorText.text = orientationSensor?.let {
            "Sensor: ${it.name}"
        } ?: "No rotation-vector sensor found on this phone"
        calibrateButton.isEnabled = orientationSensor != null
        rideButton.isEnabled = orientationSensor != null

        calibrateButton.setOnClickListener {
            if (recording) {
                statusText.text = "Stop the ride before calibrating"
                return@setOnClickListener
            }
            estimator.beginCalibration()
            calibrateButton.isEnabled = false
            statusText.text = "Hold the motorcycle upright for one second..."
        }

        rideButton.setOnClickListener {
            if (!estimator.isCalibrated()) {
                statusText.text = "Calibrate upright before starting a ride"
                return@setOnClickListener
            }

            recording = !recording
            estimator.setTracking(recording)

            if (recording) {
                rideButton.setText(R.string.stop_ride)
                statusText.text = "Recording live maximums"
            } else {
                rideButton.setText(R.string.start_ride)
                saveLastRide()
                statusText.text = "Ride saved"
            }
            refreshPeakText()
        }

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            estimator.resetPeaks()
            refreshPeakText()
            statusText.text = if (recording) {
                "Maximums reset for this ride"
            } else {
                "Maximums reset"
            }
        }

        loadLastRideText()
        refreshPeakText()
    }

    override fun onResume() {
        super.onResume()
        orientationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor != orientationSensor) return

        val reading = estimator.process(event.values, event.timestamp) ?: return
        currentLeanDegrees = reading.leanDegrees
        gauge.leanDegrees = currentLeanDegrees

        if (reading.calibrating) {
            statusText.text = "Calibrating upright: ${(reading.calibrationProgress * 100f).toInt()}%"
        } else if (reading.calibrated) {
            calibrateButton.isEnabled = true
            gauge.maxLeftDegrees = reading.maxLeftDegrees
            gauge.maxRightDegrees = reading.maxRightDegrees
            refreshPeakText()

            if (!recording && statusText.text.toString().startsWith("Calibrating")) {
                statusText.text = "Calibrated — ready to start a ride"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun refreshPeakText() {
        val left = estimator.maxLeftDegrees
        val right = estimator.maxRightDegrees
        maxLeftText.text = "LEFT MAX\n${formatDegrees(left)}"
        maxRightText.text = "RIGHT MAX\n${formatDegrees(right)}"
        gauge.maxLeftDegrees = left
        gauge.maxRightDegrees = right
    }

    private fun saveLastRide() {
        preferences.edit()
            .putFloat(KEY_LAST_LEFT, estimator.maxLeftDegrees)
            .putFloat(KEY_LAST_RIGHT, estimator.maxRightDegrees)
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .apply()
        loadLastRideText()
    }

    private fun loadLastRideText() {
        if (!preferences.contains(KEY_LAST_TIME)) {
            lastRideText.text = "No saved ride yet"
            return
        }

        val left = preferences.getFloat(KEY_LAST_LEFT, 0f)
        val right = preferences.getFloat(KEY_LAST_RIGHT, 0f)
        val time = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        ).format(Date(preferences.getLong(KEY_LAST_TIME, 0L)))
        lastRideText.text = "Last saved ride ($time): L ${formatDegrees(left)} • R ${formatDegrees(right)}"
    }

    private fun formatDegrees(value: Float): String = String.format(Locale.US, "%.1f°", value)

    companion object {
        private const val PREFERENCES_NAME = "lean_angle_sensor"
        private const val KEY_LAST_LEFT = "last_left"
        private const val KEY_LAST_RIGHT = "last_right"
        private const val KEY_LAST_TIME = "last_time"
    }
}
