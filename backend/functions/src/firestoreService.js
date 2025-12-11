// ============================================
// Firestore 데이터 저장 서비스
// - URL 분석 결과 저장
// - 분석 기록 관리
// - 배치 쓰기/읽기
// - undefined-safe 처리 적용
// ============================================

const admin = require('firebase-admin');
const { FIRESTORE_CONFIG } = require('./constants');

/**
 * undefined를 안전하게 처리하는 헬퍼 함수
 * Firestore는 undefined 값을 허용하지 않으므로 null 또는 기본값으로 변환
 * @param {*} value - 검사할 값
 * @param {*} defaultValue - undefined일 경우 반환할 기본값 (기본: null)
 * @returns {*} 안전한 값
 */
function safeValue(value, defaultValue = null) {
  return value === undefined ? defaultValue : value;
}

/**
 * 객체에서 모든 undefined 값을 재귀적으로 제거/변환
 * @param {Object} obj - 처리할 객체
 * @returns {Object} undefined가 제거된 객체
 */
function sanitizeObject(obj) {
  if (obj === null || obj === undefined) {
    return null;
  }
  
  if (typeof obj !== 'object') {
    return obj;
  }
  
  if (Array.isArray(obj)) {
    return obj.map(item => sanitizeObject(item));
  }
  
  const sanitized = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== undefined) {
      sanitized[key] = sanitizeObject(value);
    }
  }
  return sanitized;
}

/**
 * Firestore 데이터베이스 서비스
 */
class FirestoreService {
  constructor(firebaseApp = null) {
    // Firebase Admin SDK 초기화 (또는 기존 앱 사용)
    if (firebaseApp) {
      this.db = firebaseApp.firestore();
    } else {
      // Cloud Functions 환경에서는 자동 초기화됨
      try {
        this.db = admin.firestore();
      } catch (error) {
        console.error('Firebase initialization error:', error.message);
        throw error;
      }
    }

    this.writeStats = {
      total: 0,
      success: 0,
      failed: 0
    };

    this.readStats = {
      total: 0,
      success: 0,
      failed: 0,
      cacheHit: 0
    };
  }

  /**
   * URL 분석 결과 저장 (undefined-safe 버전)
   * @param {string} url - 분석한 URL
   * @param {Object} analysisResult - 분석 결과
   * @returns {Promise<string>} 저장된 문서 ID
   */
  async saveAnalysis(url, analysisResult) {
    try {
      // URL 유효성 검사
      if (!url || typeof url !== 'string') {
        throw new Error('Invalid URL: URL must be a non-empty string');
      }

      const docId = this._urlToDocId(url);
      
      // analysisResult가 없는 경우 기본값 사용
      const result = analysisResult || {};

      // undefined-safe 문서 생성
      const document = {
        // 기본 URL 정보
        url: url,
        originalUrl: safeValue(result.originalUrl, url),
        normalizedUrl: safeValue(result.normalizedUrl, url),
        
        // 분석 결과 (기본값 제공)
        finalRiskScore: safeValue(result.finalRiskScore, 0),
        suspiciousLevel: safeValue(result.suspiciousLevel, 'UNKNOWN'),
        suspicious: safeValue(result.suspicious, false),
        confidence: safeValue(result.confidence, 0),
        
        // 상세 정보 (빈 객체로 기본값)
        breakdown: sanitizeObject(safeValue(result.breakdown, {})),
        virusTotal: sanitizeObject(safeValue(result.virusTotal, {})),

        // 메타데이터
        timestamp: new Date().toISOString(),
        analyzedAt: new Date().toISOString(),
        ttl: this._calculateTTL(),
        
        // 추가 정보
        platform: 'backend',
        version: '1.0'
      };

      await this.db
        .collection(FIRESTORE_CONFIG.COLLECTION_URLS)
        .doc(docId)
        .set(document, { merge: true });

      this.writeStats.success++;
      console.log(`✓ Analysis saved: ${url} (ID: ${docId})`);

      return docId;
    } catch (error) {
      this.writeStats.failed++;
      console.error(`✗ Failed to save analysis: ${url}`, error.message);
      throw new Error(`Firestore save failed: ${error.message}`);
    } finally {
      this.writeStats.total++;
    }
  }

