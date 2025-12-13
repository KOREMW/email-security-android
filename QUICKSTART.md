# 🚀 팀원 빠른 시작 가이드

## 5분 안에 앱 실행하기!

### Step 1: 저장소 클론
```bash
git clone https://github.com/YOUR_REPO/email-security-android.git
cd email-security-android
```

### Step 2: google-services.json 받기
팀장에게 `google-services.json` 파일 요청 후:
```
mobile/app/google-services.json  ← 여기에 복사!
```

### Step 3: Android Studio에서 열기
1. Android Studio 실행
2. `File` → `Open` → `mobile` 폴더 선택
3. Gradle Sync 완료 대기 (2-3분)

### Step 4: 실행
▶️ Run 버튼 클릭!

---

## ❓ 문제 발생 시

### "google-services.json not found"
→ Step 2 확인. 팀장에게 파일 요청

### "SHA-1 오류" (Google 로그인 실패)
```bash
cd mobile
./gradlew signingReport
```
→ 출력된 SHA-1을 팀장에게 전달

### Gradle Sync 실패
```bash
cd mobile
./gradlew clean
./gradlew --refresh-dependencies
```

---

## 📁 받아야 할 파일 목록

| 파일 | 위치 | 누구에게? |
|------|------|----------|
| google-services.json | mobile/app/ | 팀장 |
| (선택) .env | backend/functions/ | 팀장 (Backend 작업 시) |

---

**질문 있으면 팀 채팅방에 문의하세요! 🙋**
