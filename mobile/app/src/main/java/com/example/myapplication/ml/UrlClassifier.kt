package com.example.myapplication.ml

import android.content.Context
import android.util.Log

/**
 * URL 분류기 (임시 더미 구현)
 * TODO: 실제 TFLite 모델 연동 시 주석 해제
 */
class UrlClassifier(context: Context) {

    // private val interpreter: Interpreter  // TODO: 모델 연동 시 활성화

    init {
        // TODO: 실제 모델 연동 시 아래 주석 해제
        // interpreter = Interpreter(loadModelFile(context, "url_model.tflite"))
        Log.d("UrlClassifier", "더미 모드로 초기화됨 (TFLite 모델 미사용)")
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

    // 단일 URL 예측 (0=정상, 1=피싱) - 더미 구현
    fun predict(url: String): Float {
        val features = extractFeatures(url)
        
        // TODO: 실제 모델 연동 시 아래 코드로 교체
        // val inputs = arrayOf(features)
        // val outputs = Array(1) { FloatArray(1) }
        // interpreter.run(inputs, outputs)
        // return outputs[0][0]
        
        // 더미 로직: 피처 기반 간단한 휴리스틱 점수 반환
        val hasIp = features[3]
        val hasSuspicious = features[4]
        val lengthScore = if (features[0] > 50) 0.2f else 0f
        val specialScore = if (features[2] > 10) 0.2f else 0f
        
        return (hasIp * 0.3f + hasSuspicious * 0.3f + lengthScore + specialScore).coerceIn(0f, 1f)
    }

    fun close() {
        // TODO: 실제 모델 연동 시 아래 주석 해제
        // interpreter.close()
    }
}
