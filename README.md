<<<<<<< HEAD
# 🛡️ 피싱 URL 탐지 앱 (Phishing Detection App)

> 기업 직원의 개인 이메일/SMS/메신저를 통한 악성 링크 및 QR코드 탐지 솔루션

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![TensorFlow](https://img.shields.io/badge/TensorFlow-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/)

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [시스템 아키텍처](#-시스템-아키텍처)
- [기술 스택](#-기술-스택)
- [AI 활용 명세](#-ai-활용-명세)
- [로컬 실행 방법](#-로컬-실행-방법)
- [프로젝트 구조](#-프로젝트-구조)
- [팀원 소개](#-팀원-소개)
- [라이선스](#-라이선스)

---

## 🎯 프로젝트 개요

### 문제 정의

기업 환경에서 업무용 이메일은 보안 솔루션으로 보호되지만, **개인 이메일/SMS/메신저는 무방비 상태**입니다. 최근 스미싱 공격의 39.6%가 카카오톡을 통해, 27.3%가 URL을 통해 이루어지고 있습니다 (AhnLab ASEC, 2024 Q2).

### 해결 방안

**온디바이스 ML + 클라우드 위협 인텔리전스 하이브리드** 방식으로 실시간 피싱 URL 탐지 및 차단 기능을 제공합니다.

### 핵심 가치

| 구분 | 설명 |
|------|------|
| 🚀 **실시간 탐지** | 알림 수신 즉시 URL 자동 분석 |
| 🤖 **하이브리드 AI** | 온디바이스 ML (40%) + 클라우드 API (60%) 앙상블 |
| 🛑 **자동 차단** | 위험 URL 접근 시 경고 화면으로 차단 |
| 📊 **AI 보고서** | OpenAI 기반 상세 보안 분석 리포트 제공 |

---

## ✨ 주요 기능

### 1. 실시간 알림 모니터링
- Gmail, SMS, 카카오톡 등 알림에서 URL 자동 감지
- 백그라운드에서 지속적인 모니터링

### 2. URL 위험도 분석
- 13가지 특성 기반 온디바이스 ML 분석
- VirusTotal API 연동 (70+ 보안 엔진)
- 위험도 점수화 (안전/의심/위험)

### 3. QR 코드 스캔
- 카메라를 통한 QR 코드 인식
- 포함된 URL 즉시 분석

### 4. 악성 URL 차단
- 접근성 서비스를 통한 브라우저 URL 감지
- 위험 URL 접근 시 전체 화면 경고

### 5. AI 보안 보고서
- OpenAI GPT 기반 상세 분석
- 위험 요소 및 대응 방안 제시

### 6. 클라우드 동기화
- Google 로그인 연동
- 분석 기록 및 차단 목록 동기화

---

## 🏗 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Android Application                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Notification│  │   URL Input │  │  QR Scanner │  │ Accessibility│        │
│  │   Listener  │  │   Screen    │  │   (CameraX) │  │   Service   │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                │                │
│         └────────────────┴────────────────┴────────────────┘                │
│                                   │                                         │
│                                   ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        URL Analysis Engine                           │   │
│  │  ┌───────────────────┐          ┌───────────────────┐               │   │
│  │  │  On-Device ML     │          │  Cloud Analysis   │               │   │
│  │  │  (TensorFlow Lite)│          │  (Firebase Func)  │               │   │
│  │  │                   │          │                   │               │   │
│  │  │  • URL Length     │          │  • VirusTotal API │               │   │
│  │  │  • Digit Count    │          │  • Safe Browsing  │               │   │
│  │  │  • Special Chars  │          │  • OpenAI Report  │               │   │
│  │  │  • IP Detection   │          │                   │               │   │
│  │  │  • Keywords       │          │                   │               │   │
│  │  └─────────┬─────────┘          └─────────┬─────────┘               │   │
│  │            │                              │                          │   │
│  │            └──────────┬───────────────────┘                          │   │
│  │                       ▼                                              │   │
│  │            ┌─────────────────────┐                                   │   │
│  │            │  Ensemble Scoring   │                                   │   │
│  │            │  ML 40% + VT 60%    │                                   │   │
│  │            └─────────────────────┘                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                   │                                         │
│                                   ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Data Layer                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │   │
│  │  │ Room DB     │  │ DataStore   │  │ SharedPrefs │                  │   │
│  │  │ (분석 기록) │  │ (설정)      │  │ (차단 목록) │                  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Firebase Cloud Functions                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  analyzeUrl │  │ getAiReport │  │  syncData   │  │  Cache DB   │        │
│  │  (분석 API) │  │ (AI 보고서) │  │ (동기화)    │  │ (Firestore) │        │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┘  └─────────────┘        │
│         │                │                                                  │
│         ▼                ▼                                                  │
│  ┌─────────────┐  ┌─────────────┐                                          │
│  │ VirusTotal  │  │   OpenAI    │                                          │
│  │    API      │  │    API      │                                          │
│  └─────────────┘  └─────────────┘                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 데이터 흐름

```
[알림 수신] → [URL 추출] → [온디바이스 ML 분석] → [서버 API 호출]
                                    ↓                     ↓
                            [ML 예측 점수]        [VirusTotal 결과]
                                    ↓                     ↓
                              [앙상블 스코어링 (40:60)]
                                         ↓
                    [위험도 판정: 안전/의심/위험]
                                         ↓
                    [결과 저장 & UI 표시 & 자동 차단]
```

---

## 🛠 기술 스택

### Android (Frontend)

| 기술 | 용도 |
|------|------|
| **Kotlin** | 주 개발 언어 |
| **Jetpack Compose** | 선언형 UI 프레임워크 |
| **Material Design 3** | UI 컴포넌트 및 테마 |
| **Navigation Compose** | 화면 전환 관리 |
| **ViewModel + StateFlow** | 상태 관리 |
| **Room Database** | 로컬 분석 기록 저장 |
| **DataStore** | 앱 설정 저장 |
| **CameraX + ML Kit** | QR 코드 스캔 |
| **TensorFlow Lite** | 온디바이스 ML 추론 |

### Backend (Cloud)

| 기술 | 용도 |
|------|------|
| **Firebase Cloud Functions** | 서버리스 백엔드 |
| **Firebase Firestore** | 분석 결과 캐싱 |
| **Firebase Auth** | Google 로그인 연동 |
| **Node.js (TypeScript)** | Cloud Functions 런타임 |

### External APIs

| API | 용도 |
|-----|------|
| **VirusTotal API** | 70+ 보안 엔진 기반 URL 검사 |
| **Google Safe Browsing** | 악성 사이트 데이터베이스 조회 |
| **OpenAI GPT-4** | AI 기반 보안 분석 보고서 생성 |

### 주요 라이브러리

```gradle
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// ML
implementation("org.tensorflow:tensorflow-lite:2.14.0")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-functions-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")

// Camera & QR
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("com.google.mlkit:barcode-scanning:17.2.0")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

---

## 🤖 AI 활용 명세

### 1. 온디바이스 ML 모델 (TensorFlow Lite)

| 항목 | 내용 |
|------|------|
| **위치** | `mobile/app/src/main/assets/phishing_model.tflite` |
| **목적** | 실시간 URL 피싱 여부 1차 판별 |
| **사용 방법** | URL에서 13가지 특성 추출 → TFLite 모델 추론 |
| **입력 특성** | URL 길이, 숫자 개수, 특수문자 비율, IP 주소 여부, 의심 키워드 포함 여부 등 |
| **출력** | 피싱 확률 (0.0 ~ 1.0) |
| **장점** | 네트워크 없이 즉시 분석, 프라이버시 보호 |

```kotlin
// 특성 추출 예시
private fun extractFeatures(url: String): FloatArray {
    return floatArrayOf(
        url.length.toFloat(),                    // URL 길이
        url.count { it.isDigit() }.toFloat(),   // 숫자 개수
        url.count { it == '.' }.toFloat(),      // 점 개수
        url.count { it == '-' }.toFloat(),      // 하이픈 개수
        url.count { it == '/' }.toFloat(),      // 슬래시 개수
        // ... 13가지 특성
    )
}
```

### 2. VirusTotal API 연동

| 항목 | 내용 |
|------|------|
| **위치** | Firebase Cloud Functions (`functions/src/index.ts`) |
| **목적** | 70+ 보안 엔진 기반 URL 악성 여부 검증 |
| **사용 방법** | URL 해시 → VT API 조회 → 탐지 결과 집계 |
| **출력** | malicious, suspicious, harmless 카운트 |

```typescript
// VirusTotal API 호출
const response = await axios.get(
  `https://www.virustotal.com/api/v3/urls/${urlId}`,
  { headers: { 'x-apikey': VT_API_KEY } }
);
```

### 3. 앙상블 스코어링

| 항목 | 내용 |
|------|------|
| **위치** | `UrlAnalysisViewModel.kt` |
| **목적** | ML과 VT 결과 통합하여 최종 위험도 산출 |
| **가중치** | 온디바이스 ML 40% + VirusTotal 60% |

```kotlin
// 앙상블 스코어 계산
val ensembleScore = (mlScore * 0.4f) + (vtScore * 0.6f)
val riskLevel = when {
    ensembleScore >= 70 -> RiskLevel.DANGER
    ensembleScore >= 40 -> RiskLevel.WARNING
    else -> RiskLevel.SAFE
}
```

### 4. OpenAI GPT 보안 보고서

| 항목 | 내용 |
|------|------|
| **위치** | Firebase Cloud Functions (`getAiReport`) |
| **목적** | 분석 결과를 사용자 친화적인 보안 보고서로 변환 |
| **사용 방법** | 분석 데이터 → GPT-4 프롬프트 → 한국어 보고서 |
| **출력** | 위험 요약, 상세 분석, 권장 조치 |

```typescript
const prompt = `
당신은 사이버 보안 전문가입니다. 다음 URL 분석 결과를 바탕으로 
사용자에게 보안 보고서를 작성해주세요.

URL: ${url}
위험도 점수: ${riskScore}%
VirusTotal 탐지: 악성 ${malicious}개, 의심 ${suspicious}개
ML 예측: ${mlPrediction}%

보고서를 한국어로 작성하고, 다음을 포함해주세요:
1. 위험 수준 요약
2. 발견된 위험 요소
3. 권장 조치 사항
`;
```

### 5. AI 도구 활용 (개발 과정)

| 도구 | 활용 내용 |
|------|----------|
| **Claude** | 코드 리뷰, 디버깅, 아키텍처 설계 조언 |
| **GitHub Copilot** | 코드 자동 완성, 보일러플레이트 생성 |
| **GPT-4** | API 연동 코드 작성, 문서화 |
| **Perplexity** | 기술 리서치, 라이브러리 비교 |

---

## 🚀 로컬 실행 방법

### 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Node.js 18+ (Cloud Functions용)
- Firebase CLI
=======
# 🛡️ Email Security - AI 피싱 탐지 앱

AI 기반 피싱 URL 탐지 Android 애플리케이션

## 📱 주요 기능

- **실시간 URL 분석**: 알림에서 URL 자동 감지 및 분석
- **AI 분석**: TensorFlow Lite (온디바이스) + OpenAI GPT-4o-mini (상세 분석)
- **VirusTotal 연동**: 90+ 보안 엔진 검사 결과 통합
- **QR 코드 스캔**: 카메라로 QR 코드 URL 분석
- **위험 URL 자동 차단**: DANGER 등급 URL 자동 차단

## 🏗️ 기술 스택

| 분류 | 기술 |
|------|------|
| **Language** | Kotlin 100% |
| **UI** | Jetpack Compose |
| **Architecture** | MVVM + Repository Pattern |
| **DI** | Manual (ViewModel Factory) |
| **Local DB** | Room Database |
| **Settings** | DataStore |
| **Network** | Retrofit + OkHttp |
| **Auth** | Firebase Authentication (Google Sign-In) |
| **Cloud** | Firebase Cloud Functions |
| **ML** | TensorFlow Lite (13-feature model) |
| **AI** | OpenAI GPT-4o-mini |
| **Security API** | VirusTotal API |

## 📋 사전 요구사항

- **Android Studio**: Hedgehog (2023.1.1) 이상
- **JDK**: 17 이상
- **Android SDK**: 
  - compileSdk: 35
  - minSdk: 26
  - targetSdk: 35
- **Node.js**: 18 이상 (Backend용)
- **Firebase CLI**: `npm install -g firebase-tools`

## 🚀 프로젝트 설정
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

### 1. 저장소 클론

```bash
<<<<<<< HEAD
git clone https://github.com/your-username/phishing-detection-app.git
cd phishing-detection-app
```

### 2. Firebase 설정

1. [Firebase Console](https://console.firebase.google.com/)에서 프로젝트 생성
2. Android 앱 추가 (패키지명: `com.mop.phishing`)
3. `google-services.json` 다운로드 → `mobile/app/` 폴더에 배치

### 3. API 키 설정

`functions/.env` 파일 생성:

```env
=======
git clone https://github.com/YOUR_REPO/email-security-android.git
cd email-security-android
```

### 2. Firebase 설정 (필수!)

#### 2-1. Firebase Console에서 프로젝트 생성
1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 생성 또는 기존 프로젝트 선택
3. Android 앱 추가:
   - 패키지명: `com.mop.phishing`
   - SHA-1 인증서 추가 (Google 로그인용)

#### 2-2. google-services.json 다운로드
1. Firebase Console → 프로젝트 설정 → 일반
2. `google-services.json` 다운로드
3. 파일을 `mobile/app/` 폴더에 복사

```
mobile/
└── app/
    ├── google-services.json  ← 여기에 복사!
    ├── build.gradle.kts
    └── src/
```

#### 2-3. Firebase 서비스 활성화
Firebase Console에서 다음 서비스 활성화:
- ✅ Authentication (Google 로그인)
- ✅ Firestore Database
- ✅ Cloud Functions

### 3. Backend 설정 (Cloud Functions)

#### 3-1. Firebase CLI 로그인

```bash
cd backend/functions
npm install
firebase login
firebase use YOUR_PROJECT_ID
```

#### 3-2. API 키 설정

**방법 1: Firebase Secrets (권장)**
```bash
firebase functions:secrets:set VIRUSTOTAL_API_KEY
# 프롬프트에 API 키 입력

firebase functions:secrets:set OPENAI_API_KEY
# 프롬프트에 API 키 입력
```

**방법 2: .env 파일**
```bash
# backend/functions/.env 파일 생성
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
VIRUSTOTAL_API_KEY=your_virustotal_api_key
OPENAI_API_KEY=your_openai_api_key
```

<<<<<<< HEAD
### 4. Cloud Functions 배포

```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

### 5. Android 앱 빌드

```bash
# Android Studio에서 프로젝트 열기
# mobile/ 폴더를 Android Studio로 열기

# 또는 커맨드 라인에서:
=======
#### 3-3. Functions 배포

```bash
firebase deploy --only functions
```

#### API 키 발급 방법

| API | 발급 링크 | 비고 |
|-----|----------|------|
| VirusTotal | [virustotal.com](https://www.virustotal.com/gui/join-us) | 무료 계정: 4회/분 |
| OpenAI | [platform.openai.com](https://platform.openai.com/api-keys) | 유료 (사용량 기반) |

### 4. Android 앱 빌드

#### Android Studio에서 열기
1. Android Studio 실행
2. `File` → `Open` → `email-security-android/mobile` 폴더 선택
3. Gradle Sync 대기 (자동 실행)

#### 빌드 및 실행
```bash
# 터미널에서 (선택사항)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
cd mobile
./gradlew assembleDebug
```

<<<<<<< HEAD
### 6. 앱 설치 및 실행

```bash
# 에뮬레이터 또는 실제 기기에 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 7. 필수 권한 설정 (앱 내에서)

1. **알림 접근 권한**: 설정 > 알림 접근 > 피싱 탐지 활성화
2. **접근성 서비스**: 설정 > 접근성 > 피싱 URL 차단 서비스 활성화
3. **다른 앱 위에 표시**: 설정 > 앱 > 특별 액세스 > 다른 앱 위에 표시

---
=======
또는 Android Studio에서 ▶️ Run 버튼 클릭
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

## 📁 프로젝트 구조

```
<<<<<<< HEAD
phishing-detection-app/
├── mobile/                          # Android 앱
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/mop/phishing/
│   │   │   │   ├── ui/              # UI 레이어
│   │   │   │   │   ├── components/  # 재사용 컴포넌트
│   │   │   │   │   ├── navigation/  # 네비게이션
│   │   │   │   │   ├── screens/     # 화면별 UI
│   │   │   │   │   └── theme/       # 테마 설정
│   │   │   │   ├── data/            # 데이터 레이어
│   │   │   │   │   ├── local/       # Room DB, DataStore
│   │   │   │   │   └── remote/      # API 클라이언트
│   │   │   │   ├── service/         # 백그라운드 서비스
│   │   │   │   │   ├── NotificationListener.kt
│   │   │   │   │   └── UrlBlockingAccessibilityService.kt
│   │   │   │   ├── ml/              # ML 모델 관련
│   │   │   │   │   └── PhishingDetector.kt
│   │   │   │   ├── auth/            # 인증 관련
│   │   │   │   └── util/            # 유틸리티
│   │   │   ├── assets/
│   │   │   │   └── phishing_model.tflite
│   │   │   └── res/                 # 리소스
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
├── functions/                       # Firebase Cloud Functions
│   ├── src/
│   │   └── index.ts                 # API 엔드포인트
│   ├── package.json
│   └── tsconfig.json
│
├── docs/                            # 문서
│   ├── terms_of_service.md
│   └── privacy_policy.md
=======
email-security-android/
├── mobile/                      # Android 앱
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/mop/phishing/
│   │   │   │   ├── ui/          # Compose UI
│   │   │   │   ├── data/        # Repository, Room, API
│   │   │   │   ├── auth/        # Firebase Auth
│   │   │   │   ├── ml/          # TensorFlow Lite
│   │   │   │   └── util/        # 유틸리티
│   │   │   ├── assets/          # ML 모델 파일
│   │   │   └── res/             # 리소스
│   │   └── google-services.json # ⚠️ 별도 설정 필요
│   └── build.gradle.kts
│
├── backend/                     # Firebase Cloud Functions
│   └── functions/
│       ├── index.js             # API 엔드포인트
│       ├── src/                 # 서비스 모듈
│       └── package.json
│
├── ml/                          # ML 모델 학습 (Python)
│   ├── train_model.py
│   └── requirements.txt
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
│
└── README.md
```

<<<<<<< HEAD
---

## 👥 팀원 소개

<table>
  <tr>
    <td align="center">
      <b>엄민욱</b><br>
      Backend / Cloud 개발<br>
      <sub>Firebase Functions, API 설계</sub>
    </td>
    <td align="center">
      <b>이가연</b><br>
      Frontend / Android 개발<br>
      <sub>Jetpack Compose, UI/UX</sub>
    </td>
    <td align="center">
      <b>모정원</b><br>
      AI / ML 개발<br>
      <sub>TensorFlow Lite, 모델 학습</sub>
    </td>
  </tr>
</table>

**소속**: 한성대학교 융합보안학과

**프로젝트 기간**: 2025년 9월 ~ 12월 (산학협력 PBL)

---

## 📄 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

### 오픈소스 라이선스

이 앱은 다음 오픈소스 라이브러리를 사용합니다:

- TensorFlow Lite - Apache 2.0
- Retrofit2 - Apache 2.0
- OkHttp3 - Apache 2.0
- Jetpack Compose - Apache 2.0
- Room Database - Apache 2.0
- Firebase SDK - Apache 2.0
- ML Kit - Apache 2.0
- CameraX - Apache 2.0
- Kotlin Coroutines - Apache 2.0
- Material Design 3 - Apache 2.0

---

## 📞 문의

프로젝트 관련 문의사항은 아래로 연락해 주세요.

- 📧 mojungwon2@gmail.com
- 📧 minwook2000@naver.com
- 📧 lucy1789@naver.com

---

<p align="center">
  <b>🛡️ 안전한 모바일 환경을 위한 피싱 탐지 솔루션 🛡️</b>
</p>
=======
## 🔧 트러블슈팅

### google-services.json 없음
```
ERROR: File google-services.json is missing
```
→ Firebase Console에서 다운로드 후 `mobile/app/`에 복사

### SHA-1 인증서 오류 (Google 로그인 실패)
```bash
# 디버그 SHA-1 확인
cd mobile
./gradlew signingReport
```
→ 출력된 SHA-1을 Firebase Console에 추가

### API 키 미설정
```
ERROR: VirusTotal API key not configured
```
→ Firebase Secrets 또는 .env 파일에 API 키 설정

### Gradle Sync 실패
```bash
# 캐시 삭제 후 재시도
cd mobile
./gradlew clean
./gradlew --refresh-dependencies
```

## 🔑 민감 정보 관리

⚠️ **절대 Git에 커밋하지 마세요!**

| 파일 | 내용 | 공유 방법 |
|------|------|----------|
| `google-services.json` | Firebase 설정 | 팀 드라이브/메신저로 별도 공유 |
| `backend/functions/.env` | API 키 | 각자 발급 또는 팀장이 공유 |
| `*.keystore` | 서명 키 | 팀 드라이브로 별도 공유 |

## 👥 팀원 온보딩 체크리스트

- [ ] 저장소 클론
- [ ] google-services.json 받아서 복사
- [ ] Android Studio에서 프로젝트 열기
- [ ] Gradle Sync 성공 확인
- [ ] 에뮬레이터/실기기에서 앱 실행
- [ ] (선택) Backend 설정 및 배포

## 📞 문의

프로젝트 관련 문의: [팀장 이름/연락처]

---

**한성대학교 융합보안학과 PBL 프로젝트**
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
