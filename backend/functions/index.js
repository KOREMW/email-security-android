// ============================================
// Firebase Cloud Functions 메인 엔트리포인트
// Phishing 탐지 API + VirusTotal + OpenAI 연동
// 업데이트: 에러 로깅 강화 + 캐시 자동 정리 개선
// 최종 완성본 - 2024.12.11
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
const { createLogger, ErrorCategory, measureTime } = require('./src/errorLogger');

// ============================================
// Firebase Admin SDK 초기화
// ============================================

if (!admin.apps.length) {
  admin.initializeApp();
}

// ============================================
// 환경 변수에서 API 키 로드 (우선순위: Secrets > Config > .env)
// 
// Firebase Secrets 설정 방법:
// firebase functions:secrets:set VIRUSTOTAL_API_KEY
// firebase functions:secrets:set OPENAI_API_KEY
// ============================================

const getVirusTotalApiKey = () => {
  // 1순위: Firebase Secrets (process.env로 자동 주입됨)
  // 2순위: Firebase Config (legacy)
  // 3순위: .env 파일
  return process.env.VIRUSTOTAL_API_KEY || 
         functions.config().virustotal?.api_key || 
         null;
};

const getOpenAIApiKey = () => {
  return process.env.OPENAI_API_KEY || 
         functions.config().openai?.api_key || 
         null;
};

// API 키 마스킹 (로깅용)
const maskApiKey = (key) => {
  if (!key) return 'NOT_SET';
  if (key.length <= 8) return '****';
  return key.substring(0, 4) + '****' + key.substring(key.length - 4);
};

// ============================================
// 유틸리티 함수
// ============================================

