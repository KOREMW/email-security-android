 # Cloud Function API Specification

이 문서는 안드로이드 앱과 Firebase Cloud Functions 백엔드 사이에서
주고받는 HTTP API 스펙을 정리한 문서입니다.

---

## 1. GET /health

서버 상태를 확인하는 엔드포인트입니다.

### Request

- Method: GET  
- Body: 없음

### Response (예시)
{
"status": "ok",
"version": "1.0.0",
"timestamp": "2025-11-29T12:00:00.000Z"
}

---

## 2. POST /analyzeSuspiciousUrl

의심스러운 URL을 서버로 보내 분석 결과를 받는 엔드포인트입니다.

### Request

- Method: POST  
- Header: `Content-Type: application/json`  
- Body (예시):
{
"url": "https://secure-login.example-bank.com",
"source": "notification",
"localRiskScore": 0.73
}

필드 설명:
- `url` (string, required): 분석할 대상 URL
- `source` (string, optional): URL이 어디서 왔는지  
  - 예: `"manual" | "notification" | "qr"`
- `localRiskScore` (number, optional): 온디바이스 TFLite 모델이 계산한 위험 점수

### Response (예시)
{
"suspicious": true,
"finalRiskScore": 0.73,
"virusTotal": {
"malicious": 3,
"harmless": 40,
"undetected": 10
},
"redirects": [
"https://secure-login.example-bank.com",
"http://example-redirect.com/"
],
"error": null,
"debug": {
"source": "notification"
}
}

필드 설명:
- `suspicious` (boolean): 피싱 가능성이 높은지 여부
- `finalRiskScore` (number): 최종 위험 점수 (0.0 ~ 1.0)
- `virusTotal` (object): VirusTotal 요약 정보 (현재는 더미 데이터)
- `redirects` (string[]): 리다이렉트 체인에 포함된 URL 목록
- `error` (string | null): 오류 메시지 (정상일 때는 null)
- `debug.source` (string): 요청 시 전달된 source 값

---

## 3. POST /getAIAnalysis

URL 분석 결과를 기반으로 LLM이 생성한 설명/추천을 받는 엔드포인트입니다.

### Request

- Method: POST  
- Header: `Content-Type: application/json`  
- Body (예시):
{
"url": "https://secure-login.example-bank.com",
"finalRiskScore": 0.91,
"virusTotal": {
"malicious": 8,
"harmless": 30,
"undetected": 52
}
}

필드 설명:
- `url` (string, required): 분석 대상 URL
- `finalRiskScore` (number, optional): 서버에서 계산한 최종 위험 점수
- `virusTotal` (object, optional): VirusTotal 검사 결과 요약

### Response (예시)
{
"summary": "이 URL은 피싱 사이트일 가능성이 높습니다.",
"reasons": [
"의심스러운 도메인 구조 및 로그인 관련 단어 포함",
"보안 엔진에서 일부 악성으로 탐지됨 (VirusTotal 정보 기반)"
],
"recommendation": "공식 사이트/앱을 통해 접속하고, 이 링크로는 로그인/결제 정보를 입력하지 마세요.",
"echo": {
"url": "https://secure-login.example-bank.com",
"finalRiskScore": 0.91,
"virusTotal": {
"malicious": 8,
"harmless": 30,
"undetected": 52
}
},
"error": null
}

필드 설명:
- `summary` (string): URL 위험도에 대한 한 줄 요약
- `reasons` (string[]): 위험하다고 판단한 이유 목록
- `recommendation` (string): 사용자 행동 가이드
- `echo` (object): 요청에서 받은 주요 데이터를 그대로 되돌려줌
- `error` (string | null): 오류 메시지 (정상일 때는 null)

