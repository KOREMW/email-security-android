package com.mop.phishing.ui.screens.main

<<<<<<< HEAD
import android.widget.Toast
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
<<<<<<< HEAD
import androidx.compose.ui.platform.LocalContext
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.screens.detail.AnalysisDetails
import com.mop.phishing.ui.screens.detail.AnalysisResult
import com.mop.phishing.ui.screens.detail.AnalysisState
import com.mop.phishing.ui.theme.*
<<<<<<< HEAD
import com.mop.phishing.util.UrlBlockManager
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

/**
 * 현재 테마가 다크모드인지 확인하는 헬퍼 함수
 * MaterialTheme의 배경색 밝기로 판단 (시스템 설정이 아닌 실제 적용된 테마 기준)
 */
@Composable
fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputScreen(
    analysisState: AnalysisState?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAnalyzeClick: (String) -> Unit,
<<<<<<< HEAD
    onReanalyzeClick: (String) -> Unit = {},  // 🆕 재분석 콜백 (캐시 무시)
=======
    onQrScanClick: () -> Unit,
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
<<<<<<< HEAD
    val context = LocalContext.current
    
    // 차단 상태 추적
    var isBlocked by remember { mutableStateOf(false) }
    
    // URL 입력 변경 시 차단 상태 확인
    LaunchedEffect(urlInput) {
        isBlocked = if (urlInput.isNotBlank()) {
            UrlBlockManager.isBlocked(context, urlInput)
        } else false
    }
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL 검사") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryLight.copy(alpha = 0.3f))
            ) {
                Column(
<<<<<<< HEAD
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
=======
                    modifier = Modifier.padding(20.dp),
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Primary, PrimaryGradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "URL 안전성 검사",
                        style = MaterialTheme.typography.titleLarge,
<<<<<<< HEAD
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
=======
                        color = MaterialTheme.colorScheme.onSurface
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "의심스러운 링크를 입력하면\nAI가 위험도를 분석해드립니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
<<<<<<< HEAD
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
=======
                        textAlign = TextAlign.Center
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    )
                }
            }

            // URL 입력 필드
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; urlError = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL 입력") },
                placeholder = { Text("https://example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            clipboardManager.getText()?.let {
                                urlInput = it.toString()
                                urlError = null
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "붙여넣기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "지우기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it, color = DangerRed) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
<<<<<<< HEAD
                    if (validateUrl(urlInput)) onAnalyzeClick(normalizeUrl(urlInput))
                    else urlError = "올바른 URL 형식이 아닙니다 (예: example.com)"
=======
                    if (validateUrl(urlInput)) onAnalyzeClick(urlInput)
                    else urlError = "올바른 URL 형식이 아닙니다"
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                }),
                shape = RoundedCornerShape(12.dp)
            )

            // 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
<<<<<<< HEAD
                // 차단/차단해제 버튼
                OutlinedButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            if (isBlocked) {
                                UrlBlockManager.unblockUrl(context, urlInput)
                                Toast.makeText(context, "차단이 해제되었습니다", Toast.LENGTH_SHORT).show()
                                isBlocked = false
                            } else {
                                UrlBlockManager.blockUrl(context, urlInput)
                                Toast.makeText(context, "URL이 차단되었습니다", Toast.LENGTH_SHORT).show()
                                isBlocked = true
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = urlInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    colors = if (isBlocked) {
                        ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
=======
                OutlinedButton(
                    onClick = onQrScanClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
<<<<<<< HEAD
                    Text(if (isBlocked) "차단 해제" else "차단하기")
=======
                    Text("QR 스캔")
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
<<<<<<< HEAD
                        if (validateUrl(urlInput)) onAnalyzeClick(normalizeUrl(urlInput))
                        else urlError = "올바른 URL 형식이 아닙니다 (예: example.com)"
=======
                        if (validateUrl(urlInput)) onAnalyzeClick(urlInput)
                        else urlError = "올바른 URL 형식이 아닙니다"
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    },
                    modifier = Modifier.weight(1f),
                    enabled = urlInput.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLoading) "분석 중..." else "검사하기")
                }
            }

            // 분석 결과 표시
            when (analysisState) {
                is AnalysisState.Loading -> AnalysisLoadingCard()
<<<<<<< HEAD
                is AnalysisState.Completed -> AnalysisResultCard(
                    result = analysisState.result,
                    onReanalyzeClick = { onReanalyzeClick(normalizeUrl(urlInput)) },
                    isReanalyzing = isLoading
                )
=======
                is AnalysisState.Completed -> AnalysisResultCard(analysisState.result)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                is AnalysisState.Error -> AnalysisErrorCard(analysisState.message)
                null -> UsageTipsCard()
            }
        }
    }
}

<<<<<<< HEAD
/**
 * URL 정규화: 프로토콜이 없으면 https://를 자동 추가
 */
private fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.isNotBlank() -> "https://$trimmed"
        else -> trimmed
    }
}

/**
 * URL 유효성 검사: 정규화된 URL이 유효한지 확인
 */
private fun validateUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val normalized = normalizeUrl(url)
    return try {
        val uri = java.net.URI(normalized)
        uri.host != null && uri.host.contains(".")
    } catch (e: Exception) {
        false
    }
}
=======
private fun validateUrl(url: String) = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

