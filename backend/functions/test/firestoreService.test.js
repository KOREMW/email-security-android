// ============================================
// Firestore 서비스 테스트
// ============================================

jest.mock('firebase-admin', () => ({
  firestore: () => ({
    collection: jest.fn((collectionName) => ({
      doc: jest.fn((docId) => ({
        set: jest.fn().mockResolvedValue(undefined),
        get: jest.fn().mockResolvedValue({
          exists: false,
          data: () => ({})
        })
      })),
      where: jest.fn(function() {
        return this;
      }),
      get: jest.fn().mockResolvedValue({
        size: 0,
        forEach: jest.fn()
      })
    })),
    batch: jest.fn(() => ({
      set: jest.fn(),
      delete: jest.fn(),
      commit: jest.fn().mockResolvedValue(undefined)
    }))
  })
}));

const FirestoreService = require('../src/firestoreService');

describe('Firestore Service Tests', () => {
  let service;
  let mockFirebaseApp;

  beforeEach(() => {
    mockFirebaseApp = {
      firestore: () => ({
        collection: jest.fn((collectionName) => ({
          doc: jest.fn((docId) => ({
            set: jest.fn().mockResolvedValue(undefined),
            get: jest.fn().mockResolvedValue({
              exists: false,
              data: () => ({})
            })
          })),
          where: jest.fn(function() {
            return this;
          }),
          get: jest.fn().mockResolvedValue({
            size: 0,
            forEach: jest.fn()
          })
        })),
        batch: jest.fn(() => ({
          set: jest.fn(),
          delete: jest.fn(),
          commit: jest.fn().mockResolvedValue(undefined)
        }))
      })
    };

    FirestoreService.instance = null;
    service = new FirestoreService(mockFirebaseApp);
  });

  describe('constructor', () => {
    test('should create service instance', () => {
      expect(service).toBeInstanceOf(FirestoreService);
      expect(service.db).toBeDefined();
    });

    test('should initialize write stats', () => {
      const stats = service.getWriteStats();
      expect(stats.total).toBe(0);
      expect(stats.success).toBe(0);
      expect(stats.failed).toBe(0);
    });

    test('should initialize read stats', () => {
      const stats = service.getReadStats();
      expect(stats.total).toBe(0);
      expect(stats.success).toBe(0);
      expect(stats.failed).toBe(0);
      expect(stats.cacheHit).toBe(0);
    });
  });

  describe('_urlToDocId', () => {
    test('should convert URL to document ID', () => {
      const url = 'https://example.com';
      const docId = service._urlToDocId(url);

      expect(typeof docId).toBe('string');
      expect(docId.length).toBeGreaterThan(0);
    });

    test('should generate same ID for same URL', () => {
      const url = 'https://example.com';
      const id1 = service._urlToDocId(url);
      const id2 = service._urlToDocId(url);

      expect(id1).toBe(id2);
    });

    test('should generate different IDs for different URLs', () => {
      const url1 = 'https://example.com';
      const url2 = 'https://google.com';
      const id1 = service._urlToDocId(url1);
      const id2 = service._urlToDocId(url2);

      expect(id1).not.toBe(id2);
    });
  });

  describe('_calculateTTL', () => {
    test('should calculate TTL date', () => {
      const ttl = service._calculateTTL();
      const ttlDate = new Date(ttl);
      const now = new Date();

      expect(ttlDate.getTime()).toBeGreaterThan(now.getTime());
    });

    test('should be approximately 30 days in future', () => {
      const ttl = service._calculateTTL();
      const ttlDate = new Date(ttl);
      const now = new Date();
      const diffDays = (ttlDate - now) / (1000 * 60 * 60 * 24);

      expect(diffDays).toBeGreaterThanOrEqual(29);
      expect(diffDays).toBeLessThanOrEqual(31);
    });
  });

  describe('_isExpired', () => {
    test('should return false for future TTL', () => {
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 10);

      expect(service._isExpired(futureDate.toISOString())).toBe(false);
    });

    test('should return true for past TTL', () => {
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 10);

      expect(service._isExpired(pastDate.toISOString())).toBe(true);
    });

    test('should return false for no TTL', () => {
      expect(service._isExpired(null)).toBe(false);
      expect(service._isExpired(undefined)).toBe(false);
    });
  });

  describe('saveAnalysis', () => {
    test('should save analysis result', async () => {
      const url = 'https://example.com';
      const analysisResult = {
        finalRiskScore: 0.5,
        suspiciousLevel: 'SUSPICIOUS',
        suspicious: true,
        confidence: 0.9
      };

      const docId = await service.saveAnalysis(url, analysisResult);

      expect(docId).toBeDefined();
      expect(typeof docId).toBe('string');
    });

    test('should update write stats on success', async () => {
      const url = 'https://example.com';
      const analysisResult = {
        finalRiskScore: 0.5,
        suspiciousLevel: 'SUSPICIOUS',
        suspicious: true,
        confidence: 0.9
      };

      await service.saveAnalysis(url, analysisResult);

      const stats = service.getWriteStats();
      expect(stats.total).toBe(1);
      expect(stats.success).toBe(1);
      expect(stats.failed).toBe(0);
    });
  });

  describe('getAnalysis', () => {
    test('should return null for non-existent URL', async () => {
      const url = 'https://nonexistent.com';

      const result = await service.getAnalysis(url);

      expect(result).toBeNull();
    });

    test('should update read stats', async () => {
      const url = 'https://example.com';

      await service.getAnalysis(url);

      const stats = service.getReadStats();
      expect(stats.total).toBe(1);
    });
  });

  describe('saveBatch', () => {
    test('should return empty array for empty input', async () => {
      const results = await service.saveBatch([]);

      expect(Array.isArray(results)).toBe(true);
      expect(results.length).toBe(0);
    });

    test('should save multiple analyses', async () => {
      const analyses = [
        {
          url: 'https://example.com',
          analysisResult: {
            finalRiskScore: 0.5,
            suspiciousLevel: 'SUSPICIOUS'
          }
        },
        {
          url: 'https://google.com',
          analysisResult: {
            finalRiskScore: 0.2,
            suspiciousLevel: 'SAFE'
          }
        }
      ];

      const docIds = await service.saveBatch(analyses);

      expect(Array.isArray(docIds)).toBe(true);
      expect(docIds.length).toBe(2);
    });
  });

  describe('getBatch', () => {
    test('should return empty array for empty input', async () => {
      const results = await service.getBatch([]);

      expect(Array.isArray(results)).toBe(true);
      expect(results.length).toBe(0);
    });

    test('should retrieve multiple analyses', async () => {
      const urls = ['https://example.com', 'https://google.com'];

      const results = await service.getBatch(urls);

      expect(Array.isArray(results)).toBe(true);
    });
  });

  describe('saveLog', () => {
    test('should save log entry', async () => {
      const logData = {
        action: 'analyze_url',
        url: 'https://example.com',
        status: 'success'
      };

      const logId = await service.saveLog(logData);

      expect(logId).toBeDefined();
      expect(typeof logId).toBe('string');
    });
  });

  describe('getStats', () => {
    test('should return write stats with success rate', () => {
      const stats = service.getWriteStats();

      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('success');
      expect(stats).toHaveProperty('failed');
      expect(stats).toHaveProperty('successRate');
    });

    test('should return read stats with hit rate', () => {
      const stats = service.getReadStats();

      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('success');
      expect(stats).toHaveProperty('failed');
      expect(stats).toHaveProperty('cacheHit');
      expect(stats).toHaveProperty('hitRate');
    });

    test('should show N/A when no operations', () => {
      const freshService = new FirestoreService(mockFirebaseApp);
      const stats = freshService.getWriteStats();

      expect(stats.successRate).toBe('N/A');
    });
  });

  describe('cleanup', () => {
    test('should handle cleanup with no documents', async () => {
      const deleted = await service.cleanup(30);

      expect(typeof deleted).toBe('number');
      expect(deleted).toBe(0);
    });
  });

  describe('singleton pattern', () => {
    test('should return same instance on subsequent calls', () => {
      FirestoreService.instance = null;
      const service1 = FirestoreService.getInstance(mockFirebaseApp);
      const service2 = FirestoreService.getInstance(mockFirebaseApp);

      expect(service1).toBe(service2);
    });
  });
});
