package com.example.poco

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ClassificationResult(
    val label: String,
    val score: Float
)

class PocoClassifier(context: Context) : AutoCloseable {
    private val interpreter = Interpreter(loadModel(context, MODEL_FILE))
    private val labelMap = loadLabelMap(context)

    fun classify(embedding: FloatArray): ClassificationResult {
        require(embedding.size == INPUT_DIM) {
            "Expected $INPUT_DIM embedding values, got ${embedding.size}"
        }

        val input = arrayOf(embedding)
        val output = Array(1) { FloatArray(labelMap.size) }
        interpreter.run(input, output)

        val scores = output[0]
        val bestIndex = scores.indices.maxBy { scores[it] }
        return ClassificationResult(
            label = labelMap[bestIndex] ?: bestIndex.toString(),
            score = scores[bestIndex]
        )
    }

    fun labels(): List<String> = labelMap.toSortedMap().values.toList()

    override fun close() {
        interpreter.close()
    }

    private fun loadLabelMap(context: Context): Map<Int, String> {
        val jsonText = context.assets.open(LABEL_MAP_FILE).bufferedReader().use { it.readText() }
        val json = JSONObject(jsonText)
        return json.keys().asSequence().associate { key ->
            key.toInt() to json.getString(key)
        }
    }

    private fun loadModel(context: Context, fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    private companion object {
        const val MODEL_FILE = "classifier.tflite"
        const val LABEL_MAP_FILE = "label_map.json"
        const val INPUT_DIM = 1024
    }
}

object PocoAssets {
    fun labels(context: Context): List<String> {
        val jsonText = context.assets.open("label_map.json").bufferedReader().use { it.readText() }
        val json = JSONObject(jsonText)
        return json.keys().asSequence()
            .map { it.toInt() to json.getString(it.toString()) }
            .sortedBy { it.first }
            .map { it.second }
            .toList()
    }
}
