#!/bin/bash

echo "==================================="
echo "긴급 디버깅 스크립트"
echo "==================================="
echo ""

# 1. APK 버전 확인
echo "1. 빌드된 APK 확인:"
ls -lh "app/build/outputs/apk/debug/app-debug.apk" 2>/dev/null || echo "  APK 파일 없음!"
echo ""

# 2. 패키지 이름 확인
echo "2. 앱 패키지 이름:"
grep "applicationId" app/build.gradle.kts || grep "applicationId" app/build.gradle
echo ""

# 3. 로그 캡처 가이드
echo "3. 로그 캡처 방법:"
echo "  다음 명령어를 실행하세요:"
echo ""
echo "  adb logcat -c  # 기존 로그 삭제"
echo "  adb logcat | grep -E 'UrlClassifier|UrlAnalysisVM|ResponseMapper|MailNotificationListener' > debug.log"
echo ""
echo "  그 다음 앱에서 URL 분석 후 Ctrl+C로 중단하고:"
echo "  cat debug.log"
echo ""

# 4. 설치된 앱 확인
echo "4. 설치된 앱 확인:"
echo "  adb shell pm list packages | grep phishing"
echo ""

# 5. APK 재설치 명령어
echo "5. 완전 재설치 명령어:"
echo "  adb uninstall com.mop.phishing"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""

echo "==================================="
