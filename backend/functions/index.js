// ============================================
// Firebase Cloud Functions 메인 엔트리포인트
// Phishing 탐지 API + VirusTotal + OpenAI 연동
// 최종 완성본 - 2024.12.10
// ============================================

const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');

// 내부 모듈
const FirestoreService = require('./src/firestoreService');
const CacheManager = require('./src/cacheManager');
const VirusTotalService = require('./src/virusTotalService');
const { FUNCTION_CONFIG } = require('./src/constants');
const urlAnalyzer = require('./src/urlAnalyzer');
const scoreCalculator = require('./src/scoreCalculator');

// ============================================
// Firebase Admin SDK 초기화
// ============================================

if (!admin.apps.length) {
  admin.initializeApp();
}

// ============================================
// 환경 변수에서 API 키 로드
// ============================================

const getVirusTotalApiKey = () => {
  return functions.config().virustotal?.apikey || process.env.VIRUSTOTAL_API_KEY || null;
};

const getOpenAIApiKey = () => {
  return functions.config().openai?.apikey || process.env.OPENAI_API_KEY || null;
};

// ============================================
// 유틸리티 함수
// ============================================

/**
 * URL 유효성 검증
 * @param {string} urlString - 검증할 URL
 * @returns {boolean} 유효하면 true
 */
