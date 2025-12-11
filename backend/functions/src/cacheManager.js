// ============================================
// 메모리 캐시 관리 서비스
// - LRU 캐시 구현
// - TTL 기반 만료 처리
// - 캐시 통계 추적
// - 자동 정리 기능 추가
// ============================================

const { CACHE_CONFIG } = require('./constants');

/**
 * LRU 캐시 서비스
 * 최근 사용한 항목을 메모리에 보관하고,
 * TTL이 지나면 자동 제거
 */
class CacheManager {
  constructor(options = {}) {
    this.cache = new Map();
    this.stats = {
      hits: 0,
      misses: 0,
      evictions: 0
    };
    
    // 자동 정리 타이머 (옵션)
    this.cleanupTimer = null;
    
    // 자동 정리 활성화 (기본값: 비활성화)
    if (options.enableAutoCleanup) {
      this.startAutoCleanup(options.cleanupInterval || CACHE_CONFIG.CLEANUP_INTERVAL);
    }
  }

  /**
   * 캐시에서 데이터 조회
   * @param {string} key - URL
   * @returns {Object|null} 캐시된 데이터 또는 null
   */
  get(key) {
    // key 유효성 검사
    if (!key || typeof key !== 'string') {
      this.stats.misses++;
      return null;
    }

    const entry = this.cache.get(key);

    if (!entry) {
      this.stats.misses++;
      return null;
    }

    // TTL 확인
    if (this._isExpired(entry.ttl)) {
      this.cache.delete(key);
      this.stats.misses++;
      return null;
    }

    // LRU: 접근한 항목을 끝으로 이동
    this.cache.delete(key);
    this.cache.set(key, entry);

    this.stats.hits++;
    return entry.data;
  }

  /**
   * 캐시에 데이터 저장
   * @param {string} key - URL
   * @param {Object} data - 저장할 데이터
   * @param {number} ttlSeconds - TTL (초 단위, 옵션)
   */
  set(key, data, ttlSeconds = CACHE_CONFIG.TTL_SECONDS) {
    // key 유효성 검사
    if (!key || typeof key !== 'string') {
      console.warn('Invalid key provided to cache.set()');
      return;
    }

    // data가 undefined인 경우 저장하지 않음
    if (data === undefined) {
      console.warn('Cannot cache undefined value');
      return;
    }

    // 기존 키가 있으면 삭제 (LRU 유지)
    if (this.cache.has(key)) {
      this.cache.delete(key);
    }

    // 캐시 크기 초과 확인 (LRU eviction)
    if (this.cache.size >= CACHE_CONFIG.MAX_SIZE) {
      // 가장 오래된 항목 (Map의 첫 번째) 제거
      const firstKey = this.cache.keys().next().value;
      this.cache.delete(firstKey);
      this.stats.evictions++;
    }

    // 새 항목 추가
    const ttl = new Date();
    ttl.setSeconds(ttl.getSeconds() + ttlSeconds);

    this.cache.set(key, {
      data,
      ttl: ttl.toISOString(),
      createdAt: new Date().toISOString()
    });
  }

  /**
   * 특정 키 캐시에서 제거
   * @param {string} key - 제거할 키
   * @returns {boolean} 제거 성공 여부
   */
  delete(key) {
    if (!key || typeof key !== 'string') {
      return false;
    }
    return this.cache.delete(key);
  }

  /**
   * 캐시에 키가 있는지 확인 (만료 여부 포함)
   * @param {string} key - 확인할 키
   * @returns {boolean} 존재 여부
   */
  has(key) {
    if (!key || typeof key !== 'string') {
      return false;
    }

    const entry = this.cache.get(key);
    if (!entry) {
      return false;
    }

    // TTL 만료 확인
    if (this._isExpired(entry.ttl)) {
      this.cache.delete(key);
      return false;
    }

    return true;
  }

  /**
   * 캐시 전체 삭제
   */
  clear() {
    this.cache.clear();
    console.log('Cache cleared');
  }

  /**
   * 만료된 항목 정리
   * @returns {number} 삭제된 항목 수
   */
  cleanup() {
    let deleted = 0;
    const now = new Date();

    for (const [key, entry] of this.cache) {
      if (new Date(entry.ttl) < now) {
        this.cache.delete(key);
        deleted++;
      }
    }

    if (deleted > 0) {
      console.log(`Cache cleanup: ${deleted} expired entries removed`);
    }

    return deleted;
  }

  /**
   * 자동 정리 시작
   * @param {number} intervalMs - 정리 주기 (밀리초)
   */
  startAutoCleanup(intervalMs = CACHE_CONFIG.CLEANUP_INTERVAL) {
    // 기존 타이머 정리
    this.stopAutoCleanup();
    
    this.cleanupTimer = setInterval(() => {
      this.cleanup();
    }, intervalMs);
    
    console.log(`Auto cleanup started (interval: ${intervalMs}ms)`);
  }

  /**
   * 자동 정리 중지
   */
  stopAutoCleanup() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
      this.cleanupTimer = null;
      console.log('Auto cleanup stopped');
    }
  }

  /**
   * 캐시 통계 조회
   * @returns {Object} 캐시 통계
   */
  getStats() {
    const total = this.stats.hits + this.stats.misses;
    const hitRate =
      total > 0
        ? ((this.stats.hits / total) * 100).toFixed(2) + '%'
        : 'N/A';

    return {
      hits: this.stats.hits,
      misses: this.stats.misses,
      hitRate,
      evictions: this.stats.evictions,
      size: this.cache.size,
      maxSize: CACHE_CONFIG.MAX_SIZE,
      autoCleanup: this.cleanupTimer !== null
    };
  }

  /**
   * 통계 초기화
   */
  resetStats() {
    this.stats = {
      hits: 0,
      misses: 0,
      evictions: 0
    };
    console.log('Cache stats reset');
  }

  /**
   * 프라이빗: TTL 만료 여부 확인
   * @param {string} ttl - ISO 형식 만료 시간
   * @returns {boolean} 만료 여부
   */
  _isExpired(ttl) {
    if (!ttl) return false;
    return new Date(ttl) < new Date();
  }

  /**
   * 리소스 정리 (서버 종료 시 호출)
   */
  destroy() {
    this.stopAutoCleanup();
    this.clear();
    console.log('CacheManager destroyed');
  }

  // 싱글톤 인스턴스
  static instance = null;

  /**
   * 싱글톤 인스턴스 획득
   * @param {Object} options - 옵션 (enableAutoCleanup, cleanupInterval)
   * @returns {CacheManager} 싱글톤 인스턴스
   */
  static getInstance(options = {}) {
    if (!this.instance) {
      this.instance = new CacheManager(options);
    }
    return this.instance;
  }

  /**
   * 싱글톤 인스턴스 초기화 (테스트용)
   */
  static resetInstance() {
    if (this.instance) {
      this.instance.destroy();
      this.instance = null;
    }
  }
}

module.exports = CacheManager;