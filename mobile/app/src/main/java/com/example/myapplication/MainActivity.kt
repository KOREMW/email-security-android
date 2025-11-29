package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.myapplication.ml.UrlClassifier
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import androidx.core.app.NotificationManagerCompat



// onCreate 안에서 한 번만 체크

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: UrlClassifier

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

        val inputUrl = findViewById<EditText>(R.id.inputUrl)
        val btnCheck = findViewById<Button>(R.id.btnCheck)
        val txtResult = findViewById<TextView>(R.id.txtResult)

        btnCheck.setOnClickListener {
            val url = inputUrl.text.toString()
            if (url.isBlank()) {
                txtResult.text = "URL을 입력하세요."
                return@setOnClickListener
            }

            val prob = classifier.predict(url)  // 0.0 ~ 1.0
            val threshold = 0.5f                // 나중에 조정 가능

            val label = if (prob >= threshold) "피싱 의심" else "정상 가능성 높음"
            txtResult.text = "피싱 확률: ${"%.2f".format(prob)} ($label)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}