function isValidUrl(urlString) {
  try {
    const url = new URL(urlString);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch (e) {
    return false;
  }
}

/**
 * localRiskScore 검증 및 정규화
 * @param {*} score - 입력 점수
 * @returns {number} 0.0 ~ 1.0 범위의 점수
 */
function validateLocalScore(score) {
  if (score === null || score === undefined) {
    return 0.5; // 기본값 (중립)
  }
  const parsed = parseFloat(score);
  if (isNaN(parsed)) {
    return 0.5;
  }
  return Math.max(0, Math.min(1, parsed)); // 0.0 ~ 1.0 클램핑
}

/**
 * 딜레이 함수
 * @param {number} ms - 대기 시간 (밀리초)
 */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ============================================
// 1️⃣ 핵심 API: 의심 URL 분석 (모바일 앱에서 호출)
// POST /analyzeSuspiciousUrl
// ============================================

exports.analyzeSuspiciousUrl = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 60, memory: '512MB' })
  .https.onRequest(async (req, res) => {
    try {
      // CORS 처리
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      // 요청 파라미터 추출
      const { url, source = 'app', localRiskScore = null } = req.body;

      // URL 필수 검증
      if (!url) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required parameter: url'
        });
        return;
      }

      // URL 형식 검증
      if (!isValidUrl(url)) {
        res.status(400).json({
          error: 'Invalid URL Format',
          message: `Invalid URL: ${url}`
        });
        return;
      }

      // localRiskScore 검증
      const validatedLocalScore = validateLocalScore(localRiskScore);

      functions.logger.info('analyzeSuspiciousUrl 요청:', { 
        url, 
        source, 
        localRiskScore: validatedLocalScore 
      });

      // ============================================
      // Step 1: 캐시 확인 (메모리 → Firestore)
      // ============================================
      const cache = CacheManager.getInstance();
      const cachedResult = cache.get(url);
      
      if (cachedResult) {
        functions.logger.info('메모리 캐시 히트:', url);
        res.status(200).json({
          ...cachedResult,
          cached: true,
          cacheSource: 'memory',
          timestamp: new Date().toISOString()
        });
        return;
      }

      // Firestore 캐시 확인
      const firestoreService = FirestoreService.getInstance();
      const firestoreCached = await firestoreService.getAnalysis(url);
      
      if (firestoreCached) {
        // 메모리 캐시에도 저장
        cache.set(url, firestoreCached);
        functions.logger.info('Firestore 캐시 히트:', url);
        res.status(200).json({
          ...firestoreCached,
          cached: true,
          cacheSource: 'firestore',
          timestamp: new Date().toISOString()
        });
        return;
      }

      // ============================================
      // Step 2: URL 정규화 + 피처 추출
      // ============================================
      const urlAnalysis = await urlAnalyzer.analyzeUrl(url);
      functions.logger.info('URL 분석 완료:', { 
        normalizedUrl: urlAnalysis.normalizedUrl 
      });

      // ============================================
      // Step 3: VirusTotal API 분석
      // ============================================
      let virusTotalResult = {
        malicious: 0,
        harmless: 0,
        undetected: 0,
        suspicious: 0,
        status: 'skipped'
      };

      const vtApiKey = getVirusTotalApiKey();
      
      if (vtApiKey) {
        try {
          const vtService = VirusTotalService.getInstance(vtApiKey);
          
          // 먼저 캐시된 결과 확인 (빠름)
          const vtCached = await vtService.getUrlReport(url).catch(() => null);
          
          if (vtCached) {
            virusTotalResult = {
              malicious: vtCached.malicious || 0,
              harmless: vtCached.harmless || 0,
              undetected: vtCached.undetected || 0,
              suspicious: vtCached.suspicious || 0,
              status: 'cached'
            };
            functions.logger.info('VirusTotal 캐시 결과:', virusTotalResult);
          } else {
            // 새로 분석 요청 (시간 소요)
            const vtAnalysis = await vtService.analyzeUrl(url, 5, 3000);
            virusTotalResult = {
              malicious: vtAnalysis.malicious || 0,
              harmless: vtAnalysis.harmless || 0,
              undetected: vtAnalysis.undetected || 0,
              suspicious: vtAnalysis.suspicious || 0,
              status: 'analyzed'
            };
            functions.logger.info('VirusTotal 분석 결과:', virusTotalResult);
          }
        } catch (vtError) {
          functions.logger.warn('VirusTotal API 오류 (계속 진행):', vtError.message);
          virusTotalResult.status = 'error';
          virusTotalResult.errorMessage = vtError.message;
        }
      } else {
        functions.logger.warn('VirusTotal API 키 없음, 스킵');
      }

      // ============================================
      // Step 4: 앙상블 점수 계산
      // ============================================
      const finalResult = scoreCalculator.calculateFinalRiskScore(
        validatedLocalScore,
        virusTotalResult
      );

      // suspicious 플래그
      const suspicious = finalResult.suspiciousLevel === 'SUSPICIOUS' || 
                        finalResult.suspiciousLevel === 'DANGEROUS';

      // ============================================
      // Step 5: 최종 응답 객체 구성
      // ============================================
      const analysisResult = {
        // 기본 정보
        url: url,
        originalUrl: urlAnalysis.originalUrl,
        normalizedUrl: urlAnalysis.normalizedUrl,
        
        // 분석 결과
        suspicious: suspicious,
        finalRiskScore: finalResult.finalRiskScore,
        suspiciousLevel: finalResult.suspiciousLevel,
        confidence: finalResult.confidence,
        
        // VirusTotal 결과
        virusTotal: virusTotalResult,
        
        // 점수 상세
        breakdown: finalResult.breakdown,
        
        // URL 피처
        urlFeatures: urlAnalysis.features,
        
        // 메타데이터
        source: source,
        cached: false,
        analyzedAt: new Date().toISOString(),
        
        // 에러 정보
        error: urlAnalysis.error || null
      };

      // ============================================
      // Step 6: Firestore + 캐시에 저장
      // ============================================
      try {
        await firestoreService.saveAnalysis(url, analysisResult);
        cache.set(url, analysisResult);
        functions.logger.info('분석 결과 저장 완료:', url);
      } catch (saveError) {
        functions.logger.error('저장 오류 (응답은 정상 반환):', saveError.message);
      }

      // ============================================
      // Step 7: 응답 반환
      // ============================================
      res.status(200).json({
        ...analysisResult,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('analyzeSuspiciousUrl 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 2️⃣ AI 분석 보고서 생성 (OpenAI 연동) - 요구사항 필수
// POST /getAIAnalysis
// ============================================

exports.getAIAnalysis = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 30, memory: '256MB' })
  .https.onRequest(async (req, res) => {
    try {
      // CORS 처리
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      const { url, analysisResult } = req.body;

      if (!url || !analysisResult) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required fields: url, analysisResult'
        });
        return;
      }

      const openaiApiKey = getOpenAIApiKey();
      
      if (!openaiApiKey) {
        res.status(503).json({
          error: 'Service Unavailable',
          message: 'OpenAI API key not configured'
        });
        return;
      }

      functions.logger.info('AI 분석 보고서 생성 요청:', url);

      // ============================================
      // OpenAI API 호출
      // ============================================
      const prompt = generateAIPrompt(url, analysisResult);
      
      const openaiResponse = await axios.post(
        'https://api.openai.com/v1/chat/completions',
        {
          model: 'gpt-4o-mini', // 최신 모델 (저렴 + 빠름)
          messages: [
            {
              role: 'system',
              content: `당신은 사이버 보안 전문가입니다. URL 분석 결과를 바탕으로 사용자에게 이해하기 쉬운 보안 보고서를 작성해주세요.

응답 형식 (JSON):
{
  "riskSummary": "한 줄 요약",
  "riskFactors": ["위험 요소 1", "위험 요소 2"],
  "recommendations": ["권장 행동 1", "권장 행동 2", "권장 행동 3"],
  "details": "상세 설명 (2-3문장)"
}

반드시 위 JSON 형식으로만 응답하세요.`
            },
            {
              role: 'user',
              content: prompt
            }
          ],
          max_tokens: 500,
          temperature: 0.3 // 일관된 응답을 위해 낮춤
        },
        {
          headers: {
            'Authorization': `Bearer ${openaiApiKey}`,
            'Content-Type': 'application/json'
          },
          timeout: 25000
        }
      );

      const aiContent = openaiResponse.data.choices[0].message.content;
      
      // JSON 파싱 시도
      let aiReport;
      try {
        aiReport = JSON.parse(aiContent);
      } catch (parseError) {
        // JSON 파싱 실패 시 기본 구조 사용
        aiReport = {
          riskSummary: aiContent.substring(0, 100),
          riskFactors: ['분석 결과를 확인하세요'],
          recommendations: [
            '출처가 불분명한 링크는 클릭하지 마세요',
            '개인정보 입력을 요구하면 의심하세요',
            '의심스러운 경우 IT 보안팀에 문의하세요'
          ],
          details: aiContent
        };
      }

      functions.logger.info('AI 보고서 생성 완료:', url);

      res.status(200).json({
        url: url,
        aiInsight: aiReport.details || aiReport.riskSummary,
        riskSummary: aiReport.riskSummary,
        riskFactors: aiReport.riskFactors || [],
        recommendations: aiReport.recommendations || [],
        analysisScore: analysisResult.finalRiskScore || 0,
        suspiciousLevel: analysisResult.suspiciousLevel || 'UNKNOWN',
        generatedAt: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('getAIAnalysis 오류:', error);
      
      // OpenAI API 오류 상세 처리
      if (error.response?.status === 429) {
        res.status(429).json({
          error: 'Rate Limit Exceeded',
          message: 'OpenAI API 호출 한도 초과. 잠시 후 다시 시도하세요.'
        });
        return;
      }

      if (error.response?.status === 401) {
        res.status(401).json({
          error: 'Unauthorized',
          message: 'OpenAI API 키가 유효하지 않습니다.'
        });
        return;
      }
      
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 3️⃣ 간단한 URL 분석 (VirusTotal 없이, 저장 옵션)
// GET/POST /analyzeUrl
// ============================================

exports.analyzeUrl = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    try {
      // CORS 처리
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      // GET 또는 POST 허용
      let url, save;
      if (req.method === 'GET') {
        url = req.query.url;
        save = req.query.save === 'true';
      } else if (req.method === 'POST') {
        url = req.body.url;
        save = req.body.save === true;
      } else {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Use GET or POST'
        });
        return;
      }

      if (!url) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required parameter: url'
        });
        return;
      }

      if (!isValidUrl(url)) {
        res.status(400).json({
          error: 'Invalid URL Format',
          message: `Invalid URL: ${url}`
        });
        return;
      }

      // 캐시 확인
      const cache = CacheManager.getInstance();
      const cachedResult = cache.get(url);
      if (cachedResult) {
        res.status(200).json({
          ...cachedResult,
          cached: true,
          timestamp: new Date().toISOString()
        });
        return;
      }

      // URL 분석
      const analysis = await urlAnalyzer.analyzeUrl(url);
      
      // 저장 옵션이 true일 때만 저장
      if (save) {
        const firestoreService = FirestoreService.getInstance();
        await firestoreService.saveAnalysis(url, analysis);
        cache.set(url, analysis);
      }

      res.status(200).json({
        ...analysis,
        saved: save,
        cached: false,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('analyzeUrl 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message
      });
    }
  });

