import re
from urllib.parse import urlparse

import pandas as pd


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


def build_feature_dataset(input_csv: str, output_csv: str):
    df = pd.read_csv(input_csv)

    feature_rows = []
    for _, row in df.iterrows():
        url = row["url"]
        label = row["label"]  # 0/1이라고 가정
        feats = extract_url_features(url)
        feats["label"] = label
        feature_rows.append(feats)

    df_out = pd.DataFrame(feature_rows)
    df_out.to_csv(output_csv, index=False)
    print(f"Saved feature dataset to: {output_csv}")
    print(df_out.head())


if __name__ == "__main__":
    INPUT = "data/raw_urls.csv"     # 원본 (url, label)
    OUTPUT = "data/dataset.csv"    # 피처가 들어간 최종 학습용
    build_feature_dataset(INPUT, OUTPUT)
