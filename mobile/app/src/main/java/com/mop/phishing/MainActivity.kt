package com.mop.phishing

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.mop.phishing.data.remote.ResponseMapper
import com.mop.phishing.ml.UrlClassifier
import com.mop.phishing.ui.screens.detail.AnalysisState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * XML 레이아웃 기반 메인 액티비티
 * 간단한 URL 테스트용
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var classifier: UrlClassifier? = null
    private lateinit var viewModel: UrlAnalysisViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 알림 권한 확인
        ensureNotificationPermission()

        // 초기화 (싱글톤 사용)
        classifier = UrlClassifier.getInstance(this)
        viewModel = ViewModelProvider(this)[UrlAnalysisViewModel::class.java]

        // UI 요소
        val inputUrl = findViewById<EditText>(R.id.inputUrl)
        val btnCheck = findViewById<Button>(R.id.btnCheck)
        val txtResult = findViewById<TextView>(R.id.txtResult)

        // 분석 상태 관찰
        lifecycleScope.launch {
            viewModel.analysisState.collectLatest { state ->
                when (state) {
                    is AnalysisState.Loading -> {
                        btnCheck.isEnabled = false
                        btnCheck.text = "분석 중..."
                        txtResult.text = "서버에 분석 요청 중..."
                    }
                    is AnalysisState.Completed -> {
                        btnCheck.isEnabled = true
                        btnCheck.text = "검사"
                        
                        val result = state.result
                        val serverResponse = viewModel.serverResponse.value
                        val aiResponse = viewModel.aiResponse.value
                        
                        val displayText = buildString {
                            appendLine("═══════════════════════")
                            appendLine("📊 분석 결과")
                            appendLine("═══════════════════════")
                            appendLine()
                            appendLine("🎯 위험도: ${result.riskLevel.name}")
                            appendLine("📈 위험 점수: ${result.riskScore}/100")
                            appendLine("🚦 접속 가능: ${if (result.canAccess) "예 ✓" else "아니오 ✗"}")
                            appendLine()
                            appendLine("───────────────────────")
                            appendLine("📋 상세 정보")
                            appendLine("───────────────────────")
                            
                            serverResponse?.let { resp ->
                                appendLine("• 서버 점수: ${String.format("%.2f", resp.finalRiskScore)}")
                                appendLine("• 서버 레벨: ${resp.suspiciousLevel}")
                                appendLine("• 캐시 여부: ${resp.cached ?: false}")
                                
                                resp.virusTotal?.let { vt ->
                                    appendLine()
                                    appendLine("🔍 VirusTotal:")
                                    appendLine("  - 악성: ${vt.malicious}")
                                    appendLine("  - 의심: ${vt.suspicious}")
                                    appendLine("  - 정상: ${vt.harmless}")
                                    appendLine("  - 상태: ${vt.status ?: "N/A"}")
                                }
                                
                                resp.breakdown?.let { bd ->
                                    appendLine()
                                    appendLine("📊 점수 내역:")
                                    appendLine("  - 로컬 ML: ${String.format("%.2f", bd.localMlScore ?: 0.0)}")
                                    appendLine("  - VT 점수: ${String.format("%.2f", bd.virusTotalScore ?: 0.0)}")
                                }
                            }
                            
                            aiResponse?.let { ai ->
                                appendLine()
                                appendLine("───────────────────────")
                                appendLine("🤖 AI 분석")
                                appendLine("───────────────────────")
                                ai.riskSummary?.let { appendLine("요약: $it") }
                                ai.riskFactors?.takeIf { it.isNotEmpty() }?.let { factors ->
                                    appendLine("위험 요소:")
                                    factors.forEach { appendLine("  • $it") }
                                }
                                ai.recommendations?.takeIf { it.isNotEmpty() }?.let { recs ->
                                    appendLine("권장사항:")
                                    recs.forEach { appendLine("  • $it") }
                                }
                            }
                            
                            appendLine()
                            appendLine("───────────────────────")
                            appendLine("💡 권장 조치")
                            appendLine("───────────────────────")
                            appendLine(result.recommendation)
                        }
                        
                        txtResult.text = displayText
                    }
                    is AnalysisState.Error -> {
                        btnCheck.isEnabled = true
                        btnCheck.text = "검사"
                        txtResult.text = "❌ 분석 실패\n\n${state.message}"
                    }
                }
            }
        }

        // 로컬 점수 관찰
        lifecycleScope.launch {
            viewModel.localScore.collectLatest { score ->
                Log.d(TAG, "로컬 ML 점수: $score")
            }
        }

        // 검사 버튼 클릭
        btnCheck.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            
            if (url.isBlank()) {
                txtResult.text = "URL을 입력하세요."
                return@setOnClickListener
            }

            // URL 형식 검증
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                txtResult.text = "올바른 URL 형식이 아닙니다.\nhttp:// 또는 https://로 시작해야 합니다."
                return@setOnClickListener
            }

            // 분석 실행
            viewModel.analyzeUrl(url, source = "manual", includeAIAnalysis = true)
        }
    }

    /**
     * 알림 리스너 권한 확인 및 요청
     */
    private fun ensureNotificationPermission() {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!enabledListeners.contains(packageName)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 싱글톤이므로 close하지 않음
        classifier = null
    }
}
