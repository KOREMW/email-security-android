// ============================================
// VirusTotal API 서비스
// - URL 스캔 요청
// - 스캔 결과 조회
// - 배치 처리
// ============================================

const axios = require('axios');
const { VIRUS_TOTAL } = require('./constants');

/**
 * VirusTotal API 클라이언트
 */
class VirusTotalService {
  constructor(apiKey) {
    if (!apiKey) {
      throw new Error('VirusTotal API key is required');
    }

    this.apiKey = apiKey;
    this.client = axios.create({
      baseURL: VIRUS_TOTAL.API_BASE_URL,
      headers: {
        'x-apikey': apiKey,
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      timeout: VIRUS_TOTAL.API_TIMEOUT_MS
    });

    this.requestStats = {
      total: 0,
      success: 0,
      failed: 0,
      avgTime: 0
    };
  }

  /**
   * URL을 VirusTotal에 제출하고 분석 요청
   * @param {string} url - 분석할 URL
   * @returns {Promise<Object>} {analysis_id, url}
   */
  async submitUrl(url) {
    try {
      const startTime = Date.now();

      const response = await this.client.post('/urls', `url=${encodeURIComponent(url)}`);

      const elapsedTime = Date.now() - startTime;
      this._updateStats(true, elapsedTime);

      console.log(`✓ URL submitted: ${url} (ID: ${response.data.data.id})`);

      return {
        analysisId: response.data.data.id,
        url: url,
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      this._updateStats(false);
      console.error(`✗ Failed to submit URL: ${url}`, error.message);
      throw new Error(`VirusTotal submission failed: ${error.message}`);
    }
  }

  /**
   * 분석 ID로 결과 조회
   * @param {string} analysisId - VirusTotal 분석 ID
   * @returns {Promise<Object>} 분석 결과
   */
  async getAnalysisResult(analysisId) {
    try {
      const startTime = Date.now();

      const response = await this.client.get(`/analyses/${analysisId}`);

      const elapsedTime = Date.now() - startTime;
      this._updateStats(true, elapsedTime);

      const attributes = response.data.data.attributes;
      const stats = attributes.stats;

      console.log(`✓ Analysis retrieved: ${analysisId}`);
      console.log(`  Stats: ${stats.malicious} malicious, ${stats.harmless} harmless, ${stats.undetected} undetected`);

      return {
        analysisId,
        status: attributes.status,
        malicious: stats.malicious || 0,
        harmless: stats.harmless || 0,
        undetected: stats.undetected || 0,
        suspicious: stats.suspicious || 0,
        verdict: stats.malicious > 0 ? 'MALICIOUS' : 'CLEAN',
        scanDate: attributes.date,
        results: attributes.results  // 엔진별 상세 결과
      };
    } catch (error) {
      this._updateStats(false);
      console.error(`✗ Failed to get analysis: ${analysisId}`, error.message);
      throw new Error(`VirusTotal analysis retrieval failed: ${error.message}`);
    }
  }

  /**
   * URL 직접 분석 (submit + wait + get)
   * 주의: VirusTotal API 할당량 제한이 있음 (4개/분)
   * @param {string} url - 분석할 URL
   * @param {number} maxRetries - 최대 재시도 횟수
   * @param {number} retryDelayMs - 재시도 대기 시간
   * @returns {Promise<Object>} 분석 결과
   */
  async analyzeUrl(url, maxRetries = 10, retryDelayMs = 2000) {
    try {
      // Step 1: URL 제출
      const submission = await this.submitUrl(url);

      // Step 2: 결과 폴링 (최대 maxRetries번)
      for (let attempt = 0; attempt < maxRetries; attempt++) {
        try {
          const result = await this.getAnalysisResult(submission.analysisId);

          // 분석 완료되면 바로 반환
          if (result.status === 'completed') {
            return {
              ...result,
              url: url,
              analysisTime: result.scanDate
            };
          }

          console.log(`[${attempt + 1}/${maxRetries}] Analysis pending... waiting ${retryDelayMs}ms`);
          await this._sleep(retryDelayMs);
        } catch (error) {
          if (attempt === maxRetries - 1) throw error;
          await this._sleep(retryDelayMs);
        }
      }

      throw new Error(`Analysis timeout after ${maxRetries} retries`);
    } catch (error) {
      console.error(`✗ URL analysis failed: ${url}`, error.message);
      throw error;
    }
  }

  /**
   * 여러 URL 배치 분석
   * API 할당량 제한을 고려하여 순차 처리
   * @param {Array<string>} urls - 분석할 URL 배열
   * @returns {Promise<Array<Object>>} 분석 결과 배열
   */
  async analyzeUrlsBatch(urls) {
    if (!Array.isArray(urls) || urls.length === 0) {
      return [];
    }

    console.log(`\n📊 Starting batch analysis for ${urls.length} URLs`);

    const results = [];
    const batchSize = VIRUS_TOTAL.BATCH_SIZE;
    const delayBetweenRequests = 15000;  // API 할당량: 4/분 = 1개/15초

    for (let i = 0; i < urls.length; i += batchSize) {
      const batch = urls.slice(i, i + batchSize);
      const batchNum = Math.floor(i / batchSize) + 1;

      console.log(`\n[Batch ${batchNum}] Processing ${batch.length} URLs (${i + 1}~${Math.min(i + batchSize, urls.length)})`);

      for (const url of batch) {
        try {
          const result = await this.analyzeUrl(url);
          results.push(result);

          // API 할당량 보호: 요청 사이에 딜레이
          if (url !== batch[batch.length - 1]) {
            await this._sleep(delayBetweenRequests);
          }
        } catch (error) {
          console.error(`✗ Failed to analyze: ${url}`, error.message);
          results.push({
            url: url,
            error: error.message,
            malicious: 0,
            harmless: 0,
            undetected: 0,
            verdict: 'ERROR'
          });
        }
      }
    }

    console.log(`\n✅ Batch analysis completed: ${results.length}/${urls.length} succeeded`);
    return results;
  }

  /**
   * URL 빠른 조회 (캐시된 결과만)
   * 새 분석 요청 없이 기존 결과만 반환
   * @param {string} url - 조회할 URL
   * @returns {Promise<Object>} 캐시된 분석 결과 또는 null
   */
  async getUrlReport(url) {
    try {
      const startTime = Date.now();

      const urlId = this._urlToId(url);
      const response = await this.client.get(`/urls/${urlId}`);

      const elapsedTime = Date.now() - startTime;
      this._updateStats(true, elapsedTime);

      const attributes = response.data.data.attributes;
      const stats = attributes.last_analysis_stats;

      console.log(`✓ URL report retrieved: ${url}`);

      return {
        url: url,
        malicious: stats.malicious || 0,
        harmless: stats.harmless || 0,
        undetected: stats.undetected || 0,
        suspicious: stats.suspicious || 0,
        verdict: stats.malicious > 0 ? 'MALICIOUS' : 'CLEAN',
        lastAnalysisDate: attributes.last_analysis_date,
        results: attributes.last_analysis_results
      };
    } catch (error) {
      if (error.response?.status === 404) {
        console.log(`ℹ URL not found in VirusTotal cache: ${url}`);
        return null;
      }

      this._updateStats(false);
      console.error(`✗ Failed to get URL report: ${url}`, error.message);
      throw new Error(`VirusTotal report retrieval failed: ${error.message}`);
    }
  }

  /**
   * 요청 통계 조회
   * @returns {Object} 통계 정보
   */
  getStats() {
    return {
      ...this.requestStats,
      successRate: this.requestStats.total > 0
        ? ((this.requestStats.success / this.requestStats.total) * 100).toFixed(2) + '%'
        : 'N/A'
    };
  }

  /**
   * 프라이빗: 요청 통계 업데이트
   */
  _updateStats(success, elapsedTime = 0) {
    this.requestStats.total++;

    if (success) {
      this.requestStats.success++;
      // 평균 시간 계산
      const prevTotal = this.requestStats.success - 1;
      this.requestStats.avgTime =
        (this.requestStats.avgTime * prevTotal + elapsedTime) /
        this.requestStats.success;
    } else {
      this.requestStats.failed++;
    }
  }

  /**
   * 프라이빗: URL을 VirusTotal ID로 변환
   * URL을 base64 인코딩 후 URL-safe하게 변환
   */
  _urlToId(url) {
    return Buffer.from(url)
      .toString('base64')
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }

  /**
   * 프라이빗: 대기 함수
   */
  _sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * 싱글톤: VirusTotal 서비스 인스턴스
   */
  static instance = null;

  /**
   * 싱글톤 인스턴스 획득
   */
  static getInstance(apiKey) {
    if (!this.instance) {
      if (!apiKey) {
        throw new Error('VirusTotal API key required for first initialization');
      }
      this.instance = new VirusTotalService(apiKey);
    }
    return this.instance;
  }
}

module.exports = VirusTotalService;
