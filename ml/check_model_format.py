"""
Check TFLite model input/output format and test inference
"""

import tensorflow as tf
import numpy as np
import json

def check_model_format():
    print("=" * 60)
    print("TFLite Model Format Check")
    print("=" * 60)

    # Load model
    model_path = "data/phishing_model_v10.tflite"
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    # Get input details
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("\n=== INPUT TENSOR ===")
    for i, detail in enumerate(input_details):
        print(f"Input {i}:")
        print(f"  Name: {detail['name']}")
        print(f"  Shape: {detail['shape']}")
        print(f"  Dtype: {detail['dtype']}")
        print(f"  Index: {detail['index']}")

    print("\n=== OUTPUT TENSOR ===")
    for i, detail in enumerate(output_details):
        print(f"Output {i}:")
        print(f"  Name: {detail['name']}")
        print(f"  Shape: {detail['shape']}")
        print(f"  Dtype: {detail['dtype']}")
        print(f"  Index: {detail['index']}")

    # Load scaler config
    with open("data/scaler_config_v10.json", "r") as f:
        scaler = json.load(f)

    mean = np.array(scaler["mean"], dtype=np.float32)
    scale = np.array(scaler["scale"], dtype=np.float32)

    print("\n=== SCALER CONFIG ===")
    print(f"Mean: {mean}")
    print(f"Scale: {scale}")

    # Test inference with google.com features
    print("\n=== TEST INFERENCE: google.com ===")

    # Raw features for "https://www.google.com"
    raw_features = np.array([
        22,     # urlLength
        14,     # domainLength  (www.google.com)
        0,      # pathLength
        1,      # subdomainCount (www)
        1,      # hasHttps
        0,      # hasPort
        0,      # paramCount
        0,      # digitRatio
        0.0909, # specialCharRatio (2 dots / 22)
        0,      # hasIpAddress
        0,      # hasSuspiciousKeyword
        0,      # hasSuspiciousTld
        2.9219  # entropy of "www.google.com"
    ], dtype=np.float32)

    print(f"Raw features: {raw_features}")

    # Normalize
    normalized = (raw_features - mean) / scale
    print(f"Normalized features: {normalized}")

    # Reshape for model input
    input_data = normalized.reshape(1, -1).astype(np.float32)
    print(f"Input shape: {input_data.shape}")
    print(f"Input dtype: {input_data.dtype}")

    # Run inference
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])

    print(f"Raw output: {output_data}")
    print(f"Output shape: {output_data.shape}")
    print(f"Prediction: {output_data[0][0] * 100:.1f}%")

    # Test with phishing features
    print("\n=== TEST INFERENCE: phishing URL ===")

    # Raw features for a phishing URL
    raw_features_phishing = np.array([
        45,     # urlLength
        28,     # domainLength
        10,     # pathLength
        2,      # subdomainCount
        0,      # hasHttps (HTTP)
        0,      # hasPort
        0,      # paramCount
        0.1,    # digitRatio
        0.1,    # specialCharRatio
        0,      # hasIpAddress
        1,      # hasSuspiciousKeyword
        1,      # hasSuspiciousTld
        3.8     # entropy
    ], dtype=np.float32)

    print(f"Raw features: {raw_features_phishing}")

    normalized_phishing = (raw_features_phishing - mean) / scale
    print(f"Normalized features: {normalized_phishing}")

    input_data_phishing = normalized_phishing.reshape(1, -1).astype(np.float32)

    interpreter.set_tensor(input_details[0]['index'], input_data_phishing)
    interpreter.invoke()
    output_data_phishing = interpreter.get_tensor(output_details[0]['index'])

    print(f"Raw output: {output_data_phishing}")
    print(f"Prediction: {output_data_phishing[0][0] * 100:.1f}%")

    # Test with all zeros
    print("\n=== TEST INFERENCE: all zeros (normalized) ===")
    input_zeros = np.zeros((1, 13), dtype=np.float32)
    interpreter.set_tensor(input_details[0]['index'], input_zeros)
    interpreter.invoke()
    output_zeros = interpreter.get_tensor(output_details[0]['index'])
    print(f"All zeros -> Output: {output_zeros[0][0] * 100:.1f}%")

    # Test with feature that should cause 50%
    print("\n=== INVESTIGATE 50% OUTPUT ===")
    # What input would cause exactly 50%?
    # For sigmoid output, 50% means logit = 0
    # Let's see what happens with mean values (should give ~50% if model learned well)
    input_mean = np.zeros((1, 13), dtype=np.float32)  # Normalized mean = 0
    interpreter.set_tensor(input_details[0]['index'], input_mean)
    interpreter.invoke()
    output_mean = interpreter.get_tensor(output_details[0]['index'])
    print(f"Mean input (all 0s normalized) -> Output: {output_mean[0][0] * 100:.1f}%")

if __name__ == "__main__":
    check_model_format()
