package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.*

/**
 * 위험도 상태 enum
 */
enum class RiskLevel {
    SAFE,       // 안전 (초록)
    CAUTION,    // 주의 (노랑)
    DANGER,     // 위험 (빨강)
    UNKNOWN     // 미분석 (회색)
}

/**
 * 위험도 배지 컴포넌트
 * 목업의 "안전", "주의", "위험" 배지 구현
 */
@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon, text) = when (riskLevel) {
        RiskLevel.SAFE -> listOf(
            SafeGreenLight,
            SafeGreen,
            Icons.Filled.CheckCircle,
            "안전"
        )
        RiskLevel.CAUTION -> listOf(
            WarningYellowLight,
            WarningYellow,
            Icons.Filled.Info,
            "주의"
        )
        RiskLevel.DANGER -> listOf(
            DangerRedLight,
            DangerRed,
            Icons.Filled.Warning,
            "위험"
        )
        RiskLevel.UNKNOWN -> listOf(
            Color(0xFFF5F5F5),
            TextSecondary,
            Icons.Filled.Info,
            "미분석"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor as Color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = text as String,
                tint = textColor as Color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * 위험도 점수로 RiskLevel 반환
 */
fun getRiskLevel(score: Int): RiskLevel {
    return when {
        score < 30 -> RiskLevel.SAFE
        score < 70 -> RiskLevel.CAUTION
        else -> RiskLevel.DANGER
    }
}

@Preview(showBackground = true)
@Composable
fun RiskBadgePreview() {
    EmailSecurityTheme {
        Row {
            RiskBadge(riskLevel = RiskLevel.SAFE)
            RiskBadge(riskLevel = RiskLevel.CAUTION, modifier = Modifier.padding(start = 8.dp))
            RiskBadge(riskLevel = RiskLevel.DANGER, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
