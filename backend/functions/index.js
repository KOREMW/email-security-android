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

/**
 * 재시도 로직이 포함된 API 호출 함수
 * @param {Function} fn - 실행할 함수
 * @param {number} maxRetries - 최대 재시도 횟수 (기본: 3)
 * @param {number} baseDelay - 기본 지연 시간 ms (기본: 1000)
 * @returns {Promise} 함수 실행 결과
 */
async function retryWithBackoff(fn, maxRetries = 3, baseDelay = 1000) {
  let lastError;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      // 재시도하지 않을 에러 유형
      const noRetryStatuses = [400, 401, 403, 404];
      if (error.response && noRetryStatuses.includes(error.response.status)) {
        throw error;
      }

      // 마지막 시도면 에러 던지기
      if (attempt === maxRetries) {
        throw error;
      }

      // 지수 백오프 (1초, 2초, 4초...)
      const delay = baseDelay * Math.pow(2, attempt - 1);
      console.log(`[Retry] 시도 ${attempt}/${maxRetries} 실패, ${delay}ms 후 재시도...`);
      await sleep(delay);
    }
  }

  throw lastError;
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

<<<<<<< HEAD
      const { url, source = 'app', localRiskScore = null, forceRefresh = false } = req.body;
=======
      const { url, source = 'app', localRiskScore = null } = req.body;
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8

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
<<<<<<< HEAD
      logger.info('분석 요청', { url, source, localRiskScore: validatedLocalScore, forceRefresh });

      // Step 1: 캐시 확인 (forceRefresh가 true이면 건너뜀)
=======
      logger.info('분석 요청', { url, source, localRiskScore: validatedLocalScore });

      // Step 1: 캐시 확인 (로컬 점수 차이가 크면 재분석)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
      const cache = CacheManager.getInstance();
      const firestoreService = FirestoreService.getInstance();
      const SCORE_DIFF_THRESHOLD = 0.1; // 로컬 점수 차이 임계값

<<<<<<< HEAD
      // 🆕 forceRefresh가 true이면 캐시 무시
      if (forceRefresh) {
        logger.info('🔄 강제 재분석 요청 - 캐시 무시', { url });
      }

      // 메모리 캐시 확인 (forceRefresh가 아닌 경우에만)
      const cachedResult = !forceRefresh ? cache.get(url) : null;
=======
      // 메모리 캐시 확인
      const cachedResult = cache.get(url);
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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

<<<<<<< HEAD
      // Firestore 캐시 확인 (forceRefresh가 아닌 경우에만)
      let firestoreCached = null;
      if (!forceRefresh) {
        try {
          firestoreCached = await firestoreService.getAnalysis(url);
        } catch (dbError) {
          logger.error('Firestore 조회 실패', dbError, { url });
        }
      }

=======
      // Firestore 캐시 확인
      let firestoreCached = null;
      try {
        firestoreCached = await firestoreService.getAnalysis(url);
      } catch (dbError) {
        logger.error('Firestore 조회 실패', dbError, { url });
      }
      
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
      // 🔧 단축 URL인 경우 해제된 URL로 VT 호출 (bit.ly → 실제 URL)
      const urlForVT = urlAnalysis.expandedUrl || url;
      const isExpandedUrl = urlForVT !== url;

      if (isExpandedUrl) {
        logger.info('🔗 VT 분석용 URL 변경', {
          original: url,
          expanded: urlForVT
        });
      }

=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
      let virusTotalResult = {
        malicious: 0,
        harmless: 0,
        undetected: 0,
        suspicious: 0,
        status: 'skipped'
      };

      const vtApiKey = getVirusTotalApiKey();
<<<<<<< HEAD

