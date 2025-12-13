package com.mop.phishing.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.ln

class UrlClassifier private constructor(context: Context) {

    companion object {
        private const val TAG = "UrlClassifier"

        @Volatile
        private var INSTANCE: UrlClassifier? = null

        /**
         * 싱글톤 인스턴스 반환
         * 앱 전체에서 하나의 TFLite 모델만 로드하여 메모리 절약
         */
        fun getInstance(context: Context): UrlClassifier {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UrlClassifier(context.applicationContext).also {
                    INSTANCE = it
                    Log.d(TAG, "UrlClassifier 싱글톤 인스턴스 생성")
                }
            }
        }

        /**
         * 인스턴스 해제 (앱 종료 시)
         */
        fun releaseInstance() {
            synchronized(this) {
                INSTANCE?.interpreter?.close()
                INSTANCE = null
                Log.d(TAG, "UrlClassifier 인스턴스 해제")
            }
        }
    }

    private val interpreter: Interpreter

    // ============================================
    // StandardScaler 값 (v6 모델 - scaler_config.json)
    // ============================================
    private val scalerMean = floatArrayOf(
        36.8404875f,           // urlLength
        21.852175f,            // domainLength
        4.4470375f,            // pathLength
        1.1668625f,            // subdomainCount
        0.74435f,              // hasHttps
        0.0001125f,            // hasPort
        0.07055f,              // paramCount
        0.032738347f,          // digitRatio
        0.08162040f,           // specialCharRatio
        0.00305f,              // hasIpAddress
        0.0473125f,            // hasSuspiciousKeyword
        0.0595875f,            // hasSuspiciousTld
        3.4893980f             // entropy
    )

    private val scalerScale = floatArrayOf(
        44.992049f,            // urlLength
        9.631491f,             // domainLength
        29.664013f,            // pathLength
        0.632451f,             // subdomainCount
        0.436226f,             // hasHttps
        0.010606f,             // hasPort
        0.610408f,             // paramCount
        0.074481f,             // digitRatio
        0.026087f,             // specialCharRatio
        0.055143f,             // hasIpAddress
        0.212306f,             // hasSuspiciousKeyword
        0.236721f,             // hasSuspiciousTld
        0.410255f              // entropy
    )

    // 의심 키워드 목록 (v6 모델과 동일)
    private val suspiciousKeywords = listOf(
        "login", "verify", "account", "secure", "update", "confirm",
        "password", "authenticate", "credentials", "signin", "banking",
        "paypal", "ebay", "amazon", "apple", "microsoft", "google",
        "facebook", "netflix", "wallet", "suspend", "unusual", "alert", "urgent"
    )

    // 의심 TLD 목록 (v6 모델과 동일)
    private val suspiciousTlds = listOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top",
        ".click", ".link", ".download", ".stream", ".racing", ".win", ".bid", ".loan"
    )

    init {
        interpreter = Interpreter(loadModelFile(context, "phishing_model.tflite"))
        Log.d(TAG, "TFLite 모델 로드 완료 (v6, 13개 피처, StandardScaler 적용)")
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.length
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * URL에서 13개 피처 추출 (v6 학습 코드와 100% 동일)
     */
    private fun extractFeatures(url: String): FloatArray {
        val uri = try {
            java.net.URI(url)
        } catch (e: Exception) {
            null
        }

        val hostname = (uri?.host ?: "").lowercase()
        val path = uri?.path ?: ""
        val scheme = (uri?.scheme ?: "").lowercase()
        val query = uri?.query ?: ""
        val port = uri?.port ?: -1

        // 1. urlLength
        val urlLength = url.length.toFloat()

        // 2. domainLength
        val domainLength = hostname.length.toFloat()

        // 3. pathLength
        val pathLength = path.length.toFloat()

        // 4. subdomainCount (hostname.count('.') - 1, 최소 0)
        val subdomainCount = maxOf(0, hostname.count { it == '.' } - 1).toFloat()

        // 5. hasHttps
        val hasHttps = if (scheme == "https") 1f else 0f

        // 6. hasPort
        val hasPort = if (port != -1 && port != 80 && port != 443) 1f else 0f

        // 7. paramCount
        val paramCount = if (query.isNotEmpty()) {
            query.split("&").size.toFloat()
        } else 0f

        // 8. digitRatio
        val digitCount = url.count { it.isDigit() }
        val digitRatio = if (url.isNotEmpty()) digitCount.toFloat() / url.length else 0f

        // 9. specialCharRatio (@ - _ . 문자만)
        val specialChars = "@-_."
        val specialCount = url.count { it in specialChars }
        val specialCharRatio = if (url.isNotEmpty()) specialCount.toFloat() / url.length else 0f

        // 10. hasIpAddress
        val ipPattern = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")
        val hostWithoutPort = hostname.split(":")[0]
        val hasIpAddress = if (ipPattern.matches(hostWithoutPort)) 1f else 0f

        // 11. hasSuspiciousKeyword
        val urlLower = url.lowercase()
        val hasSuspiciousKeyword = if (suspiciousKeywords.any { urlLower.contains(it) }) 1f else 0f

        // 12. hasSuspiciousTld
        val hasSuspiciousTld = if (suspiciousTlds.any { hostname.contains(it) }) 1f else 0f

        // 13. entropy
        val entropy = calculateEntropy(hostname)

        val features = floatArrayOf(
            urlLength, domainLength, pathLength, subdomainCount,
            hasHttps, hasPort, paramCount, digitRatio, specialCharRatio,
            hasIpAddress, hasSuspiciousKeyword, hasSuspiciousTld, entropy
        )

        Log.d(TAG, "피처 추출: $url")
        Log.d(TAG, "  raw: ${features.toList()}")

        return features
    }

    /**
     * 엔트로피 계산 (문자열의 랜덤성 측정)
     */
    private fun calculateEntropy(str: String): Float {
        if (str.isEmpty()) return 0f

        val freq = mutableMapOf<Char, Int>()
        for (c in str) {
            freq[c] = freq.getOrDefault(c, 0) + 1
        }

        var entropy = 0.0
        val len = str.length.toDouble()
        for (count in freq.values) {
            val p = count / len
            if (p > 0) {
                entropy -= p * (ln(p) / ln(2.0))
            }
        }

        return entropy.toFloat()
    }

    /**
     * StandardScaler 정규화 적용
     * 공식: (value - mean) / scale
     */
    private fun normalize(features: FloatArray): FloatArray {
        val normalized = FloatArray(features.size)
        for (i in features.indices) {
            normalized[i] = (features[i] - scalerMean[i]) / scalerScale[i]
        }
        Log.d(TAG, "  normalized: ${normalized.toList()}")
        return normalized
    }

    /**
     * 단일 URL 예측
     * @return 피싱 확률 (0.0 ~ 1.0)
     */
    fun predict(url: String): Float {
        // 1. 피처 추출
        val rawFeatures = extractFeatures(url)

        // 2. 정규화 적용
        val normalizedFeatures = normalize(rawFeatures)

        // 3. 모델 추론
        val inputs = arrayOf(normalizedFeatures)
        val outputs = Array(1) { FloatArray(1) }

        interpreter.run(inputs, outputs)

        val rawScore = outputs[0][0].coerceIn(0f, 1f)
        // 점수 반전 (모델이 0=피싱, 1=정상으로 학습됨)
        val score = 1f - rawScore
        Log.d(TAG, "  raw: $rawScore, 반전 후: $score")

        return score
    }

    /**
     * URL 분석 결과 반환
     */
    fun analyze(url: String): UrlAnalysisResult {
        val score = predict(url)

        // v6 모델 권장 임계값: 0.5275
        val level = when {
            score < 0.3f -> "SAFE"
            score < 0.7f -> "SUSPICIOUS"
            else -> "DANGEROUS"
        }

        return UrlAnalysisResult(
            url = url,
            riskScore = score,
            riskLevel = level
        )
    }

    /**
     * @deprecated 싱글톤 패턴 사용으로 직접 close 호출 불필요
     * 대신 UrlClassifier.releaseInstance() 사용
     */
    @Deprecated("Use UrlClassifier.releaseInstance() instead", ReplaceWith("UrlClassifier.releaseInstance()"))
    fun close() {
        // 싱글톤이므로 개별 close 호출 무시 (releaseInstance로만 해제)
        Log.w(TAG, "close() 호출됨 - 싱글톤이므로 무시됨. releaseInstance() 사용 권장")
    }
}

/**
 * URL 분석 결과 데이터 클래스
 */
data class UrlAnalysisResult(
    val url: String,
    val riskScore: Float,
    val riskLevel: String
)
