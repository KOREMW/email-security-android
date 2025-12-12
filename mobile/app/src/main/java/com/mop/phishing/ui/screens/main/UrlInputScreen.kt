package com.mop.phishing.ui.screens.main

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
import androidx.compose.ui.platform.LocalClipboardManager
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputScreen(
    analysisState: AnalysisState?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAnalyzeClick: (String) -> Unit,
    onQrScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("URL 검사") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") } },
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background))
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(PrimaryLight.copy(0.3f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Primary, PrimaryGradientEnd))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, null, Color.White, Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("URL 안전성 검사", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("의심스러운 링크를 입력하면\nAI가 위험도를 분석해드립니다", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            OutlinedTextField(
                value = urlInput, onValueChange = { urlInput = it; urlError = null }, modifier = Modifier.fillMaxWidth(),
                label = { Text("URL 입력") }, placeholder = { Text("https://example.com") },
                leadingIcon = { Icon(Icons.Default.Link, null, Primary) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { clipboardManager.getText()?.let { urlInput = it.toString(); urlError = null } }) { Icon(Icons.Default.ContentPaste, "붙여넣기", MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (urlInput.isNotEmpty()) IconButton(onClick = { urlInput = "" }) { Icon(Icons.Default.Clear, "지우기", MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                },
                isError = urlError != null, supportingText = urlError?.let { { Text(it, color = DangerRed) } }, singleLine = true,
                keyboardOptions = KeyboardOptions(KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (validateUrl(urlInput)) onAnalyzeClick(urlInput) else urlError = "올바른 URL 형식이 아닙니다" }),
                shape = RoundedCornerShape(12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onQrScanClick, Modifier.weight(1f), RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("QR 스캔")
                }
                Button(onClick = { focusManager.clearFocus(); if (validateUrl(urlInput)) onAnalyzeClick(urlInput) else urlError = "올바른 URL 형식이 아닙니다" },
                    Modifier.weight(1f), urlInput.isNotBlank() && !isLoading, RoundedCornerShape(12.dp), ButtonDefaults.buttonColors(Primary), contentPadding = PaddingValues(vertical = 16.dp)) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White, 2.dp) else Icon(Icons.Default.Security, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text(if (isLoading) "분석 중..." else "검사하기")
                }
            }

            when (analysisState) {
                is AnalysisState.Loading -> AnalysisLoadingCard()
                is AnalysisState.Completed -> AnalysisResultCard(analysisState.result)
                is AnalysisState.Error -> AnalysisErrorCard(analysisState.message)
                null -> UsageTipsCard()
            }
        }
    }
}

private fun validateUrl(url: String) = url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

@Composable
fun AnalysisLoadingCard(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Primary, Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("URL 분석 중...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("AI가 위험도를 검사하고 있습니다", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnalysisResultCard(result: AnalysisResult, modifier: Modifier = Modifier) {
    val (backgroundColor, statusIcon, statusText) = when (result.riskLevel) {
        RiskLevel.SAFE -> Triple(SafeGreen, Icons.Default.CheckCircle, "안전")
        RiskLevel.CAUTION -> Triple(WarningYellow, Icons.Default.Warning, "주의")
        RiskLevel.DANGER -> Triple(DangerRed, Icons.Default.Dangerous, "위험")
        RiskLevel.UNKNOWN -> Triple(TextSecondary, Icons.Default.Help, "미분석")
    }
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(Color.Transparent)) {
        Box(Modifier.fillMaxWidth().background(backgroundColor).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, null, Color.White, Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) { Text("분석 결과", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f)); Text(statusText, style = MaterialTheme.typography.headlineSmall, color = Color.White) }
                Text("${result.riskScore}", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp), color = Color.White)
            }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp)) {
            Text("권장 사항", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(result.recommendation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            result.details?.let { details ->
                Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailChip("로컬 ML", "${(details.localMlScore * 100).toInt()}%")
                    DetailChip("악성 탐지", "${details.virusTotalMalicious}개")
                    DetailChip("의심 탐지", "${details.virusTotalSuspicious}개")
                }
                details.aiExplanation?.let { explanation ->
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(PrimaryLight.copy(0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.AutoAwesome, null, Primary, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column { Text("AI 분석", style = MaterialTheme.typography.labelMedium, color = Primary); Spacer(Modifier.height(4.dp)); Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AnalysisErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(DangerRedLight)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, DangerRed, Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text("분석 실패", style = MaterialTheme.typography.titleMedium, color = DangerRed); Spacer(Modifier.height(4.dp)); Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun UsageTipsCard(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lightbulb, null, WarningYellow, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("사용 팁", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.height(16.dp))
            TipItem(Icons.Default.ContentPaste, "클립보드 버튼을 눌러 복사한 URL을 붙여넣을 수 있습니다")
            TipItem(Icons.Default.QrCodeScanner, "QR 코드를 스캔하여 URL을 자동으로 입력할 수 있습니다")
            TipItem(Icons.Default.Notifications, "알림의 URL은 자동으로 검사됩니다")
        }
    }
}

@Composable
fun TipItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
