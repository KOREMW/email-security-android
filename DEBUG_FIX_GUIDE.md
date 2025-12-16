# 🔧 ML 점수 50% 고정 문제 해결 가이드

**수정 일시**: 2025-12-16
**빌드 ID**: `2025-12-16-FIX-V3`

---

## 📋 수정 내용 요약

### 1. 핵심 버그 수정 (ResponseMapper.kt)

**문제**: `calculateAdjustedScore()` 함수가 `actualLocalMlScore`를 받지 않고, 백엔드 응답의 `localMlScore`만 사용했습니다. 백엔드가 50%를 반환하면 그대로 50%가 표시되었습니다.

**해결**: 
```kotlin
// 이전 (버그)
private fun calculateAdjustedScore(response: AnalyzeSuspiciousUrlResponse): Pair<Int, RiskLevel> {
    val mlScore = response.breakdown?.localMlScore ?: 0.0  // 백엔드 값만 사용
    ...
}

// 이후 (수정)
private fun calculateAdjustedScore(
    response: AnalyzeSuspiciousUrlResponse,
    actualLocalMlScore: Float? = null  // 로컬 ML 점수 추가
): Pair<Int, RiskLevel> {
    val mlScore = actualLocalMlScore?.toDouble()  // 로컬 값 우선!
        ?: response.breakdown?.localMlScore 
        ?: 0.0
    ...
}
```

### 2. 가중치 조정

| 항목 | 이전 | 이후 |
|------|------|------|
| 로컬 ML | 30% | **40%** |
| VirusTotal | 70% | **60%** |

### 3. 디버그 로그 강화

모든 핵심 파일에 **WARN 레벨 로그** 추가 (필터 없이도 확인 가능):
- `UrlAnalysisViewModel.kt` - BUILD_ID, classifier 상태
- `MailNotificationListener.kt` - BUILD_ID, 서비스 연결 상태
- `UrlClassifier.kt` - predict() 과정 전체
- `ResponseMapper.kt` - 점수 계산 과정

---

## 🚀 빌드 및 설치 방법

### 방법 1: Android Studio 사용

```bash
1. Android Studio에서 프로젝트 열기
2. Build → Clean Project
3. Build → Rebuild Project
4. Run → Run 'app' (또는 Shift+F10)
```

### 방법 2: 명령줄 사용

```bash
# 프로젝트 폴더로 이동
cd "C:\Users\USER\Desktop\email-security-android-main(2)\mobile"

# 클린 빌드
./gradlew clean assembleDebug

# 설치 (USB 디버깅 필요)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 방법 3: 강제 재설치 (권장)

```bash
# 기존 앱 완전 삭제
adb uninstall com.mop.phishing

# 새 APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔍 새 APK 설치 확인 방법

### Logcat에서 BUILD_ID 확인

1. Android Studio → Logcat 창 열기
2. 필터 없이 전체 로그 보기 (또는 `UrlAnalysisVM` 필터)
3. 앱 실행 시 다음 로그가 보여야 함:

```
W/UrlAnalysisVM: ████████████████████████████████████████
W/UrlAnalysisVM: ██ BUILD_ID: 2025-12-16-FIX-V3 ██
W/UrlAnalysisVM: ████████████████████████████████████████
```

### ADB로 버전 확인

```bash
adb shell dumpsys package com.mop.phishing | findstr "versionName"
```

---

## 📊 테스트 방법

### 1. 정상 URL 테스트

```
URL: https://google.com
기대 결과: ML 5% (신뢰 도메인)
```

### 2. 의심 URL 테스트

```
URL: http://google-login-verify.xyz/account
기대 결과: ML 60~80% (피싱 특징 감지)
```

### Logcat 로그 예시 (정상 작동 시)

```
W/UrlClassifier: ╭────────── PREDICT START (VERSION: v10.2-DEBUG) ──────────╮
W/UrlClassifier: │ URL: https://google.com
W/UrlClassifier: │ hostname: google.com
W/UrlClassifier: │ ★ 신뢰 도메인 감지: google.com → 5%
W/UrlClassifier: ╰────────── PREDICT END (TRUSTED) ──────────╯

W/UrlAnalysisVM: ══════════ runLocalAnalysis 시작 (BUILD: 2025-12-16-FIX-V3) ══════════
W/UrlAnalysisVM: │ URL: https://google.com
W/UrlAnalysisVM: │ classifier: ✅ OK (v10.2-DEBUG)
W/UrlAnalysisVM: │ ✅ predict() 성공!
W/UrlAnalysisVM: │ ★ ML 점수: 0.05 (5%)
W/UrlAnalysisVM: ══════════ runLocalAnalysis 종료 (SUCCESS) ══════════

D/ResponseMapper: ━━━━ mapToAnalysisResult 시작 ━━━━
D/ResponseMapper:   actualLocalMlScore 전달값: 0.05
D/ResponseMapper: 🔍 ML 점수 소스: actualLocal=0.05, backend=null, using=0.05
```

---

## ⚠️ 로그가 안 보이는 경우

### 1. Logcat 필터 확인

- 필터를 **비활성화**하고 전체 로그 확인
- 또는 필터에 `UrlAnalysisVM|UrlClassifier|ResponseMapper|NotiListener` 입력

### 2. 로그 레벨 확인

- Logcat 우측 상단 드롭다운에서 **"Verbose"** 또는 **"Debug"** 선택

### 3. 프로세스 확인

- Logcat 상단에서 **앱 프로세스** (`com.mop.phishing`) 선택

### 4. 로그 버퍼 클리어

```bash
adb logcat -c
adb logcat | grep -E "UrlAnalysisVM|UrlClassifier|BUILD_ID"
```

---

## 🔧 추가 디버깅 (로그가 전혀 안 보일 경우)

### 앱 데이터 초기화

```bash
adb shell pm clear com.mop.phishing
```

### APK 파일 직접 확인

```bash
# APK 내 클래스 확인
aapt dump xmltree app-debug.apk AndroidManifest.xml | head -20

# APK 빌드 시간 확인
dir "C:\Users\USER\Desktop\email-security-android-main(2)\mobile\app\build\outputs\apk\debug"
```

---

## 📁 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `ResponseMapper.kt` | `calculateAdjustedScore()`에 `actualLocalMlScore` 파라미터 추가 |
| `UrlAnalysisViewModel.kt` | BUILD_ID 추가, WARN 레벨 로그 |
| `MailNotificationListener.kt` | BUILD_ID 추가, WARN 레벨 로그 |
| `UrlClassifier.kt` | predict() 함수에 WARN 레벨 로그 |

---

**작성자**: Claude AI
**날짜**: 2025-12-16
