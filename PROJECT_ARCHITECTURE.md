# 📱 Email Security Android - 프로젝트 구조 및 아키텍처 가이드

## 프로젝트 개요
**피싱 URL 탐지 Android 앱** - Gmail, SMS, 카카오톡 등의 알림에서 URL을 감지하고 ML + 서버 API로 위험도를 분석하는 앱

**기술 스택:**
- Android: Kotlin + Jetpack Compose + MVVM
- ML: TensorFlow Lite (온디바이스)
- Backend: Firebase Cloud Functions (Node.js)
- DB: Room (로컬) + Firebase Firestore (클라우드)
- Auth: Firebase Authentication (Google Sign-In)

---

## 📁 전체 프로젝트 구조

```
email-security-android-main/
├── mobile/                              # 🤖 Android 앱
│   └── app/src/main/
│       ├── java/com/mop/phishing/       # Kotlin 소스코드
│       ├── assets/                      # ML 모델 파일
│       │   └── phishing_model.tflite    # TFLite v6 모델 (13피처)
│       └── AndroidManifest.xml          # 앱 설정, 권한
│
├── backend/functions/                   # ☁️ Firebase Cloud Functions
│   └── src/
│       ├── index.js                     # 메인 엔드포인트
│       ├── scoreCalculator.js           # 앙상블 점수 계산
│       ├── virusTotalService.js         # VirusTotal API
│       └── cacheManager.js              # Redis 캐시
│
└── ml/                                  # 🧠 ML 모델 학습 (Python)
    ├── train_model.py                   # 모델 학습
    └── phishing_model_v6.tflite         # 학습된 모델
```

---

## 📱 Android 앱 상세 구조

### 루트 레벨 파일 (`com.mop.phishing/`)

| 파일 | 역할 | 연결 |
|------|------|------|
| `MainActivityCompose.kt` | 앱 진입점, Compose 테마 설정, 네비게이션 호스트 | → AppNavigation |
| `MainActivity.kt` | XML 레이아웃 기반 (미사용) | - |
| `MailNotificationListener.kt` | **핵심!** 시스템 알림 감시, URL 추출, 자동 분석 | → NotificationRepository, UrlClassifier |
| `NotificationViewModel.kt` | 알림 목록 상태 관리 | → NotificationRepository |
| `SettingsViewModel.kt` | 설정 상태 관리 (다크모드 등) | → SettingsRepository |
| `UrlAnalysisViewModel.kt` | URL 분석 로직, 캐싱, API 호출 | → CloudFunctionRepository, UrlClassifier, NotificationRepository |

---

### 📁 auth/ (인증)

| 파일 | 역할 | 연결 |
|------|------|------|
| `AuthRepository.kt` | Firebase Auth 래퍼, Google 로그인 처리 | → Firebase Auth SDK |
| `AuthViewModel.kt` | 로그인 상태 관리, 동기화 트리거 | → AuthRepository, CloudSyncRepository |

---

### 📁 data/local/ (로컬 데이터)

| 파일 | 역할 | 연결 |
|------|------|------|
| `AppDatabase.kt` | Room Database 정의 (싱글톤) | → NotificationDao |
| `NotificationDao.kt` | 알림 CRUD 쿼리 정의 | ← NotificationRepository |
| `NotificationEntity.kt` | 알림 데이터 모델 (DB 테이블) | ↔ NotificationItem 변환 |
| `NotificationRepository.kt` | 알림 데이터 접근 계층 | → NotificationDao, ← ViewModel들 |
| `SettingsRepository.kt` | DataStore 기반 설정 저장 | ← SettingsViewModel |

**데이터 흐름:**
```
ViewModel → Repository → DAO → Room DB
                ↓
         NotificationEntity ↔ NotificationItem (UI 모델)
```

---

### 📁 data/remote/ (원격 API)

| 파일 | 역할 | 연결 |
|------|------|------|
| `CloudFunctionApi.kt` | Retrofit API 인터페이스 정의 | ← CloudFunctionRepository |
| `CloudFunctionModels.kt` | 요청/응답 데이터 클래스 | ↔ ResponseMapper |
| `CloudFunctionRepository.kt` | API 호출 로직 (싱글톤) | → CloudFunctionApi, ← UrlAnalysisViewModel |
| `CloudSyncRepository.kt` | Firestore 동기화 | → Firebase Firestore, ← AuthViewModel |
| `ResponseMapper.kt` | 서버 응답 → UI 모델 변환 | ← UrlAnalysisViewModel |

