package com.example.poco

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavFileWriter {
    fun writeMono16(file: File, samples: ShortArray, sampleRate: Int) {
        file.parentFile?.mkdirs()

        val dataSize = samples.size * 2
        FileOutputStream(file).use { out ->
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            out.writeIntLE(36 + dataSize)
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            out.writeIntLE(16)
            out.writeShortLE(1)
            out.writeShortLE(1)
            out.writeIntLE(sampleRate)
            out.writeIntLE(sampleRate * 2)
            out.writeShortLE(2)
            out.writeShortLE(16)
            out.write("data".toByteArray(Charsets.US_ASCII))
            out.writeIntLE(dataSize)

            val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { buffer.putShort(it) }
            out.write(buffer.array())
        }
    }

    private fun FileOutputStream.writeIntLE(value: Int) {
        write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    }

    private fun FileOutputStream.writeShortLE(value: Int) {
        write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array())
    }
}
