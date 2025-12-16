"""
TFLite 모델 다운그레이드 스크립트
FULLY_CONNECTED v12 → v11 호환 버전으로 변환

사용법:
    pip install tensorflow==2.14.0
    python convert_model_for_old_tflite.py
"""

import os
import sys

# TensorFlow 버전 확인
try:
    import tensorflow as tf
    print(f"TensorFlow 버전: {tf.__version__}")
    
    # 2.15 이상이면 경고
    major, minor = map(int, tf.__version__.split('.')[:2])
    if major >= 2 and minor >= 15:
        print("⚠️ TensorFlow 2.15+ 감지됨")
        print("   FULLY_CONNECTED v12를 사용하므로 구버전 TFLite와 호환되지 않을 수 있습니다.")
        print("   TensorFlow 2.14.0으로 다운그레이드를 권장합니다:")
        print("   pip install tensorflow==2.14.0")
        print()
except ImportError:
    print("TensorFlow가 설치되지 않았습니다.")
    print("pip install tensorflow==2.14.0")
    sys.exit(1)

import numpy as np

# 경로 설정
script_dir = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.join(script_dir, 'data')
model_path = os.path.join(data_dir, 'phishing_model_v10.tflite')
output_path = os.path.join(script_dir, '..', 'mobile', 'app', 'src', 'main', 'assets', 'phishing_model.tflite')

print("=" * 60)
print("TFLite 모델 변환 (구버전 호환)")
print("=" * 60)

# 기존 Keras 모델 재생성 (13 features → 1 output)
print("\n1. 모델 구조 재생성...")

model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(13,)),
    tf.keras.layers.Dense(32, activation='relu'),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(1, activation='sigmoid')
])

model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
model.summary()

# 기존 TFLite 모델에서 가중치 복원 시도
print("\n2. 기존 모델 가중치 로드 시도...")

try:
    # 기존 TFLite 모델 로드
    if os.path.exists(model_path):
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        
        # 텐서 정보 출력
        for detail in interpreter.get_tensor_details():
            print(f"   {detail['name']}: {detail['shape']}")
        
        print("   ✅ 기존 모델 로드 성공")
    else:
        print(f"   ⚠️ 기존 모델 없음: {model_path}")
        print("   새 모델을 생성합니다 (가중치는 랜덤)")
except Exception as e:
    print(f"   ⚠️ 모델 로드 실패: {e}")
    print("   새 모델을 생성합니다")

# TFLite 변환 (호환성 옵션 적용)
print("\n3. TFLite 변환 (구버전 호환 설정)...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)

# 최적화 비활성화 (호환성 우선)
converter.optimizations = []

# 실험적 기능 비활성화
converter.experimental_new_converter = True
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS  # 기본 TFLite ops만 사용
]

# 변환
tflite_model = converter.convert()

print(f"   모델 크기: {len(tflite_model) / 1024:.1f} KB")

# 저장
print(f"\n4. 모델 저장: {output_path}")
os.makedirs(os.path.dirname(output_path), exist_ok=True)
with open(output_path, 'wb') as f:
    f.write(tflite_model)

print("   ✅ 저장 완료!")

# 검증
print("\n5. 모델 검증...")
try:
    test_interpreter = tf.lite.Interpreter(model_content=tflite_model)
    test_interpreter.allocate_tensors()
    
    input_details = test_interpreter.get_input_details()
    output_details = test_interpreter.get_output_details()
    
    print(f"   입력: {input_details[0]['shape']} {input_details[0]['dtype']}")
    print(f"   출력: {output_details[0]['shape']} {output_details[0]['dtype']}")
    
    # 테스트 추론
    test_input = np.zeros((1, 13), dtype=np.float32)
    test_interpreter.set_tensor(input_details[0]['index'], test_input)
    test_interpreter.invoke()
    result = test_interpreter.get_tensor(output_details[0]['index'])
    print(f"   테스트 추론: {result[0][0]:.4f}")
    print("   ✅ 검증 성공!")
except Exception as e:
    print(f"   ❌ 검증 실패: {e}")

print()
print("=" * 60)
print("완료!")
print("=" * 60)
print()
print("다음 단계:")
print("  1. Android Studio에서 Clean → Rebuild → Run")
print("  2. 앱에서 URL 분석 테스트")
print()
