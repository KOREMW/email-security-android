package com.mop.phishing.ui.components

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

enum class AnalysisStep { URL_EXTRACT, LOCAL_ML, CLOUD_FUNCTION, VIRUS_TOTAL, OPENAI }
data class AnalysisStepState(val step: AnalysisStep, val status: StepStatus)
enum class StepStatus { PENDING, LOADING, COMPLETED, SKIPPED, ERROR }

@Composable
fun AnalysisProgressCard(steps: List<AnalysisStepState>, modifier: Modifier = Modifier) {
    val isAnalyzing = steps.any { it.status == StepStatus.LOADING }
    val allCompleted = steps.all { it.status == StepStatus.COMPLETED || it.status == StepStatus.SKIPPED }
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface), CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAnalyzing) CircularProgressIndicator(Modifier.size(24.dp), Primary, 2.dp)
                else if (allCompleted) Icon(Icons.Default.CheckCircle, "완료", SafeGreen, Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(when { allCompleted -> "분석 완료"; isAnalyzing -> "분석 중..."; else -> "분석 대기" }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(20.dp))
            steps.forEach { AnalysisStepItem(it); if (it != steps.last()) Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun AnalysisStepItem(stepState: AnalysisStepState, modifier: Modifier = Modifier) {
    val (icon, label) = getStepInfo(stepState.step)
    val alpha = when (stepState.status) { StepStatus.PENDING -> 0.4f; StepStatus.SKIPPED -> 0.6f; else -> 1f }
    val iconTint = when (stepState.status) { StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant; StepStatus.LOADING -> Primary; StepStatus.COMPLETED -> SafeGreen; StepStatus.SKIPPED -> TextSecondary; StepStatus.ERROR -> DangerRed }
    val displayIcon = when (stepState.status) { StepStatus.COMPLETED -> Icons.Default.CheckCircle; StepStatus.SKIPPED -> Icons.Default.Close; StepStatus.ERROR -> Icons.Default.Warning; else -> icon }
    Row(modifier.fillMaxWidth().alpha(alpha), verticalAlignment = Alignment.CenterVertically) {
        Icon(displayIcon, label, iconTint, Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(when (stepState.status) { StepStatus.PENDING -> label; StepStatus.LOADING -> "$label 중..."; StepStatus.COMPLETED -> "$label 완료"; StepStatus.SKIPPED -> "$label 건너뜀"; StepStatus.ERROR -> "$label 실패" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (stepState.status == StepStatus.LOADING) CircularProgressIndicator(Modifier.size(16.dp), Primary, 2.dp)
    }
}

fun getStepInfo(step: AnalysisStep): Pair<ImageVector, String> = when (step) {
    AnalysisStep.URL_EXTRACT -> Icons.Default.Search to "URL 추출"
    AnalysisStep.LOCAL_ML -> Icons.Default.Settings to "온디바이스 ML 분석"
    AnalysisStep.CLOUD_FUNCTION -> Icons.Default.Info to "서버 분석"
    AnalysisStep.VIRUS_TOTAL -> Icons.Default.Warning to "VirusTotal 검사"
    AnalysisStep.OPENAI -> Icons.Default.CheckCircle to "AI 보고서 생성"
}
