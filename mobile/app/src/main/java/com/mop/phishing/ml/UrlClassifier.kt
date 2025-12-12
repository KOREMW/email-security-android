package com.mop.phishing.ml

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite 기반 URL 피싱 분류기
 * 
 * 필요한 파일 (assets 폴더):
 * - phishing_model.tflite: TFLite 모델 파일
 * - scaler_params.json: StandardScaler 파라미터 (mean, scale)
 */
class UrlClassifier(private val context: Context) {

    companion object {
        private const val TAG = "UrlClassifier"
        private const val MODEL_FILE = "phishing_model.tflite"
        private const val SCALER_FILE = "scaler_params.json"
        private const val NUM_FEATURES = 5
    }

    private var interpreter: Interpreter? = null
    private var scalerMean: FloatArray? = null
    private var scalerScale: FloatArray? = null
    private var isInitialized = false

    init {
        try {
            loadModel()
            loadScaler()
            isInitialized = true
            Log.d(TAG, "TFLite 모델 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG, "TFLite 모델 초기화 실패, 더미 모드로 전환: ${e.message}")
            isInitialized = false
        }
    }

    /**
     * TFLite 모델 로드
     */
    private fun loadModel() {
        val modelBuffer = loadModelFile(MODEL_FILE)
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(modelBuffer, options)
        Log.d(TAG, "모델 로드 완료: $MODEL_FILE")
    }

    /**
     * assets에서 모델 파일 로드
     */
    private fun loadModelFile(filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Scaler 파라미터 로드 (StandardScaler의 mean, scale)
     */
    private fun loadScaler() {
        val jsonString = context.assets.open(SCALER_FILE).bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)
        
        val meanArray = json.getJSONArray("mean")
        val scaleArray = json.getJSONArray("scale")
        
        scalerMean = FloatArray(meanArray.length()) { i -> meanArray.getDouble(i).toFloat() }
        scalerScale = FloatArray(scaleArray.length()) { i -> scaleArray.getDouble(i).toFloat() }
        
        Log.d(TAG, "Scaler 로드 완료 - mean: ${scalerMean?.toList()}, scale: ${scalerScale?.toList()}")
    }

    /**
     * URL에서 피처 추출 (5개 피처)
     * 
     * 1. length: URL 전체 길이
     * 2. num_digits: 숫자 개수
     * 3. num_special: 특수문자 개수
     * 4. has_ip: IP 주소 포함 여부 (0 or 1)
     * 5. has_suspicious_word: 의심 단어 포함 여부 (0 or 1)
     */
    fun extractFeatures(url: String): FloatArray {
        val length = url.length
        val numDigits = url.count { it.isDigit() }
        val numSpecial = url.count { !it.isLetterOrDigit() }

        // 호스트 추출
        val host = try {
            java.net.URI(url).host ?: ""
        } catch (e: Exception) {
            ""
        }
        
        // IP 주소 패턴 체크
        val hasIp = if (host.matches(Regex("[0-9.]+"))) 1 else 0

        // 피싱 의심 단어 체크
        val suspiciousWords = listOf(
            "login", "verify", "update", "secure", "account", "bank",
            "signin", "confirm", "password", "credential", "suspend",
            "unusual", "activity", "limited", "urgent"
        )
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

    /**
     * 피처 정규화 (StandardScaler 적용)
     */
    private fun normalizeFeatures(features: FloatArray): FloatArray {
        val mean = scalerMean ?: return features
        val scale = scalerScale ?: return features
        
        return FloatArray(features.size) { i ->
            (features[i] - mean[i]) / scale[i]
        }
    }

    /**
     * URL 피싱 확률 예측
     * 
     * @param url 분석할 URL
     * @return 피싱 확률 (0.0 ~ 1.0, 1에 가까울수록 피싱)
     */
    fun predict(url: String): Float {
        val features = extractFeatures(url)
        
        // TFLite 모델이 초기화되지 않은 경우 더미 로직 사용
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "TFLite 미초기화, 더미 예측 사용")
            return predictDummy(features)
        }

        return try {
            // 피처 정규화
            val normalizedFeatures = normalizeFeatures(features)
            
            // 입력 버퍼 준비 (1 x 5 shape)
            val inputBuffer = ByteBuffer.allocateDirect(NUM_FEATURES * 4).apply {
                order(ByteOrder.nativeOrder())
                normalizedFeatures.forEach { putFloat(it) }
                rewind()
            }

            // 출력 버퍼 준비 (1 x 1 shape)
            val outputBuffer = ByteBuffer.allocateDirect(4).apply {
                order(ByteOrder.nativeOrder())
            }

            // 추론 실행
            interpreter?.run(inputBuffer, outputBuffer)
            
            // 결과 추출
            outputBuffer.rewind()
            val probability = outputBuffer.float
            
            Log.d(TAG, "URL: $url")
            Log.d(TAG, "Features: ${features.toList()}")
            Log.d(TAG, "Normalized: ${normalizedFeatures.toList()}")
            Log.d(TAG, "Prediction: $probability")
            
            probability.coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "예측 오류: ${e.message}")
            predictDummy(features)
        }
    }

    /**
     * 배치 예측 (여러 URL 동시 분석)
     */
    fun predictBatch(urls: List<String>): List<Float> {
        return urls.map { predict(it) }
    }

    /**
     * 더미 예측 로직 (모델 로드 실패 시 폴백)
     */
    private fun predictDummy(features: FloatArray): Float {
        val hasIp = features[3]
        val hasSuspicious = features[4]
        val lengthScore = if (features[0] > 50) 0.2f else 0f
        val specialScore = if (features[2] > 10) 0.2f else 0f
        val digitScore = if (features[1] > 10) 0.1f else 0f
        
        return (hasIp * 0.3f + hasSuspicious * 0.3f + lengthScore + specialScore + digitScore)
            .coerceIn(0f, 1f)
    }

    /**
     * 상세 분석 결과 반환
     */
    fun analyzeUrl(url: String): LocalUrlAnalysisResult {
        val features = extractFeatures(url)
        val probability = predict(url)
        
        return LocalUrlAnalysisResult(
            url = url,
            probability = probability,
            features = LocalUrlFeatures(
                length = features[0].toInt(),
                numDigits = features[1].toInt(),
                numSpecial = features[2].toInt(),
                hasIp = features[3] > 0.5f,
                hasSuspiciousWord = features[4] > 0.5f
            ),
            isModelLoaded = isInitialized
        )
    }

    /**
     * 리소스 해제
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Log.d(TAG, "TFLite 인터프리터 해제됨")
    }

    /**
     * 모델 로드 상태 확인
     */
    fun isModelLoaded(): Boolean = isInitialized
}

/**
 * 로컬 ML 분석용 URL 피처 정보
 * (CloudFunctionModels.kt의 UrlFeatures와 별개)
 */
data class LocalUrlFeatures(
    val length: Int,
    val numDigits: Int,
    val numSpecial: Int,
    val hasIp: Boolean,
    val hasSuspiciousWord: Boolean
)

/**
 * 로컬 ML URL 분석 결과
 */
data class LocalUrlAnalysisResult(
    val url: String,
    val probability: Float,
    val features: LocalUrlFeatures,
    val isModelLoaded: Boolean
)
