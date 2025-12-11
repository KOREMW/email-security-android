import re
from urllib.parse import urlparse

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report
import joblib  # 모델 저장/로드


# ===========================
# 1. URL feature 추출 함수
# ===========================
def extract_url_features(url: str) -> dict:
    parsed = urlparse(url)
    length = len(url)
    num_digits = sum(c.isdigit() for c in url)
    num_special = sum(not c.isalnum() for c in url)
    host = parsed.netloc
    has_ip = 1 if re.fullmatch(r"[0-9\\.]+", host) else 0
    suspicious_words = ["login", "verify", "update", "secure", "account", "bank"]
    lower_url = url.lower()
    has_suspicious_word = 1 if any(word in lower_url for word in suspicious_words) else 0
    return {
        "length": length,
        "num_digits": num_digits,
        "num_special": num_special,
        "has_ip": has_ip,
        "has_suspicious_word": has_suspicious_word,
    }


# ===========================
# 2. 학습, 모델 저장
# ===========================
def train_and_save():
    DATA_PATH = "data/dataset.csv"
    df_features = pd.read_csv(DATA_PATH)
    X = df_features.drop(columns=["label"])
    y = df_features["label"]
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.4, random_state=42
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    model = LogisticRegression(max_iter=1000)
    model.fit(X_train_scaled, y_train)

    # 저장 (모델, scaler 둘 다)
    joblib.dump(model, "data/model.joblib")
    joblib.dump(scaler, "data/scaler.joblib")
    print("모델/스케일러 저장 완료")

    y_pred = model.predict(X_test_scaled)
    print("정확도:", accuracy_score(y_test, y_pred))
    print("--- Classification report ---")
    print(classification_report(y_test, y_pred))


# ===========================
# 3. 모델/스케일러 로드 + 단일 URL 예측
# ===========================
def predict_url(url: str):
    # 저장된 모델, scaler 로드
    model = joblib.load("data/model.joblib")
    scaler = joblib.load("data/scaler.joblib")

    # URL → feature → scaling
    feat = extract_url_features(url)
    X_input = pd.DataFrame([feat])
    X_scaled = scaler.transform(X_input)

    pred = model.predict(X_scaled)[0]
    print(f"입력 URL: {url}")
    print(f"예측 결과: {pred} (0=정상, 1=피싱)")


if __name__ == "__main__":
    # 1) 학습 & 저장 (처음 한 번 실행)
    train_and_save()

    # 2) 예측 테스트 (원하는 URL로 바꿔서 테스트)
    test_url = "https://secure-login.example-bank.com/account/update?user=abc123"
    predict_url(test_url)
