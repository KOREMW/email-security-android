// ============================================
// 점수 계산 모듈 (앙상블)
// - 로컬 ML 점수 + VirusTotal 점수 결합
// - 최종 위험도 계산
// - 신뢰도 평가
// ============================================

const { ENSEMBLE_WEIGHTS, VIRUS_TOTAL } = require('./constants');

/**
 * VirusTotal 점수 정규화 (0.0 ~ 1.0)
 * @param {Object} virusTotalResult - VirusTotal 분석 결과
 * @returns {Object} {vtScore, confidence, stats}
 */
function calculateVirusTotalScore(virusTotalResult) {
  const total =
    virusTotalResult.malicious +
    virusTotalResult.harmless +
    virusTotalResult.undetected;

  // VT Score = malicious / total
  const vtScore = total > 0 ? virusTotalResult.malicious / total : 0.5;

  // 신뢰도: 스캔 엔진이 많을수록 높음 (점수에 영향 안 줌)
  const confidence = Math.min(
    total / VIRUS_TOTAL.MIN_DETECTION_CONFIDENCE,
    1.0
  );

  return {
    vtScore: Math.min(Math.max(vtScore, 0), 1.0),
    confidence,
    stats: {
      malicious: virusTotalResult.malicious,
      harmless: virusTotalResult.harmless,
      undetected: virusTotalResult.undetected,
      total
    }
  };
}

/**
 * 위험도 점수를 텍스트 레벨로 변환
 * 범위:
 * - SAFE:       score < 0.3
 * - SUSPICIOUS: 0.3 ≤ score < 0.7
 * - DANGEROUS:  score ≥ 0.7
 *
 * @param {number} score - 위험도 점수 (0.0 ~ 1.0)
 * @returns {string} 위험도 레벨 (SAFE, SUSPICIOUS, DANGEROUS)
 */
function getSuspiciousLevel(score) {
  if (score >= 0.7) return 'DANGEROUS';
  if (score >= 0.3) return 'SUSPICIOUS';
  return 'SAFE';
}

/**
 * 최종 위험도 점수 계산 (앙상블)
 * finalRiskScore = (0.4 * localMlScore) + (0.6 * virusTotalScore)
 *
 * @param {number} localRiskScore - 모바일 ML 모델의 위험도 (0.0 ~ 1.0)
 * @param {Object} virusTotalResult - VirusTotal 분석 결과
 * @returns {Object} 최종 점수 및 상세 정보
 */
function calculateFinalRiskScore(localRiskScore, virusTotalResult) {
  // VirusTotal 점수 계산
  const vtData = calculateVirusTotalScore(virusTotalResult);

  // 최종 점수 = (0.4 * 로컬) + (0.6 * VT)
  const finalScore =
    ENSEMBLE_WEIGHTS.LOCAL_ML * localRiskScore +
    ENSEMBLE_WEIGHTS.VIRUS_TOTAL * vtData.vtScore;

  // 정규화
  const normalizedScore = Math.min(Math.max(finalScore, 0), 1.0);

  // 위험도 레벨 결정
  const suspiciousLevel = getSuspiciousLevel(normalizedScore);

  return {
    finalRiskScore: normalizedScore,
    suspiciousLevel,
    confidence: vtData.confidence,
    breakdown: {
      localMlScore: Math.min(Math.max(localRiskScore, 0), 1.0),
      virusTotalScore: vtData.vtScore,
      weights: {
        localMl: ENSEMBLE_WEIGHTS.LOCAL_ML,
        virusTotal: ENSEMBLE_WEIGHTS.VIRUS_TOTAL
      },
      virusTotalStats: vtData.stats
    }
  };
}

/**
 * Soft Label 변환 (학습용)
 * @param {number} malicious - VirusTotal 악성 판정 수
 * @param {number} harmless - VirusTotal 정상 판정 수
 * @param {number} undetected - VirusTotal 미탐지 수
 * @returns {number} Soft Label (0.0 ~ 1.0)
 */
function convertToSoftLabel(malicious, harmless, undetected) {
  const total = malicious + harmless + undetected;

  if (total === 0) {
    return 0.5;
  }

  // 기본 위험도 점수
  const riskScore = malicious / total;

  // 신뢰도 계산
  const confidence = Math.min(
    total / VIRUS_TOTAL.MIN_DETECTION_CONFIDENCE,
    1.0
  );

  // 신뢰도에 따라 조정
  const adjustedScore = riskScore * confidence + 0.5 * (1 - confidence);

  return Math.min(Math.max(adjustedScore, 0), 1.0);
}

/**
 * 점수 검증 및 범위 확인
 * @param {number} score - 점수
 * @param {number} min - 최소값 (기본: 0)
 * @param {number} max - 최대값 (기본: 1)
 * @returns {boolean} 유효 여부
 */
function isValidScore(score, min = 0, max = 1) {
  return typeof score === 'number' && score >= min && score <= max;
}

/**
 * 전체 점수 계산 요약 반환
 * @param {number} localRiskScore - 로컬 ML 점수
 * @param {Object} virusTotalResult - VirusTotal 결과
 * @returns {Object} 최종 분석 결과
 */
function getFullAnalysisResult(localRiskScore, virusTotalResult) {
  // 입력 검증
  if (!isValidScore(localRiskScore)) {
    throw new Error('Invalid localRiskScore: must be between 0 and 1');
  }

  if (!virusTotalResult || typeof virusTotalResult !== 'object') {
    throw new Error('Invalid virusTotalResult: must be an object');
  }

  // 최종 점수 계산
  const result = calculateFinalRiskScore(localRiskScore, virusTotalResult);

  // suspicious = SUSPICIOUS 또는 DANGEROUS일 때 true
  const suspicious =
    result.suspiciousLevel === 'SUSPICIOUS' ||
    result.suspiciousLevel === 'DANGEROUS';

  return {
    // 최종 판정
    suspicious,
    finalRiskScore: result.finalRiskScore,
    suspiciousLevel: result.suspiciousLevel,
    confidence: result.confidence,

    // VirusTotal 통계
    virusTotal: result.breakdown.virusTotalStats,

    // 상세 점수 정보
    breakdown: result.breakdown,

    // 타임스탬프
    timestamp: new Date().toISOString()
  };
}

module.exports = {
  calculateFinalRiskScore,
  calculateVirusTotalScore,
  getSuspiciousLevel,
  convertToSoftLabel,
  isValidScore,
  getFullAnalysisResult
};
