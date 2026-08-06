package com.example.poco

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

private const val SLEEP_MINUTES = 30L        // ACTIVE -> SLEEP 확정까지 무활동 지속 시간
private const val WAKE_CONFIRM_MINUTES = 5L  // WAKE_CANDIDATE -> ACTIVE 확정 유예 시간

/** 판정에 포함: 사람이 직접 개입하거나(식사/가사/TV 시청) 위급 상황(scream)을 나타내는 소리. */
val SLEEP_ACTIVITY_LABELS = setOf("dishes", "microwave", "vacuum", "water", "tv", "scream")
// washing_machine(장시간 가동 중에도 수면 가능), car_horn(실외 소음, 수면 중에도 들릴 수 있음)은 판정에서 제외.

enum class SleepState { ACTIVE, SLEEP, WAKE_CANDIDATE }

/** SLEEP/WAKE가 확정된 시점에만 만들어지는 서버 전송용 이벤트. */
data class SleepWakeEvent(val type: String, val timestamp: Long) // type: "sleep" | "wake"

/**
 * 취침/기상 상태머신. session 폴더의 MealSession, DishwashingSession 등과 동일하게
 * 상태 전이 + confirm 콜백 패턴을 따른다.
 *
 * ACTIVE -> SLEEP: 가속도계 움직임 없음 + 활동성 이벤트([SLEEP_ACTIVITY_LABELS]) 미감지가
 *   [SLEEP_MINUTES]분간 연속 유지되면 확정. 취침 시각은 확정 시점(30분 타이머 종료 시점)으로 기록한다.
 * SLEEP -> WAKE_CANDIDATE: 움직임 또는 활동성 이벤트 감지 시 후보 상태로 전이.
 * WAKE_CANDIDATE -> ACTIVE: 후보 진입 후 [WAKE_CONFIRM_MINUTES]분간 활동이 이어지면 WAKE 확정
 *   (기상 시각 = 최초 감지 시점). 그 전에 다시 무활동으로 돌아가면 오탐으로 보고 SLEEP으로 되돌리며
 *   서버 전송은 하지 않는다.
 *
 * 즉시 전환 대신 SLEEP과 동일하게 확정 유예 구간을 둔 이유: 야간 화장실 이동이나 순간적 뒤척임이
 * 곧바로 기상으로 오탐되는 걸 막기 위함. 유예 시간(5분)은 DishwashingSession의 T2(2분)와
 * MealSession의 T1(10분) 사이 값으로 설정했다.
 */
class SleepDetector(
    context: Context,
    private val sleepMillis: Long = SLEEP_MINUTES * 60_000L,
    private val wakeConfirmMillis: Long = WAKE_CONFIRM_MINUTES * 60_000L
) : SensorEventListener {

    @Volatile
    var state: SleepState = SleepState.ACTIVE
        private set

    @Volatile
    private var lastActivityTimeMs = System.currentTimeMillis()
    private var wakeCandidateStartMs = 0L

    /** SLEEP/WAKE가 확정될 때만 호출된다. 오탐으로 폐기된 WAKE_CANDIDATE는 호출되지 않는다. */
    var onConfirmed: ((SleepWakeEvent) -> Unit)? = null

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

    /** AudioMonitorService가 5초 세그먼트 분류 결과를 넘겨줄 때마다 호출한다. */
    fun onSoundEvent(label: String, timestamp: Long = System.currentTimeMillis()) {
        if (label in SLEEP_ACTIVITY_LABELS) {
            registerActivity(timestamp)
        }
        checkTimeout(timestamp)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()
        if (abs(magnitude - GRAVITY) > MOVEMENT_THRESHOLD) {
            registerActivity(now)
        }
        // 가속도계는 움직임이 없어도 주기적으로 값을 흘려보내므로, 타임아웃 체크의 주기 틱으로도 활용한다.
        checkTimeout(now)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun registerActivity(timestamp: Long) {
        lastActivityTimeMs = timestamp
        if (state == SleepState.SLEEP) {
            state = SleepState.WAKE_CANDIDATE
            wakeCandidateStartMs = timestamp
            Log.d(TAG, "SleepDetector: SLEEP → WAKE_CANDIDATE")
        }
    }

    private fun checkTimeout(now: Long) {
        when (state) {
            SleepState.ACTIVE -> {
                if (now - lastActivityTimeMs >= sleepMillis) {
                    confirmSleep(now)
                }
            }
            SleepState.WAKE_CANDIDATE -> {
                if (now - lastActivityTimeMs >= wakeConfirmMillis) {
                    // 후보 진입 후 활동이 이어지지 않음 -> 오탐 폐기, 서버 전송 없이 SLEEP 유지
                    Log.d(TAG, "SleepDetector: WAKE_CANDIDATE → SLEEP (오탐 폐기)")
                    state = SleepState.SLEEP
                } else if (now - wakeCandidateStartMs >= wakeConfirmMillis) {
                    confirmWake(wakeCandidateStartMs)
                }
            }
            SleepState.SLEEP -> {}
        }
    }

    private fun confirmSleep(timestamp: Long) {
        state = SleepState.SLEEP
        Log.d(TAG, "SleepDetector: ACTIVE → SLEEP 확정 (t=$timestamp)")
        onConfirmed?.invoke(SleepWakeEvent("sleep", timestamp))
    }

    private fun confirmWake(timestamp: Long) {
        state = SleepState.ACTIVE
        lastActivityTimeMs = System.currentTimeMillis()
        Log.d(TAG, "SleepDetector: WAKE_CANDIDATE → ACTIVE WAKE 확정 (t=$timestamp)")
        onConfirmed?.invoke(SleepWakeEvent("wake", timestamp))
    }

    companion object {
        private const val TAG = "POCO"
        private const val GRAVITY = 9.80665
        private const val MOVEMENT_THRESHOLD = 0.5 // m/s²
    }
}
