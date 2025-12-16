package com.mop.phishing.ui.screens.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mop.phishing.ml.UrlClassifier
import com.mop.phishing.ml.PredictDebugResult
import com.mop.phishing.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ML 모델 테스트 결과
 */
data class MlTestResult(
    val url: String,
    val score: Float,
    val expectedLabel: String, // "phishing" or "safe"
    val predictedLabel: String,
    val isCorrect: Boolean,
    val debugInfo: String = ""  // 디버그 정보
)

/**
 * ML 모델 배치 테스트 화면
 * 여러 URL을 한 번에 테스트하여 ML 모델의 성능을 확인
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MlTestScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 테스트 결과
    var testResults by remember { mutableStateOf<List<MlTestResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("all") } // all, phishing, safe
    
    // 통계
    var totalCount by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var phishingAsPhishing by remember { mutableStateOf(0) } // True Positive
    var phishingAsSafe by remember { mutableStateOf(0) }     // False Negative
    var safeAsSafe by remember { mutableStateOf(0) }         // True Negative
    var safeAsPhishing by remember { mutableStateOf(0) }     // False Positive
    
    // 테스트 URL 데이터셋
    val testDataset = remember { getTestDataset() }
    
    // 테스트 실행
    fun runBatchTest() {
        scope.launch {
            isLoading = true
            testResults = emptyList()
            
            try {
                // 강제 리셋하여 최신 코드 사용 보장
                val classifier = UrlClassifier.forceResetInstance(context)
                val results = mutableListOf<MlTestResult>()
                
                withContext(Dispatchers.IO) {
                    testDataset.forEach { (url, expectedLabel) ->
                        val debugResult = classifier.predictWithDebug(url)
                        val score = debugResult.calibratedScore
                        val predictedLabel = if (score >= 0.4f) "phishing" else "safe"
                        val isCorrect = predictedLabel == expectedLabel

                        results.add(
                            MlTestResult(
                                url = url,
                                score = score,
                                expectedLabel = expectedLabel,
                                predictedLabel = predictedLabel,
                                isCorrect = isCorrect,
                                debugInfo = debugResult.toDebugString()
                            )
                        )
                    }
                }
                
                testResults = results
                
                // 통계 계산
                totalCount = results.size
                correctCount = results.count { it.isCorrect }
                phishingAsPhishing = results.count { it.expectedLabel == "phishing" && it.predictedLabel == "phishing" }
                phishingAsSafe = results.count { it.expectedLabel == "phishing" && it.predictedLabel == "safe" }
                safeAsSafe = results.count { it.expectedLabel == "safe" && it.predictedLabel == "safe" }
                safeAsPhishing = results.count { it.expectedLabel == "safe" && it.predictedLabel == "phishing" }
                
            } catch (e: Exception) {
                Toast.makeText(context, "테스트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }
    
    // 필터링된 결과
    val filteredResults = remember(testResults, selectedCategory) {
        when (selectedCategory) {
            "phishing" -> testResults.filter { it.expectedLabel == "phishing" }
            "safe" -> testResults.filter { it.expectedLabel == "safe" }
            "wrong" -> testResults.filter { !it.isCorrect }
            else -> testResults
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ML 모델 테스트") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { runBatchTest() }, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, "테스트 실행")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "ML 모델 검증 도구 (${UrlClassifier.VERSION})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "피싱/정상 URL ${testDataset.size}개로 모델 성능 테스트",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { runBatchTest() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("테스트 중... (${testResults.size}/${testDataset.size})")
                            } else {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(8.dp))
                                Text("배치 테스트 실행")
                            }
                        }
                    }
                }
            }
            
            // 통계 카드 (결과가 있을 때만)
            if (testResults.isNotEmpty()) {
                item {
                    StatisticsCard(
                        totalCount = totalCount,
                        correctCount = correctCount,
                        phishingAsPhishing = phishingAsPhishing,
                        phishingAsSafe = phishingAsSafe,
                        safeAsSafe = safeAsSafe,
                        safeAsPhishing = safeAsPhishing
                    )
                }
                
                // 점수 분포 카드
                item {
                    ScoreDistributionCard(testResults)
                }
                
                // 필터 탭
                item {
                    FilterTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        totalCount = testResults.size,
                        phishingCount = testResults.count { it.expectedLabel == "phishing" },
                        safeCount = testResults.count { it.expectedLabel == "safe" },
                        wrongCount = testResults.count { !it.isCorrect }
                    )
                }
                
                // 결과 리스트
                items(filteredResults) { result ->
                    TestResultItem(result)
                }
            }
            
            // 테스트 데이터셋 정보
            item {
                DatasetInfoCard(testDataset)
            }
        }
    }
}

@Composable
fun StatisticsCard(
    totalCount: Int,
    correctCount: Int,
    phishingAsPhishing: Int,
    phishingAsSafe: Int,
    safeAsSafe: Int,
    safeAsPhishing: Int
) {
    val accuracy = if (totalCount > 0) (correctCount.toFloat() / totalCount * 100) else 0f
    val precision = if (phishingAsPhishing + safeAsPhishing > 0) {
        phishingAsPhishing.toFloat() / (phishingAsPhishing + safeAsPhishing) * 100
    } else 0f
    val recall = if (phishingAsPhishing + phishingAsSafe > 0) {
        phishingAsPhishing.toFloat() / (phishingAsPhishing + phishingAsSafe) * 100
    } else 0f
    val f1Score = if (precision + recall > 0) {
        2 * precision * recall / (precision + recall)
    } else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (accuracy >= 80) SafeGreenLight 
                            else if (accuracy >= 60) WarningYellowLight 
                            else DangerRedLight
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📊 모델 성능 지표",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            // 정확도 큰 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.1f%%".format(accuracy),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            accuracy >= 80 -> SafeGreen
                            accuracy >= 60 -> WarningYellow
                            else -> DangerRed
                        }
                    )
                    Text(
                        "정확도 (Accuracy)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "$correctCount / $totalCount 정답",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            // 상세 지표
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("정밀도", "%.1f%%".format(precision), "Precision")
                MetricItem("재현율", "%.1f%%".format(recall), "Recall")
                MetricItem("F1 점수", "%.1f".format(f1Score), "F1 Score")
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            // Confusion Matrix
            Text(
                "혼동 행렬 (Confusion Matrix)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConfusionItem("TP", phishingAsPhishing, "피싱→피싱", SafeGreen)
                ConfusionItem("FN", phishingAsSafe, "피싱→정상", DangerRed)
                ConfusionItem("TN", safeAsSafe, "정상→정상", SafeGreen)
                ConfusionItem("FP", safeAsPhishing, "정상→피싱", WarningYellow)
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfusionItem(label: String, count: Int, description: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScoreDistributionCard(results: List<MlTestResult>) {
    val phishingScores = results.filter { it.expectedLabel == "phishing" }.map { it.score }
    val safeScores = results.filter { it.expectedLabel == "safe" }.map { it.score }
    
    val phishingAvg = if (phishingScores.isNotEmpty()) phishingScores.average() else 0.0
    val safeAvg = if (safeScores.isNotEmpty()) safeScores.average() else 0.0
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📈 점수 분포",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            // 피싱 URL 점수 분포
            Text("🚨 피싱 URL (기대: 높은 점수)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("평균: ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.1f%%".format(phishingAvg * 100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (phishingAvg >= 0.4) SafeGreen else DangerRed
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (phishingAvg >= 0.4) "✓ 양호" else "✗ 개선 필요",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (phishingAvg >= 0.4) SafeGreen else DangerRed
                )
            }
            ScoreBar(phishingScores, DangerRed)
            
            Spacer(Modifier.height(16.dp))
            
            // 정상 URL 점수 분포
            Text("✅ 정상 URL (기대: 낮은 점수)", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("평균: ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%.1f%%".format(safeAvg * 100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (safeAvg < 0.4) SafeGreen else DangerRed
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (safeAvg < 0.4) "✓ 양호" else "✗ 개선 필요",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (safeAvg < 0.4) SafeGreen else DangerRed
                )
            }
            ScoreBar(safeScores, SafeGreen)
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            
            Text(
                "※ 임계값: 40% (0.4 이상 = 피싱 판정)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScoreBar(scores: List<Float>, color: Color) {
    if (scores.isEmpty()) return
    
    val buckets = IntArray(10) // 0-10%, 10-20%, ..., 90-100%
    scores.forEach { score ->
        val bucket = (score * 10).toInt().coerceIn(0, 9)
        buckets[bucket]++
    }
    val maxCount = buckets.maxOrNull() ?: 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        buckets.forEachIndexed { index, count ->
            val height = if (maxCount > 0) (count.toFloat() / maxCount) else 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((height * 30).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(
                            if (index >= 4) color else color.copy(alpha = 0.3f)
                        )
                )
                Text(
                    "${index}0",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FilterTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    totalCount: Int,
    phishingCount: Int,
    safeCount: Int,
    wrongCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == "all",
            onClick = { onCategorySelected("all") },
            label = { Text("전체 ($totalCount)") }
        )
        FilterChip(
            selected = selectedCategory == "phishing",
            onClick = { onCategorySelected("phishing") },
            label = { Text("피싱 ($phishingCount)") }
        )
        FilterChip(
            selected = selectedCategory == "safe",
            onClick = { onCategorySelected("safe") },
            label = { Text("정상 ($safeCount)") }
        )
        FilterChip(
            selected = selectedCategory == "wrong",
            onClick = { onCategorySelected("wrong") },
            label = { Text("오답 ($wrongCount)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DangerRed.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun TestResultItem(result: MlTestResult) {
    var showDebug by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isCorrect)
                MaterialTheme.colorScheme.surface
            else
                DangerRedLight.copy(alpha = 0.3f)
        ),
        onClick = { showDebug = !showDebug }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 정답/오답 아이콘
                Icon(
                    imageVector = if (result.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (result.isCorrect) SafeGreen else DangerRed,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(12.dp))

                // URL 및 정보
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text(
                            "실제: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            result.expectedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.expectedLabel == "phishing") DangerRed else SafeGreen
                        )
                        Text(
                            " → 예측: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            result.predictedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.predictedLabel == "phishing") DangerRed else SafeGreen
                        )
                    }
                }

                // 점수
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.1f%%".format(result.score * 100),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            result.score >= 0.7 -> DangerRed
                            result.score >= 0.4 -> WarningYellow
                            else -> SafeGreen
                        }
                    )
                    Text(
                        "ML 점수",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 디버그 정보 (클릭 시 표시)
            if (showDebug && result.debugInfo.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    result.debugInfo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DatasetInfoCard(dataset: List<Pair<String, String>>) {
    val phishingCount = dataset.count { it.second == "phishing" }
    val safeCount = dataset.count { it.second == "safe" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "📋 테스트 데이터셋 정보",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("• 총 URL: ${dataset.size}개")
            Text("• 피싱 URL: ${phishingCount}개")
            Text("• 정상 URL: ${safeCount}개")
            Spacer(Modifier.height(8.dp))
            Text(
                "※ 이 데이터셋은 ML 모델의 일반화 성능을 테스트하기 위한 샘플입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 테스트용 URL 데이터셋
 * Pair<URL, 라벨> - 라벨: "phishing" 또는 "safe"
 */