**API 엔드포인트:**
```
POST /analyzeSuspiciousUrl  → 메인 분석 API
POST /getAIAnalysis         → OpenAI 상세 분석
```

---

### 📁 ml/ (머신러닝)

| 파일 | 역할 | 연결 |
|------|------|------|
| `UrlClassifier.kt` | TFLite 모델 로드, 피처 추출, 추론 (싱글톤) | ← UrlAnalysisViewModel, MailNotificationListener |

**ML 파이프라인:**
```
URL 입력
    ↓
13개 피처 추출 (urlLength, hasHttps, entropy 등)
    ↓
StandardScaler 정규화
    ↓
TFLite 모델 추론
    ↓
피싱 확률 (0.0 ~ 1.0)
```

---

### 📁 ui/components/ (재사용 UI)

| 파일 | 역할 |
|------|------|
| `AnalysisProgress.kt` | 분석 단계 표시 (URL추출 → ML → API → VT → AI) |
| `NotificationCard.kt` | 알림 목록 카드, NotificationItem/Source 정의 |
| `RiskBadge.kt` | 위험도 뱃지 (안전/주의/위험), RiskLevel enum |
| `SettingsItem.kt` | 설정 화면 항목 컴포넌트 |

---

### 📁 ui/navigation/

| 파일 | 역할 | 연결 |
|------|------|------|
| `AppNavigation.kt` | 전체 화면 네비게이션, Screen 정의 | → 모든 Screen, ViewModel들 |

**화면 라우트:**
```kotlin
Screen.Onboarding       → "/onboarding"
Screen.NotificationList → "/notification_list"
Screen.AnalysisDetail   → "/analysis_detail/{notificationId}"
Screen.Settings         → "/settings"
Screen.UrlInput         → "/url_input"
Screen.QrScanner        → "/qr_scanner"
```

---

### 📁 ui/screens/ (화면)

| 폴더/파일 | 화면 | 주요 기능 |
|-----------|------|----------|
| `onboarding/OnboardingScreen.kt` | 온보딩 | 3단계 슬라이드, 권한 요청, 로그인 |
| `main/NotificationListScreen.kt` | 알림 목록 | 메인 화면, FAB 메뉴 |
| `main/UrlInputScreen.kt` | URL 입력 | 수동 입력, 클립보드, 분석 결과 표시 |
| `main/QrScannerScreen.kt` | QR 스캔 | CameraX + ML Kit |
| `detail/AnalysisDetailScreen.kt` | 분석 상세 | 점수 카드, 권장사항, VT 결과, AI 분석 |
| `settings/SettingsScreen.kt` | 설정 | 다크모드, 알림 설정, 로그인, 테스트 버튼 |

---

### 📁 ui/theme/ (테마)

| 파일 | 역할 |
|------|------|
| `Color.kt` | 색상 정의 (Primary, SafeGreen, DangerRed 등) |
| `Theme.kt` | 라이트/다크 테마, MaterialTheme 설정 |
| `Type.kt` | 타이포그래피 정의 |

---

### 📁 util/ (유틸리티)

| 파일 | 역할 |
|------|------|
| `UrlActionUtils.kt` | URL 공유, 브라우저 열기, 신고하기 |
| `UrlBlockManager.kt` | URL/도메인 차단 관리 (SharedPreferences) |
| `TestNotificationHelper.kt` | 테스트용 알림 생성, 샘플 URL 분석 |
| `AccessibilityUtils.kt` | 설정 화면 열기 유틸리티 |

---

## 🔄 핵심 데이터 흐름

### 1. 알림 감지 → 분석 플로우
```
[시스템 알림 수신]
       ↓
MailNotificationListener.onNotificationPosted()
       ↓
URL 추출 (정규식)
       ↓
NotificationRepository.saveNotification() → Room DB 저장
       ↓
UrlClassifier.predict() → 로컬 ML 점수
       ↓
CloudFunctionRepository.analyzeSuspiciousUrl() → 서버 API
       ↓
NotificationRepository.updateAnalysisResult() → 결과 저장
       ↓
[위험 시] 푸시 알림 표시
```

### 2. 수동 URL 분석 플로우
```
[사용자 URL 입력]
       ↓
UrlInputScreen → onAnalyzeClick()
       ↓
UrlAnalysisViewModel.analyzeUrl()
       ↓
1. UrlClassifier.predict() → 로컬 ML
2. CloudFunctionRepository.analyzeSuspiciousUrl() → 서버 API
3. (선택) CloudFunctionRepository.getAIAnalysis() → OpenAI
       ↓
ResponseMapper.mapToAnalysisResult() → UI 모델 변환
       ↓
AnalysisState.Completed → UI 업데이트
```

