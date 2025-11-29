// functions/index.js

const { setGlobalOptions } = require("firebase-functions/v2");
const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

setGlobalOptions({ maxInstances: 10 });

// 예제 함수
exports.helloWorld = onRequest((request, response) => {
  logger.info("Hello logs!", { structuredData: true });
  response.send("Hello from Firebase!");
});

// 1) 헬스 체크
exports.health = onRequest((request, response) => {
  response.set("Access-Control-Allow-Origin", "*");
  response.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  response.set("Access-Control-Allow-Headers", "Content-Type");

  if (request.method === "OPTIONS") {
    return response.status(204).send("");
  }

  if (request.method !== "GET") {
    return response.status(405).json({ error: "Method Not Allowed" });
  }

  return response.status(200).json({
    status: "ok",
    version: "1.0.0",
    timestamp: new Date().toISOString(),
  });
});

// 2) 의심 URL 분석 (더미)
exports.analyzeSuspiciousUrl = onRequest(async (request, response) => {
  response.set("Access-Control-Allow-Origin", "*");
  response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  response.set("Access-Control-Allow-Headers", "Content-Type");

  if (request.method === "OPTIONS") {
    return response.status(204).send("");
  }

  if (request.method !== "POST") {
    return response.status(405).json({ error: "Method Not Allowed" });
  }

  try {
    const body = request.body || {};

    const url = body.url;
    const source = body.source || "manual";
    const localRiskScore = body.localRiskScore ?? null;

    if (!url || typeof url !== "string") {
      return response.status(400).json({ error: "url is required" });
    }

    const dummyResult = {
      suspicious: true,
      finalRiskScore: localRiskScore ?? 0.8,
      virusTotal: {
        malicious: 3,
        harmless: 40,
        undetected: 10,
      },
      redirects: [
        url,
        "http://example-redirect.com/",
      ],
      error: null,
      debug: {
        source,
      },
    };

    return response.status(200).json(dummyResult);
  } catch (err) {
    console.error(err);
    return response.status(500).json({
      error: "internal_error",
      message: err.message || String(err),
    });
  }
});

// 3) AI 분석(LLM)용 엔드포인트 – 더미
exports.getAIAnalysis = onRequest(async (request, response) => {
  response.set("Access-Control-Allow-Origin", "*");
  response.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  response.set("Access-Control-Allow-Headers", "Content-Type");

  if (request.method === "OPTIONS") {
    return response.status(204).send("");
  }

  if (request.method !== "POST") {
    return response.status(405).json({ error: "Method Not Allowed" });
  }

  try {
    const body = request.body || {};

    const url = body.url;
    const finalRiskScore = body.finalRiskScore ?? null;
    const virusTotal = body.virusTotal || null;

    if (!url || typeof url !== "string") {
      return response.status(400).json({ error: "url is required" });
    }

    // TODO: 여기서 나중에 실제 LLM(OpenAI/Gemini 등) 호출할 예정
    // 지금은 더미 메시지 반환
    const summary = "이 URL은 피싱 사이트일 가능성이 높습니다.";
    const reasons = [
      "의심스러운 도메인 구조 및 로그인 관련 단어 포함",
      "보안 엔진에서 일부 악성으로 탐지됨 (VirusTotal 정보 기반)",
    ];
    const recommendation =
      "공식 사이트/앱을 통해 접속하고, 이 링크로는 로그인/결제 정보를 입력하지 마세요.";

    const result = {
      summary,
      reasons,
      recommendation,
      echo: {
        url,
        finalRiskScore,
        virusTotal,
      },
      error: null,
    };

    return response.status(200).json(result);
  } catch (err) {
    console.error(err);
    return response.status(500).json({
      error: "internal_error",
      message: err.message || String(err),
    });
  }
});