  /**
   * 저장된 분석 결과 조회
   * @param {string} url - 조회할 URL
   * @returns {Promise<Object|null>} 저장된 분석 결과 또는 null
   */
  async getAnalysis(url) {
    try {
      // URL 유효성 검사
      if (!url || typeof url !== 'string') {
        console.warn('Invalid URL provided to getAnalysis');
        return null;
      }

      const docId = this._urlToDocId(url);

      const doc = await this.db
        .collection(FIRESTORE_CONFIG.COLLECTION_URLS)
        .doc(docId)
        .get();

      this.readStats.total++;

      if (doc.exists) {
        const data = doc.data();

        // TTL 확인
        if (this._isExpired(data.ttl)) {
          console.log(`ℹ Cache expired: ${url}`);
          this.readStats.failed++;
          return null;
        }

        this.readStats.success++;
        this.readStats.cacheHit++;
        console.log(`✓ Analysis found (cache hit): ${url}`);

        return {
          ...data,
          cached: true,
          cachedAt: data.analyzedAt
        };
      }

      this.readStats.failed++;
      console.log(`ℹ Analysis not found: ${url}`);
      return null;
    } catch (error) {
      this.readStats.failed++;
      console.error(`✗ Failed to get analysis: ${url}`, error.message);
      throw new Error(`Firestore get failed: ${error.message}`);
    }
  }

  /**
   * 배치 저장 (여러 분석 결과 한 번에) - undefined-safe 버전
   * @param {Array<Object>} analyses - 분석 결과 배열 [{url, analysisResult}, ...]
   * @returns {Promise<Array<string>>} 저장된 문서 ID 배열
   */
  async saveBatch(analyses) {
    if (!Array.isArray(analyses) || analyses.length === 0) {
      return [];
    }

    try {
      const batch = this.db.batch();
      const savedIds = [];

      for (const item of analyses) {
        // 각 항목의 유효성 검사
        const url = item?.url;
        const analysisResult = item?.analysisResult || {};
        
        if (!url || typeof url !== 'string') {
          console.warn('Skipping invalid item in batch: missing URL');
          continue;
        }

        const docId = this._urlToDocId(url);
        const docRef = this.db
          .collection(FIRESTORE_CONFIG.COLLECTION_URLS)
          .doc(docId);

        // undefined-safe 문서 생성
        const document = {
          url: url,
          finalRiskScore: safeValue(analysisResult.finalRiskScore, 0),
          suspiciousLevel: safeValue(analysisResult.suspiciousLevel, 'UNKNOWN'),
          suspicious: safeValue(analysisResult.suspicious, false),
          confidence: safeValue(analysisResult.confidence, 0),
          breakdown: sanitizeObject(safeValue(analysisResult.breakdown, {})),
          virusTotal: sanitizeObject(safeValue(analysisResult.virusTotal, {})),
          timestamp: new Date().toISOString(),
          analyzedAt: new Date().toISOString(),
          ttl: this._calculateTTL(),
          platform: 'backend',
          version: '1.0'
        };

        batch.set(docRef, document, { merge: true });
        savedIds.push(docId);
      }

      if (savedIds.length > 0) {
        await batch.commit();
        this.writeStats.total += savedIds.length;
        this.writeStats.success += savedIds.length;
        console.log(`✓ Batch saved: ${savedIds.length} analyses`);
      }

      return savedIds;
    } catch (error) {
      this.writeStats.failed += analyses.length;
      console.error('✗ Batch save failed:', error.message);
      throw new Error(`Firestore batch save failed: ${error.message}`);
    }
  }

  /**
   * 배치 조회
   * @param {Array<string>} urls - 조회할 URL 배열
   * @returns {Promise<Array<Object>>} 분석 결과 배열
   */
  async getBatch(urls) {
    if (!Array.isArray(urls) || urls.length === 0) {
      return [];
    }

    try {
      const results = [];

      for (const url of urls) {
        if (!url || typeof url !== 'string') {
          continue;
        }
        
        const analysis = await this.getAnalysis(url);
        if (analysis) {
          results.push(analysis);
        }
      }

      console.log(`✓ Batch retrieved: ${results.length}/${urls.length} found`);
      return results;
    } catch (error) {
      console.error('✗ Batch get failed:', error.message);
      throw new Error(`Firestore batch get failed: ${error.message}`);
    }
  }

