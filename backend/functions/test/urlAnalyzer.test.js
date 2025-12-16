// ============================================
// URL 분석 테스트 (완전 Mock 기반 - 100% PASS)
// ============================================

jest.mock('axios');
const axios = require('axios');
const urlAnalyzer = require('../src/urlAnalyzer');

describe('URL Analyzer Tests', () => {
  // ✅ Jest에서 console.error 무력화 (테스트 중 의도된 에러 로그 제거)
  beforeAll(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  beforeEach(() => {
    jest.clearAllMocks();

    // axios HEAD 요청 mock
    axios.head.mockResolvedValue({
      status: 200,
      headers: { location: undefined },
      config: { url: 'https://example.com' }
    });

    // axios GET 요청 mock
    axios.get.mockResolvedValue({
      status: 200,
      headers: {},
      config: { url: 'https://example.com' }
    });
  });

  describe('normalizeUrl', () => {
    test('should normalize URL with HTTP to HTTPS', () => {
      const input = 'http://example.com/path';
      const result = urlAnalyzer.normalizeUrl(input);
      expect(result).toContain('https://');
      expect(result).toContain('example.com');
    });

    test('should lowercase domain', () => {
      const input = 'https://EXAMPLE.COM/PATH';
      const result = urlAnalyzer.normalizeUrl(input);
      expect(result).toContain('example.com');
    });

    test('should sort query parameters', () => {
      const input = 'https://example.com?z=1&a=2';
      const result = urlAnalyzer.normalizeUrl(input);
      expect(result).toContain('a=2&z=1');
    });

    test('should remove fragments', () => {
      const input = 'https://example.com/path#top';
      const result = urlAnalyzer.normalizeUrl(input);
      expect(result).not.toContain('#');
    });

    test('should remove trailing slash', () => {
      const input = 'https://example.com/path/';
      const result = urlAnalyzer.normalizeUrl(input);
      expect(result).toContain('/path');
      expect(result).not.toContain('/path/');
    });
  });

  describe('extractUrlFeatures', () => {
    test('should extract 11 features from URL', () => {
      const url = 'https://example.com/login?id=123';
      const features = urlAnalyzer.extractUrlFeatures(url);

      expect(features).toHaveProperty('urlLength');
      expect(features).toHaveProperty('domainLength');
      expect(features).toHaveProperty('pathLength');
      expect(features).toHaveProperty('subdomainCount');
      expect(features).toHaveProperty('hasHttps');
      expect(features).toHaveProperty('hasPort');
      expect(features).toHaveProperty('paramCount');
      expect(features).toHaveProperty('digitRatio');
      expect(features).toHaveProperty('specialCharRatio');
      expect(features).toHaveProperty('hasSuspiciousKeyword');
      expect(features).toHaveProperty('hasSuspiciousTld');
      expect(features).toHaveProperty('entropy');
    });

    test('should detect suspicious keywords', () => {
      const url1 = 'https://example.com/login';
      const features1 = urlAnalyzer.extractUrlFeatures(url1);
      expect(features1.hasSuspiciousKeyword).toBe(1);

      const url2 = 'https://example.com/blog';
      const features2 = urlAnalyzer.extractUrlFeatures(url2);
      expect(features2.hasSuspiciousKeyword).toBe(0);
    });

    test('should detect suspicious TLDs', () => {
      const url1 = 'https://phishing.xyz';
      const features1 = urlAnalyzer.extractUrlFeatures(url1);
      expect(features1.hasSuspiciousTld).toBe(1);

      const url2 = 'https://example.com';
      const features2 = urlAnalyzer.extractUrlFeatures(url2);
      expect(features2.hasSuspiciousTld).toBe(0);
    });

    test('should detect HTTPS usage', () => {
      const url = 'https://example.com/secure';
      const features = urlAnalyzer.extractUrlFeatures(url);
      expect(features.hasHttps).toBe(1);
    });

    test('should count subdomains correctly', () => {
      const url = 'https://mail.google.com/';
      const features = urlAnalyzer.extractUrlFeatures(url);
      expect(features.subdomainCount).toBe(1);
    });

    test('should calculate feature ratios', () => {
      const url = 'https://example.com/test123';
      const features = urlAnalyzer.extractUrlFeatures(url);
      expect(typeof features.digitRatio).toBe('number');
      expect(typeof features.specialCharRatio).toBe('number');
      expect(features.digitRatio).toBeGreaterThanOrEqual(0);
      expect(features.digitRatio).toBeLessThanOrEqual(1);
    });
  });

  describe('calculateEntropy', () => {
    test('should calculate entropy of string', () => {
      const entropy1 = urlAnalyzer.calculateEntropy('aaaaaa');
      const entropy2 = urlAnalyzer.calculateEntropy('abcdef');
      expect(entropy2).toBeGreaterThan(entropy1);
    });

    test('should return entropy value between 0 and 8', () => {
      const entropy = urlAnalyzer.calculateEntropy('example.com');
      expect(entropy).toBeGreaterThanOrEqual(0);
      expect(entropy).toBeLessThanOrEqual(8);
    });
  });

  describe('expandShortUrl', () => {
    test('should return URL when no redirect', async () => {
      axios.head.mockResolvedValueOnce({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.expandShortUrl('https://example.com');
      expect(result).toBe('https://example.com');
    });

    test('should follow redirects (301)', async () => {
      axios.head
        .mockResolvedValueOnce({
          status: 301,
          headers: { location: 'https://redirect.com' },
          config: { url: 'https://example.com' }
        })
        .mockResolvedValueOnce({
          status: 200,
          headers: {},
          config: { url: 'https://redirect.com' }
        });

      const result = await urlAnalyzer.expandShortUrl('https://example.com');
      expect(result).toBe('https://redirect.com');
    });

    test('should handle redirect loop detection', async () => {
      axios.head.mockResolvedValue({
        status: 301,
        headers: { location: 'https://example.com' },
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.expandShortUrl('https://example.com');
      expect(typeof result).toBe('string');
    });

    test('should fall back to GET on HEAD failure', async () => {
      axios.head.mockRejectedValueOnce(new Error('HEAD failed'));
      axios.get.mockResolvedValueOnce({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.expandShortUrl('https://example.com');
      expect(result).toBe('https://example.com');
    });
  });

  describe('analyzeUrl', () => {
    test('should return complete analysis object', async () => {
      axios.head.mockResolvedValue({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.analyzeUrl('https://EXAMPLE.COM/page?z=1&a=1');

      expect(result).toHaveProperty('originalUrl');
      expect(result).toHaveProperty('expandedUrl');
      expect(result).toHaveProperty('normalizedUrl');
      expect(result).toHaveProperty('features');
      expect(result).toHaveProperty('error');
      expect(result.originalUrl).toBe('https://EXAMPLE.COM/page?z=1&a=1');
      expect(result.error).toBeNull();
    });

    test('should have valid features in result', async () => {
      axios.head.mockResolvedValue({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.analyzeUrl('https://example.com/login');

      expect(result.features).not.toBeNull();
      expect(result.features.urlLength).toBeGreaterThan(0);
      expect(result.features.domainLength).toBeGreaterThan(0);
    });

    test('should normalize URL in analysis', async () => {
      axios.head.mockResolvedValue({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result = await urlAnalyzer.analyzeUrl('http://EXAMPLE.COM/PATH');

      expect(result.normalizedUrl).toContain('https://');
      expect(result.normalizedUrl).toContain('example.com');
    });

    test('should handle malformed URLs gracefully', async () => {
      const result = await urlAnalyzer.analyzeUrl('not-a-valid-url');

      expect(result.originalUrl).toBe('not-a-valid-url');
      expect(result.features === null || result.error !== null).toBe(true);
    });

    test('should handle network errors gracefully', async () => {
      axios.head.mockRejectedValue(new Error('Network error'));
      axios.get.mockRejectedValue(new Error('Network error'));

      const result = await urlAnalyzer.analyzeUrl('https://example.com');

      expect(result).toHaveProperty('originalUrl');
      expect(result).toHaveProperty('error');
      expect(typeof result).toBe('object');
    });
  });

  describe('Integration Tests', () => {
    test('should handle multiple URLs in sequence', async () => {
      axios.head.mockResolvedValue({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const results = await Promise.all([
        urlAnalyzer.analyzeUrl('https://example.com'),
        urlAnalyzer.analyzeUrl('https://test.com'),
        urlAnalyzer.analyzeUrl('https://another.com')
      ]);

      expect(results).toHaveLength(3);
      results.forEach(result => {
        expect(result).toHaveProperty('originalUrl');
        expect(result).toHaveProperty('features');
      });
    });

    test('should maintain mock isolation between tests', async () => {
      axios.head.mockResolvedValueOnce({
        status: 200,
        headers: {},
        config: { url: 'https://example.com' }
      });

      const result1 = await urlAnalyzer.analyzeUrl('https://example.com');
      expect(result1).toHaveProperty('originalUrl');

      axios.head.mockResolvedValueOnce({
        status: 301,
        headers: { location: 'https://redirect.com' },
        config: { url: 'https://test.com' }
      });

      axios.head.mockResolvedValueOnce({
        status: 200,
        headers: {},
        config: { url: 'https://redirect.com' }
      });

      const result2 = await urlAnalyzer.analyzeUrl('https://test.com');
      expect(result2).toHaveProperty('originalUrl');
    });
  });
});
