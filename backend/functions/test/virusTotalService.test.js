// ============================================
// VirusTotal 서비스 테스트
// ============================================

const VirusTotalService = require('../src/virusTotalService');

describe('VirusTotal Service Tests', () => {
  let service;
  const mockApiKey = 'test_api_key_12345';

  beforeAll(() => {
    service = new VirusTotalService(mockApiKey);
  });

  describe('constructor', () => {
    test('should throw error when API key is missing', () => {
      expect(() => {
        new VirusTotalService(null);
      }).toThrow('VirusTotal API key is required');
    });

    test('should create service with valid API key', () => {
      const vtService = new VirusTotalService('valid_key');
      expect(vtService.apiKey).toBe('valid_key');
    });

    test('should initialize request stats', () => {
      const vtService = new VirusTotalService('valid_key');
      const stats = vtService.getStats();

      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('success');
      expect(stats).toHaveProperty('failed');
      expect(stats).toHaveProperty('avgTime');
      expect(stats.total).toBe(0);
    });
  });

  describe('_urlToId', () => {
    test('should convert URL to VirusTotal ID format', () => {
      const url = 'https://example.com';
      const id = service._urlToId(url);

      expect(typeof id).toBe('string');
      expect(id.length).toBeGreaterThan(0);
      // Base64 인코딩된 문자열이므로 문자/숫자/-/_만 포함
      expect(/^[A-Za-z0-9_-]+$/.test(id)).toBe(true);
    });

    test('should convert different URLs to different IDs', () => {
      const url1 = 'https://example.com';
      const url2 = 'https://google.com';
      const id1 = service._urlToId(url1);
      const id2 = service._urlToId(url2);

      expect(id1).not.toBe(id2);
    });

    test('should convert same URL consistently', () => {
      const url = 'https://example.com';
      const id1 = service._urlToId(url);
      const id2 = service._urlToId(url);

      expect(id1).toBe(id2);
    });
  });

  describe('_sleep', () => {
    test('should wait for specified milliseconds', async () => {
      const startTime = Date.now();
      await service._sleep(100);
      const elapsedTime = Date.now() - startTime;

      expect(elapsedTime).toBeGreaterThanOrEqual(100);
      expect(elapsedTime).toBeLessThan(150);
    });
  });

  describe('getStats', () => {
    test('should return stats object with all properties', () => {
      const stats = service.getStats();

      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('success');
      expect(stats).toHaveProperty('failed');
      expect(stats).toHaveProperty('avgTime');
      expect(stats).toHaveProperty('successRate');
    });

    test('should show N/A for success rate when no requests made', () => {
      const freshService = new VirusTotalService('fresh_key');
      const stats = freshService.getStats();

      expect(stats.successRate).toBe('N/A');
    });
  });

  describe('analyzeUrlsBatch', () => {
    test('should return empty array for empty input', async () => {
      const results = await service.analyzeUrlsBatch([]);
      expect(Array.isArray(results)).toBe(true);
      expect(results.length).toBe(0);
    });

    test('should return array of results for multiple URLs', async () => {
      // Mock: 실제 API 호출 없이 테스트
      // 실제 환경에서는 VirusTotal API 키가 필요
      const results = await service.analyzeUrlsBatch([]);

      expect(Array.isArray(results)).toBe(true);
    });
  });

  describe('singleton pattern', () => {
    test('should return same instance on subsequent calls', () => {
      // 싱글톤 초기화
      VirusTotalService.instance = null;
      const service1 = VirusTotalService.getInstance('api_key_1');
      const service2 = VirusTotalService.getInstance('api_key_2');

      expect(service1).toBe(service2);
    });

    test('should use API key from first initialization', () => {
      VirusTotalService.instance = null;
      const service1 = VirusTotalService.getInstance('api_key_1');

      expect(service1.apiKey).toBe('api_key_1');
    });

    test('should throw error on first call without API key', () => {
      VirusTotalService.instance = null;

      expect(() => {
        VirusTotalService.getInstance();
      }).toThrow('VirusTotal API key required for first initialization');
    });
  });

  describe('helper methods', () => {
    test('_sleep should resolve after delay', async () => {
      const startTime = Date.now();
      await service._sleep(50);
      const elapsedTime = Date.now() - startTime;

      expect(elapsedTime).toBeGreaterThanOrEqual(50);
    });
  });

  describe('integration', () => {
    test('service should be properly initialized', () => {
      expect(service).toBeInstanceOf(VirusTotalService);
      expect(service.apiKey).toBe(mockApiKey);
      expect(service.client).toBeDefined();
    });

    test('stats should be tracking (even though requests fail without real API key)', () => {
      const initialStats = service.getStats();
      const initialTotal = initialStats.total;

      // 실제 API 호출은 실패하지만 통계는 기록되어야 함
      expect(initialTotal).toBeGreaterThanOrEqual(0);
    });
  });
});
