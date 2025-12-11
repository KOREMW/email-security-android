package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * 분석 단계 enum
 */
enum class AnalysisStep {
    URL_EXTRACT,        // URL 추출
    LOCAL_ML,           // 온디바이스 ML 분류
    CLOUD_FUNCTION,     // Cloud Functions 호출
    VIRUS_TOTAL,        // VirusTotal 분석
    OPENAI              // OpenAI 분석
}

/**
 * 분석 진행 상태
 */
data class AnalysisStepState(
    val step: AnalysisStep,
    val status: StepStatus
)

enum class StepStatus {
    PENDING,    // 대기 중
    LOADING,    // 진행 중
    COMPLETED,  // 완료
    ERROR       // 오류
}

/**
 * 분석 진행 상태 카드
 */
@Composable
fun AnalysisProgressCard(
    steps: List<AnalysisStepState>,
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
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "분석 중...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 분석 단계 목록
            steps.forEach { stepState ->
                AnalysisStepItem(stepState = stepState)
                if (stepState != steps.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * 개별 분석 단계 아이템
 */
@Composable
fun AnalysisStepItem(
    stepState: AnalysisStepState,
    modifier: Modifier = Modifier
) {
    val (icon, label) = getStepInfo(stepState.step)
    
    val alpha = when (stepState.status) {
        StepStatus.PENDING -> 0.4f
        StepStatus.LOADING -> 1f
        StepStatus.COMPLETED -> 1f
        StepStatus.ERROR -> 1f
    }
    
    val iconTint = when (stepState.status) {
        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        StepStatus.LOADING -> Primary
        StepStatus.COMPLETED -> SafeGreen
        StepStatus.ERROR -> DangerRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 아이콘
        Icon(
            imageVector = if (stepState.status == StepStatus.COMPLETED) 
                Icons.Default.CheckCircle else icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 라벨
        Text(
            text = when (stepState.status) {
                StepStatus.PENDING -> label
                StepStatus.LOADING -> "$label 중..."
                StepStatus.COMPLETED -> "$label 완료"
                StepStatus.ERROR -> "$label 실패"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // 로딩 인디케이터
        if (stepState.status == StepStatus.LOADING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Primary
            )
        }
    }
}

/**
 * 단계별 아이콘 및 라벨 정보
 */
fun getStepInfo(step: AnalysisStep): Pair<ImageVector, String> {
    return when (step) {
        AnalysisStep.URL_EXTRACT -> Pair(Icons.Default.Search, "URL 추출")
        AnalysisStep.LOCAL_ML -> Pair(Icons.Default.Settings, "온디바이스 ML 분류")
        AnalysisStep.CLOUD_FUNCTION -> Pair(Icons.Default.Info, "Cloud Functions 호출")
        AnalysisStep.VIRUS_TOTAL -> Pair(Icons.Default.Warning, "VirusTotal 분석")
        AnalysisStep.OPENAI -> Pair(Icons.Default.CheckCircle, "OpenAI 분석")
    }
}

@Preview(showBackground = true)
@Composable
fun AnalysisProgressCardPreview() {
    EmailSecurityTheme {
        AnalysisProgressCard(
            steps = listOf(
                AnalysisStepState(AnalysisStep.URL_EXTRACT, StepStatus.COMPLETED),
                AnalysisStepState(AnalysisStep.LOCAL_ML, StepStatus.COMPLETED),
                AnalysisStepState(AnalysisStep.CLOUD_FUNCTION, StepStatus.LOADING),
                AnalysisStepState(AnalysisStep.VIRUS_TOTAL, StepStatus.PENDING),
                AnalysisStepState(AnalysisStep.OPENAI, StepStatus.PENDING)
            )
        )
    }
}
