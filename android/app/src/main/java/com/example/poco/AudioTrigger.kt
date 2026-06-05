package com.example.poco

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.log10
import kotlin.math.sqrt

class AudioTrigger {

    private val sampleRate = 16000

    fun startListening() {

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val buffer = ShortArray(bufferSize)

        recorder.startRecording()

        while (true) {

            recorder.read(buffer, 0, buffer.size)

            val db = calculateDb(buffer)

            Log.d("POCO", "dB = $db")

            if (db > 55) {
                Log.d("POCO", "TRIGGER!")
            }
        }
    }

    private fun calculateDb(buffer: ShortArray): Double {

        var sum = 0.0

        for (sample in buffer) {
            sum += sample * sample
        }

        val rms = sqrt(sum / buffer.size)

        return 20 * log10(rms + 1)
    }
}