function isValidUrl(urlString) {
  try {
    const url = new URL(urlString);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch (e) {
    return false;
  }
}

function validateLocalScore(score) {
  if (score === null || score === undefined) {
    return 0.5;
  }
  const parsed = parseFloat(score);
  if (isNaN(parsed)) {
    return 0.5;
  }
  return Math.max(0, Math.min(1, parsed));
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ============================================
// 1️⃣ 핵심 API: 의심 URL 분석
// POST /analyzeSuspiciousUrl
// ============================================

exports.analyzeSuspiciousUrl = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 60, memory: '512MB' })
  .https.onRequest(async (req, res) => {
    const logger = createLogger('analyzeSuspiciousUrl');
    const startTime = Date.now();

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
        logger.warn('잘못된 HTTP 메서드', { method: req.method });
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      const { url, source = 'app', localRiskScore = null } = req.body;

      // URL 검증
      if (!url) {
        logger.warn('URL 누락');
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required parameter: url'
        });
        return;
      }

      if (!isValidUrl(url)) {
        logger.warn('잘못된 URL 형식', { url });
        res.status(400).json({
          error: 'Invalid URL Format',
          message: `Invalid URL: ${url}`
        });
        return;
      }

      const validatedLocalScore = validateLocalScore(localRiskScore);
      logger.info('분석 요청', { url, source, localRiskScore: validatedLocalScore });

      // Step 1: 캐시 확인 (로컬 점수 차이가 크면 재분석)
      const cache = CacheManager.getInstance();
      const firestoreService = FirestoreService.getInstance();
      const SCORE_DIFF_THRESHOLD = 0.1; // 로컬 점수 차이 임계값

      // 메모리 캐시 확인
      const cachedResult = cache.get(url);
      if (cachedResult) {
        const cachedLocalScore = cachedResult.breakdown?.localMlScore ?? 0.5;
        const scoreDiff = Math.abs(cachedLocalScore - validatedLocalScore);
        
        if (scoreDiff < SCORE_DIFF_THRESHOLD) {
          logger.info('메모리 캐시 히트', { url, scoreDiff, duration: Date.now() - startTime });
          res.status(200).json({
            ...cachedResult,
            cached: true,
            cacheSource: 'memory',
            timestamp: new Date().toISOString()
          });
          return;
        }
        logger.info('로컬 점수 변경 감지, 재분석 진행', { 
          url, cachedLocalScore, newLocalScore: validatedLocalScore, scoreDiff 
        });
      }

      // Firestore 캐시 확인
      let firestoreCached = null;
      try {
        firestoreCached = await firestoreService.getAnalysis(url);
      } catch (dbError) {
        logger.error('Firestore 조회 실패', dbError, { url });
      }
      
      if (firestoreCached) {
        const cachedLocalScore = firestoreCached.breakdown?.localMlScore ?? 0.5;
        const scoreDiff = Math.abs(cachedLocalScore - validatedLocalScore);
        
        if (scoreDiff < SCORE_DIFF_THRESHOLD) {
          cache.set(url, firestoreCached);
          logger.info('Firestore 캐시 히트', { url, scoreDiff, duration: Date.now() - startTime });
          res.status(200).json({
            ...firestoreCached,
            cached: true,
            cacheSource: 'firestore',
            timestamp: new Date().toISOString()
          });
          return;
        }
        logger.info('로컬 점수 변경 감지, 재분석 진행', { 
          url, cachedLocalScore, newLocalScore: validatedLocalScore, scoreDiff 
        });
      }

      // Step 2: URL 분석
      let urlAnalysis;
      try {
        urlAnalysis = await urlAnalyzer.analyzeUrl(url);
        logger.info('URL 피처 추출 완료', { normalizedUrl: urlAnalysis.normalizedUrl });
      } catch (analysisError) {
        logger.error('URL 분석 실패', analysisError, { url });
        throw analysisError;
      }

      // Step 3: VirusTotal API
      let virusTotalResult = {
        malicious: 0,
        harmless: 0,
        undetected: 0,
        suspicious: 0,
        status: 'skipped'
      };

      const vtApiKey = getVirusTotalApiKey();
      
      if (vtApiKey) {
        const vtStartTime = Date.now();
        try {
          const vtService = VirusTotalService.getInstance(vtApiKey);
          const vtCached = await vtService.getUrlReport(url).catch(() => null);
          
          if (vtCached) {
            virusTotalResult = {
              malicious: vtCached.malicious || 0,
              harmless: vtCached.harmless || 0,
              undetected: vtCached.undetected || 0,
              suspicious: vtCached.suspicious || 0,
              status: 'cached'
            };
            logger.info('VirusTotal 캐시 결과', { ...virusTotalResult, duration: Date.now() - vtStartTime });
          } else {
            const vtAnalysis = await vtService.analyzeUrl(url, 5, 3000);
            virusTotalResult = {
              malicious: vtAnalysis.malicious || 0,
              harmless: vtAnalysis.harmless || 0,
              undetected: vtAnalysis.undetected || 0,
              suspicious: vtAnalysis.suspicious || 0,
              status: 'analyzed'
            };
            logger.info('VirusTotal 분석 완료', { ...virusTotalResult, duration: Date.now() - vtStartTime });
          }
        } catch (vtError) {
          logger.error('VirusTotal API 오류', vtError, { 
            url, 
            duration: Date.now() - vtStartTime,
            apiKeySet: !!vtApiKey 
          });
          virusTotalResult.status = 'error';
          virusTotalResult.errorMessage = vtError.message;
        }
      } else {
        logger.warn('VirusTotal API 키 미설정');
      }

      // Step 4: 앙상블 점수 계산
      const finalResult = scoreCalculator.calculateFinalRiskScore(
        validatedLocalScore,
        virusTotalResult
      );

      const suspicious = finalResult.suspiciousLevel === 'SUSPICIOUS' || 
                        finalResult.suspiciousLevel === 'DANGEROUS';

      // Step 5: 응답 구성
      const analysisResult = {
        url: url,
        originalUrl: urlAnalysis.originalUrl,
        normalizedUrl: urlAnalysis.normalizedUrl,
        suspicious: suspicious,
        finalRiskScore: finalResult.finalRiskScore,
        suspiciousLevel: finalResult.suspiciousLevel,
        confidence: finalResult.confidence,
        virusTotal: virusTotalResult,
        breakdown: finalResult.breakdown,
        urlFeatures: urlAnalysis.features,
        source: source,
        cached: false,
        analyzedAt: new Date().toISOString(),
        error: urlAnalysis.error || null
      };

      // Step 6: 저장
      try {
        await firestoreService.saveAnalysis(url, analysisResult);
        cache.set(url, analysisResult);
        logger.info('분석 결과 저장 완료', { url });
      } catch (saveError) {
        logger.error('저장 오류 (응답은 정상 반환)', saveError, { url });
      }

      // Step 7: 응답
      const totalDuration = Date.now() - startTime;
      logger.info('분석 완료', { 
        url, 
        suspicious, 
        finalRiskScore: finalResult.finalRiskScore,
        duration: totalDuration 
      });

      res.status(200).json({
        ...analysisResult,
        processingTime: totalDuration,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      const duration = Date.now() - startTime;
      logger.critical('분석 중 치명적 오류', error, { duration });
      
      // Firestore에 에러 로그 저장 (선택적)
      try {
        const firestoreService = FirestoreService.getInstance();
        await firestoreService.saveLog({
          type: 'ERROR',
          endpoint: 'analyzeSuspiciousUrl',
          error: error.message,
          stack: error.stack,
          duration
        });
      } catch (logError) {
        // 무시
      }

      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 2️⃣ AI 분석 보고서 생성
// POST /getAIAnalysis
// ============================================

exports.getAIAnalysis = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 30, memory: '256MB' })
  .https.onRequest(async (req, res) => {
    const logger = createLogger('getAIAnalysis');
    const startTime = Date.now();

    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        logger.warn('잘못된 HTTP 메서드', { method: req.method });
        res.status(405).json({
          error: 'Method Not Allowed',
          message: 'Only POST is allowed'
        });
        return;
      }

      const { url, analysisResult } = req.body;

      if (!url || !analysisResult) {
        logger.warn('필수 파라미터 누락', { url: !!url, analysisResult: !!analysisResult });
        res.status(400).json({
          error: 'Bad Request',
          message: 'Missing required fields: url, analysisResult'
        });
        return;
      }

      const openaiApiKey = getOpenAIApiKey();
      
      if (!openaiApiKey) {
        logger.error('OpenAI API 키 미설정', null);
        res.status(503).json({
          error: 'Service Unavailable',
          message: 'OpenAI API key not configured'
        });
        return;
      }

      logger.info('AI 보고서 생성 시작', { url, apiKeyMasked: maskApiKey(openaiApiKey) });

      // OpenAI API 호출
      const prompt = generateAIPrompt(url, analysisResult);
      const openaiStartTime = Date.now();
      
      let openaiResponse;
      try {
        openaiResponse = await axios.post(
          'https://api.openai.com/v1/chat/completions',
          {
            model: 'gpt-4o-mini',
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
              { role: 'user', content: prompt }
            ],
            max_tokens: 500,
            temperature: 0.3
          },
          {
            headers: {
              'Authorization': `Bearer ${openaiApiKey}`,
              'Content-Type': 'application/json'
            },
            timeout: 25000
          }
        );
        
        logger.info('OpenAI API 호출 성공', { 
          duration: Date.now() - openaiStartTime,
          model: 'gpt-4o-mini'
        });
      } catch (openaiError) {
        logger.error('OpenAI API 호출 실패', openaiError, {
          status: openaiError.response?.status,
          statusText: openaiError.response?.statusText,
          duration: Date.now() - openaiStartTime
        });

        if (openaiError.response?.status === 429) {
          res.status(429).json({
            error: 'Rate Limit Exceeded',
            message: 'OpenAI API 호출 한도 초과. 잠시 후 다시 시도하세요.'
          });
          return;
        }

        if (openaiError.response?.status === 401) {
          res.status(401).json({
            error: 'Unauthorized',
            message: 'OpenAI API 키가 유효하지 않습니다.'
          });
          return;
        }

        throw openaiError;
      }

      const aiContent = openaiResponse.data.choices[0].message.content;
      
      let aiReport;
      try {
        aiReport = JSON.parse(aiContent);
      } catch (parseError) {
        logger.warn('AI 응답 JSON 파싱 실패, 기본 구조 사용', { content: aiContent.substring(0, 100) });
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

      const totalDuration = Date.now() - startTime;
      logger.info('AI 보고서 생성 완료', { url, duration: totalDuration });

      res.status(200).json({
        url: url,
        aiInsight: aiReport.details || aiReport.riskSummary,
        riskSummary: aiReport.riskSummary,
        riskFactors: aiReport.riskFactors || [],
        recommendations: aiReport.recommendations || [],
        analysisScore: analysisResult.finalRiskScore || 0,
        suspiciousLevel: analysisResult.suspiciousLevel || 'UNKNOWN',
        processingTime: totalDuration,
        generatedAt: new Date().toISOString()
      });

    } catch (error) {
      const duration = Date.now() - startTime;
      logger.critical('AI 보고서 생성 중 치명적 오류', error, { duration });
      
      res.status(500).json({
        error: 'Internal Server Error',
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 3️⃣ 간단한 URL 분석
// GET/POST /analyzeUrl
// ============================================

exports.analyzeUrl = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    const logger = createLogger('analyzeUrl');

    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      let url, save;
      if (req.method === 'GET') {
        url = req.query.url;
        save = req.query.save === 'true';
      } else if (req.method === 'POST') {
        url = req.body.url;
        save = req.body.save === true;
      } else {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      if (!url) {
        res.status(400).json({ error: 'Bad Request', message: 'Missing url' });
        return;
      }

      if (!isValidUrl(url)) {
        res.status(400).json({ error: 'Invalid URL Format' });
        return;
      }

      const cache = CacheManager.getInstance();
      const cachedResult = cache.get(url);
      if (cachedResult) {
        res.status(200).json({ ...cachedResult, cached: true });
        return;
      }

      const analysis = await urlAnalyzer.analyzeUrl(url);
      
      if (save) {
        const firestoreService = FirestoreService.getInstance();
        await firestoreService.saveAnalysis(url, analysis);
        cache.set(url, analysis);
      }

      res.status(200).json({ ...analysis, saved: save, cached: false });

    } catch (error) {
      logger.error('URL 분석 오류', error);
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
    }
  });

// ============================================
// 4️⃣ 분석 결과 저장 API
// POST /saveAnalysis
// ============================================

exports.saveAnalysis = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    const logger = createLogger('saveAnalysis');

    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      const { url, analysisData } = req.body;

      if (!url || !analysisData) {
        res.status(400).json({ error: 'Bad Request', message: 'Missing url or analysisData' });
        return;
      }

      if (!isValidUrl(url)) {
        res.status(400).json({ error: 'Invalid URL Format' });
        return;
      }

      const firestoreService = FirestoreService.getInstance();
      await firestoreService.saveAnalysis(url, analysisData);

      const cache = CacheManager.getInstance();
      cache.set(url, analysisData);

      logger.info('분석 결과 저장 완료', { url });

      res.status(201).json({
        message: 'Analysis saved successfully',
        url,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      logger.error('저장 오류', error);
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
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
      
      if (req.method !== 'GET' && req.method !== 'OPTIONS') {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
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
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
    }
  });

// ============================================
// 6️⃣ 캐시 정리 API (수동 + 강화)
// POST /cleanupCache
// ============================================

exports.cleanupCache = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 120, memory: '512MB' })
  .https.onRequest(async (req, res) => {
    const logger = createLogger('cleanupCache');

    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      // 옵션: 며칠 이상 된 데이터 삭제 (기본 7일)
      const daysOld = req.body.daysOld || 7;
      
      logger.info('캐시 정리 시작', { daysOld });

      // 메모리 캐시 정리
      const cache = CacheManager.getInstance();
      const memoryCleaned = cache.cleanup();

      // Firestore 정리 (오래된 분석 결과)
      const firestoreService = FirestoreService.getInstance();
      const firestoreCleaned = await firestoreService.cleanup(daysOld);

      // 에러 로그도 정리 (30일 이상)
      let logsCleaned = 0;
      try {
        const db = admin.firestore();
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 30);
        
        const logsQuery = db.collection('analysis_logs')
          .where('timestamp', '<', cutoffDate.toISOString())
          .limit(500);
        
        const logsSnapshot = await logsQuery.get();
        
        if (!logsSnapshot.empty) {
          const batch = db.batch();
          logsSnapshot.forEach(doc => batch.delete(doc.ref));
          await batch.commit();
          logsCleaned = logsSnapshot.size;
        }
      } catch (logCleanError) {
        logger.warn('로그 정리 중 오류 (무시)', { error: logCleanError.message });
      }

      logger.info('캐시 정리 완료', { 
        memoryCleaned, 
        firestoreCleaned,
        logsCleaned 
      });

      res.status(200).json({
        message: 'Cache cleanup completed',
        results: {
          memoryCache: memoryCleaned,
          firestoreAnalysis: firestoreCleaned,
          oldLogs: logsCleaned
        },
        cacheStats: cache.getStats(),
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      logger.error('캐시 정리 오류', error);
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
    }
  });

