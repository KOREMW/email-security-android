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

### 1. 저장소 클론

```bash
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
VIRUSTOTAL_API_KEY=your_virustotal_api_key
OPENAI_API_KEY=your_openai_api_key
```

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
cd mobile
./gradlew assembleDebug
```

또는 Android Studio에서 ▶️ Run 버튼 클릭

## 📁 프로젝트 구조

```
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
│
└── README.md
```

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
