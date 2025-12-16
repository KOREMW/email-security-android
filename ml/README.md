# 피싱 URL 탐지 ML 모델 (v7)

## 빠른 시작

### 방법 1: 자동 Setup (권장)

```bash
cd ml
python setup_model.py
```

이 스크립트가 자동으로 다음을 수행합니다:
1. 공개 데이터셋 다운로드 (PhiUSIIL 235K URLs)
2. 200,000개 샘플 데이터셋 생성 (100K benign + 100K phishing)
3. 모델 학습 (약 5-10분)
4. TFLite 변환
5. Android assets 폴더에 복사

### 방법 2: 수동 실행

```bash
# 1. 데이터셋 생성
python build_dataset_v7.py

# 2. 모델 학습
python train_model_v7.py

# 3. 모델 복사 (수동)
# ml/data/phishing_model_v7.tflite → mobile/app/src/main/assets/phishing_model.tflite
```

## 요구사항

```
Python 3.8+
tensorflow >= 2.10
scikit-learn
pandas
numpy
```

설치:
```bash
pip install tensorflow scikit-learn pandas numpy
```

## 파일 구조

```
ml/
├── setup_model.py          # 자동 Setup 스크립트
├── build_dataset_v7.py     # 데이터셋 생성
├── train_model_v7.py       # 모델 학습
├── test_model_v7.py        # 모델 테스트
└── data/
    ├── top-1m.csv              # Tranco 상위 도메인 (자동 다운로드)
    ├── phishing_urls.csv       # PhishTank 피싱 URL (자동 다운로드)
    ├── dataset_v7.csv          # 생성된 학습 데이터셋
    ├── phishing_model_v7.keras # Keras 모델
    ├── phishing_model_v7.tflite# TFLite 모델
    └── scaler_config_v7.json   # 스케일러 파라미터
```

## 모델 성능

| 지표 | 값 |
|------|-----|
| 정확도 | 99.3% (검증셋) |
| 재현율 | 100% |
| 임계값 | 0.4 |

200,000개 학습 데이터 기준 (정상 100K + 피싱 100K)

### 참고
- Android 앱에서는 신뢰 도메인 화이트리스트가 적용되어 google.com, facebook.com 등은 자동으로 안전으로 분류됩니다.

## 모델 특징

### 13개 피처
1. `urlLength` - URL 전체 길이
2. `domainLength` - 도메인 길이
3. `pathLength` - 경로 길이
4. `subdomainCount` - 서브도메인 개수
5. `hasHttps` - HTTPS 사용 여부
6. `hasPort` - 비표준 포트 사용 여부
7. `paramCount` - 쿼리 파라미터 개수
8. `digitRatio` - 숫자 비율
9. `specialCharRatio` - 특수문자 비율
10. `hasIpAddress` - IP 주소 기반 여부
11. `hasSuspiciousKeyword` - 의심 키워드 포함 여부
12. `hasSuspiciousTld` - 의심 TLD 사용 여부
13. `entropy` - 도메인 엔트로피

### 데이터 소스
- **PhiUSIIL 데이터셋**: [UCI ML Repository](https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset)
  - 235,795개 URL (정상 134,850개 + 피싱 100,945개)
  - CC BY 4.0 라이선스
- **대체 소스** (PhiUSIIL 미사용 시):
  - 정상 URL: [Tranco Top 1M](https://tranco-list.eu/) 도메인
  - 피싱 URL: [PhishTank](https://www.phishtank.com/) 데이터

### 화이트리스트
신뢰할 수 있는 도메인은 자동으로 안전으로 분류:
- google.com, facebook.com, amazon.com, naver.com 등 25개 도메인

## Android 적용

### UrlClassifier.kt 업데이트

모델 재학습 후 `scaler_config_v7.json`의 값으로 업데이트:

```kotlin
private val scalerMean = floatArrayOf(
    36.81348125f,  // urlLength
    21.83995625f,  // domainLength
    // ... scaler_config_v7.json의 mean 값
)

private val scalerScale = floatArrayOf(
    46.6592769f,   // urlLength
    9.6527240f,    // domainLength
    // ... scaler_config_v7.json의 scale 값
)
```

## 문제 해결

### "모델 파일을 찾을 수 없음"
```bash
python setup_model.py
```
위 명령어로 모델을 생성하세요.

### "TensorFlow 설치 오류"
```bash
pip install --upgrade tensorflow
```

### "학습 데이터가 너무 적음"
`build_dataset_v7.py`의 `max_samples_per_class` 값을 조정:
```python
build_dataset(..., max_samples_per_class=20000)  # 기본값: 10000
```

## 라이선스

- Tranco 데이터: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- PhishTank 데이터: [Creative Commons](https://www.phishtank.com/developer_info.php)