// ============================================
// 7️⃣ 헬스 체크 API (강화)
// GET /healthCheck
// ============================================

exports.healthCheck = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    const logger = createLogger('healthCheck');

    try {
      res.set('Access-Control-Allow-Origin', '*');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      const cache = CacheManager.getInstance();
      const vtApiKey = getVirusTotalApiKey();
      const openaiApiKey = getOpenAIApiKey();

      // Firestore 연결 테스트
      let firestoreStatus = 'unknown';
      try {
        const db = admin.firestore();
        await db.collection('_health_check').doc('test').set({ 
          timestamp: new Date().toISOString() 
        });
        firestoreStatus = 'connected';
      } catch (dbError) {
        firestoreStatus = 'error: ' + dbError.message;
        logger.warn('Firestore 헬스 체크 실패', { error: dbError.message });
      }

      const health = {
        status: 'OK',
        version: '2.0.0',
        timestamp: new Date().toISOString(),
        services: {
          firestore: {
            status: firestoreStatus
          },
          memoryCache: {
            status: 'running',
            stats: cache.getStats()
          },
          virusTotal: {
            status: vtApiKey ? 'configured' : 'not_configured',
            keyMasked: maskApiKey(vtApiKey)
          },
          openai: {
            status: openaiApiKey ? 'configured' : 'not_configured',
            keyMasked: maskApiKey(openaiApiKey)
          }
        },
        environment: {
          region: FUNCTION_CONFIG.REGION,
          nodeVersion: process.version
        },
        endpoints: {
          core: [
            'POST /analyzeSuspiciousUrl',
            'POST /getAIAnalysis'
          ],
          utility: [
            'GET|POST /analyzeUrl',
            'POST /saveAnalysis',
            'GET /getCacheStats',
            'POST /cleanupCache',
            'POST /batchAnalyzeUrls'
          ]
        }
      };

      logger.info('헬스 체크 완료', { firestoreStatus });
      res.status(200).json(health);

    } catch (error) {
      logger.error('헬스 체크 오류', error);
      res.status(500).json({
        status: 'ERROR',
        error: error.message,
        timestamp: new Date().toISOString()
      });
    }
  });

