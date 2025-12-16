package com.mop.phishing.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.theme.*

sealed class AnalysisState {
    object Loading : AnalysisState()
    data class Completed(val result: AnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

data class AnalysisResult(
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val recommendation: String,
    val canAccess: Boolean,
    val details: AnalysisDetails? = null
)

data class AnalysisDetails(
    val localMlScore: Float = 0f,
    val virusTotalMalicious: Int = 0,
    val virusTotalSuspicious: Int = 0,
    val virusTotalHarmless: Int = 0,   // 🆕 정상 탐지 개수
    val virusTotalTotal: Int = 0,
    val redirectCount: Int = 0,
    val finalScore: Int = 0,
    val aiExplanation: String? = null,
    val expandedUrl: String? = null    // 🆕 단축 URL 해제 결과
)

/**
 * 현재 테마가 다크모드인지 확인하는 헬퍼 함수
 * MaterialTheme의 배경색 밝기로 판단 (시스템 설정이 아닌 실제 적용된 테마 기준)
 */
@Composable
private fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailScreen(
    notification: NotificationItem,
    analysisState: AnalysisState,
    analysisSteps: List<AnalysisStepState>,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReanalyzeClick: () -> Unit = {},
    isBlocked: Boolean = false,
    isReanalyzing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { AnalysisDetailTopBar(onBackClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            NotificationInfoCard(notification, Modifier.padding(16.dp))
            when (analysisState) {
                is AnalysisState.Loading -> {
                    AnalysisProgressCard(analysisSteps, Modifier.padding(horizontal = 16.dp))
                }
                is AnalysisState.Completed -> {
                    AnalysisResultContent(
                        result = analysisState.result,
                        onShareClick = onShareClick,
                        onBlockClick = onBlockClick,
                        onReanalyzeClick = onReanalyzeClick,
                        isBlocked = isBlocked,
                        isReanalyzing = isReanalyzing,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is AnalysisState.Error -> {
                    AnalysisErrorContent(analysisState.message, Modifier.padding(horizontal = 16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
                Text(
                    text = "목록으로",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun NotificationInfoCard(notification: NotificationItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(notification.source)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = getSourceName(notification.source),
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            UrlChip(notification.url)
        }
    }
}

@Composable
fun AnalysisResultContent(
    result: AnalysisResult,
    onShareClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReanalyzeClick: () -> Unit = {},
    isBlocked: Boolean = false,
    isReanalyzing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        RiskScoreCard(result.riskLevel, result.riskScore)
        Spacer(Modifier.height(16.dp))
        RecommendationCard(result.recommendation, result.riskLevel)
        Spacer(Modifier.height(16.dp))
        result.details?.let {
            AnalysisDetailsCard(
                details = it,
                onReanalyzeClick = onReanalyzeClick,
                isReanalyzing = isReanalyzing
            )
            Spacer(Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("공유하기")
            }
            
            // 위험도별 버튼 스타일 결정
            val (buttonText, buttonColor, buttonIcon) = when {
                // 이미 차단된 상태
                isBlocked -> Triple(
                    "차단 해제",
                    DangerRed,
                    Icons.Default.LockOpen
                )
                // 차단되지 않은 상태 - 위험도에 따라 다르게 표시
                result.riskLevel == RiskLevel.DANGER -> Triple(
                    "차단하기",
                    DangerRed,
                    Icons.Default.Block
                )
                result.riskLevel == RiskLevel.CAUTION -> Triple(
                    "차단 권장",
                    WarningYellow,
                    Icons.Default.Warning
                )
                else -> Triple(
                    "차단하기",
                    Color(0xFF9E9E9E),  // 회색
                    Icons.Default.Block
                )
            }
            
            Button(
                onClick = onBlockClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(buttonText)
            }
        }
    }
}

@Composable
fun RiskScoreCard(riskLevel: RiskLevel, riskScore: Int, modifier: Modifier = Modifier) {
    val (backgroundColor, statusText, statusIcon) = when (riskLevel) {
        RiskLevel.SAFE -> Triple(
            Brush.linearGradient(listOf(SafeGreen, SafeGreen.copy(alpha = 0.8f))),
            "안전",
            Icons.Default.CheckCircle
        )
        RiskLevel.CAUTION -> Triple(
            Brush.linearGradient(listOf(WarningYellow, WarningYellow.copy(alpha = 0.8f))),
            "주의",
            Icons.Default.Warning
        )
        RiskLevel.DANGER -> Triple(
            Brush.linearGradient(listOf(DangerRed, DangerRed.copy(alpha = 0.8f))),
            "위험",
            Icons.Default.Close
        )
        RiskLevel.UNKNOWN -> Triple(
            Brush.linearGradient(listOf(TextSecondary, TextSecondary.copy(alpha = 0.8f))),
            "미분석",
            Icons.Default.Info
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "위험도",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.8f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "위험 점수",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.8f)
                    )
                    Text(
                        text = riskScore.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(recommendation: String, riskLevel: RiskLevel, modifier: Modifier = Modifier) {
    // MaterialTheme 기반 다크모드 감지 (시스템 설정이 아닌 실제 테마)
    val isDark = isDarkTheme()

    // 위험도에 따른 배경색 (다크모드 대응)
    val backgroundColor = when (riskLevel) {
        RiskLevel.SAFE -> if (isDark) SafeGreenDark else SafeGreenLight
        RiskLevel.CAUTION -> if (isDark) Color(0xFF2D1F00) else WarningYellowLight  // 어두운 주황/노랑
        RiskLevel.DANGER -> if (isDark) Color(0xFF2D0A0A) else DangerRedLight  // 어두운 빨강
        RiskLevel.UNKNOWN -> if (isDark) CardDark else Color(0xFFF5F5F5)
    }

    // 다크모드에서 텍스트 색상 조정
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    // 위험도에 따른 아이콘/뱃지 색상
    val accentColor = when (riskLevel) {
        RiskLevel.SAFE -> if (isDark) Color(0xFF81C784) else SafeGreen  // 밝은 초록
        RiskLevel.CAUTION -> if (isDark) Color(0xFFFFB74D) else WarningYellow  // 밝은 주황
        RiskLevel.DANGER -> if (isDark) Color(0xFFFF8A80) else DangerRed  // 밝은 빨강
        RiskLevel.UNKNOWN -> if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(backgroundColor)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = accentColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "권장 사항",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (riskLevel) {
                            RiskLevel.SAFE -> Icons.Default.CheckCircle
                            RiskLevel.CAUTION -> Icons.Default.Warning
                            RiskLevel.DANGER -> Icons.Default.Close
                            RiskLevel.UNKNOWN -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (riskLevel) {
                            RiskLevel.SAFE -> "접속 가능"
                            RiskLevel.CAUTION -> "주의 필요"
                            RiskLevel.DANGER -> "접속 차단 권장"
                            RiskLevel.UNKNOWN -> "분석 필요"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisDetailsCard(
    details: AnalysisDetails,
    onReanalyzeClick: () -> Unit = {},
    isReanalyzing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            // 헤더: 제목 + 전체 재분석 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "상세 분석 결과",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 전체 재분석 버튼
                Button(
                    onClick = onReanalyzeClick,
                    enabled = !isReanalyzing,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isReanalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "분석 중...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "전체 재분석",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "전체 재분석",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // 🆕 단축 URL 해제 결과 표시
            details.expandedUrl?.let { expandedUrl ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "단축 URL 해제됨",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "→ $expandedUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
            }

            ScoreDetailItem(
                icon = Icons.Default.Settings,
                label = "온디바이스 ML 분석",
                score = details.localMlScore,
                description = "TensorFlow Lite 모델 기반 위험도 분석"
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            VirusTotalDetailItem(
                malicious = details.virusTotalMalicious,
                suspicious = details.virusTotalSuspicious,
                total = details.virusTotalTotal
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            EnsembleScoreItem(
                mlScore = details.localMlScore,
                vtMalicious = details.virusTotalMalicious,
                finalScore = details.finalScore
            )

            details.aiExplanation?.let { explanation ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI 분석 요약",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreDetailItem(
    icon: ImageVector,
    label: String,
    score: Float,
    description: String
) {
    val scorePercent = (score * 100).toInt()
    val scoreColor = when {
        scorePercent >= 70 -> DangerRed
        scorePercent >= 40 -> WarningYellow
        else -> SafeGreen
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${scorePercent}%",
                style = MaterialTheme.typography.titleMedium,
                color = scoreColor
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { score },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = scoreColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VirusTotalDetailItem(
    malicious: Int,
    suspicious: Int,
    total: Int
) {
    val hasResults = total > 0

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "VirusTotal 검사",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = malicious.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (malicious > 0) DangerRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "악성",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = suspicious.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (suspicious > 0) WarningYellow else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "의심",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val clean = if (total > 0) total - malicious - suspicious else 0
                Text(
                    text = clean.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = SafeGreen
                )
                Text(
                    text = "정상",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasResults) "총 ${total}개 엔진에서 검사됨" 
                   else "VirusTotal API 결과 없음 - 상단의 '전체 재분석' 버튼을 눌러보세요",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasResults) MaterialTheme.colorScheme.onSurfaceVariant else WarningYellow
        )
    }
}

@Composable
fun EnsembleScoreItem(
    mlScore: Float,
    vtMalicious: Int,
    finalScore: Int
) {
    val finalScoreColor = when {
        finalScore >= 70 -> DangerRed
        finalScore >= 40 -> WarningYellow
        else -> SafeGreen
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "최종 위험도 점수",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = "ML과 VirusTotal 결과를 종합한 최종 점수입니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "최종 위험도",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${finalScore}점",
                        style = MaterialTheme.typography.headlineSmall,
                        color = finalScoreColor
                    )
                    Text(
                        text = " / 100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { finalScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = finalScoreColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        finalScore >= 70 -> "⚠️ 고위험: 접속하지 마세요"
                        finalScore >= 40 -> "⚡ 주의 필요: 신중하게 접근하세요"
                        else -> "✅ 안전: 정상적인 링크입니다"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = finalScoreColor
                )
            }
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Primary
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AnalysisErrorContent(message: String, modifier: Modifier = Modifier) {
    val isDark = isDarkTheme()
    val backgroundColor = if (isDark) DangerRedDark else DangerRedLight
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val accentColor = if (isDark) Color(0xFFFF8A80) else DangerRed

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = accentColor
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "분석 실패",
                style = MaterialTheme.typography.titleMedium,
                color = accentColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
