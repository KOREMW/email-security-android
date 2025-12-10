package com.example.phishing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.phishing.ml.UrlClassifier
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: UrlClassifier
    private lateinit var viewModel: UrlAnalysisViewModel

    private fun ensureNotificationPermission() {
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!sets.contains(packageName)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureNotificationPermission()

        classifier = UrlClassifier(this)
        viewModel = ViewModelProvider(this).get(UrlAnalysisViewModel::class.java)

        val inputUrl = findViewById<EditText>(R.id.inputUrl)
        val btnCheck = findViewById<Button>(R.id.btnCheck)
        val txtResult = findViewById<TextView>(R.id.txtResult)

        // ViewModel 상태 구독
        viewModel.localScore.observe(this) { score ->
            Log.d("MainActivity", "Local score: $score")
        }

        viewModel.serverResponse.observe(this) { response ->
            if (response != null) {
                val displayText = """
                    |로컬 점수: ${"%.2f".format(viewModel.localScore.value ?: 0f)}
                    |서버 최종 점수: ${"%.2f".format(response.finalRiskScore)}
                    |피싱 의심: ${if (response.suspicious) "YES ⚠️" else "NO ✓"}
                    |VirusTotal - 악성: ${response.virusTotal?.malicious ?: 0}
                    |리다이렉트 체인: ${response.redirects.size}개
                """.trimMargin()
                txtResult.text = displayText
            }
        }

        viewModel.statusMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Log.d("MainActivity", "Status: $message")
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            btnCheck.isEnabled = !isLoading
            btnCheck.text = if (isLoading) "분석 중..." else "검사"
        }

        btnCheck.setOnClickListener {
            val url = inputUrl.text.toString()
            if (url.isBlank()) {
                txtResult.text = "URL을 입력하세요."
                return@setOnClickListener
            }

            // 1) 로컬 TFLite 점수 계산
            val localProb = classifier.predict(url)  // 0.0 ~ 1.0
            viewModel.setLocalScore(localProb)

            // 2) 서버에 전송 (로컬 점수 함께)
            viewModel.analyzeUrlWithServer(url, localRiskScore = localProb)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}
