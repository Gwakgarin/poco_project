package com.example.poco

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class YamNetOutput(
    val meanEmbedding: FloatArray,
    val meanScores: FloatArray
)

class YamNetEmbedder(context: Context) : AutoCloseable {
    private val interpreter = Interpreter(loadModel(context))

    // 기존에 embedding만 쓰던 코드가 있다면 그대로 동작하도록 유지
    fun extractMeanEmbedding(waveform: FloatArray): FloatArray = extract(waveform).meanEmbedding

    fun extract(waveform: FloatArray): YamNetOutput {
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

        val scoresOutput = outputShapes.firstOrNull { (_, shape) ->
            shape.size >= 2 && shape.last() == SCORE_CLASS_COUNT
        } ?: error(
            "YAMNet scores output not found. Shapes: ${
                outputShapes.joinToString { (index, shape) -> "$index=${shape.contentToString()}" }
            }"
        )

        Log.d(
            "POCO",
            "YAMNet shapes ${
                outputShapes.joinToString { (index, shape) -> "$index=${shape.contentToString()}" }
            }; embeddingOutput=${embeddingOutput.first}; scoresOutput=${scoresOutput.first}"
        )

        val embeddingOutputIndex = embeddingOutput.first
        val embeddingShape = embeddingOutput.second
        val frameCount = embeddingShape[0]
        val embeddingDim = embeddingShape[1]
        val embeddings = Array(frameCount) { FloatArray(embeddingDim) }

        val scoresOutputIndex = scoresOutput.first
        val scoresShape = scoresOutput.second
        val scoreFrameCount = scoresShape[0]
        val scoreClassCount = scoresShape[1]
        val scores = Array(scoreFrameCount) { FloatArray(scoreClassCount) }

        val outputs = mutableMapOf<Int, Any>()
        outputShapes.forEach { (index, shape) ->
            outputs[index] = when (index) {
                embeddingOutputIndex -> embeddings
                scoresOutputIndex -> scores
                else -> createOutputBuffer(shape)
            }
        }

        interpreter.runForMultipleInputsOutputs(arrayOf(waveform), outputs)

        val meanEmbedding = FloatArray(embeddingDim)
        embeddings.forEach { frame ->
            for (i in frame.indices) {
                meanEmbedding[i] += frame[i]
            }
        }
        for (i in meanEmbedding.indices) {
            meanEmbedding[i] /= frameCount.coerceAtLeast(1)
        }

        val meanScores = FloatArray(scoreClassCount)
        scores.forEach { frame ->
            for (i in frame.indices) {
                meanScores[i] += frame[i]
            }
        }
        for (i in meanScores.indices) {
            meanScores[i] /= scoreFrameCount.coerceAtLeast(1)
        }

        return YamNetOutput(meanEmbedding, meanScores)
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

    companion object {
        const val EMBEDDING_DIM = 1024
        const val SCORE_CLASS_COUNT = 521

        // yamnet_class_map.csv (공식 배포 파일) 기준 인덱스 - 아래 URL로 직접 확인함
        // https://raw.githubusercontent.com/tensorflow/models/master/research/audioset/yamnet/yamnet_class_map.csv
        const val SPEECH_INDEX = 0
        const val CONVERSATION_INDEX = 2
        const val TELEVISION_INDEX = 518
    }
}