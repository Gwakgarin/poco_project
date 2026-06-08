package com.example.poco

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt

class AudioTrigger(
    private val context: Context,
    private val onTrigger: () -> Unit
) {
    private val sampleRate = 16000
    private val thresholdDb = 55.0
    private val triggerCooldownMs = 5_000L

    @Volatile
    private var isRunning = false

    private var recorder: AudioRecord? = null
    private var workerThread: Thread? = null
    private var lastTriggerAtMs = 0L

    fun startListening() {
        if (isRunning) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        isRunning = true
        workerThread = Thread(::runAudioLoop, "POCO-AudioTrigger").also { it.start() }
    }

    fun stopListening() {
        isRunning = false

        try {
            recorder?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "recorder already stopped: ${e.message}")
        }

        if (Thread.currentThread() != workerThread) {
            try {
                workerThread?.join(1_000L)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        workerThread = null
        Log.d(TAG, "AudioTrigger stopped")
    }

    @SuppressLint("MissingPermission")
    private fun runAudioLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $minBufferSize")
            isRunning = false
            return
        }

        val buffer = ShortArray(minBufferSize / BYTES_PER_SAMPLE)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        recorder = audioRecord

        try {
            audioRecord.startRecording()
            Log.d(TAG, "AudioTrigger started")

            while (isRunning) {
                val readCount = audioRecord.read(buffer, 0, buffer.size)

                if (readCount > 0) {
                    val db = calculateDb(buffer, readCount)
                    Log.d(TAG, "dB = $db")

                    if (shouldTrigger(db)) {
                        lastTriggerAtMs = System.currentTimeMillis()
                        Log.d(TAG, "TRIGGER!")
                        onTrigger()
                    }
                } else if (readCount < 0) {
                    Log.e(TAG, "AudioRecord read error: $readCount")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrigger error: ${e.message}", e)
        } finally {
            releaseRecorder(audioRecord)
            if (recorder === audioRecord) {
                recorder = null
            }
            isRunning = false
        }
    }

    private fun shouldTrigger(db: Double): Boolean {
        val now = System.currentTimeMillis()
        return db > thresholdDb && now - lastTriggerAtMs >= triggerCooldownMs
    }

    private fun releaseRecorder(audioRecord: AudioRecord) {
        try {
            audioRecord.stop()
        } catch (_: IllegalStateException) {
        }

        audioRecord.release()
    }

    private fun calculateDb(buffer: ShortArray, readCount: Int): Double {
        var sum = 0.0

        for (i in 0 until readCount) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }

        val rms = sqrt(sum / readCount)
        return 20 * log10(rms + 1.0)
    }

    companion object {
        private const val TAG = "POCO"
        private const val BYTES_PER_SAMPLE = 2
    }
}
