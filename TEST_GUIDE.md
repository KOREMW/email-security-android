# ML 점수 50% 문제 디버깅 가이드

## 테스트 준비
1. 앱 완전 삭제
2. 앱 데이터/캐시 모두 삭제
3. 새로 빌드된 APK 설치

## 로그 확인 방법

### 1. Logcat 필터링
```bash
adb logcat -c  # 기존 로그 삭제
adb logcat | grep -E "UrlClassifier|UrlAnalysisVM|ResponseMapper"
```

### 2. 기대되는 로그 순서

#### A. URL 분석 시작
```
UrlAnalysisVM: 🧠 온디바이스 ML 분석 시작: https://example.com
```

#### B. ML 모델 실행
```
UrlClassifier: ========== PREDICT START ==========
UrlClassifier: predict: https://example.com
UrlClassifier:   VERSION: v10.2-DEBUG
UrlClassifier:   FEATURES (13개): [...]
UrlClassifier:   NORMALIZED (13개): [...]
UrlClassifier:   RAW OUTPUT (모델 직접 출력): 0.XXX
UrlClassifier:   ML raw: XX% → calibrated: XX%
UrlClassifier: ========== PREDICT END ==========
```

#### C. ViewModel에서 점수 확인
```
UrlAnalysisVM: 🧠 온디바이스 ML 분석 완료: 점수=0.XXX (XX%)
```

#### D. ResponseMapper에서 점수 사용
```
ResponseMapper: mapToAnalysisDetails: actualLocalMlScore=0.XXX, backendScore=0.5, using=0.XXX
```

#### E. 최종 결과
```
UrlAnalysisVM: ✅ 분석 완료: riskLevel=XXX, riskScore=XX, localMlScore=0.XXX
```

## 문제 진단

### Case 1: ML 모델이 실행되지 않음
- UrlClassifier 로그가 없음
- → ML 모델 파일(phishing_model.tflite) 누락 확인

### Case 2: ML 모델은 실행되지만 50% 고정
- UrlClassifier 로그에서 "RAW OUTPUT"이 항상 0.5
- → ML 모델 자체 문제 또는 입력 데이터 문제

### Case 3: ML 모델 결과는 정상이지만 UI에 50%
- UrlClassifier: calibrated: 25% (정상)
- ResponseMapper: using=0.5 (문제!)
- → actualLocalMlScore가 null로 전달됨

### Case 4: 캐시된 데이터 사용
- 로그에 "캐시된 분석 결과 사용" 메시지
- → 전체 재분석 버튼 클릭 필요

## 예상 원인별 해결 방법

### 원인 1: 이전 빌드 APK 설치됨
**증상**: ResponseMapper 로그가 없음
**해결**:
```bash
adb uninstall com.mop.phishing
adb install app-debug.apk
```

### 원인 2: ML 모델 파일 누락
**증상**: UrlClassifier 로그가 없거나 에러
**확인**:
```bash
# APK 내부 assets 확인
unzip -l app-debug.apk | grep tflite
```

### 원인 3: localRiskScore가 null로 전달
**증상**: ResponseMapper 로그에 actualLocalMlScore=null
**확인**: ViewModel 코드 237번 라인 확인

### 원인 4: 캐시 사용
**증상**: "캐시된 분석 결과 사용" 로그
**해결**: 앱에서 "전체 재분석" 버튼 클릭

## 빠른 테스트 절차

1. **로그 캡처 시작**
   ```bash
   adb logcat -c
   adb logcat | grep -E "UrlClassifier|UrlAnalysisVM|ResponseMapper" > ml_test_log.txt
   ```

2. **앱에서 URL 분석**
   - 테스트 URL: https://google.com
   - 분석 시작

3. **로그 확인**
   ```bash
   cat ml_test_log.txt
   ```

4. **로그를 이 파일에 복사해서 분석 요청**
