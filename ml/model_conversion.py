"""
Keras 모델 → TFLite 변환 스크립트
Android assets 폴더에 복사할 파일들을 생성합니다.
"""

import json
import numpy as np
import tensorflow as tf
from tensorflow import keras


def convert_keras_to_tflite():
    """Keras 모델을 TFLite로 변환"""
    print("=" * 50)
    print("Keras → TFLite 변환 시작")
    print("=" * 50)

    # 1. Keras 모델 로드
    keras_model_path = "data/keras_url_model.keras"
    print(f"\n[1/4] Keras 모델 로드: {keras_model_path}")
    model = keras.models.load_model(keras_model_path)
    model.summary()

    # 2. TFLite 변환
    print("\n[2/4] TFLite 변환 중...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # 최적화 옵션 (선택사항)
    # converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    tflite_model = converter.convert()

    # 3. TFLite 모델 저장
    tflite_path = "data/url_model.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"[3/4] TFLite 모델 저장 완료: {tflite_path}")
    print(f"      파일 크기: {len(tflite_model) / 1024:.2f} KB")

    # 4. Scaler 정보를 JSON으로 저장 (Android에서 쉽게 로드)
    print("\n[4/4] Scaler 정보 저장 중...")
    scaler_mean = np.load("data/scaler_mean.npy")
    scaler_scale = np.load("data/scaler_scale.npy")

    scaler_info = {
        "mean": scaler_mean.tolist(),
        "scale": scaler_scale.tolist(),
        "feature_names": [
            "length",
            "num_digits",
            "num_special",
            "has_ip",
            "has_suspicious_word"
        ]
    }

    scaler_json_path = "data/scaler_params.json"
    with open(scaler_json_path, "w") as f:
        json.dump(scaler_info, f, indent=2)
    print(f"      Scaler 파라미터 저장: {scaler_json_path}")

    print("\n" + "=" * 50)
    print("변환 완료!")
    print("=" * 50)
    print("\n다음 파일들을 Android assets 폴더에 복사하세요:")
    print(f"  1. {tflite_path}")
    print(f"  2. {scaler_json_path}")
    print("\n복사 경로:")
    print("  mobile/app/src/main/assets/")


def verify_tflite_model():
    """TFLite 모델 검증"""
    print("\n" + "=" * 50)
    print("TFLite 모델 검증")
    print("=" * 50)

    # 모델 로드
    interpreter = tf.lite.Interpreter(model_path="data/url_model.tflite")
    interpreter.allocate_tensors()

    # 입출력 정보
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"\n입력 텐서 정보:")
    print(f"  Shape: {input_details[0]['shape']}")
    print(f"  Type: {input_details[0]['dtype']}")

    print(f"\n출력 텐서 정보:")
    print(f"  Shape: {output_details[0]['shape']}")
    print(f"  Type: {output_details[0]['dtype']}")

    # 테스트 예측
    print("\n테스트 예측:")
    
    # Scaler 로드
    with open("data/scaler_params.json", "r") as f:
        scaler = json.load(f)
    
    mean = np.array(scaler["mean"], dtype=np.float32)
    scale = np.array(scaler["scale"], dtype=np.float32)

    # 테스트 URL 피처 (정상 URL 예시)
    test_features = np.array([[20, 2, 5, 0, 0]], dtype=np.float32)
    test_scaled = (test_features - mean) / scale

    interpreter.set_tensor(input_details[0]['index'], test_scaled)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    print(f"  정상 URL 피처: {test_features[0].tolist()}")
    print(f"  피싱 확률: {output[0][0]:.4f}")

    # 의심 URL 피처 (피싱 URL 예시)
    test_features2 = np.array([[80, 10, 20, 1, 1]], dtype=np.float32)
    test_scaled2 = (test_features2 - mean) / scale

    interpreter.set_tensor(input_details[0]['index'], test_scaled2)
    interpreter.invoke()
    output2 = interpreter.get_tensor(output_details[0]['index'])
    print(f"  의심 URL 피처: {test_features2[0].tolist()}")
    print(f"  피싱 확률: {output2[0][0]:.4f}")


if __name__ == "__main__":
    convert_keras_to_tflite()
    verify_tflite_model()
