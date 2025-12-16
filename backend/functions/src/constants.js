// ============================================
// Firebase Cloud Functions 상수 정의 모듈
// ============================================

/**
 * Firebase Cloud Functions 설정
 * index.js에서 사용됨
 */
const FUNCTION_CONFIG = {
  REGION: 'us-central1',
  TIMEOUT: 540,
  MEMORY: '512MB',
  MAX_INSTANCES: 100
};

/**
 * ML 모델 설정 (모바일용 TFLite – 참고용)
 */
const ML_MODEL = {
  PATH: './models/phishing-detection-model.tflite',
  BATCH_SIZE_MAX: 100,
  INFERENCE_TIMEOUT_MS: 5000
};

/**
 * VirusTotal API 설정
 */
const VIRUS_TOTAL = {
  API_BASE_URL: 'https://www.virustotal.com/api/v3',
  API_TIMEOUT_MS: 10000,
  BATCH_SIZE: 40,
  MIN_DETECTION_CONFIDENCE: 10
};

/**
 * 앙상블 가중치
 * finalRiskScore = 0.4 * localMlScore + 0.6 * virusTotalScore
 */
const ENSEMBLE_WEIGHTS = {
  LOCAL_ML: 0.4,
  VIRUS_TOTAL: 0.6
};

/**
 * 위험도 레벨
 * SAFE: score < 0.3
 * SUSPICIOUS: 0.3 ≤ score < 0.7
 * DANGEROUS: score ≥ 0.7
 */
const RISK_LEVELS = {
  SAFE: {
    min: 0.0,
    max: 0.3,
    label: 'SAFE',
    description: '안전한 URL'
  },
  SUSPICIOUS: {
    min: 0.3,
    max: 0.7,
    label: 'SUSPICIOUS',
    description: '의심스러운 URL'
  },
  DANGEROUS: {
    min: 0.7,
    max: 1.0,
    label: 'DANGEROUS',
    description: '위험한 URL'
  }
};

/**
 * Firestore 컬렉션 설정
 */
const FIRESTORE_CONFIG = {
  COLLECTION_URLS: 'analyzed_urls',
  COLLECTION_CACHE: 'url_cache',
  COLLECTION_LOGS: 'analysis_logs',
  TTL_DAYS: 30
};

/**
 * URL 정규화 설정
 */
const URL_NORMALIZER = {
  MAX_REDIRECTS: 5,
  REDIRECT_TIMEOUT: 5000
};

/**
 * URL 관련 설정
 */
const URL_CONFIG = {
  SUSPICIOUS_KEYWORDS: [
    'login', 'signin', 'account', 'verify', 'confirm',
    'update', 'security', 'alert', 'urgent', 'action',
    'click', 'bank', 'pay', 'paypal', 'amazon', 'apple'
  ],
  SUSPICIOUS_TLDS: [
    'tk', 'ml', 'ga', 'cf',
    'xyz', 'top', 'online', 'site',
    'download', 'stream', 'win'
  ],
  SHORT_URL_SERVICES: [
    'bit.ly', 'tinyurl.com', 'goo.gl', 'ow.ly',
    'short.link', 'rebrand.ly'
  ]
};

/**
 * 캐시 설정 (cacheManager.js에서 사용)
 */
const CACHE_CONFIG = {
  ENABLED: true,
  TTL_SECONDS: 3600, // 1시간
  MAX_SIZE: 10000,
  CLEANUP_INTERVAL: 600000 // 10분마다 정리
};

/**
 * 에러 메시지
 */
const ERROR_MESSAGES = {
  INVALID_URL: 'Invalid URL format',
  ANALYSIS_FAILED: 'URL analysis failed',
  DB_ERROR: 'Database operation failed',
  API_ERROR: 'External API error',
  TIMEOUT: 'Request timeout'
};

/**
 * 모든 상수 export
 */
module.exports = {
  FUNCTION_CONFIG,
  ML_MODEL,
  VIRUS_TOTAL,
  ENSEMBLE_WEIGHTS,
  RISK_LEVELS,
  FIRESTORE_CONFIG,
  URL_NORMALIZER,
  URL_CONFIG,
  CACHE_CONFIG,
  ERROR_MESSAGES
};