fun getTestDataset(): List<Pair<String, String>> {
    return listOf(
        // ============================================
        // 정상 URL (Safe) - 대기업/유명 서비스
        // ============================================
        "https://www.google.com" to "safe",
        "https://www.google.co.kr/search?q=test" to "safe",
        "https://www.naver.com" to "safe",
        "https://mail.naver.com" to "safe",
        "https://www.daum.net" to "safe",
        "https://www.kakao.com" to "safe",
        "https://accounts.kakao.com/login" to "safe",
        "https://www.facebook.com" to "safe",
        "https://www.instagram.com" to "safe",
        "https://www.youtube.com" to "safe",
        "https://www.youtube.com/watch?v=abc123" to "safe",
        "https://twitter.com" to "safe",
        "https://www.amazon.com" to "safe",
        "https://www.amazon.co.kr" to "safe",
        "https://www.apple.com" to "safe",
        "https://www.microsoft.com" to "safe",
        "https://login.microsoftonline.com" to "safe",
        "https://github.com" to "safe",
        "https://github.com/user/repo" to "safe",
        "https://stackoverflow.com" to "safe",
        "https://www.wikipedia.org" to "safe",
        "https://en.wikipedia.org/wiki/Test" to "safe",
        "https://www.reddit.com" to "safe",
        "https://www.linkedin.com" to "safe",
        "https://www.paypal.com" to "safe",
        "https://www.netflix.com" to "safe",
        "https://www.tistory.com" to "safe",
        "https://blog.naver.com/username" to "safe",
        "https://www.coupang.com" to "safe",
        "https://www.11st.co.kr" to "safe",
        
        // ============================================
        // 피싱 URL 패턴 - 브랜드 사칭
        // ============================================
        "http://google-login.fake-site.com/verify" to "phishing",
        "http://naver-account.xyz/login" to "phishing",
        "http://kakao-verify.top/auth" to "phishing",
        "http://facebook-security.ml/confirm" to "phishing",
        "http://instagram-login.tk/account" to "phishing",
        "http://paypal-secure.ga/update" to "phishing",
        "http://amazon-order.cf/track" to "phishing",
        "http://apple-id.gq/verify" to "phishing",
        "http://microsoft-account.click/reset" to "phishing",
        "http://netflix-billing.stream/update" to "phishing",
        
        // ============================================
        // 피싱 URL 패턴 - 의심 키워드
        // ============================================
        "http://secure-verify-account.com/login" to "phishing",
        "http://account-suspended.net/restore" to "phishing",
        "http://urgent-security-alert.com/action" to "phishing",
        "http://verify-your-identity.org/confirm" to "phishing",
        "http://account-confirmation.xyz/validate" to "phishing",
        "http://security-update-required.top/now" to "phishing",
        "http://unusual-activity-detected.ml/check" to "phishing",
        "http://your-account-blocked.tk/unlock" to "phishing",
        "http://password-expired.ga/reset" to "phishing",
        "http://reactivate-account.cf/verify" to "phishing",
        
        // ============================================
        // 피싱 URL 패턴 - IP 주소
        // ============================================
        "http://192.168.1.100/login.php" to "phishing",
        "http://45.67.89.123/secure/bank" to "phishing",
        "http://103.45.67.89:8080/admin" to "phishing",
        "http://185.234.56.78/paypal/login" to "phishing",
        
        // ============================================
        // 피싱 URL 패턴 - 의심 TLD
        // ============================================
        "http://bank-login.xyz/secure" to "phishing",
        "http://free-gift.top/claim" to "phishing",
        "http://winner-prize.click/get" to "phishing",
        "http://lottery-result.win/check" to "phishing",
        "http://best-deal.bid/offer" to "phishing",
        "http://easy-loan.loan/apply" to "phishing",
        "http://quick-money.work/earn" to "phishing",
        "http://hot-date.date/meet" to "phishing",
        "http://product-review.review/best" to "phishing",
        "http://download-free.download/get" to "phishing",
        
        // ============================================
        // 피싱 URL 패턴 - 긴 URL / 많은 파라미터
        // ============================================
        "http://very-long-suspicious-domain-name-to-confuse-users.tk/login?user=test&session=abc123&redirect=home&token=xyz789" to "phishing",
        "http://a1b2c3d4e5.ml/verify?id=123&code=456&ref=789&auth=abc&track=def" to "phishing",
        
        // ============================================
        // 피싱 URL 패턴 - 특수문자/숫자 많음
        // ============================================
        "http://g00gle-l0gin.com/verify" to "phishing",
        "http://n4v3r-s3cur1ty.net/auth" to "phishing",
        "http://fac3b00k-acc0unt.org/login" to "phishing",
        "http://paypа1-secure.com/update" to "phishing", // 키릴 문자 а
        
        // ============================================
        // 경계 케이스 - 정상이지만 의심스러워 보일 수 있는 URL
        // ============================================
        "https://accounts.google.com/signin/v2/identifier" to "safe",
        "https://login.live.com/login.srf" to "safe",
        "https://appleid.apple.com/auth/authorize" to "safe",
        "https://www.paypal.com/signin?returnUri=..." to "safe",
        "https://nid.naver.com/nidlogin.login" to "safe",
        
        // ============================================
        // 경계 케이스 - HTTP 사용하는 정상 사이트 (드묾)
        // ============================================
        "http://example.com" to "safe",
        "http://httpforever.com" to "safe",
        
        // ============================================
        // 추가 피싱 패턴
        // ============================================
        "http://kb-banking.xyz/login" to "phishing",
        "http://shinhan-secure.top/verify" to "phishing",
        "http://woori-account.ml/update" to "phishing",
        "http://hana-bank.tk/confirm" to "phishing",
        "http://nh-banking.ga/auth" to "phishing",
        "http://coupang-delivery.cf/track" to "phishing",
        "http://baemin-order.gq/status" to "phishing",
        "http://carrot-market.xyz/chat" to "phishing"
    )
}
