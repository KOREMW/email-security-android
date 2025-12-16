import re
from urllib.parse import urlparse

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (
    confusion_matrix,
    classification_report,
    precision_score,
    recall_score,
    f1_score,
)

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers


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


def load_dataset(path="data/dataset.csv"):
    df = pd.read_csv(path)
    X = df.drop(columns=["label"]).values.astype(np.float32)
    y = df["label"].values.astype(np.float32)
    return X, y


def build_model(input_dim: int):
    model = keras.Sequential(
        [
            layers.Input(shape=(input_dim,)),
            layers.Dense(16, activation="relu"),
            layers.Dense(8, activation="relu"),
            layers.Dense(1, activation="sigmoid"),
        ]
    )
    model.compile(
        optimizer="adam",
        loss="binary_crossentropy",
        metrics=["accuracy"],
    )
    return model


def train_and_export():
    X, y = load_dataset("data/dataset.csv")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.4, random_state=42
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    model = build_model(input_dim=X_train.shape[1])
    model.summary()

    model.fit(
        X_train_scaled,
        y_train,
        epochs=20,
        batch_size=8,
        validation_data=(X_test_scaled, y_test),
        verbose=1,
    )

    # 확률 예측
    y_prob = model.predict(X_test_scaled).ravel()

    # 여러 threshold에서 지표 확인
    for thr in [0.3, 0.5, 0.7, 0.9]:
        y_pred = (y_prob >= thr).astype(int)
        acc = (y_pred == y_test).mean()
        prec = precision_score(y_test, y_pred, zero_division=0)
        rec = recall_score(y_test, y_pred, zero_division=0)
        f1 = f1_score(y_test, y_pred, zero_division=0)
        print(f"\n=== threshold = {thr} ===")
        print(f"정확도: {acc:.4f}")
        print(f"정밀도(precision): {prec:.4f}")
        print(f"재현율(recall): {rec:.4f}")
        print(f"F1-score: {f1:.4f}")
        print("혼동 행렬 (TN, FP; FN, TP):")
        print(confusion_matrix(y_test, y_pred))

    # 모델 / 스케일러 / TFLite 저장 (이 부분은 그대로 유지)
    keras_model_path = "data/keras_url_model.keras"
    model.save(keras_model_path)
    print(f"\nKeras 모델 저장 완료: {keras_model_path}")

    np.save("data/scaler_mean.npy", scaler.mean_)
    np.save("data/scaler_scale.npy", scaler.scale_)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    tflite_path = "data/url_model.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"TFLite 모델 저장 완료: {tflite_path}")


if __name__ == "__main__":
    train_and_export()
