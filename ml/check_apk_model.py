"""
Check if APK contains the correct model file
"""
import zipfile
import hashlib
import os

apk_path = r"C:\Users\minwo\email-security-android-main(2)\mobile\app\build\outputs\apk\debug\app-debug.apk"
source_model_path = r"C:\Users\minwo\email-security-android-main(2)\mobile\app\src\main\assets\phishing_model.tflite"
v10_model_path = r"C:\Users\minwo\email-security-android-main(2)\ml\data\phishing_model_v10.tflite"

def get_file_hash(filepath):
    with open(filepath, 'rb') as f:
        return hashlib.md5(f.read()).hexdigest()

def main():
    print("=" * 60)
    print("APK Model File Check")
    print("=" * 60)

    # Check source model
    print(f"\n1. Source model (assets folder):")
    print(f"   Path: {source_model_path}")
    if os.path.exists(source_model_path):
        source_hash = get_file_hash(source_model_path)
        print(f"   MD5: {source_hash}")
        print(f"   Size: {os.path.getsize(source_model_path)} bytes")
    else:
        print("   NOT FOUND!")
        source_hash = None

    # Check v10 model
    print(f"\n2. V10 model (ml/data folder):")
    print(f"   Path: {v10_model_path}")
    if os.path.exists(v10_model_path):
        v10_hash = get_file_hash(v10_model_path)
        print(f"   MD5: {v10_hash}")
        print(f"   Size: {os.path.getsize(v10_model_path)} bytes")
    else:
        print("   NOT FOUND!")
        v10_hash = None

    # Check APK
    print(f"\n3. APK model:")
    print(f"   APK: {apk_path}")

    if not os.path.exists(apk_path):
        print("   APK NOT FOUND!")
        return

    with zipfile.ZipFile(apk_path, 'r') as z:
        # List all files in APK
        all_files = z.namelist()

        # Find model file
        model_files = [f for f in all_files if 'phishing_model' in f.lower() or 'tflite' in f.lower()]
        print(f"   Model files in APK: {model_files}")

        # Check assets folder
        assets_files = [f for f in all_files if f.startswith('assets/')]
        print(f"   Assets folder contents: {assets_files}")

        # Extract and check model
        if 'assets/phishing_model.tflite' in all_files:
            model_data = z.read('assets/phishing_model.tflite')
            apk_hash = hashlib.md5(model_data).hexdigest()
            print(f"   MD5: {apk_hash}")
            print(f"   Size: {len(model_data)} bytes")

            # Compare hashes
            print(f"\n4. Hash Comparison:")
            print(f"   Source = V10: {source_hash == v10_hash}")
            print(f"   APK = Source: {apk_hash == source_hash}")
            print(f"   APK = V10: {apk_hash == v10_hash}")

            if apk_hash == v10_hash:
                print("\n   ✅ APK contains correct V10 model!")
            else:
                print("\n   ❌ APK model DOES NOT match V10 model!")
        else:
            print("   phishing_model.tflite NOT FOUND in APK!")

if __name__ == "__main__":
    main()