### 3. 캐싱 플로우
```
[알림 클릭]
       ↓
UrlAnalysisViewModel.analyzeNotification()
       ↓
NotificationRepository.getAnalyzedResultById()
       ↓
[캐시 있음?]
   ├─ YES → 즉시 결과 표시 (API 호출 ❌)
   └─ NO  → 새 분석 실행 → DB 저장
```

---

## ☁️ Backend (Cloud Functions)

### 엔드포인트

| 함수 | 경로 | 역할 |
|------|------|------|
| `analyzeSuspiciousUrl` | POST /analyzeSuspiciousUrl | 메인 분석 (VT + 앙상블) |
| `getAIAnalysis` | POST /getAIAnalysis | OpenAI GPT-4 상세 분석 |

### 앙상블 점수 계산
```javascript
// scoreCalculator.js
최종점수 = (0.4 × ML점수) + (0.6 × VT점수)

// VT점수 계산
VT점수 = (malicious × 100 + suspicious × 50) / total_engines
```

### 재시도 로직
```javascript
// 지수 백오프: 1초 → 2초 → 4초
retryWithBackoff(fn, maxRetries=3, baseDelay=1000)
// 400, 401, 403, 404는 재시도 안함
```

---

## 🧠 ML 모델 (v6)

### 13개 피처
```
1. urlLength          - URL 전체 길이
2. domainLength       - 도메인 길이
3. pathLength         - 경로 길이
4. subdomainCount     - 서브도메인 개수
5. hasHttps           - HTTPS 여부 (0/1)
6. hasPort            - 비표준 포트 여부
7. paramCount         - 쿼리 파라미터 개수
8. digitRatio         - 숫자 비율
9. specialCharRatio   - 특수문자 비율
10. hasIpAddress      - IP 주소 사용 여부
11. hasSuspiciousKeyword - 의심 키워드 포함
12. hasSuspiciousTld  - 의심 TLD (.tk, .xyz 등)
13. entropy           - 도메인 엔트로피
```

### 정규화
```kotlin
normalized[i] = (value - mean[i]) / scale[i]  // StandardScaler
```

---

## 📊 주요 데이터 모델

### NotificationEntity (Room)
```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    id: String,              // PK
    source: String,          // GMAIL, SMS, KAKAOTALK
    title: String,
    content: String,
    url: String,
    receivedAt: Long,
    isAnalyzed: Boolean,
    riskLevel: String,       // SAFE, CAUTION, DANGER, UNKNOWN
    riskScore: Int,          // 0-100
    localMlScore: Float,     // 0.0-1.0
    virusTotalMalicious: Int,
    virusTotalSuspicious: Int,
    virusTotalTotal: Int,
    finalScore: Int,
    aiExplanation: String?,
    recommendation: String?,
    canAccess: Boolean
)
```

### AnalysisResult (UI)
```kotlin
data class AnalysisResult(
    riskLevel: RiskLevel,
    riskScore: Int,
    recommendation: String,
    canAccess: Boolean,
    details: AnalysisDetails?
)
```

---

## 🔐 권한

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 알림 리스너 (특수 권한 - 설정에서 수동 허용 필요) -->
<service android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
```

---

## 🚀 완성도 현황

```
핵심 기능 (피싱 탐지)     ██████████████████████  95%  ✅
UI/UX                    ██████████████████████  95%  ✅
데이터 저장/캐싱         ████████████████████░░  90%  ✅
서버 API 연동            ██████████████████████  95%  ✅
URL 차단 기능            ██████████████████████  95%  ✅
보안                     ████████████████████░░  85%  ✅
테스트                   ████░░░░░░░░░░░░░░░░░░  20%  ⚠️

종합: ~88%
```

---

## 📝 개발 시 참고사항

1. **UrlClassifier는 싱글톤** - `getInstance()`로 접근
2. **Repository도 싱글톤** - 메모리 효율
3. **다크모드 감지** - `MaterialTheme.colorScheme.background.luminance()` 사용
4. **분석 결과 캐싱** - `isAnalyzed == true`면 DB에서 바로 로드
5. **Cloud Functions URL** - Production URL 하드코딩됨

---

*마지막 업데이트: 2025년 12월 13일*