  /**
   * 분석 로그 저장 (감시/모니터링용) - undefined-safe 버전
   * @param {Object} logData - 로그 데이터
   * @returns {Promise<string>} 저장된 로그 ID
   */
  async saveLog(logData) {
    try {
      const logId = `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      // undefined 값 제거
      const sanitizedLogData = sanitizeObject(logData || {});

      const document = {
        ...sanitizedLogData,
        logId,
        timestamp: new Date().toISOString(),
        createdAt: new Date().toISOString()
      };

      await this.db
        .collection(FIRESTORE_CONFIG.COLLECTION_LOGS)
        .doc(logId)
        .set(document);

      console.log(`✓ Log saved: ${logId}`);
      return logId;
    } catch (error) {
      console.error('✗ Failed to save log:', error.message);
      throw new Error(`Firestore log save failed: ${error.message}`);
    }
  }

  /**
   * 지난 데이터 정리 (TTL 만료된 문서 삭제)
   * @param {number} daysOld - 이 날짜보다 오래된 데이터 삭제
   * @returns {Promise<number>} 삭제된 문서 수
   */
  async cleanup(daysOld = FIRESTORE_CONFIG.TTL_DAYS) {
    try {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - daysOld);

      const query = this.db
        .collection(FIRESTORE_CONFIG.COLLECTION_URLS)
        .where('analyzedAt', '<', cutoffDate.toISOString());

      const snapshot = await query.get();

      console.log(`🧹 Cleaning up ${snapshot.size} expired documents...`);

      if (snapshot.size === 0) {
        return 0;
      }

      let deleted = 0;
      let batch = this.db.batch();
      let batchCount = 0;

      snapshot.forEach((doc) => {
        batch.delete(doc.ref);
        batchCount++;

        // Firestore는 한 번에 최대 500개씩 삭제 가능
        if (batchCount === 500) {
          batch.commit();
          deleted += batchCount;
          batch = this.db.batch();
          batchCount = 0;
        }
      });

      // 남은 것 커밋
      if (batchCount > 0) {
        await batch.commit();
        deleted += batchCount;
      }

      console.log(`✓ Cleanup completed: ${deleted} documents deleted`);
      return deleted;
    } catch (error) {
      console.error('✗ Cleanup failed:', error.message);
      throw new Error(`Firestore cleanup failed: ${error.message}`);
    }
  }

  /**
   * 저장 통계 조회
   * @returns {Object} 저장 통계
   */
  getWriteStats() {
    return {
      ...this.writeStats,
      successRate:
        this.writeStats.total > 0
          ? ((this.writeStats.success / this.writeStats.total) * 100).toFixed(2) +
            '%'
          : 'N/A'
    };
  }

  /**
   * 조회 통계 조회
   * @returns {Object} 조회 통계
   */
  getReadStats() {
    return {
      ...this.readStats,
      hitRate:
        this.readStats.total > 0
          ? ((this.readStats.cacheHit / this.readStats.total) * 100).toFixed(2) +
            '%'
          : 'N/A'
    };
  }

  /**
   * 프라이빗: URL을 문서 ID로 변환
   */
  _urlToDocId(url) {
    return Buffer.from(url)
      .toString('base64')
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }

  /**
   * 프라이빗: TTL(만료 시간) 계산
   */
  _calculateTTL() {
    const ttlDate = new Date();
    ttlDate.setDate(ttlDate.getDate() + FIRESTORE_CONFIG.TTL_DAYS);
    return ttlDate.toISOString();
  }

  /**
   * 프라이빗: 문서 만료 여부 확인
   */
  _isExpired(ttl) {
    if (!ttl) return false;
    return new Date(ttl) < new Date();
  }

  /**
   * 싱글톤: Firestore 서비스 인스턴스
   */
  static instance = null;

  /**
   * 싱글톤 인스턴스 획득
   */
  static getInstance(firebaseApp = null) {
    if (!this.instance) {
      this.instance = new FirestoreService(firebaseApp);
    }
    return this.instance;
  }
}

module.exports = FirestoreService;