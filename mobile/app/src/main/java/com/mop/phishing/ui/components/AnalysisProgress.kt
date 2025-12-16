package com.mop.phishing.ui.components

<<<<<<< HEAD
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.theme.*

// 기존 enum/data class 유지 (호환성)
=======
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.theme.*

>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
enum class AnalysisStep { URL_EXTRACT, LOCAL_ML, CLOUD_FUNCTION, VIRUS_TOTAL, OPENAI }
data class AnalysisStepState(val step: AnalysisStep, val status: StepStatus)
enum class StepStatus { PENDING, LOADING, COMPLETED, SKIPPED, ERROR }

<<<<<<< HEAD
/**
 * 단순화된 분석 로딩 카드
 * 기존의 복잡한 단계별 UI 대신 통합된 "분석 중..." UI 표시
 */
@Composable
fun AnalysisProgressCard(steps: List<AnalysisStepState>, modifier: Modifier = Modifier) {
    // 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // 회전 애니메이션
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
=======
@Composable
fun AnalysisProgressCard(steps: List<AnalysisStepState>, modifier: Modifier = Modifier) {
    val isAnalyzing = steps.any { it.status == StepStatus.LOADING }
    val allCompleted = steps.all { it.status == StepStatus.COMPLETED || it.status == StepStatus.SKIPPED }
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
<<<<<<< HEAD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아이콘 + 로딩 링
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // 배경 원
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.2f),
                                    Primary.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // 로딩 인디케이터
                CircularProgressIndicator(
                    modifier = Modifier.size(90.dp),
                    color = Primary,
                    strokeWidth = 3.dp,
                    trackColor = Primary.copy(alpha = 0.1f)
                )
                
                // 중앙 아이콘
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Primary
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 메인 텍스트
            Text(
                text = "URL 분석 중...",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 서브 텍스트
            Text(
                text = "AI가 URL의 위험도를 분석하고 있습니다\n잠시만 기다려주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))
            
            // 분석 항목 표시 (간단한 텍스트)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AnalysisChip("ML 분석")
                AnalysisChip("VirusTotal")
                AnalysisChip("AI 보고서")
=======
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                } else if (allCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "완료",
                        modifier = Modifier.size(24.dp),
                        tint = SafeGreen
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = when {
                        allCompleted -> "분석 완료"
                        isAnalyzing -> "분석 중..."
                        else -> "분석 대기"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(20.dp))
            steps.forEach { stepState ->
                AnalysisStepItem(stepState)
                if (stepState != steps.last()) {
                    Spacer(Modifier.height(12.dp))
                }
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            }
        }
    }
}

@Composable
<<<<<<< HEAD
private fun AnalysisChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Primary
        )
    }
}

// 기존 함수 유지 (호환성)
fun getStepInfo(step: AnalysisStep): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> = when (step) {
    AnalysisStep.URL_EXTRACT -> Icons.Default.Security to "URL 추출"
    AnalysisStep.LOCAL_ML -> Icons.Default.Security to "온디바이스 ML 분석"
    AnalysisStep.CLOUD_FUNCTION -> Icons.Default.Security to "서버 분석"
    AnalysisStep.VIRUS_TOTAL -> Icons.Default.Security to "VirusTotal 검사"
    AnalysisStep.OPENAI -> Icons.Default.Security to "AI 보고서 생성"
=======
fun AnalysisStepItem(stepState: AnalysisStepState, modifier: Modifier = Modifier) {
    val (icon, label) = getStepInfo(stepState.step)
    
    val alpha = when (stepState.status) {
        StepStatus.PENDING -> 0.4f
        StepStatus.SKIPPED -> 0.6f
        else -> 1f
    }
    
    val iconTint = when (stepState.status) {
        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        StepStatus.LOADING -> Primary
        StepStatus.COMPLETED -> SafeGreen
        StepStatus.SKIPPED -> TextSecondary
        StepStatus.ERROR -> DangerRed
    }
    
    val displayIcon = when (stepState.status) {
        StepStatus.COMPLETED -> Icons.Default.CheckCircle
        StepStatus.SKIPPED -> Icons.Default.Close
        StepStatus.ERROR -> Icons.Default.Warning
        else -> icon
    }
    
    val statusText = when (stepState.status) {
        StepStatus.PENDING -> label
        StepStatus.LOADING -> "$label 중..."
        StepStatus.COMPLETED -> "$label 완료"
        StepStatus.SKIPPED -> "$label 건너뜀"
        StepStatus.ERROR -> "$label 실패"
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = displayIcon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (stepState.status == StepStatus.LOADING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Primary,
                strokeWidth = 2.dp
            )
        }
    }
}

fun getStepInfo(step: AnalysisStep): Pair<ImageVector, String> = when (step) {
    AnalysisStep.URL_EXTRACT -> Icons.Default.Search to "URL 추출"
    AnalysisStep.LOCAL_ML -> Icons.Default.Settings to "온디바이스 ML 분석"
    AnalysisStep.CLOUD_FUNCTION -> Icons.Default.Info to "서버 분석"
    AnalysisStep.VIRUS_TOTAL -> Icons.Default.Warning to "VirusTotal 검사"
    AnalysisStep.OPENAI -> Icons.Default.CheckCircle to "AI 보고서 생성"
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
}
