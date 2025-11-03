# Email Security Android App

## 🎯 프로젝트 개요

AI 기반 모바일 이메일 보안 앱으로, 실시간 악성 URL 및 QR코드 탐지를 통해 
사용자의 이메일 피싱 및 악성코드 감염을 방지하는 애플리케이션입니다.

### 핵심 기능
- 📨 이메일 알림 감지 (NotificationListener)
- 🔗 악성 URL/QR코드 실시간 탐지
- 🤖 TensorFlow Lite 온디바이스 ML 모델
- ☁️ Firebase Cloud Functions 연동
- 🔍 VirusTotal & OpenAI API 기반 위험도 분석
- 💡 AI 생성 위험 설명 제공

---

## 👥 팀원 및 역할

| 이름 | 역할 | 담당 영역 |
|------|------|----------|
| 엄민욱 | Backend 개발 | Firebase Cloud Functions, API 통합 |
| 이가연 | Frontend 개발 | Android UI/UX, Jetpack Compose |
| 모정원 | ML 개발 | TensorFlow Lite, 모델 학습 및 최적화 |

---

## 🛠️ 기술 스택

### Frontend
- **언어**: Kotlin
- **UI**: Jetpack Compose
- **아키텍처**: MVVM
- **라이브러리**: Retrofit2, Coroutine, FCM

### Backend
- **플랫폼**: Firebase Cloud Functions
- **런타임**: Node.js
- **외부 API**: VirusTotal, OpenAI GPT-4

### ML
- **프레임워크**: TensorFlow Lite
- **학습**: Python, TensorFlow
- **모델 포맷**: .tflite (온디바이스)

### DevOps
- **버전 관리**: Git & GitHub
- **CI/CD**: GitHub Actions (예정)
- **IDE**: Android Studio, VS Code, PyCharm

---

## 📅 프로젝트 일정

| 주차 | 목표 | 상태 |
|------|------|------|
| Week 10 | 개발 환경 구축, 기본 프로토타입 | 🔄 진행 중 |
| Week 11 | 이메일 감지 + ML 모델 완성 | ⏳ 예정 |
| Week 12 | 클라우드 연동 + API 통합 | ⏳ 예정 |
| Week 13 | UI 완성 + 성능 최적화 | ⏳ 예정 |
| Week 14 | Play Store 배포 + 발표 준비 | ⏳ 예정 |

---

## 📁 프로젝트 구조
email-security-android/
├── mobile/
│ ├── app/
│ │ ├── src/
│ │ │ ├── main/
│ │ │ │ ├── kotlin/
│ │ │ │ │ └── com/example/emailsecurity/
│ │ │ │ │ ├── MainActivity.kt
│ │ │ │ │ ├── ui/
│ │ │ │ │ ├── viewmodel/
│ │ │ │ │ └── data/
│ │ │ │ └── res/
│ │ │ └── test/
│ │ └── build.gradle.kts
│ └── settings.gradle.kts
│
├── backend/
│ ├── functions/
│ │ ├── index.js
│ │ └── package.json
│ ├── .firebaserc
│ └── firebase.json
│
├── ml/
│ ├── model_training.py
│ ├── model_conversion.py
│ ├── requirements.txt
│ └── model.tflite
│
├── docs/
│ ├── architecture.md
│ ├── api.md
│ └── roadmap.md
│
├── .gitignore
├── LICENSE
└── README.md

---

## 🚀 로컬 실행 방법

### 사전 요구사항
- Android Studio (최신 버전)
- Python 3.9+
- Node.js 14+
- Firebase 프로젝트 생성
- GitHub 저장소 클론

### Android 앱 실행

