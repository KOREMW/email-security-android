# 🐛 ML 점수 50% 고정 문제 - 최종 해결 보고서

## 📋 문제 요약
- **증상**: UI에서 온디바이스 ML 분석 결과가 항상 50%로 표시
- **발생 위치**: 이메일 알림 수신 시 분석 결과 화면
- **실제 상황**: 백엔드 API는 정상 작동, 로컬 ML 모델도 정상 작동

## 🔍 원인 분석

### 문제 발생 경로
1. **MailNotificationListener.kt** (이메일 알림 수신 시)
   - 265번: `localScore = classifier?.predict(url) ?: 0.5f` ✅ ML 모델 정상 실행
   - 285번: `localRiskScore = localScore.toDouble()` ✅ 백엔드에 정상 전달
   - **296번: `ResponseMapper.mapToAnalysisResult(response, null)` ❌ 문제 발견!**
     - 세 번째 파라미터 `actualLocalMlScore` 누락
     - ResponseMapper가 백엔드 응답값(0.5)을 사용

2. **ResponseMapper.kt**
   - 170번: `val localMlScoreToUse = actualLocalMlScore ?: response.breakdown?.localMlScore?.toFloat() ?: 0f`
   - actualLocalMlScore가 null이므로 백엔드 값 사용 → 50%

3. **백엔드 (index.js)**
   - 72-81번: `validateLocalScore()` 함수
   - null이 들어오면 기본값 0.5 (50%) 반환
   - 안드로이드는 제대로 보냈지만, UI 표시용으로는 사용 안 됨

### 왜 ViewModel은 괜찮았나?
- **UrlAnalysisViewModel.kt:237**
  ```kotlin
  val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse, localRiskScore)
  ```
  → 수동 URL 입력/QR 스캔 시에는 `localRiskScore` 전달 ✅

- **MailNotificationListener.kt:296** (수정 전)
  ```kotlin
  val analysisResult = ResponseMapper.mapToAnalysisResult(response, null)
  ```
  → 이메일 알림 수신 시에는 `localScore` 미전달 ❌

## ✅ 해결 방법

### 수정 1: UrlAnalysisViewModel.kt
**위치**: 237번 라인
```kotlin
// 변경 전
val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse)

// 변경 후
val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse, localRiskScore)
```

### 수정 2: ResponseMapper.kt
**위치**: 135-139번, 156-161번, 170번 라인

**함수 시그니처 변경**:
```kotlin
fun mapToAnalysisResult(
    response: AnalyzeSuspiciousUrlResponse,
    aiResponse: GetAIAnalysisResponse? = null,
    actualLocalMlScore: Float? = null  // ← 추가
): AnalysisResult
```

**실제 로컬 ML 점수 우선 사용**:
```kotlin
val localMlScoreToUse = actualLocalMlScore ?: response.breakdown?.localMlScore?.toFloat() ?: 0f
```

**디버깅 로그 추가**:
```kotlin
Log.d(TAG, "mapToAnalysisDetails: actualLocalMlScore=$actualLocalMlScore, backendScore=${response.breakdown?.localMlScore}, using=$localMlScoreToUse")
```

### 수정 3: MailNotificationListener.kt (최종 수정!)
**위치**: 296번 라인
```kotlin
// 변경 전
val analysisResult = ResponseMapper.mapToAnalysisResult(response, null)

// 변경 후  
val analysisResult = ResponseMapper.mapToAnalysisResult(response, null, localScore)
```

## 📊 영향 범위

### 수정 전
| 사용 경로 | localScore 전달 | UI 표시 |
|---------|---------------|---------|
| 수동 URL 입력 | ❌ (수정됨) | 50% 고정 |
| QR 코드 스캔 | ❌ (수정됨) | 50% 고정 |
| 이메일 알림 | ❌ | **50% 고정** |
| 캐시 사용 | - | 이전 값 |

### 수정 후
| 사용 경로 | localScore 전달 | UI 표시 |
|---------|---------------|---------|
| 수동 URL 입력 | ✅ | **실제 ML 점수** |
| QR 코드 스캔 | ✅ | **실제 ML 점수** |
| 이메일 알림 | ✅ | **실제 ML 점수** |
| 캐시 사용 | - | 재분석 필요 |

## 🧪 테스트 방법

### 1. 앱 재빌드 및 설치
```bash
cd mobile
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. 로그 확인
```bash
adb logcat -c
adb logcat | grep -E "UrlClassifier|ResponseMapper|MailNotificationListener"
```

### 3. 기대되는 로그
```
UrlClassifier: ML raw: 25% → calibrated: 18%
MailNotificationListener: ML 분석 완료 - 점수=18%
ResponseMapper: mapToAnalysisDetails: actualLocalMlScore=0.18, backendScore=0.5, using=0.18
```

### 4. UI 확인
- 분석 상세 화면
- "온디바이스 ML 분석" 항목
- **기대값**: 백엔드 50%가 아닌 **실제 ML 점수 (예: 18%)** 표시

## 📝 학습 포인트

1. **같은 기능, 다른 코드 경로**: ViewModel과 NotificationListener가 별도 경로
2. **파라미터 전달 누락**: 새 파라미터 추가 시 모든 호출부 확인 필요
3. **로깅의 중요성**: 디버깅 로그가 없었다면 문제 파악 어려웠을 것
4. **테스트 시나리오**: 모든 사용자 경로 테스트 필요

## ✨ 최종 결과

### Before
```
URL: https://example.com
로컬 ML: 50% ← 항상 고정
VirusTotal: 정상 작동
최종 위험도: 정상 작동
```

### After
```
URL: https://example.com
로컬 ML: 18% ← 실제 ML 모델 결과 ✅
VirusTotal: 정상 작동
최종 위험도: 정상 작동
```

## 🎯 수정 파일 목록
1. `mobile/app/src/main/java/com/mop/phishing/UrlAnalysisViewModel.kt`
2. `mobile/app/src/main/java/com/mop/phishing/data/remote/ResponseMapper.kt`
3. `mobile/app/src/main/java/com/mop/phishing/MailNotificationListener.kt` ← **핵심**

---
**수정 완료 시간**: 2025-12-16 17:30
**빌드 버전**: v1.1 (ML 점수 수정)
