package com.example.poco

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class SleepDetector(
    context: Context,
    private val onTransition: (SleepState) -> Unit
) : SensorEventListener {

    enum class SleepState { ACTIVE, SLEEP }

    @Volatile
    var state = SleepState.ACTIVE
        private set

    @Volatile
    private var lastActivityTimeMs = System.currentTimeMillis()

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun start() {
        if (accelerometerSensor != null) {
            sensorManager?.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "SleepDetector: accelerometer registered")
        } else {
            Log.w(TAG, "SleepDetector: no accelerometer sensor")
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    // Call when sound above active threshold is detected (resets silence timer)
    fun onSoundActivity() {
        lastActivityTimeMs = System.currentTimeMillis()
        if (state == SleepState.SLEEP) {
            transition(SleepState.ACTIVE)
        }
    }

    // Call periodically from the audio loop to check if sleep transition should occur
    fun checkAndMaybeTransitionToSleep() {
        if (state == SleepState.ACTIVE) {
            val inactiveMs = System.currentTimeMillis() - lastActivityTimeMs
            if (inactiveMs >= SLEEP_INACTIVITY_MS) {
                transition(SleepState.SLEEP)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val magnitude = sqrt(x * x + y * y + z * z)
        if (abs(magnitude - GRAVITY) > MOVEMENT_THRESHOLD) {
            lastActivityTimeMs = System.currentTimeMillis()
            if (state == SleepState.SLEEP) {
                transition(SleepState.ACTIVE)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun transition(newState: SleepState) {
        if (state == newState) return
        Log.d(TAG, "SleepDetector: $state → $newState")
        state = newState
        onTransition(newState)
    }

    companion object {
        private const val TAG = "POCO"
        private const val GRAVITY = 9.80665
        private const val MOVEMENT_THRESHOLD = 0.5 // m/s²
        val SLEEP_INACTIVITY_MS = 30 * 60 * 1000L // 30 minutes of silence + no movement
    }
}
