package com.mop.phishing.ui.components

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
enum class AnalysisStep { URL_EXTRACT, LOCAL_ML, CLOUD_FUNCTION, VIRUS_TOTAL, OPENAI }
data class AnalysisStepState(val step: AnalysisStep, val status: StepStatus)
enum class StepStatus { PENDING, LOADING, COMPLETED, SKIPPED, ERROR }

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
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
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
            }
        }
    }
}

@Composable
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
}
