"""
TFLite 모델 및 Scaler를 Android assets 폴더로 복사하는 스크립트
"""

import shutil
import os

def copy_to_assets():
    # 소스 파일 경로
    ml_data_dir = "data"
    model_file = os.path.join(ml_data_dir, "url_model.tflite")
    scaler_file = os.path.join(ml_data_dir, "scaler_params.json")
    
    # Android assets 폴더 경로 (상대 경로)
    assets_dir = "../mobile/app/src/main/assets"
    
    # assets 폴더가 없으면 생성
    os.makedirs(assets_dir, exist_ok=True)
    
    print("=" * 50)
    print("Android assets 폴더로 파일 복사")
    print("=" * 50)
    
    # 파일 존재 확인 및 복사
    files_to_copy = [
        (model_file, "url_model.tflite"),
        (scaler_file, "scaler_params.json")
    ]
    
    for src, filename in files_to_copy:
        if os.path.exists(src):
            dst = os.path.join(assets_dir, filename)
            shutil.copy2(src, dst)
            file_size = os.path.getsize(dst) / 1024
            print(f"✓ 복사 완료: {filename} ({file_size:.2f} KB)")
        else:
            print(f"✗ 파일 없음: {src}")
            print(f"  → 먼저 model_conversion.py를 실행하세요!")
    
    print("\n" + "=" * 50)
    print("복사 완료!")
    print("=" * 50)
    print(f"\nassets 폴더 경로: {os.path.abspath(assets_dir)}")
    print("\n이제 Android Studio에서 앱을 빌드하세요.")


if __name__ == "__main__":
    copy_to_assets()
