package com.mop.phishing.ui.screens.detail

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
    val redirectCount: Int = 0,
    val aiExplanation: String? = null
)

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
        topBar = { AnalysisDetailTopBar(onBackClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            NotificationInfoCard(notification, Modifier.padding(16.dp))
            when (analysisState) {
                is AnalysisState.Loading -> AnalysisProgressCard(analysisSteps, Modifier.padding(horizontal = 16.dp))
                is AnalysisState.Completed -> AnalysisResultContent(analysisState.result, onShareClick, onBlockClick, Modifier.padding(horizontal = 16.dp))
                is AnalysisState.Error -> AnalysisErrorContent(analysisState.message, Modifier.padding(horizontal = 16.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") }
                Text("목록으로", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun NotificationInfoCard(notification: NotificationItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(notification.source)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(getSourceName(notification.source), style = MaterialTheme.typography.titleMedium, color = Primary)
                    Text(notification.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(notification.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            UrlChip(notification.url)
        }
    }
}

@Composable
fun AnalysisResultContent(result: AnalysisResult, onShareClick: () -> Unit, onBlockClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        RiskScoreCard(result.riskLevel, result.riskScore)
        Spacer(Modifier.height(16.dp))
        RecommendationCard(result.recommendation, result.canAccess)
        Spacer(Modifier.height(16.dp))
        result.details?.let { AnalysisDetailsCard(it); Spacer(Modifier.height(16.dp)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onShareClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("공유하기")
            }
            Button(onClick = onBlockClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (result.canAccess) DangerRed else Primary)) {
                Icon(if (result.canAccess) Icons.Default.Close else Icons.Default.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (result.canAccess) "차단하기" else "접속 가능")
            }
        }
    }
}

@Composable
fun RiskScoreCard(riskLevel: RiskLevel, riskScore: Int, modifier: Modifier = Modifier) {
    val (backgroundColor, statusText) = when (riskLevel) {
        RiskLevel.SAFE -> Brush.linearGradient(listOf(SafeGreen, SafeGreen.copy(alpha = 0.8f))) to "안전"
        RiskLevel.CAUTION -> Brush.linearGradient(listOf(WarningYellow, WarningYellow.copy(alpha = 0.8f))) to "주의"
        RiskLevel.DANGER -> Brush.linearGradient(listOf(DangerRed, DangerRed.copy(alpha = 0.8f))) to "위험"
        RiskLevel.UNKNOWN -> Brush.linearGradient(listOf(TextSecondary, TextSecondary.copy(alpha = 0.8f))) to "미분석"
    }
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.Transparent)) {
        Box(Modifier.fillMaxWidth().background(backgroundColor).padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Icon(when (riskLevel) { RiskLevel.SAFE -> Icons.Default.CheckCircle; RiskLevel.CAUTION -> Icons.Default.Warning; RiskLevel.DANGER -> Icons.Default.Close; RiskLevel.UNKNOWN -> Icons.Default.Info }, null, Modifier.size(32.dp), Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("위험도", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                    Text(statusText, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("위험 점수", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                    Text(riskScore.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(recommendation: String, canAccess: Boolean, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(if (canAccess) SafeGreenLight else DangerRedLight)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(24.dp), if (canAccess) SafeGreen else DangerRed)
                Spacer(Modifier.width(8.dp))
                Text("권장 사항", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            Text(recommendation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            Surface(RoundedCornerShape(8.dp), if (canAccess) SafeGreen else DangerRed) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (canAccess) Icons.Default.CheckCircle else Icons.Default.Close, null, Modifier.size(16.dp), Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(if (canAccess) "접속 가능" else "접속 차단 권장", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AnalysisDetailsCard(details: AnalysisDetails, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Text("상세 분석", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            DetailItem(Icons.Default.Settings, "온디바이스 ML", String.format("%.1f%%", details.localMlScore * 100))
            DetailItem(Icons.Default.Warning, "VirusTotal", "악성 ${details.virusTotalMalicious}개 / 의심 ${details.virusTotalSuspicious}개")
            DetailItem(Icons.Default.Search, "리다이렉트", "${details.redirectCount}회")
            details.aiExplanation?.let {
                Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, Modifier.size(20.dp), Primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI 분석", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), Primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AnalysisErrorContent(message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(DangerRedLight)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, Modifier.size(48.dp), DangerRed)
            Spacer(Modifier.height(12.dp))
            Text("분석 실패", style = MaterialTheme.typography.titleMedium, color = DangerRed)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}
