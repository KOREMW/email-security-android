package com.example.phishing.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class UrlClassifier(context: Context) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, "url_model.tflite"))
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.length
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }



    // url에서 우리가 정의한 5개 피처를 추출하는 함수
    private fun extractFeatures(url: String): FloatArray {
        val length = url.length
        val numDigits = url.count { it.isDigit() }
        val numSpecial = url.count { !it.isLetterOrDigit() }

        val host = try {
            java.net.URI(url).host ?: ""
        } catch (e: Exception) {
            ""
        }
        val hasIp = if (host.matches(Regex("[0-9.]+"))) 1 else 0

        val suspiciousWords = listOf("login", "verify", "update", "secure", "account", "bank")
        val lowerUrl = url.lowercase()
        val hasSuspiciousWord = if (suspiciousWords.any { lowerUrl.contains(it) }) 1 else 0

        return floatArrayOf(
            length.toFloat(),
            numDigits.toFloat(),
            numSpecial.toFloat(),
            hasIp.toFloat(),
            hasSuspiciousWord.toFloat()
        )
    }

    // 단일 URL 예측 (0=정상, 1=피싱)
    fun predict(url: String): Float {
        val input = extractFeatures(url)
        val inputs = arrayOf(input)
        val outputs = Array(1) { FloatArray(1) }

        interpreter.run(inputs, outputs)
        return outputs[0][0]  // 0.0 ~ 1.0 확률
    }

    fun close() {
        interpreter.close()
    }
}