=======
      
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
      if (vtApiKey) {
        const vtStartTime = Date.now();
        try {
          const vtService = VirusTotalService.getInstance(vtApiKey);
<<<<<<< HEAD
          // 🔧 해제된 URL로 VT 조회
          const vtCached = await vtService.getUrlReport(urlForVT).catch(() => null);

=======
          const vtCached = await vtService.getUrlReport(url).catch(() => null);
          
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
          if (vtCached) {
            virusTotalResult = {
              malicious: vtCached.malicious || 0,
              harmless: vtCached.harmless || 0,
              undetected: vtCached.undetected || 0,
              suspicious: vtCached.suspicious || 0,
              status: 'cached'
            };
<<<<<<< HEAD
            logger.info('VirusTotal 캐시 결과', { url: urlForVT, ...virusTotalResult, duration: Date.now() - vtStartTime });
          } else {
            // 🔧 해제된 URL로 VT 분석
            const vtAnalysis = await vtService.analyzeUrl(urlForVT, 5, 3000);
=======
            logger.info('VirusTotal 캐시 결과', { ...virusTotalResult, duration: Date.now() - vtStartTime });
          } else {
            const vtAnalysis = await vtService.analyzeUrl(url, 5, 3000);
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            virusTotalResult = {
              malicious: vtAnalysis.malicious || 0,
              harmless: vtAnalysis.harmless || 0,
              undetected: vtAnalysis.undetected || 0,
              suspicious: vtAnalysis.suspicious || 0,
              status: 'analyzed'
            };
<<<<<<< HEAD
            logger.info('VirusTotal 분석 완료', { url: urlForVT, ...virusTotalResult, duration: Date.now() - vtStartTime });
          }
        } catch (vtError) {
          logger.error('VirusTotal API 오류', vtError, {
            url: urlForVT,
            originalUrl: url,
            duration: Date.now() - vtStartTime,
            apiKeySet: !!vtApiKey
=======
            logger.info('VirusTotal 분석 완료', { ...virusTotalResult, duration: Date.now() - vtStartTime });
          }
        } catch (vtError) {
          logger.error('VirusTotal API 오류', vtError, { 
            url, 
            duration: Date.now() - vtStartTime,
            apiKeySet: !!vtApiKey 
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
      // 🆕 단축 URL이 해제된 경우 expandedUrl 포함
      const isShortUrlExpanded = urlAnalysis.expandedUrl &&
                                  urlAnalysis.expandedUrl !== url &&
                                  urlAnalysis.expandedUrl !== urlAnalysis.originalUrl;

      const analysisResult = {
        url: url,
        originalUrl: urlAnalysis.originalUrl,
        expandedUrl: isShortUrlExpanded ? urlAnalysis.expandedUrl : null,  // 🆕 단축 URL 해제 결과
=======
      const analysisResult = {
        url: url,
        originalUrl: urlAnalysis.originalUrl,
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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

<<<<<<< HEAD
      if (isShortUrlExpanded) {
        logger.info('🔗 단축 URL 해제됨', {
          original: url,
          expanded: urlAnalysis.expandedUrl
        });
      }

=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD

=======
      
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
      if (!openaiApiKey) {
        logger.error('OpenAI API 키 미설정', null);
        res.status(503).json({
          error: 'Service Unavailable',
          message: 'OpenAI API key not configured'
        });
        return;
      }

<<<<<<< HEAD
      // 🔧 단축 URL 해제 (AI가 실제 URL을 분석하도록)
      let urlForAI = url;
      try {
        const expandedUrl = await urlAnalyzer.expandShortUrl(url);
        if (expandedUrl && expandedUrl !== url) {
          logger.info('🔗 AI 분석용 URL 해제', { original: url, expanded: expandedUrl });
          urlForAI = expandedUrl;
        }
      } catch (expandError) {
        logger.warn('URL 해제 실패, 원본 사용', { url, error: expandError.message });
      }

      logger.info('AI 보고서 생성 시작', { url: urlForAI, originalUrl: url, apiKeyMasked: maskApiKey(openaiApiKey) });

      // OpenAI API 호출 (재시도 로직 포함)
      const prompt = generateAIPrompt(urlForAI, analysisResult);
=======
      logger.info('AI 보고서 생성 시작', { url, apiKeyMasked: maskApiKey(openaiApiKey) });

      // OpenAI API 호출 (재시도 로직 포함)
      const prompt = generateAIPrompt(url, analysisResult);
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
      const openaiStartTime = Date.now();

      let openaiResponse;
      try {
        openaiResponse = await retryWithBackoff(async () => {
          return await axios.post(
            'https://api.openai.com/v1/chat/completions',
            {
              model: 'gpt-4o-mini',
              messages: [
                {
                  role: 'system',
                  content: ENHANCED_SYSTEM_PROMPT
                },
                { role: 'user', content: prompt }
              ],
              max_tokens: 1000,
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
        }, 3, 1000);  // 최대 3회 재시도, 1초 기본 지연

        logger.info('OpenAI API 호출 성공', {
          duration: Date.now() - openaiStartTime,
          model: 'gpt-4o-mini'
        });
      } catch (openaiError) {
        logger.error('OpenAI API 호출 실패 (재시도 후)', openaiError, {
          status: openaiError.response?.status,
          statusText: openaiError.response?.statusText,
          duration: Date.now() - openaiStartTime,
          retries: 3
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

        // 🆕 새로 추가된 상세 분석 필드
        phishingType: aiReport.phishingType || null,
        phishingTechniques: aiReport.phishingTechniques || [],
        targetedBrand: aiReport.targetedBrand || null,
        technicalAnalysis: aiReport.technicalAnalysis || null,
        attackScenario: aiReport.attackScenario || null,

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
// OpenAI 프롬프트 생성 함수 (상세 분석 버전)
// ============================================

function generateAIPrompt(url, analysisResult) {
  const score = analysisResult.finalRiskScore || 0;
  const level = analysisResult.suspiciousLevel || 'UNKNOWN';
  const vt = analysisResult.virusTotal || {};

  // URL에서 추가 정보 추출
  let urlInfo = {};
  try {
    const parsedUrl = new URL(url);
    urlInfo = {
      domain: parsedUrl.hostname,
      path: parsedUrl.pathname,
      hasHttps: parsedUrl.protocol === 'https:',
      hasPort: parsedUrl.port ? true : false,
      queryParams: parsedUrl.search ? parsedUrl.search.split('&').length : 0
    };
  } catch (e) {
    urlInfo = { domain: url, path: '', hasHttps: false };
  }

  // 의심 키워드 탐지
  const suspiciousKeywords = ['login', 'verify', 'account', 'secure', 'update', 'confirm',
    'password', 'banking', 'paypal', 'amazon', 'apple', 'microsoft', 'google',
    'facebook', 'netflix', 'wallet', 'suspend', 'urgent', 'alert', 'naver', 'kakao', 'daum'];
  const foundKeywords = suspiciousKeywords.filter(kw => url.toLowerCase().includes(kw));

  // 의심 TLD 탐지
  const suspiciousTlds = ['.tk', '.ml', '.ga', '.cf', '.gq', '.xyz', '.top', '.click', '.link', '.work', '.site'];
  const hasSuspiciousTld = suspiciousTlds.some(tld => urlInfo.domain?.endsWith(tld));

  return `
다음 URL의 보안 분석 결과를 상세히 분석해주세요:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📌 분석 대상 URL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
URL: ${url}
도메인: ${urlInfo.domain}
경로: ${urlInfo.path || '/'}
HTTPS 사용: ${urlInfo.hasHttps ? '예' : '아니오'}
비표준 포트 사용: ${urlInfo.hasPort ? '예' : '아니오'}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 보안 분석 결과
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
위험도 점수: ${(score * 100).toFixed(1)}점 / 100점
위험 등급: ${level}

VirusTotal 검사 결과:
- 🔴 악성 판정: ${vt.malicious || 0}개 엔진
- 🟡 의심 판정: ${vt.suspicious || 0}개 엔진
- 🟢 정상 판정: ${vt.harmless || 0}개 엔진
- ⚪ 미탐지: ${vt.undetected || 0}개 엔진

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 추가 탐지 정보
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
발견된 의심 키워드: ${foundKeywords.length > 0 ? foundKeywords.join(', ') : '없음'}
의심스러운 TLD 사용: ${hasSuspiciousTld ? '예' : '아니오'}

위 정보를 바탕으로 상세한 보안 분석 보고서를 작성해주세요.
`;
}

// ============================================
// 상세 시스템 프롬프트
// ============================================

const ENHANCED_SYSTEM_PROMPT = `당신은 10년 경력의 사이버 보안 전문가입니다. URL 분석 결과를 바탕으로 일반 사용자가 이해하기 쉬운 상세 보안 보고서를 작성합니다.

다음 피싱 유형들을 정확히 구분하여 분석하세요:

【피싱 유형 분류】
1. 금융사칭 (Banking Phishing): 은행, 카드사, 증권사 사칭
2. 계정탈취 (Credential Theft): 로그인 페이지 위장, 계정 정보 탈취
3. 배송사칭 (Delivery Scam): 택배, 우편 배송 관련 사칭
4. 기업사칭 (Brand Impersonation): 대기업(네이버, 카카오, 애플, 구글 등) 사칭
5. 정부기관사칭 (Government Impersonation): 국세청, 경찰청, 법원 등 사칭
6. 이벤트/당첨사기 (Prize Scam): 경품, 이벤트 당첨 사칭
7. 기술지원사기 (Tech Support Scam): 바이러스 감염, 기술 문제 사칭
8. 투자사기 (Investment Scam): 가상화폐, 주식 투자 유도
9. 로맨스스캠 (Romance Scam): 연애/만남 관련 사기
10. 기타 악성링크 (Malicious Link): 악성코드 배포, 드라이브바이 다운로드

【피싱 기법 분석】
- 타이포스쿼팅 (Typosquatting): 도메인 오타 이용 (예: go0gle.com)
- 호모그래프 공격 (Homograph): 유사 문자 사용 (예: аpple.com - 키릴 문자)
- 서브도메인 위장: 정상 도메인을 서브도메인처럼 사용 (예: google.com.malicious.site)
- URL 단축: bit.ly 등으로 실제 URL 숨김
- 파라미터 조작: 쿼리 파라미터로 악성 데이터 전달
- HTTPS 악용: SSL 인증서로 신뢰감 위장

반드시 아래 JSON 형식으로만 응답하세요:

{
  "riskSummary": "한 줄 핵심 요약 (예: 네이버 로그인 페이지를 사칭한 계정탈취 피싱 사이트입니다)",
  "phishingType": "피싱 유형 (위 10가지 중 하나 또는 '정상 사이트')",
  "phishingTechniques": ["사용된 피싱 기법 1", "사용된 피싱 기법 2"],
  "targetedBrand": "사칭 대상 브랜드 (없으면 null)",
  "riskFactors": [
    "구체적 위험 요소 1 (예: 'naver' 키워드를 포함하지만 실제 네이버 도메인이 아님)",
    "구체적 위험 요소 2",
    "구체적 위험 요소 3"
  ],
  "technicalAnalysis": "기술적 분석 (URL 구조, 도메인 특성, 인증서 상태 등 2-3문장)",
  "attackScenario": "예상 공격 시나리오 (예: 사용자가 로그인하면 입력한 ID/비밀번호가 공격자에게 전송됩니다)",
  "recommendations": [
    "구체적인 권장 행동 1",
    "구체적인 권장 행동 2",
    "구체적인 권장 행동 3"
  ],
  "details": "상세 설명 (3-4문장으로 종합 분석)"
}

주의사항:
- 정상 사이트인 경우에도 왜 정상인지 근거를 제시하세요
- 기술 용어는 괄호 안에 쉬운 설명을 추가하세요
- 사용자가 실제로 취해야 할 행동을 구체적으로 안내하세요`;
