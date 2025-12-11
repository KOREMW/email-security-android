package com.example.myapplication.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*

/**
 * 분석 상태
 */
sealed class AnalysisState {
    object Loading : AnalysisState()
    data class Completed(val result: AnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

/**
 * 분석 결과 데이터
 */
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
    val redirectCount: Int = 0,
    val aiExplanation: String? = null
)

/**
 * 분석 상세 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailScreen(
    notification: NotificationItem,
    analysisState: AnalysisState,
    analysisSteps: List<AnalysisStepState>,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onBlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AnalysisDetailTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 알림 정보 카드
            NotificationInfoCard(
                notification = notification,
                modifier = Modifier.padding(16.dp)
            )

            // 분석 상태에 따른 컨텐츠
            when (analysisState) {
                is AnalysisState.Loading -> {
                    AnalysisProgressCard(
                        steps = analysisSteps,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is AnalysisState.Completed -> {
                    AnalysisResultContent(
                        result = analysisState.result,
                        onShareClick = onShareClick,
                        onBlockClick = onBlockClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                is AnalysisState.Error -> {
                    AnalysisErrorContent(
                        message = analysisState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 상단 앱바
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailTopBar(
    onBackClick: () -> Unit
) {
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

/**
 * 알림 정보 카드
 */
@Composable
fun NotificationInfoCard(
    notification: NotificationItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceIcon(source = notification.source)
                Spacer(modifier = Modifier.width(12.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            UrlChip(url = notification.url)
        }
    }
}

/**
 * 분석 결과 컨텐츠
 */
@Composable
fun AnalysisResultContent(
    result: AnalysisResult,
    onShareClick: () -> Unit,
    onBlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 위험도 카드
        RiskScoreCard(
            riskLevel = result.riskLevel,
            riskScore = result.riskScore
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 권장 사항 카드
        RecommendationCard(
            recommendation = result.recommendation,
            canAccess = result.canAccess
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 상세 분석 정보 (있을 경우)
        result.details?.let { details ->
            AnalysisDetailsCard(details = details)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 액션 버튼들
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
                Spacer(modifier = Modifier.width(8.dp))
                Text("공유하기")
            }

            Button(
                onClick = onBlockClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (result.canAccess) DangerRed else Primary
                )
            ) {
                Icon(
                    imageVector = if (result.canAccess) Icons.Default.Close else Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (result.canAccess) "차단하기" else "접속 가능")
            }
        }
    }
}

/**
 * 위험도 점수 카드
 */
@Composable
fun RiskScoreCard(
    riskLevel: RiskLevel,
    riskScore: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, statusText) = when (riskLevel) {
        RiskLevel.SAFE -> Pair(
            Brush.linearGradient(listOf(SafeGreen, SafeGreen.copy(alpha = 0.8f))),
            "안전"
        )
        RiskLevel.CAUTION -> Pair(
            Brush.linearGradient(listOf(WarningYellow, WarningYellow.copy(alpha = 0.8f))),
            "주의"
        )
        RiskLevel.DANGER -> Pair(
            Brush.linearGradient(listOf(DangerRed, DangerRed.copy(alpha = 0.8f))),
            "위험"
        )
        RiskLevel.UNKNOWN -> Pair(
            Brush.linearGradient(listOf(TextSecondary, TextSecondary.copy(alpha = 0.8f))),
            "미분석"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                // 아이콘 + 상태
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (riskLevel) {
                                RiskLevel.SAFE -> Icons.Default.CheckCircle
                                RiskLevel.CAUTION -> Icons.Default.Warning
                                RiskLevel.DANGER -> Icons.Default.Close
                                RiskLevel.UNKNOWN -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "위험도",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 점수
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "위험 점수",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
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

/**
 * 권장 사항 카드
 */
@Composable
fun RecommendationCard(
    recommendation: String,
    canAccess: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (canAccess) SafeGreenLight else DangerRedLight

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (canAccess) SafeGreen else DangerRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "권장 사항",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 접속 가능 여부 칩
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (canAccess) SafeGreen else DangerRed
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (canAccess) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canAccess) "접속 가능" else "접속 차단 권장",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 상세 분석 정보 카드
 */
@Composable
fun AnalysisDetailsCard(
    details: AnalysisDetails,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "상세 분석",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailItem(
                icon = Icons.Default.Settings,
                label = "온디바이스 ML",
                value = String.format("%.1f%%", details.localMlScore * 100)
            )

            DetailItem(
                icon = Icons.Default.Warning,
                label = "VirusTotal",
                value = "악성 ${details.virusTotalMalicious}개 / 의심 ${details.virusTotalSuspicious}개"
            )

            DetailItem(
                icon = Icons.Default.Search,
                label = "리다이렉트",
                value = "${details.redirectCount}회"
            )

            details.aiExplanation?.let { explanation ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI 분석",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
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

/**
 * 에러 컨텐츠
 */
@Composable
fun AnalysisErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DangerRedLight)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "분석 실패",
                style = MaterialTheme.typography.titleMedium,
                color = DangerRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalysisDetailScreenLoadingPreview() {
    EmailSecurityTheme {
        AnalysisDetailScreen(
            notification = NotificationItem(
                id = "1",
                source = NotificationSource.GMAIL,
                title = "Your Amazon Order",
                content = "Your package has been delivered. Track your order: https://amazon.com/track/order/12345",
                url = "https://amazon.com/track/order/12345",
                timeAgo = "1시간 전",
                riskLevel = RiskLevel.UNKNOWN,
                isAnalyzed = false
            ),
            analysisState = AnalysisState.Loading,
            analysisSteps = listOf(
                AnalysisStepState(AnalysisStep.URL_EXTRACT, StepStatus.COMPLETED),
                AnalysisStepState(AnalysisStep.LOCAL_ML, StepStatus.LOADING),
                AnalysisStepState(AnalysisStep.CLOUD_FUNCTION, StepStatus.PENDING),
                AnalysisStepState(AnalysisStep.VIRUS_TOTAL, StepStatus.PENDING),
                AnalysisStepState(AnalysisStep.OPENAI, StepStatus.PENDING)
            ),
            onBackClick = {},
            onShareClick = {},
            onBlockClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalysisDetailScreenCompletedPreview() {
    EmailSecurityTheme {
        AnalysisDetailScreen(
            notification = NotificationItem(
                id = "1",
                source = NotificationSource.GMAIL,
                title = "Your Amazon Order",
                content = "Your package has been delivered. Track your order: https://amazon.com/track/order/12345",
                url = "https://amazon.com/track/order/12345",
                timeAgo = "1시간 전",
                riskLevel = RiskLevel.SAFE,
                riskScore = 15,
                isAnalyzed = true
            ),
            analysisState = AnalysisState.Completed(
                AnalysisResult(
                    riskLevel = RiskLevel.SAFE,
                    riskScore = 15,
                    recommendation = "안전한 링크입니다. 정상적으로 접속하셔도 됩니다.",
                    canAccess = true,
                    details = AnalysisDetails(
                        localMlScore = 0.12f,
                        virusTotalMalicious = 0,
                        virusTotalSuspicious = 0,
                        redirectCount = 1,
                        aiExplanation = "이 URL은 Amazon의 공식 도메인에서 제공되며, HTTPS 프로토콜을 사용하고 있어 안전합니다."
                    )
                )
            ),
            analysisSteps = emptyList(),
            onBackClick = {},
            onShareClick = {},
            onBlockClick = {}
        )
    }
}