@Composable
fun AnalysisLoadingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "URL 분석 중...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "AI가 위험도를 검사하고 있습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
<<<<<<< HEAD
fun AnalysisResultCard(
    result: AnalysisResult,
    modifier: Modifier = Modifier,
    onReanalyzeClick: () -> Unit = {},
    isReanalyzing: Boolean = false
) {
=======
fun AnalysisResultCard(result: AnalysisResult, modifier: Modifier = Modifier) {
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    // MaterialTheme 기반 다크모드 감지 (시스템 설정이 아닌 실제 테마)
    val isDark = isDarkTheme()
    
    val (backgroundColor, statusIcon, statusText) = when (result.riskLevel) {
        RiskLevel.SAFE -> Triple(SafeGreen, Icons.Default.CheckCircle, "안전")
        RiskLevel.CAUTION -> Triple(WarningYellow, Icons.Default.Warning, "주의")
        RiskLevel.DANGER -> Triple(DangerRed, Icons.Default.Dangerous, "위험")
        RiskLevel.UNKNOWN -> Triple(TextSecondary, Icons.Default.Help, "미분석")
    }
    
    // 다크모드용 권장 사항 배경색 - 어두운 톤으로 변경
    val recommendationBgColor = when (result.riskLevel) {
        RiskLevel.SAFE -> if (isDark) SafeGreenDark else SafeGreenLight
        RiskLevel.CAUTION -> if (isDark) WarningYellowDark else WarningYellowLight
        RiskLevel.DANGER -> if (isDark) DangerRedDark else DangerRedLight
        RiskLevel.UNKNOWN -> MaterialTheme.colorScheme.surface
    }
    
    // 다크모드용 텍스트 색상
    val recommendationTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val recommendationLabelColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "분석 결과",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                Text(
                    text = "${result.riskScore}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    color = Color.White
                )
            }
        }
        
        // 권장 사항 영역 - 다크모드 지원
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(recommendationBgColor)
                .padding(20.dp)
        ) {
            Text(
                text = "권장 사항",
                style = MaterialTheme.typography.titleSmall,
                color = recommendationLabelColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = result.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = recommendationTextColor
            )
            
            result.details?.let { details ->
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
<<<<<<< HEAD

                // 🆕 단축 URL 해제 결과 표시
                details.expandedUrl?.let { expandedUrl ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark)
                                Primary.copy(alpha = 0.15f)
                            else
                                PrimaryLight.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDark) PrimaryLight else Primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "단축 URL 해제됨",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) PrimaryLight else Primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "→ $expandedUrl",
                                style = MaterialTheme.typography.bodySmall,
                                color = recommendationTextColor,
                                maxLines = 2
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 로컬 ML 점수
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    DetailChip("로컬 ML", "${(details.localMlScore * 100).toInt()}%", isDark)
                }

                Spacer(Modifier.height(12.dp))

                // VirusTotal 결과 (악성, 의심, 정상) - 🆕 실제 harmless 값 사용
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailChipWithColor(
                        label = "악성",
                        value = "${details.virusTotalMalicious}개",
                        isDark = isDark,
                        valueColor = if (details.virusTotalMalicious > 0) DangerRed else null
                    )
                    DetailChipWithColor(
                        label = "의심",
                        value = "${details.virusTotalSuspicious}개",
                        isDark = isDark,
                        valueColor = if (details.virusTotalSuspicious > 0) WarningYellow else null
                    )
                    DetailChipWithColor(
                        label = "정상",
                        value = "${details.virusTotalHarmless}개",
                        isDark = isDark,
                        valueColor = if (details.virusTotalHarmless > 0) SafeGreen else null
                    )
                }

                // VT 결과 없음 안내 + 재분석 버튼
                if (details.virusTotalTotal == 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "VirusTotal API 결과 없음",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningYellow,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // 🆕 재분석 버튼
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onReanalyzeClick,
                    enabled = !isReanalyzing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.5f)
                    )
                ) {
                    if (isReanalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("재분석 중...", color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("전체 재분석 (캐시 무시)", color = Color.White)
                    }
                }

=======
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailChip("로컬 ML", "${(details.localMlScore * 100).toInt()}%", isDark)
                    DetailChip("악성 탐지", "${details.virusTotalMalicious}개", isDark)
                    DetailChip("의심 탐지", "${details.virusTotalSuspicious}개", isDark)
                }
                
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                details.aiExplanation?.let { explanation ->
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) 
                                Primary.copy(alpha = 0.2f) 
                            else 
                                PrimaryLight.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isDark) PrimaryLight else Primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AI 분석",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDark) PrimaryLight else Primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = recommendationTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(label: String, value: String, isDark: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (isDark) PrimaryLight else Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
<<<<<<< HEAD
fun DetailChipWithColor(
    label: String,
    value: String,
    isDark: Boolean = false,
    valueColor: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor ?: if (isDark) PrimaryLight else Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
fun AnalysisErrorCard(message: String, modifier: Modifier = Modifier) {
    val isDark = isDarkTheme()
    val bgColor = if (isDark) DangerRedDark else DangerRedLight
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isDark) Color(0xFFFF6B6B) else DangerRed
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "분석 실패",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDark) Color(0xFFFF6B6B) else DangerRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun UsageTipsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = WarningYellow
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "사용 팁",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            TipItem(Icons.Default.ContentPaste, "클립보드 버튼을 눌러 복사한 URL을 붙여넣을 수 있습니다")
            TipItem(Icons.Default.QrCodeScanner, "QR 코드를 스캔하여 URL을 자동으로 입력할 수 있습니다")
            TipItem(Icons.Default.Notifications, "알림의 URL은 자동으로 검사됩니다")
        }
    }
}

@Composable
fun TipItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