// ============================================
// 4️⃣ 분석 결과 저장 API
// POST /saveAnalysis
// ============================================

exports.saveAnalysis = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      const { url, analysisData } = req.body;

      if (!url || !analysisData) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required fields: url, analysisData'
        });
        return;
      }

      if (!isValidUrl(url)) {
        res.status(400).json({
          error: 'Invalid URL Format',
          message: `Invalid URL: ${url}`
        });
        return;
      }

      const firestoreService = FirestoreService.getInstance();
      await firestoreService.saveAnalysis(url, analysisData);

      const cache = CacheManager.getInstance();
      cache.set(url, analysisData);

      res.status(201).json({
        message: 'Analysis saved successfully',
        url,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('saveAnalysis 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message
      });
    }
  });

// ============================================
// 5️⃣ 캐시 통계 조회 API
// GET /getCacheStats
// ============================================

exports.getCacheStats = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    try {
      res.set('Access-Control-Allow-Origin', '*');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'GET') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only GET is allowed'
        });
        return;
      }

      const cache = CacheManager.getInstance();
      const firestoreService = FirestoreService.getInstance();

      res.status(200).json({
        memoryCache: cache.getStats(),
        firestoreStats: {
          write: firestoreService.getWriteStats(),
          read: firestoreService.getReadStats()
        },
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('getCacheStats 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message
      });
    }
  });

