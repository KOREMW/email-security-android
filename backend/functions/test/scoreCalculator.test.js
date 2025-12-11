// ============================================
// 점수 계산 테스트
// ============================================

const {
  calculateFinalRiskScore,
  calculateVirusTotalScore,
  getSuspiciousLevel,
  convertToSoftLabel,
  isValidScore,
  getFullAnalysisResult
} = require('../src/scoreCalculator');

describe('Score Calculator Tests', () => {

  describe('calculateVirusTotalScore', () => {
    test('should calculate VT score correctly', () => {
      const vtResult = {
        malicious: 5,
        harmless: 40,
        undetected: 10
      };

      const result = calculateVirusTotalScore(vtResult);
      
      expect(result.vtScore).toBeCloseTo(5 / 55, 2);
      expect(result.confidence).toBeGreaterThan(0);
      expect(result.confidence).toBeLessThanOrEqual(1.0);
      expect(result.stats.total).toBe(55);
    });

    test('should handle zero detections', () => {
      const vtResult = {
        malicious: 0,
        harmless: 0,
        undetected: 0
      };

      const result = calculateVirusTotalScore(vtResult);
      
      expect(result.vtScore).toBe(0.5);
      expect(result.confidence).toBeLessThan(0.5);
    });

    test('should handle high malicious count', () => {
      const vtResult = {
        malicious: 45,
        harmless: 5,
        undetected: 0
      };

      const result = calculateVirusTotalScore(vtResult);
      
      expect(result.vtScore).toBeCloseTo(0.9, 1);
      expect(result.confidence).toBe(1.0);
    });
  });

  describe('calculateFinalRiskScore', () => {
    test('should calculate ensemble score correctly', () => {
      const localScore = 0.85;
      const vtResult = {
        malicious: 5,
        harmless: 40,
        undetected: 10
      };

      const result = calculateFinalRiskScore(localScore, vtResult);

      expect(result.finalRiskScore).toBeGreaterThan(0);
      expect(result.finalRiskScore).toBeLessThanOrEqual(1.0);
      expect(result.breakdown).toHaveProperty('localMlScore');
      expect(result.breakdown).toHaveProperty('virusTotalScore');
      expect(result.breakdown).toHaveProperty('weights');
    });

    test('should weight local ML (40%) and VT (60%) correctly', () => {
      const localScore = 1.0;
      const vtResult = {
        malicious: 0,
        harmless: 50,
        undetected: 0
      };

      const result = calculateFinalRiskScore(localScore, vtResult);

      // 예상: (0.4 * 1.0) + (0.6 * 0) = 0.4
      expect(result.finalRiskScore).toBeCloseTo(0.4, 1);
    });

    test('should return DANGEROUS level when score >= 0.7', () => {
      const localScore = 1.0;
      const vtResult = {
        malicious: 40,
        harmless: 10,
        undetected: 0
      };

      const result = calculateFinalRiskScore(localScore, vtResult);

      expect(result.suspiciousLevel).toBe('DANGEROUS');
      expect(result.finalRiskScore).toBeGreaterThanOrEqual(0.7);
    });

    test('should return SAFE level when score < 0.3', () => {
      const localScore = 0.1;
      const vtResult = {
        malicious: 0,
        harmless: 50,
        undetected: 0
      };

      const result = calculateFinalRiskScore(localScore, vtResult);

      expect(result.suspiciousLevel).toBe('SAFE');
      expect(result.finalRiskScore).toBeLessThan(0.3);
    });

    test('should return SUSPICIOUS level for medium scores', () => {
      const localScore = 0.5;
      const vtResult = {
        malicious: 20,
        harmless: 30,
        undetected: 0
      };

      const result = calculateFinalRiskScore(localScore, vtResult);

      expect(result.suspiciousLevel).toBe('SUSPICIOUS');
      expect(result.finalRiskScore).toBeGreaterThanOrEqual(0.3);
      expect(result.finalRiskScore).toBeLessThan(0.7);
    });
  });

  describe('getSuspiciousLevel', () => {
    test('should return SAFE for low scores', () => {
      expect(getSuspiciousLevel(0.0)).toBe('SAFE');
      expect(getSuspiciousLevel(0.2)).toBe('SAFE');
      expect(getSuspiciousLevel(0.29)).toBe('SAFE');
    });

    test('should return SUSPICIOUS for medium scores', () => {
      expect(getSuspiciousLevel(0.3)).toBe('SUSPICIOUS');
      expect(getSuspiciousLevel(0.5)).toBe('SUSPICIOUS');
      expect(getSuspiciousLevel(0.69)).toBe('SUSPICIOUS');
    });

    test('should return DANGEROUS for high scores', () => {
      expect(getSuspiciousLevel(0.7)).toBe('DANGEROUS');
      expect(getSuspiciousLevel(0.9)).toBe('DANGEROUS');
      expect(getSuspiciousLevel(1.0)).toBe('DANGEROUS');
    });
  });

  describe('convertToSoftLabel', () => {
    test('should convert hard label to soft label', () => {
      const softLabel = convertToSoftLabel(5, 40, 10);
      
      expect(softLabel).toBeGreaterThanOrEqual(0);
      expect(softLabel).toBeLessThanOrEqual(1.0);
    });

    test('should return 0.5 when no data available', () => {
      const softLabel = convertToSoftLabel(0, 0, 0);
      
      expect(softLabel).toBe(0.5);
    });

    test('should be higher when more malicious detections', () => {
      const softLabel1 = convertToSoftLabel(5, 40, 10);
      const softLabel2 = convertToSoftLabel(45, 5, 0);
      
      expect(softLabel2).toBeGreaterThan(softLabel1);
    });
  });

  describe('isValidScore', () => {
    test('should validate correct scores', () => {
      expect(isValidScore(0.0)).toBe(true);
      expect(isValidScore(0.5)).toBe(true);
      expect(isValidScore(1.0)).toBe(true);
    });

    test('should reject invalid scores', () => {
      expect(isValidScore(-0.1)).toBe(false);
      expect(isValidScore(1.1)).toBe(false);
      expect(isValidScore('0.5')).toBe(false);
      expect(isValidScore(null)).toBe(false);
    });

    test('should support custom range', () => {
      expect(isValidScore(5, 0, 10)).toBe(true);
      expect(isValidScore(15, 0, 10)).toBe(false);
    });
  });

  describe('getFullAnalysisResult', () => {
    test('should return complete analysis object', () => {
      const result = getFullAnalysisResult(0.85, {
        malicious: 5,
        harmless: 40,
        undetected: 10
      });

      expect(result).toHaveProperty('suspicious');
      expect(result).toHaveProperty('finalRiskScore');
      expect(result).toHaveProperty('suspiciousLevel');
      expect(result).toHaveProperty('confidence');
      expect(result).toHaveProperty('virusTotal');
      expect(result).toHaveProperty('breakdown');
      expect(result).toHaveProperty('timestamp');
    });

    test('should throw on invalid localRiskScore', () => {
      expect(() => {
        getFullAnalysisResult(1.5, { malicious: 5, harmless: 40, undetected: 10 });
      }).toThrow('Invalid localRiskScore');
    });

    test('should throw on invalid virusTotalResult', () => {
      expect(() => {
        getFullAnalysisResult(0.5, null);
      }).toThrow('Invalid virusTotalResult');
    });
  });

  describe('integration', () => {
    test('complete workflow: low risk scenario', () => {
      const localScore = 0.2;
      const vtResult = {
        malicious: 0,
        harmless: 45,
        undetected: 5
      };

      const analysis = getFullAnalysisResult(localScore, vtResult);

      expect(analysis.finalRiskScore).toBeLessThan(0.3);
      expect(analysis.suspicious).toBe(false);
      expect(analysis.suspiciousLevel).toBe('SAFE');
    });

    test('complete workflow: high risk scenario', () => {
      const localScore = 0.9;
      const vtResult = {
        malicious: 35,
        harmless: 15,
        undetected: 0
      };

      const analysis = getFullAnalysisResult(localScore, vtResult);

      expect(analysis.finalRiskScore).toBeGreaterThanOrEqual(0.7);
      expect(analysis.suspicious).toBe(true);
      expect(analysis.suspiciousLevel).toBe('DANGEROUS');
      expect(analysis.confidence).toBeGreaterThan(0.8);
    });

    test('complete workflow: medium risk scenario', () => {
      // 수정: localScore와 VT 모두 높아야 SUSPICIOUS 이상
      const localScore = 0.8;  // 80% 위험 (ML)
      const vtResult = {
        malicious: 30,  // 30/50 = 60%
        harmless: 20,
        undetected: 0
      };

      const analysis = getFullAnalysisResult(localScore, vtResult);

      // 계산: (0.4 * 0.8) + (0.6 * 0.6) = 0.32 + 0.36 = 0.68
      expect(analysis.finalRiskScore).toBeGreaterThanOrEqual(0.3);
      expect(analysis.finalRiskScore).toBeLessThan(0.7);
      expect(analysis.suspicious).toBe(true);
      expect(analysis.suspiciousLevel).toBe('SUSPICIOUS');
    });
  });
});
