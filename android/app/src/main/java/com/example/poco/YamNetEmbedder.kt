package com.example.poco

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YamNetEmbedder(context: Context) : AutoCloseable {
    private val interpreter = Interpreter(loadModel(context))

    fun extractMeanEmbedding(waveform: FloatArray): FloatArray {
        interpreter.resizeInput(0, intArrayOf(waveform.size))
        interpreter.allocateTensors()

        val outputShapes = List(interpreter.outputTensorCount) { index ->
            index to interpreter.getOutputTensor(index).shape()
        }
        val embeddingOutput = outputShapes.firstOrNull { (_, shape) ->
            shape.size >= 2 && shape.last() == EMBEDDING_DIM
        } ?: error(
            "YAMNet embedding output not found. Shapes: ${
                outputShapes.joinToString { (index, shape) -> "$index=${shape.contentToString()}" }
            }"
        )

        Log.d(
            "POCO",
            "YAMNet shapes ${
                outputShapes.joinToString { (index, shape) -> "$index=${shape.contentToString()}" }
            }; embeddingOutput=${embeddingOutput.first}"
        )

        val embeddingOutputIndex = embeddingOutput.first
        val embeddingShape = embeddingOutput.second
        val frameCount = embeddingShape[0]
        val embeddingDim = embeddingShape[1]
        val embeddings = Array(frameCount) { FloatArray(embeddingDim) }
        val outputs = mutableMapOf<Int, Any>()
        outputShapes.forEach { (index, shape) ->
            outputs[index] = if (index == embeddingOutputIndex) {
                embeddings
            } else {
                createOutputBuffer(shape)
            }
        }

        interpreter.runForMultipleInputsOutputs(arrayOf(waveform), outputs)

        val mean = FloatArray(embeddingDim)
        embeddings.forEach { frame ->
            for (i in frame.indices) {
                mean[i] += frame[i]
            }
        }
        for (i in mean.indices) {
            mean[i] /= frameCount.coerceAtLeast(1)
        }
        return mean
    }

    override fun close() {
        interpreter.close()
    }

    private fun loadModel(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("yamnet.tflite")
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    private fun createOutputBuffer(shape: IntArray): Any =
        when (shape.size) {
            1 -> FloatArray(shape[0])
            2 -> Array(shape[0]) { FloatArray(shape[1]) }
            else -> error("Unsupported YAMNet output shape: ${shape.contentToString()}")
        }

    private companion object {
        const val EMBEDDING_DIM = 1024
    }
}