// ============================================
// 6️⃣ 캐시 정리 API
// POST /cleanupCache
// ============================================

exports.cleanupCache = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    try {
      res.set('Access-Control-Allow-Origin', '*');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      // 메모리 캐시 정리
      const cache = CacheManager.getInstance();
      const memoryCleaned = cache.cleanup();

      // Firestore 정리
      const firestoreService = FirestoreService.getInstance();
      const firestoreCleaned = await firestoreService.cleanup(7); // 7일 이상

      res.status(200).json({
        message: 'Cache cleanup completed',
        memoryCacheDeleted: memoryCleaned,
        firestoreDeleted: firestoreCleaned,
        cacheStats: cache.getStats(),
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('cleanupCache 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message
      });
    }
  });

// ============================================
// 7️⃣ 헬스 체크 API
// GET /healthCheck
// ============================================

exports.healthCheck = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    try {
      res.set('Access-Control-Allow-Origin', '*');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      const cache = CacheManager.getInstance();
      const vtApiKey = getVirusTotalApiKey();
      const openaiApiKey = getOpenAIApiKey();

      const health = {
        status: 'OK',
        timestamp: new Date().toISOString(),
        services: {
          firestore: {
            status: 'running'
          },
          memoryCache: {
            status: 'running',
            stats: cache.getStats()
          },
          virusTotal: {
            status: vtApiKey ? 'configured' : 'not_configured'
          },
          openai: {
            status: openaiApiKey ? 'configured' : 'not_configured'
          }
        },
        endpoints: {
          core: [
            'POST /analyzeSuspiciousUrl - ML+VT 앙상블 분석',
            'POST /getAIAnalysis - AI 보고서 생성'
          ],
          utility: [
            'GET|POST /analyzeUrl - 간단 URL 분석',
            'POST /saveAnalysis - 결과 저장',
            'GET /getCacheStats - 캐시 통계',
            'POST /cleanupCache - 캐시 정리',
            'POST /batchAnalyzeUrls - 배치 분석'
          ]
        }
      };

      res.status(200).json(health);

    } catch (error) {
      functions.logger.error('healthCheck 오류:', error);
      res.status(500).json({
        status: 'ERROR',
        error: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 8️⃣ 배치 분석 API (Rate Limit 개선)
// POST /batchAnalyzeUrls
// ============================================

exports.batchAnalyzeUrls = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 300, memory: '1GB' })
  .https.onRequest(async (req, res) => {
    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      const { urls, localRiskScores = [] } = req.body;

      if (!Array.isArray(urls) || urls.length === 0) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing or invalid urls array'
        });
        return;
      }

      // 배치 크기 제한 (Rate Limit + 타임아웃 고려)
      if (urls.length > 30) {
        res.status(400).json({
          error: 'Bad Request',
          message: 'Maximum 30 URLs allowed per batch (Rate Limit protection)'
        });
        return;
      }

      // URL 검증
      const invalidUrls = urls.filter(url => !isValidUrl(url));
      if (invalidUrls.length > 0) {
        res.status(400).json({
          error: 'Invalid URLs',
          invalidUrls
        });
        return;
      }

      functions.logger.info('배치 분석 시작:', { count: urls.length });

      const results = [];
      const firestoreService = FirestoreService.getInstance();
      const cache = CacheManager.getInstance();
      const vtApiKey = getVirusTotalApiKey();
      const vtService = vtApiKey ? VirusTotalService.getInstance(vtApiKey) : null;

      for (let i = 0; i < urls.length; i++) {
        const url = urls[i];
        const localScore = validateLocalScore(localRiskScores[i]);

        try {
          // URL 분석
          const urlAnalysis = await urlAnalyzer.analyzeUrl(url);
          
          // VirusTotal (캐시만 조회 - 속도 최적화)
          let vtResult = { malicious: 0, harmless: 0, undetected: 0, status: 'skipped' };
          if (vtService) {
            try {
              const vtCached = await vtService.getUrlReport(url).catch(() => null);
              if (vtCached) {
                vtResult = { 
                  malicious: vtCached.malicious || 0,
                  harmless: vtCached.harmless || 0,
                  undetected: vtCached.undetected || 0,
                  status: 'cached' 
                };
              }
            } catch (e) {
              vtResult.status = 'error';
            }
          }

          // 점수 계산
          const finalResult = scoreCalculator.calculateFinalRiskScore(localScore, vtResult);
          
          const analysisResult = {
            url,
            suspicious: finalResult.suspiciousLevel !== 'SAFE',
            finalRiskScore: finalResult.finalRiskScore,
            suspiciousLevel: finalResult.suspiciousLevel,
            virusTotal: vtResult,
            status: 'success'
          };

          // 저장
          await firestoreService.saveAnalysis(url, analysisResult);
          cache.set(url, analysisResult);
          
          results.push(analysisResult);

        } catch (error) {
          results.push({
            url,
            status: 'error',
            error: error.message
          });
        }

        // ✅ Rate Limit 보호: 10개마다 1초 대기
        if (i > 0 && i % 10 === 0) {
          functions.logger.info(`Rate Limit 대기: ${i}/${urls.length} 완료`);
          await sleep(1000);
        }
      }

      const successCount = results.filter(r => r.status === 'success').length;
      const failCount = results.filter(r => r.status === 'error').length;

      functions.logger.info('배치 분석 완료:', { 
        total: urls.length, 
        success: successCount, 
        failed: failCount 
      });

      res.status(200).json({
        message: 'Batch analysis completed',
        total: urls.length,
        success: successCount,
        failed: failCount,
        results,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      functions.logger.error('batchAnalyzeUrls 오류:', error);
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message
      });
    }
  });

