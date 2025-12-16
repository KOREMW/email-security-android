# 🔥 최종 디버깅 가이드 - ML 50% 문제

## 📱 새 APK 정보
- **빌드 시간**: 2025-12-16 17:55
- **파일**: `mobile/app/build/outputs/apk/debug/app-debug.apk`
- **크기**: 42MB
- **변경사항**: 상세 디버깅 로그 추가

## 🎯 이번 빌드의 핵심 개선

### 추가된 로그
모든 ML 점수 계산 경로에 상세 로그 추가:

#### 1. ViewModel 초기화
```
UrlAnalysisVM: UrlClassifier 초기화 시작...
UrlAnalysisVM: ✅ UrlClassifier 초기화 성공!
또는
UrlAnalysisVM: ❌ UrlClassifier 초기화 실패: [에러 메시지]
```

#### 2. runLocalAnalysis
```
UrlAnalysisVM: runLocalAnalysis 시작: url=https://...
UrlAnalysisVM:   classifier 상태: OK
UrlAnalysisVM:   ✅ predict() 성공: score=0.18
또는
UrlAnalysisVM:   ❌ classifier가 null! 기본값 0.5 반환
또는
UrlAnalysisVM:   ❌ predict() 실패, 기본값 0.5 사용: [에러]
```

#### 3. MailNotificationListener
```
MailNotificationListener: UrlClassifier 초기화 시작...
MailNotificationListener: ✅ UrlClassifier 싱글톤 연결 완료
MailNotificationListener: 🧠 Step 2: 온디바이스 ML 분석 시작...
MailNotificationListener:   classifier 상태: ✅ OK
MailNotificationListener:   ✅ predict() 성공: score=0.18
```

#### 4. UrlClassifier
```
UrlClassifier: ========== PREDICT START ==========
UrlClassifier: predict: https://example.com
UrlClassifier:   hostname: example.com
UrlClassifier:   VERSION: v10.2-DEBUG
UrlClassifier:   FEATURES (13개): [...]
UrlClassifier:   NORMALIZED (13개): [...]
UrlClassifier:   RAW OUTPUT (모델 직접 출력): 0.234
UrlClassifier:   ML raw: 23% → calibrated: 18%
UrlClassifier: ========== PREDICT END ==========
```

#### 5. ResponseMapper
```
ResponseMapper: mapToAnalysisDetails: actualLocalMlScore=0.18, backendScore=0.5, using=0.18
```

## 📋 테스트 절차

### 1단계: 완전 재설치
```bash
# Windows 명령 프롬프트에서 (adb 필요)
adb uninstall com.mop.phishing
adb install -r app-debug.apk

# 또는 핸드폰에서
# 설정 → 앱 → Phishing Detector → 삭제
# 새 APK 설치
```

### 2단계: 로그 캡처
```bash
# 터미널 1 (로그 캡처)
adb logcat -c
adb logcat | grep -E "UrlClassifier|UrlAnalysisVM|ResponseMapper|MailNotificationListener" | tee ml_debug.log

# 터미널 2 (앱 테스트)
# 앱에서 URL 분석 실행
# - QR 스캔
# - 직접 URL 입력: https://google.com
# - 테스트 빌드 버튼
# - 알림 수신

# 완료 후 터미널 1에서 Ctrl+C
```

### 3단계: 로그 분석

**정상 케이스** (기대되는 로그):
```
UrlAnalysisVM: UrlClassifier 초기화 시작...
UrlAnalysisVM: ✅ UrlClassifier 초기화 성공!
...
UrlAnalysisVM: runLocalAnalysis 시작: url=https://google.com
UrlAnalysisVM:   classifier 상태: OK
...
UrlClassifier: ========== PREDICT START ==========
UrlClassifier: predict: https://google.com
UrlClassifier:   RAW OUTPUT (모델 직접 출력): 0.123
UrlClassifier:   ML raw: 12% → calibrated: 8%
UrlClassifier: ========== PREDICT END ==========
...
UrlAnalysisVM:   ✅ predict() 성공: score=0.08
...
ResponseMapper: mapToAnalysisDetails: actualLocalMlScore=0.08, backendScore=0.5, using=0.08
```

**문제 케이스 1: classifier가 null**
```
UrlAnalysisVM: UrlClassifier 초기화 시작...
UrlAnalysisVM: ❌ UrlClassifier 초기화 실패: [에러 메시지]
...
UrlAnalysisVM:   classifier 상태: NULL
UrlAnalysisVM:   ❌ classifier가 null! 기본값 0.5 반환
```
→ **원인**: ML 모델 파일(phishing_model.tflite) 문제 또는 TensorFlow Lite 초기화 실패

**문제 케이스 2: predict() 실패**
```
UrlAnalysisVM:   classifier 상태: OK
UrlAnalysisVM:   ❌ predict() 실패, 기본값 0.5 사용: [에러 메시지]
```
→ **원인**: ML 모델 추론 중 예외 발생

**문제 케이스 3: 로그 자체가 없음**
→ **원인**: 새 APK가 설치되지 않음 (이전 버전 실행 중)

## 🔧 문제별 해결 방법

### Case 1: classifier 초기화 실패
**로그 확인**:
```bash
adb logcat | grep -i "tflite\|tensorflow"
```

**해결**:
1. APK에 ML 모델 포함 확인:
   ```bash
   unzip -l app-debug.apk | grep tflite
   ```
   → `assets/phishing_model.tflite` 있어야 함

2. 없다면 assets 폴더 확인:
   ```bash
   ls -la mobile/app/src/main/assets/
   ```

### Case 2: predict() 실패
**로그 확인**:
```bash
adb logcat | grep "predict"
```

**해결**:
- 에러 메시지 확인
- 입력 URL 형식 확인
- ML 모델 버전 호환성 확인

### Case 3: 새 APK가 설치 안 됨
**확인**:
```bash
# APK 버전 확인
adb shell dumpsys package com.mop.phishing | grep versionCode

# APK 빌드 시간과 비교
stat app-debug.apk | grep Modify
```

**해결**:
```bash
# 강제 재설치
adb uninstall com.mop.phishing
adb install app-debug.apk
```

## 📤 로그 보내기

로그 파일(`ml_debug.log`)을 확인하고:

1. **정상 동작하면**: 로그 첫 50줄 공유
2. **여전히 50%면**: 전체 로그 공유

로그 확인:
```bash
cat ml_debug.log | head -50
```

## 🎯 체크리스트

- [ ] 앱 완전 삭제 후 재설치
- [ ] adb 연결 확인
- [ ] 로그 캡처 시작
- [ ] 앱에서 URL 분석 (QR/직접입력/테스트/알림 - 각각 테스트)
- [ ] 로그 확인 (classifier 초기화, predict 성공 여부)
- [ ] UI에서 ML 점수 확인 (50%인지 다른 값인지)
- [ ] 로그 파일 저장

---

**빌드 정보**:
- 날짜: 2025-12-16 17:55
- 버전: v1.2 (디버그 로그 추가)
- 파일: app-debug.apk (42MB)

**다음 단계**: 
위 로그를 확인해서 정확한 원인을 파악한 후 최종 수정
