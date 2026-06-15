package com.example.poco

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

class AudioMonitorService : Service() {
    @Volatile
    private var running = false
    private var monitorThread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startAsForeground("Monitoring", "Listening for sounds over ${DB_THRESHOLD.toInt()} dB")
        } catch (t: Throwable) {
            Log.e("POCO", "AudioMonitorService foreground start failed", t)
            sendFailure(t)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (monitorThread?.isAlive == true) {
            return START_STICKY
        }

        running = true
        monitorThread = Thread {
            monitorLoop()
        }.apply {
            name = "PocoAudioMonitor"
            start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        monitorThread?.interrupt()
        super.onDestroy()
    }

    private fun monitorLoop() {
        while (running) {
            try {
                sendStatus("마이크 감시 중")
                val samples = listenUntilTriggeredAndRecordSegment()
                if (samples == null) {
                    continue
                }

                sendStatus("5초 소리 분류 중")
                processSegment(samples)

                sendStatus("마이크 감시 중")
                Thread.sleep(TRIGGER_COOLDOWN_MS)
            } catch (t: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (t: Throwable) {
                Log.e("POCO", "AudioMonitorService failed", t)
                sendFailure(t)
                sleepQuietly(ERROR_RETRY_DELAY_MS)
            }
        }
    }

    private fun listenUntilTriggeredAndRecordSegment(): ShortArray? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("RECORD_AUDIO permission is not granted")
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            throw IllegalStateException("AudioRecord min buffer error: $minBuffer")
        }

        val readBufferSize = maxOf(minBuffer / 2, 640)
        val readBuffer = ShortArray(readBufferSize)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, SAMPLE_RATE)
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord initialization failed")
        }

        var lastDebugLogAt = 0L
        recorder.startRecording()
        Log.d("POCO", "AudioMonitorService monitoring started")

        try {
            while (running) {
                val read = recorder.read(readBuffer, 0, readBuffer.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) {
                    Log.e("POCO", "AudioRecord read failed: $read")
                    continue
                }

                val db = calculateDb(readBuffer, read)
                val peak = readBuffer.take(read).maxOf { kotlin.math.abs(it.toInt()) }
                sendLevel(db, peak)

                if (db >= DB_THRESHOLD) {
                    Log.d("POCO", "TRIGGER read=$read peak=$peak dB=$db")
                    sendStatus("큰 소리 감지: 5초 녹음 중")
                    return collectFiveSecondSegment(recorder, readBuffer, read)
                }

                val now = System.currentTimeMillis()
                if (now - lastDebugLogAt >= DEBUG_LOG_INTERVAL_MS) {
                    Log.d("POCO", "monitor read=$read peak=$peak dB=$db")
                    lastDebugLogAt = now
                }
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
            recorder.release()
            Log.d("POCO", "AudioMonitorService recorder released")
        }

        return null
    }

    private fun collectFiveSecondSegment(
        recorder: AudioRecord,
        firstBuffer: ShortArray,
        firstRead: Int
    ): ShortArray {
        val totalSamples = SAMPLE_RATE * RECORD_SECONDS
        val output = ShortArray(totalSamples)
        var offset = minOf(firstRead, totalSamples)
        firstBuffer.copyInto(output, destinationOffset = 0, startIndex = 0, endIndex = offset)

        val buffer = ShortArray(firstBuffer.size)
        while (running && offset < totalSamples) {
            val read = recorder.read(buffer, 0, minOf(buffer.size, totalSamples - offset), AudioRecord.READ_BLOCKING)
            if (read > 0) {
                buffer.copyInto(output, destinationOffset = offset, startIndex = 0, endIndex = read)
                offset += read
            } else {
                Log.e("POCO", "AudioRecord segment read failed: $read")
            }
        }

        Log.d("POCO", "AudioMonitorService segment captured samples=$offset")
        return output
    }

    private fun processSegment(samples: ShortArray) {
        val wavFile = saveWav(samples)
        val waveform = samples.toFloatWaveform()

        Log.d("POCO", "AudioMonitorService classification started")
        val embedding = YamNetEmbedder(applicationContext).use { it.extractMeanEmbedding(waveform) }
        val result = PocoClassifier(applicationContext).use { it.classify(embedding) }
        Log.d("POCO", "AudioMonitorService classification result=${result.label} score=${result.score}")

        sendResult(result, wavFile, "분류 완료: 서버 저장 전")

        val serverStatus = try {
            postSoundEvent(result, wavFile)
        } catch (t: Throwable) {
            Log.e("POCO", "AudioMonitorService server save failed", t)
            "Server save failed: ${t.message ?: t::class.java.simpleName}"
        }
        Log.d("POCO", "AudioMonitorService $serverStatus")

        sendResult(result, wavFile, serverStatus)
    }

    private fun calculateDb(buffer: ShortArray, sampleCount: Int): Double {
        var sum = 0.0
        for (i in 0 until sampleCount) {
            val sample = buffer[i]
            sum += sample * sample
        }
        val rms = sqrt(sum / sampleCount)
        return 20 * log10(rms + 1)
    }

    private fun saveWav(samples: ShortArray): File {
        val name = SimpleDateFormat("'poco_'yyyyMMdd_HHmmss'.wav'", Locale.US).format(Date())
        val file = File(filesDir, "recordings/$name")
        WavFileWriter.writeMono16(file, samples, SAMPLE_RATE)
        return file
    }

    private fun ShortArray.toFloatWaveform(): FloatArray =
        FloatArray(size) { index -> this[index] / 32768.0f }

    private fun postSoundEvent(result: ClassificationResult, wavFile: File): String {
        val request = SoundEventRequest(
            rawFile = wavFile.name,
            splitFile = wavFile.name,
            predLabel = result.label,
            predScore = result.score.toDouble(),
            segIndex = 0,
            startSec = 0,
            endSec = RECORD_SECONDS,
            smoothedLabel = result.label
        )

        val response = ServerApiClient.api.createSoundEvent(request).execute()
        return if (response.isSuccessful) {
            "Server saved: HTTP ${response.code()}"
        } else {
            "Server failed: HTTP ${response.code()}"
        }
    }

    private fun sendResult(
        result: ClassificationResult,
        wavFile: File,
        serverStatus: String
    ) {
        val intent = Intent(ACTION_RESULT).setPackage(packageName)
            .putExtra(EXTRA_MONITOR_STATUS, "마이크 감시 중")
            .putExtra(EXTRA_LABEL, result.label)
            .putExtra(EXTRA_SCORE, result.score)
            .putExtra(EXTRA_WAV_PATH, wavFile.absolutePath)
            .putExtra(EXTRA_SERVER_STATUS, serverStatus)
        sendBroadcast(intent)
    }

    private fun sendStatus(status: String) {
        val intent = Intent(ACTION_RESULT).setPackage(packageName)
            .putExtra(EXTRA_MONITOR_STATUS, status)
        sendBroadcast(intent)
        updateNotification(status, "POCO is listening in the background")
    }

    private fun sendLevel(db: Double, peak: Int) {
        val intent = Intent(ACTION_RESULT).setPackage(packageName)
            .putExtra(EXTRA_DB, db)
            .putExtra(EXTRA_PEAK, peak)
        sendBroadcast(intent)
    }

    private fun sendFailure(error: Throwable) {
        val intent = Intent(ACTION_RESULT).setPackage(packageName)
            .putExtra(EXTRA_MONITOR_STATUS, "분류 실패")
            .putExtra(EXTRA_ERROR, error.message ?: error::class.java.simpleName)
        sendBroadcast(intent)
    }

    private fun startAsForeground(title: String, text: String) {
        val foregroundNotification = notification(title, text).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                foregroundNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, foregroundNotification)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification(title, text).setOngoing(true).build())
    }

    private fun notification(title: String, text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "POCO audio detection",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun sleepQuietly(durationMs: Long) {
        try {
            Thread.sleep(durationMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        const val ACTION_RESULT = "com.example.poco.AUDIO_RESULT"
        const val EXTRA_MONITOR_STATUS = "monitor_status"
        const val EXTRA_LABEL = "label"
        const val EXTRA_SCORE = "score"
        const val EXTRA_WAV_PATH = "wav_path"
        const val EXTRA_SERVER_STATUS = "server_status"
        const val EXTRA_ERROR = "error"
        const val EXTRA_DB = "db"
        const val EXTRA_PEAK = "peak"

        private const val CHANNEL_ID = "poco_audio_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val SAMPLE_RATE = 16000
        private const val RECORD_SECONDS = 5
        private const val DB_THRESHOLD = 55.0
        private const val TRIGGER_COOLDOWN_MS = 3000L
        private const val ERROR_RETRY_DELAY_MS = 3000L
        private const val DEBUG_LOG_INTERVAL_MS = 1000L
    }
}