// ============================================
// 8️⃣ 배치 분석 API
// POST /batchAnalyzeUrls
// ============================================

exports.batchAnalyzeUrls = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 300, memory: '1GB' })
  .https.onRequest(async (req, res) => {
    const logger = createLogger('batchAnalyzeUrls');
    const startTime = Date.now();

    try {
      res.set('Access-Control-Allow-Origin', '*');
      res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
      res.set('Access-Control-Allow-Headers', 'Content-Type');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'POST') {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      const { urls, localRiskScores = [] } = req.body;

      if (!Array.isArray(urls) || urls.length === 0) {
        res.status(400).json({ error: 'Bad Request', message: 'Missing urls array' });
        return;
      }

      if (urls.length > 30) {
        res.status(400).json({ error: 'Bad Request', message: 'Max 30 URLs allowed' });
        return;
      }

      const invalidUrls = urls.filter(url => !isValidUrl(url));
      if (invalidUrls.length > 0) {
        res.status(400).json({ error: 'Invalid URLs', invalidUrls });
        return;
      }

      logger.info('배치 분석 시작', { count: urls.length });

      const results = [];
      const firestoreService = FirestoreService.getInstance();
      const cache = CacheManager.getInstance();
      const vtApiKey = getVirusTotalApiKey();
      const vtService = vtApiKey ? VirusTotalService.getInstance(vtApiKey) : null;

      for (let i = 0; i < urls.length; i++) {
        const url = urls[i];
        const localScore = validateLocalScore(localRiskScores[i]);

        try {
          const urlAnalysis = await urlAnalyzer.analyzeUrl(url);
          
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

          const finalResult = scoreCalculator.calculateFinalRiskScore(localScore, vtResult);
          
          const analysisResult = {
            url,
            suspicious: finalResult.suspiciousLevel !== 'SAFE',
            finalRiskScore: finalResult.finalRiskScore,
            suspiciousLevel: finalResult.suspiciousLevel,
            virusTotal: vtResult,
            status: 'success'
          };

          await firestoreService.saveAnalysis(url, analysisResult);
          cache.set(url, analysisResult);
          
          results.push(analysisResult);

        } catch (error) {
          logger.warn('개별 URL 분석 실패', { url, error: error.message });
          results.push({ url, status: 'error', error: error.message });
        }

        // Rate Limit 보호
        if (i > 0 && i % 10 === 0) {
          logger.info(`진행 중: ${i}/${urls.length}`);
          await sleep(1000);
        }
      }

      const successCount = results.filter(r => r.status === 'success').length;
      const failCount = results.filter(r => r.status === 'error').length;
      const duration = Date.now() - startTime;

      logger.info('배치 분석 완료', { 
        total: urls.length, 
        success: successCount, 
        failed: failCount,
        duration 
      });

      res.status(200).json({
        message: 'Batch analysis completed',
        total: urls.length,
        success: successCount,
        failed: failCount,
        processingTime: duration,
        results,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      logger.critical('배치 분석 치명적 오류', error);
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
    }
  });

// ============================================
// 9️⃣ 정기적 캐시 정리 (매 시간)
// ============================================

exports.scheduledCacheCleanup = functions
  .region(FUNCTION_CONFIG.REGION)
  .runWith({ timeoutSeconds: 120, memory: '512MB' })
  .pubsub.schedule('every 60 minutes')
  .onRun(async (context) => {
    const logger = createLogger('scheduledCacheCleanup');

    try {
      logger.info('정기 캐시 정리 시작');

      // 메모리 캐시 정리
      const cache = CacheManager.getInstance();
      const memoryCleaned = cache.cleanup();

      // Firestore 분석 결과 정리 (7일 이상)
      const firestoreService = FirestoreService.getInstance();
      const firestoreCleaned = await firestoreService.cleanup(7);

      // 오래된 로그 정리 (30일 이상)
      let logsCleaned = 0;
      try {
        const db = admin.firestore();
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 30);
        
        const logsQuery = db.collection('analysis_logs')
          .where('timestamp', '<', cutoffDate.toISOString())
          .limit(500);
        
        const logsSnapshot = await logsQuery.get();
        
        if (!logsSnapshot.empty) {
          const batch = db.batch();
          logsSnapshot.forEach(doc => batch.delete(doc.ref));
          await batch.commit();
          logsCleaned = logsSnapshot.size;
        }
      } catch (logCleanError) {
        logger.warn('로그 정리 중 오류', { error: logCleanError.message });
      }
      
      logger.info('정기 캐시 정리 완료', {
        memoryCleaned,
        firestoreCleaned,
        logsCleaned,
        timestamp: new Date().toISOString()
      });

      return null;
    } catch (error) {
      logger.critical('정기 캐시 정리 실패', error);
      throw error;
    }
  });

// ============================================
// 🔟 에러 로그 조회 API (새로 추가)
// GET /getErrorLogs
// ============================================

exports.getErrorLogs = functions
  .region(FUNCTION_CONFIG.REGION)
  .https.onRequest(async (req, res) => {
    const logger = createLogger('getErrorLogs');

    try {
      res.set('Access-Control-Allow-Origin', '*');
      
      if (req.method === 'OPTIONS') {
        res.status(200).send('OK');
        return;
      }

      if (req.method !== 'GET') {
        res.status(405).json({ error: 'Method Not Allowed' });
        return;
      }

      const limit = parseInt(req.query.limit) || 50;
      const type = req.query.type || 'all'; // all, ERROR, WARN

      const db = admin.firestore();
      let query = db.collection('analysis_logs')
        .orderBy('timestamp', 'desc')
        .limit(limit);

      if (type !== 'all') {
        query = query.where('type', '==', type);
      }

      const snapshot = await query.get();
      const logs = [];
      
      snapshot.forEach(doc => {
        logs.push({ id: doc.id, ...doc.data() });
      });

      logger.info('에러 로그 조회', { count: logs.length, type });

      res.status(200).json({
        logs,
        count: logs.length,
        timestamp: new Date().toISOString()
      });

    } catch (error) {
      logger.error('에러 로그 조회 실패', error);
      res.status(500).json({ error: 'Internal Server Error', message: error.message });
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