// ============================================
// 9️⃣ 정기적 캐시 정리 (Cloud Scheduler)
// 매 시간 실행
// ============================================

exports.scheduledCacheCleanup = functions
  .region(FUNCTION_CONFIG.REGION)
  .pubsub.schedule('every 60 minutes')
  .onRun(async (context) => {
    try {
      // 메모리 캐시 정리
      const cache = CacheManager.getInstance();
      const memoryCleaned = cache.cleanup();

      // ✅ Firestore 정리 추가
      const firestoreService = FirestoreService.getInstance();
      const firestoreCleaned = await firestoreService.cleanup(7);
      
      functions.logger.info('정기 캐시 정리 완료', {
        memoryCacheDeleted: memoryCleaned,
        firestoreDeleted: firestoreCleaned,
        timestamp: new Date().toISOString()
      });

      return null;
    } catch (error) {
      functions.logger.error('정기 캐시 정리 오류:', error);
      throw error;
    }
  });

// ============================================
// OpenAI 프롬프트 생성 함수
// ============================================

function generateAIPrompt(url, analysisResult) {
  const score = analysisResult.finalRiskScore || 0;
  const level = analysisResult.suspiciousLevel || 'UNKNOWN';
  const vt = analysisResult.virusTotal || {};
  
  return `
다음 URL의 보안 분석 결과입니다:

URL: ${url}
위험도 점수: ${(score * 100).toFixed(1)}점 (100점 만점)
위험 등급: ${level}
VirusTotal 결과:
- 악성 판정: ${vt.malicious || 0}개 보안 엔진
- 정상 판정: ${vt.harmless || 0}개 보안 엔진
- 미탐지: ${vt.undetected || 0}개 보안 엔진

위 정보를 바탕으로 일반 사용자가 이해할 수 있는 보안 보고서를 JSON 형식으로 작성해주세요.
`;
}