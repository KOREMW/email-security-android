package com.mop.phishing.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(val icon: ImageVector, val title: String, val description: String, val gradientColors: List<Color>)

val onboardingPages = listOf(
    OnboardingPage(Icons.Default.Lock, "안전한 링크 보호", "의심스러운 URL을 자동으로 감지하고\n피싱 위험으로부터 보호합니다", listOf(Primary, PrimaryGradientEnd)),
    OnboardingPage(Icons.Default.Search, "실시간 알림 분석", "카카오톡, 문자, 이메일 등\n모든 알림의 링크를 실시간으로 검사합니다", listOf(Primary, Primary)),
    OnboardingPage(Icons.Default.Star, "AI 기반 탐지", "TFLite와 OpenAI를 활용한\n정확한 피싱 URL 탐지 시스템", listOf(PrimaryGradientEnd, Primary))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit, onSkip: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TextButton(onClick = onSkip, Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Text("건너뛰기", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(100.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page -> OnboardingPageContent(onboardingPages[page]) }
            Row(Modifier.padding(bottom = 32.dp), horizontalArrangement = Arrangement.Center) {
                repeat(onboardingPages.size) { index ->
                    Box(Modifier.padding(horizontal = 4.dp).size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp).clip(CircleShape).background(if (pagerState.currentPage == index) Primary else MaterialTheme.colorScheme.outline.copy(0.3f)))
                }
            }
            Button(onClick = { if (pagerState.currentPage == onboardingPages.size - 1) onFinish() else coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(Primary)) {
                Text(if (pagerState.currentPage == onboardingPages.size - 1) "시작하기" else "다음", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(140.dp).clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(page.gradientColors)), contentAlignment = Alignment.Center) {
            Icon(page.icon, page.title, Color.White, Modifier.size(64.dp))
        }
        Spacer(Modifier.height(48.dp))
        Text(page.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(page.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
