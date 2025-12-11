// ============================================
// 캐시 매니저 테스트
// ============================================

const CacheManager = require('../src/cacheManager');

describe('CacheManager Tests', () => {
  let cache;
  const ORIGINAL_MAX_SIZE = require('../src/constants').CACHE_CONFIG.MAX_SIZE;

  beforeEach(() => {
    CacheManager.instance = null;
    cache = new CacheManager();

    // MAX_SIZE를 원본값으로 복구
    require('../src/constants').CACHE_CONFIG.MAX_SIZE = ORIGINAL_MAX_SIZE;
  });




  describe('Constructor', () => {
    test('should create cache instance', () => {
      expect(cache).toBeInstanceOf(CacheManager);
      expect(cache.cache).toBeInstanceOf(Map);
    });

    test('should initialize stats', () => {
      const stats = cache.getStats();
      expect(stats.hits).toBe(0);
      expect(stats.misses).toBe(0);
      expect(stats.evictions).toBe(0);
    });
  });

  describe('set and get', () => {
    test('should store and retrieve data', () => {
      const key = 'https://example.com';
      const data = {
        finalRiskScore: 0.5,
        suspiciousLevel: 'SUSPICIOUS'
      };

      cache.set(key, data);
      const result = cache.get(key);

      expect(result).toEqual(data);
    });

    test('should return null for missing key', () => {
      const result = cache.get('https://nonexistent.com');

      expect(result).toBeNull();
    });

    test('should track cache hit', () => {
      const key = 'https://example.com';
      const data = { score: 0.5 };

      cache.set(key, data);
      cache.get(key);

      const stats = cache.getStats();
      expect(stats.hits).toBe(1);
      expect(stats.misses).toBe(0);
    });

    test('should track cache miss', () => {
      cache.get('https://nonexistent.com');

      const stats = cache.getStats();
      expect(stats.hits).toBe(0);
      expect(stats.misses).toBe(1);
    });

    test('should update multiple times', () => {
      const key = 'https://example.com';
      const data1 = { score: 0.5 };
      const data2 = { score: 0.7 };

      cache.set(key, data1);
      expect(cache.get(key)).toEqual(data1);

      cache.set(key, data2);
      expect(cache.get(key)).toEqual(data2);
    });
  });

  describe('TTL Expiration', () => {
    test('should return null for expired entry', (done) => {
      const key = 'https://example.com';
      const data = { score: 0.5 };

      cache.set(key, data);

      // TTL을 강제로 만료시킴
      const entry = cache.cache.get(key);
      const pastDate = new Date();
      pastDate.setSeconds(pastDate.getSeconds() - 10);
      entry.ttl = pastDate.toISOString();

      const result = cache.get(key);

      expect(result).toBeNull();
      done();
    });

    test('should increment misses on expired entry', (done) => {
      const key = 'https://example.com';
      cache.set(key, { score: 0.5 });

      // TTL 만료 처리
      const entry = cache.cache.get(key);
      const pastDate = new Date();
      pastDate.setSeconds(pastDate.getSeconds() - 10);
      entry.ttl = pastDate.toISOString();

      cache.get(key);

      const stats = cache.getStats();
      expect(stats.misses).toBe(1);
      done();
    });

    test('should delete expired entry from cache', (done) => {
      const key = 'https://example.com';
      cache.set(key, { score: 0.5 });

      expect(cache.cache.has(key)).toBe(true);

      // TTL 만료 처리
      const entry = cache.cache.get(key);
      const pastDate = new Date();
      pastDate.setSeconds(pastDate.getSeconds() - 10);
      entry.ttl = pastDate.toISOString();

      cache.get(key);

      expect(cache.cache.has(key)).toBe(false);
      done();
    });
  });

  describe('LRU Eviction', () => {
    beforeEach(() => {
      // 🔥 LRU Eviction 테스트에서만 MAX_SIZE=10으로 설정
      require('../src/constants').CACHE_CONFIG.MAX_SIZE = 10;
    });

    test('should evict oldest entry when max size reached', () => {
      const maxSize = 10;
      
      // 캐시를 여러 번 채우기
      for (let i = 0; i < maxSize + 1; i++) {
        const key = `https://example${i}.com`;
        cache.set(key, { score: 0.5 + i * 0.01 });
      }

      // 첫 번째 항목이 제거됐는지 확인
      expect(cache.cache.has('https://example0.com')).toBe(false);
      expect(cache.cache.size).toBeLessThanOrEqual(maxSize);
    });

    test('should track evictions', () => {
      const maxSize = 10;

      // 최대 크기를 초과하게 추가
      for (let i = 0; i < maxSize + 5; i++) {
        cache.set(`https://example${i}.com`, { score: 0.5 });
      }

      const stats = cache.getStats();
      expect(stats.evictions).toBeGreaterThan(0);
    });
  });

  describe('LRU Ordering', () => {
    test('should move accessed entry to end (LRU order)', () => {
      const key1 = 'https://example1.com';
      const key2 = 'https://example2.com';
      const key3 = 'https://example3.com';

      cache.set(key1, { score: 0.5 });
      cache.set(key2, { score: 0.6 });
      cache.set(key3, { score: 0.7 });

      // key1에 접근 → 최근 사용으로 이동
      cache.get(key1);

      // Map의 순서 확인 (첫 번째가 가장 오래된 것)
      const keys = Array.from(cache.cache.keys());
      expect(keys[keys.length - 1]).toBe(key1);
    });
  });

  describe('clear', () => {
    test('should clear all cache entries', () => {
      cache.set('https://example1.com', { score: 0.5 });
      cache.set('https://example2.com', { score: 0.6 });

      expect(cache.cache.size).toBe(2);

      cache.clear();

      expect(cache.cache.size).toBe(0);
    });

    test('should not clear stats', () => {
      cache.set('https://example.com', { score: 0.5 });
      cache.get('https://example.com');

      cache.clear();

      const stats = cache.getStats();
      expect(stats.hits).toBe(1);
    });
  });

  describe('cleanup', () => {
    test('should remove expired entries', () => {
      cache.set('https://example1.com', { score: 0.5 });
      cache.set('https://example2.com', { score: 0.6 });

      // 첫 번째 항목 만료 처리
      const entries = Array.from(cache.cache.values());
      const pastDate = new Date();
      pastDate.setSeconds(pastDate.getSeconds() - 10);
      entries[0].ttl = pastDate.toISOString();

      const deleted = cache.cleanup();

      expect(deleted).toBe(1);
      expect(cache.cache.size).toBe(1);
    });

    test('should return deleted count', () => {
      cache.set('https://example1.com', { score: 0.5 });
      cache.set('https://example2.com', { score: 0.6 });
      cache.set('https://example3.com', { score: 0.7 });

      // 모든 항목 만료 처리
      const pastDate = new Date();
      pastDate.setSeconds(pastDate.getSeconds() - 10);

      for (const entry of cache.cache.values()) {
        entry.ttl = pastDate.toISOString();
      }

      const deleted = cache.cleanup();

      expect(deleted).toBe(3);
      expect(cache.cache.size).toBe(0);
    });

    test('should handle empty cache', () => {
      const deleted = cache.cleanup();

      expect(deleted).toBe(0);
    });
  });

  describe('getStats', () => {
    test('should return stats with hit rate', () => {
      cache.set('https://example.com', { score: 0.5 });
      cache.get('https://example.com');
      cache.get('https://nonexistent.com');

      const stats = cache.getStats();

      expect(stats).toHaveProperty('hits');
      expect(stats).toHaveProperty('misses');
      expect(stats).toHaveProperty('hitRate');
      expect(stats).toHaveProperty('evictions');
      expect(stats).toHaveProperty('size');
      expect(stats).toHaveProperty('maxSize');
    });

    test('should calculate hit rate correctly', () => {
      cache.set('https://example.com', { score: 0.5 });
      cache.get('https://example.com'); // hit
      cache.get('https://example.com'); // hit
      cache.get('https://nonexistent.com'); // miss

      const stats = cache.getStats();

      expect(stats.hits).toBe(2);
      expect(stats.misses).toBe(1);
      expect(stats.hitRate).toBe('66.67%');
    });

    test('should show N/A for hit rate when no operations', () => {
      const stats = cache.getStats();

      expect(stats.hitRate).toBe('N/A');
    });

    test('should show cache size and max size', () => {
      cache.set('https://example1.com', { score: 0.5 });
      cache.set('https://example2.com', { score: 0.6 });

      const stats = cache.getStats();

      expect(stats.size).toBe(2);
      expect(stats.maxSize).toBe(10000); // CACHE_CONFIG.MAX_SIZE
    });
  });

  describe('Singleton Pattern', () => {
    test('should create singleton instance', () => {
      CacheManager.instance = null;
      const instance1 = CacheManager.getInstance();
      const instance2 = CacheManager.getInstance();

      expect(instance1).toBe(instance2);
    });

    test('should reuse existing instance', () => {
      CacheManager.instance = null;
      const instance1 = CacheManager.getInstance();
      instance1.set('https://example.com', { score: 0.5 });

      const instance2 = CacheManager.getInstance();
      const result = instance2.get('https://example.com');

      expect(result).toEqual({ score: 0.5 });
    });
  });

  describe('Integration', () => {
    test('should handle multiple operations in sequence', () => {
      const urls = [
        'https://example1.com',
        'https://example2.com',
        'https://example3.com'
      ];

      // 저장
      urls.forEach((url, i) => {
        cache.set(url, { score: 0.5 + i * 0.1 });
      });

      // 조회
      const result1 = cache.get(urls[0]);
      const result2 = cache.get(urls[1]);
      const result3 = cache.get(urls[2]);

      expect(result1).toEqual({ score: 0.5 });
      expect(result2).toEqual({ score: 0.6 });
      expect(result3).toEqual({ score: 0.7 });

      // 통계 확인
      const stats = cache.getStats();
      expect(stats.hits).toBe(3);
      expect(stats.misses).toBe(0);
    });

    test('should handle hit and miss mix', () => {
      cache.set('https://example1.com', { score: 0.5 });
      cache.set('https://example2.com', { score: 0.6 });

      cache.get('https://example1.com'); // hit
      cache.get('https://missing.com');  // miss
      cache.get('https://example2.com'); // hit
      cache.get('https://missing2.com'); // miss

      const stats = cache.getStats();
      expect(stats.hits).toBe(2);
      expect(stats.misses).toBe(2);
      expect(stats.hitRate).toBe('50.00%');
    });
  });
});
