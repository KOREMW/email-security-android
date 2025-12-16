package com.mop.phishing.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.ln

class UrlClassifier private constructor(context: Context) {

    companion object {
        private const val TAG = "UrlClassifier"

        // 버전 정보 (디버깅용)
        const val VERSION = "v10.2-DEBUG"

        // v10 Temperature Scaling: 극단값 완화 (Label Smoothing과 함께 사용)
        // Temperature > 1: 출력을 더 부드럽게 (중간값 증가)
        // Temperature < 1: 출력을 더 극단적으로
        private const val TEMPERATURE = 2.0f

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
                    Log.d(TAG, "UrlClassifier $VERSION 싱글톤 인스턴스 생성")
                }
            }
        }

        /**
         * 강제로 인스턴스 재생성 (디버깅용)
         */
        fun forceResetInstance(context: Context): UrlClassifier {
            synchronized(this) {
                INSTANCE?.interpreter?.close()
                INSTANCE = null
                Log.d(TAG, "UrlClassifier 강제 리셋")
            }
            return getInstance(context)
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
        
        /**
         * 🆕 URL 정규화 - 프로토콜이 없으면 추가
         */
        fun normalizeUrl(url: String): String {
            val trimmed = url.trim()
            
            // 이미 프로토콜이 있으면 그대로 반환
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed
            }
            
            // 프로토콜이 없으면 https:// 추가
            return "https://$trimmed"
        }
    }

    private val interpreter: Interpreter

    // ============================================
    // StandardScaler 값 (v10 모델 - 실제 피싱 URL 데이터 기반)
    // ============================================
    private val scalerMean = floatArrayOf(
        28.570552f,            // urlLength
        16.852761f,            // domainLength
        3.398773f,             // pathLength
        0.981595f,             // subdomainCount
        0.975460f,             // hasHttps
        0.0f,                  // hasPort
        0.030675f,             // paramCount
        0.015428f,             // digitRatio
        0.086850f,             // specialCharRatio
        0.024540f,             // hasIpAddress
        0.282209f,             // hasSuspiciousKeyword
        0.417178f,             // hasSuspiciousTld
        3.380491f              // entropy
    )

    private val scalerScale = floatArrayOf(
        8.228314f,             // urlLength
        5.879394f,             // domainLength
        5.483427f,             // pathLength
        0.981250f,             // subdomainCount
        0.154718f,             // hasHttps
        1.0f,                  // hasPort
        0.172435f,             // paramCount
        0.052721f,             // digitRatio
        0.028049f,             // specialCharRatio
        0.154718f,             // hasIpAddress
        0.450074f,             // hasSuspiciousKeyword
        0.493093f,             // hasSuspiciousTld
        0.398424f              // entropy
    )

    // 대형 브랜드 도메인 (정상 사이트에서 흔히 사용)
    private val majorBrands = setOf(
        "google", "facebook", "amazon", "apple", "microsoft", "paypal",
        "ebay", "netflix", "twitter", "instagram", "linkedin", "youtube",
        "yahoo", "bing", "github", "stackoverflow", "reddit", "wikipedia",
        "naver", "daum", "kakao", "tistory", "nate"
    )

    // 신뢰할 수 있는 도메인 (점수 조정용) - 더 포괄적으로 확장
    private val trustedDomains = setOf(
        // 글로벌
        "google.com", "google.co.kr", "facebook.com", "amazon.com", "apple.com",
        "microsoft.com", "paypal.com", "ebay.com", "netflix.com", "twitter.com",
        "instagram.com", "linkedin.com", "youtube.com", "yahoo.com", "bing.com",
        "github.com", "stackoverflow.com", "reddit.com", "wikipedia.org",
        // 한국
        "naver.com", "daum.net", "kakao.com", "tistory.com", "nate.com",
        "coupang.com", "gmarket.co.kr", "11st.co.kr", "interpark.com",
        "samsung.com", "lg.com", "sk.com", "kt.com", "lotte.com",
        // 정부/공공
        "go.kr", "or.kr", "ac.kr", "re.kr"
    )

    // 의심 키워드 목록 (피싱에서 흔히 사용되는 키워드)
    private val suspiciousKeywords = listOf(
        "verify", "secure", "update", "confirm", "authenticate",
        "credentials", "suspend", "unusual", "alert", "urgent",
        "expired", "blocked", "limited", "restore", "unlock",
        "reactivate", "validate", "confirmation", "security-check",
        "verify-identity", "account-recovery"
    )

    // 의심 TLD 목록 (v10 학습 데이터와 동일)
    private val suspiciousTlds = listOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top",
        ".click", ".link", ".download", ".stream", ".racing",
        ".win", ".bid", ".loan", ".work", ".date", ".review",
        ".vip", ".cn", ".cyou", ".cc", ".shop"  // v10 추가
    )

    init {
        interpreter = Interpreter(loadModelFile(context, "phishing_model.tflite"))
        Log.d(TAG, "TFLite 모델 로드 완료 (v10, 실제 피싱 URL 데이터 + Label Smoothing)")
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
     * 🆕 URL에서 hostname 추출 (정규화 포함)
     */
    private fun extractHostname(url: String): String {
        val normalizedUrl = normalizeUrl(url)
        
        val uri = try {
            java.net.URI(normalizedUrl)
        } catch (e: Exception) {
            Log.w(TAG, "URI 파싱 실패: $normalizedUrl, ${e.message}")
            null
        }
        
        var hostname = (uri?.host ?: "").lowercase()
        
        // URI 파싱 실패 시 직접 추출 시도
        if (hostname.isEmpty()) {
            hostname = extractHostnameManually(url)
        }
        
        return hostname
    }
    
    /**
     * 🆕 수동으로 hostname 추출 (URI 파싱 실패 시 백업)
     */
    private fun extractHostnameManually(url: String): String {
        var cleaned = url.trim().lowercase()
        
        // 프로토콜 제거
        cleaned = cleaned.removePrefix("https://").removePrefix("http://")
        
        // 경로, 쿼리, 포트 제거
        cleaned = cleaned.split("/")[0]
        cleaned = cleaned.split("?")[0]
        cleaned = cleaned.split(":")[0]
        
        Log.d(TAG, "수동 hostname 추출: $url → $cleaned")
        return cleaned
    }

    /**
     * URL에서 13개 피처 추출 (v6 학습 코드와 100% 동일)
     */
    private fun extractFeatures(url: String): FloatArray {
        // 🆕 URL 정규화 적용
        val normalizedUrl = normalizeUrl(url)
        
        val uri = try {
            java.net.URI(normalizedUrl)
        } catch (e: Exception) {
            null
        }

        val hostname = extractHostname(url)
        val path = uri?.path ?: ""
        val scheme = (uri?.scheme ?: "https").lowercase()
        val query = uri?.query ?: ""
        val port = uri?.port ?: -1

        // 1. urlLength (원본 URL 기준)
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

        // 11. hasSuspiciousKeyword (v7 개선: 대형 브랜드 컨텍스트 고려)
        val urlLower = url.lowercase()
        val domainParts = hostname.split(".")
        val baseDomain = if (domainParts.size >= 2) {
            domainParts.takeLast(2).joinToString(".")
        } else hostname

        // 대형 브랜드 도메인인지 확인
        val isMajorBrandDomain = majorBrands.any { baseDomain.contains(it) }

        var hasSuspiciousKeyword = 0f
        if (!isMajorBrandDomain) {
            // 대형 브랜드가 아닌 도메인에서 의심 키워드가 있으면 의심
            if (suspiciousKeywords.any { urlLower.contains(it) }) {
                hasSuspiciousKeyword = 1f
            }
            // 대형 브랜드 이름을 가장하는 경우 (도메인에 브랜드 이름이 포함되었지만 실제 브랜드 도메인이 아님)
            for (brand in majorBrands) {
                if (hostname.contains(brand) && !hostname.endsWith("$brand.com") && !hostname.endsWith("$brand.co.kr")) {
                    hasSuspiciousKeyword = 1f
                    break
                }
            }
        }

        // 12. hasSuspiciousTld (Python 학습 코드와 동일: contains 방식)
        val hasSuspiciousTld = if (suspiciousTlds.any { it in hostname }) 1f else 0f

        // 13. entropy
        val entropy = calculateEntropy(hostname)

        val features = floatArrayOf(
            urlLength, domainLength, pathLength, subdomainCount,
            hasHttps, hasPort, paramCount, digitRatio, specialCharRatio,
            hasIpAddress, hasSuspiciousKeyword, hasSuspiciousTld, entropy
        )

        Log.d(TAG, "피처 추출: $url (정규화: $normalizedUrl)")
        Log.d(TAG, "  hostname: $hostname")
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
     * 🆕 신뢰 도메인 체크 (개선)
     */
    private fun isTrustedDomain(hostname: String): Boolean {
        if (hostname.isEmpty()) return false
        
        // 정확히 일치
        if (trustedDomains.contains(hostname)) {
            return true
        }
        
        // 서브도메인 체크 (www.naver.com, m.naver.com 등)
        for (domain in trustedDomains) {
            if (hostname == domain || hostname.endsWith(".$domain")) {
                return true
            }
        }
        
        // 한국 도메인 체크 (.go.kr, .or.kr 등)
        val koreanTrustedSuffixes = listOf(".go.kr", ".or.kr", ".ac.kr", ".re.kr")
        for (suffix in koreanTrustedSuffixes) {
            if (hostname.endsWith(suffix)) {
                return true
            }
        }
        
        return false
    }

    /**
     * 단일 URL 예측
     * @return 피싱 확률 (0.0 ~ 1.0)
     */
    fun predict(url: String): Float {
        // 🆕 hostname 추출 (정규화 포함)
        val hostname = extractHostname(url)

        Log.w(TAG, "╭────────── PREDICT START (VERSION: $VERSION) ──────────╮")
        Log.w(TAG, "│ URL: $url")
        Log.w(TAG, "│ hostname: $hostname")

        // 🆕 개선된 신뢰 도메인 체크
        if (isTrustedDomain(hostname)) {
            Log.w(TAG, "│ ★ 신뢰 도메인 감지: $hostname → 5%")
            Log.w(TAG, "╰────────── PREDICT END (TRUSTED) ──────────╯")
            return 0.05f
        }

        // 1. 피처 추출
        val rawFeatures = extractFeatures(url)
        Log.d(TAG, "  FEATURES (13개): ${rawFeatures.toList()}")

        // 2. 정규화 적용
        val normalizedFeatures = normalize(rawFeatures)
        Log.d(TAG, "  NORMALIZED (13개): ${normalizedFeatures.toList()}")

        // 3. 모델 추론 - 상세 로깅
        Log.d(TAG, "  INTERPRETER: $interpreter")
        val inputs = arrayOf(normalizedFeatures)
        val outputs = Array(1) { FloatArray(1) }

        Log.d(TAG, "  INPUT shape: [${inputs.size}][${inputs[0].size}]")
        Log.d(TAG, "  OUTPUT shape: [${outputs.size}][${outputs[0].size}]")

        try {
            interpreter.run(inputs, outputs)
            Log.d(TAG, "  INTERPRETER.run() 성공")
        } catch (e: Exception) {
            Log.e(TAG, "  INTERPRETER.run() 실패: ${e.message}", e)
            return 0.5f
        }

        val rawOutput = outputs[0][0]
        Log.d(TAG, "  RAW OUTPUT (모델 직접 출력): $rawOutput")

        val rawScore = rawOutput.coerceIn(0.0001f, 0.9999f)
        Log.d(TAG, "  RAW SCORE (coerced): $rawScore (${(rawScore * 100).toInt()}%)")

        // Temperature Scaling 적용: 극단값 완화
        // logit = log(p / (1-p)), scaled_p = sigmoid(logit / T)
        val logit = ln(rawScore / (1 - rawScore))
        val scaledLogit = logit / TEMPERATURE
        val calibratedScore = (1.0f / (1.0f + exp(-scaledLogit))).toFloat()

        Log.d(TAG, "  LOGIT: $logit, SCALED_LOGIT: $scaledLogit")
        Log.w(TAG, "│ ★ ML raw: ${(rawScore * 100).toInt()}% → calibrated: ${(calibratedScore * 100).toInt()}%")
        Log.w(TAG, "╰────────── PREDICT END (${(calibratedScore * 100).toInt()}%) ──────────╯")

        return calibratedScore.coerceIn(0f, 1f)
    }

    /**
     * URL 분석 결과 반환
     */
    fun analyze(url: String): UrlAnalysisResult {
        val score = predict(url)

        // v10 모델 임계값 (실제 피싱 URL 데이터 기반)
        val level = when {
            score < 0.2f -> "SAFE"
            score < 0.4f -> "SUSPICIOUS"
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

    /**
     * 디버그용: 상세 예측 정보 반환
     */
    fun predictWithDebug(url: String): PredictDebugResult {
        val hostname = extractHostname(url)

        // 신뢰 도메인 체크
        if (isTrustedDomain(hostname)) {
            return PredictDebugResult(
                url = url,
                hostname = hostname,
                isTrustedDomain = true,
                rawFeatures = floatArrayOf(),
                normalizedFeatures = floatArrayOf(),
                rawScore = 0.05f,
                calibratedScore = 0.05f,
                featureNames = emptyList()
            )
        }

        // 피처 추출
        val rawFeatures = extractFeatures(url)

        // 정규화
        val normalizedFeatures = normalize(rawFeatures)

        // 모델 추론
        val inputs = arrayOf(normalizedFeatures)
        val outputs = Array(1) { FloatArray(1) }
        interpreter.run(inputs, outputs)

        val rawScore = outputs[0][0].coerceIn(0.0001f, 0.9999f)

        // Temperature Scaling
        val logit = ln(rawScore / (1 - rawScore))
        val scaledLogit = logit / TEMPERATURE
        val calibratedScore = (1.0f / (1.0f + exp(-scaledLogit))).toFloat()

        val featureNames = listOf(
            "urlLength", "domainLength", "pathLength", "subdomainCount",
            "hasHttps", "hasPort", "paramCount", "digitRatio", "specialCharRatio",
            "hasIpAddress", "hasSuspiciousKeyword", "hasSuspiciousTld", "entropy"
        )

        return PredictDebugResult(
            url = url,
            hostname = hostname,
            isTrustedDomain = false,
            rawFeatures = rawFeatures,
            normalizedFeatures = normalizedFeatures,
            rawScore = rawScore,
            calibratedScore = calibratedScore.coerceIn(0f, 1f),
            featureNames = featureNames
        )
    }
}

/**
 * 디버그용 예측 결과
 */
data class PredictDebugResult(
    val url: String,
    val hostname: String,
    val isTrustedDomain: Boolean,
    val rawFeatures: FloatArray,
    val normalizedFeatures: FloatArray,
    val rawScore: Float,
    val calibratedScore: Float,
    val featureNames: List<String>
) {
    fun toDebugString(): String {
        if (isTrustedDomain) {
            return "신뢰 도메인: $hostname → 5%"
        }

        val sb = StringBuilder()
        sb.appendLine("=== 디버그 정보 ===")
        sb.appendLine("URL: $url")
        sb.appendLine("Hostname: $hostname")
        sb.appendLine()
        sb.appendLine("Raw Features:")
        for (i in featureNames.indices) {
            sb.appendLine("  ${featureNames[i]}: ${rawFeatures.getOrNull(i) ?: "N/A"}")
        }
        sb.appendLine()
        sb.appendLine("Normalized Features:")
        for (i in featureNames.indices) {
            sb.appendLine("  ${featureNames[i]}: ${normalizedFeatures.getOrNull(i)?.let { "%.4f".format(it) } ?: "N/A"}")
        }
        sb.appendLine()
        sb.appendLine("Model Output:")
        sb.appendLine("  Raw Score: ${(rawScore * 100).toInt()}%")
        sb.appendLine("  Calibrated Score: ${(calibratedScore * 100).toInt()}%")
        return sb.toString()
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
