// ============================================
// URL 분석 모듈
// - URL 단축 해제
// - URL 정규화
// - ML 입력용 특징 추출
// - null-safe 처리 적용
// ============================================

const axios = require('axios');
const { URL_NORMALIZER } = require('./constants');

/**
 * 기본 URL 피처 (에러 발생 시 사용)
 * 중립적인 값으로 설정하여 ML 모델이 0.5 근처 점수 반환하도록 함
 */
const DEFAULT_FEATURES = {
  urlLength: 50,
  domainLength: 15,
  pathLength: 10,
  subdomainCount: 0,
  hasHttps: 1,
  hasPort: 0,
  paramCount: 0,
  digitRatio: 0.1,
  specialCharRatio: 0.1,
  hasIpAddress: 0,
  hasSuspiciousKeyword: 0,
  hasSuspiciousTld: 0,
  entropy: 3.0
};

/**
 * 단축 URL 추적해서 최종 URL 찾기
 * @param {string} url - 입력 URL
 * @returns {Promise<string>} 최종 URL
 */
async function expandShortUrl(url) {
  // URL 유효성 검사
  if (!url || typeof url !== 'string') {
    console.warn('Invalid URL provided to expandShortUrl');
    return url || '';
  }

  try {
    let currentUrl = url;
    let redirectCount = 0;
    const seenUrls = new Set();

    while (redirectCount < URL_NORMALIZER.MAX_REDIRECTS) {
      // 순환 리다이렉트 감지
      if (seenUrls.has(currentUrl)) {
        throw new Error('Redirect loop detected');
      }
      seenUrls.add(currentUrl);

      try {
        // HEAD 요청으로 빠르게 리다이렉트 확인
        const response = await axios.head(currentUrl, {
          timeout: URL_NORMALIZER.REDIRECT_TIMEOUT,
          maxRedirects: 0,  // 수동 리다이렉트 처리
          validateStatus: () => true  // 모든 상태 코드 허용
        });

        // 리다이렉트 상태 코드 확인
        if ([301, 302, 303, 307, 308].includes(response.status)) {
          const location = response.headers.location;
          if (!location) break;
          
          currentUrl = location;
          redirectCount++;
        } else {
          // 리다이렉트 없음 → 최종 URL
          break;
        }
      } catch (error) {
        // HEAD 실패 → GET으로 재시도
        if (redirectCount === 0) {
          try {
            const response = await axios.get(currentUrl, {
              timeout: URL_NORMALIZER.REDIRECT_TIMEOUT,
              maxRedirects: 0,
              validateStatus: () => true
            });
            
            if (response.config.url !== currentUrl) {
              currentUrl = response.config.url;
              redirectCount++;
            } else {
              break;
            }
          } catch {
            break;
          }
        } else {
          break;
        }
      }
    }

    return currentUrl;
  } catch (error) {
    console.error('Error expanding short URL:', error.message);
    return url;  // 실패 시 원본 URL 반환
  }
}

/**
 * URL 정규화
 * @param {string} url - 정규화할 URL
 * @returns {string} 정규화된 URL
 */
function normalizeUrl(url) {
  // URL 유효성 검사
  if (!url || typeof url !== 'string') {
    console.warn('Invalid URL provided to normalizeUrl');
    return url || '';
  }

  try {
    // URL 파싱
    const urlObj = new URL(url);

    // 1. 프로토콜 통일 (http → https)
    urlObj.protocol = 'https:';

    // 2. 도메인 소문자
    urlObj.hostname = urlObj.hostname.toLowerCase();

    // 3. 쿼리 파라미터 정렬 + 중복 제거
    const params = new URLSearchParams();
    const seenParams = new Set();
    
    for (const [key, value] of urlObj.searchParams) {
      const paramKey = `${key}=${value}`;
      if (!seenParams.has(paramKey)) {
        params.append(key, value);
        seenParams.add(paramKey);
      }
    }

    // 파라미터를 키 기준으로 정렬
    const sortedParams = new URLSearchParams(
      Array.from(params).sort((a, b) => a[0].localeCompare(b[0]))
    );
    urlObj.search = sortedParams.toString();

    // 4. 앵커/프래그먼트 제거
    urlObj.hash = '';

    // 5. 뒤에 슬래시 제거 (루트가 아닌 경우)
    if (urlObj.pathname !== '/' && urlObj.pathname.endsWith('/')) {
      urlObj.pathname = urlObj.pathname.slice(0, -1);
    }

    // 6. 경로 정규화 (/a/./b → /a/b)
    const pathParts = urlObj.pathname.split('/').filter(p => p && p !== '.');
    urlObj.pathname = '/' + pathParts.join('/');

    return urlObj.toString();
  } catch (error) {
    console.error('Error normalizing URL:', error.message);
    return url;
  }
}

/**
 * 엔트로피 계산 (문자열의 랜덤성)
 * 높은 엔트로피 = 무작위 문자열 = 피싱 가능성 ↑
 * @param {string} str - 입력 문자열
 * @returns {number} 엔트로피 (0 ~ 8)
 */
function calculateEntropy(str) {
  // 빈 문자열 체크
  if (!str || typeof str !== 'string' || str.length === 0) {
    return 0;
  }

  const len = str.length;
  const freq = {};

  // 문자 빈도 계산
  for (let c of str) {
    freq[c] = (freq[c] || 0) + 1;
  }

  // 엔트로피 공식: -Σ(p_i * log2(p_i))
  let entropy = 0;
  for (const count of Object.values(freq)) {
    const p = count / len;
    entropy -= p * Math.log2(p);
  }

  return entropy;
}

/**
 * URL에서 ML 입력용 피처 추출 (null-safe 버전)
 * @param {string} url - 정규화된 URL
 * @returns {Object} 피처 객체 (13개 피처) - 실패 시 DEFAULT_FEATURES 반환
 */
function extractUrlFeatures(url) {
  // URL 유효성 검사
  if (!url || typeof url !== 'string') {
    console.warn('Invalid URL provided to extractUrlFeatures, returning DEFAULT_FEATURES');
    return { ...DEFAULT_FEATURES };
  }

  try {
    const urlObj = new URL(url);

    // 의심 키워드 목록
    const suspiciousKeywords = [
      'login', 'verify', 'account', 'secure', 'update',
      'confirm', 'password', 'authenticate', 'credentials'
    ];

    // 의심 TLD 목록
    const suspiciousTlds = [
      '.xyz', '.tk', '.ml', '.ga', '.cf', '.gq',
      '.top', '.click', '.link', '.download', '.stream'
    ];

    const hostname = urlObj.hostname;
    const fullUrl = url;

    // 특징 계산
    const features = {
      // 1. 길이 피처
      urlLength: fullUrl.length,
      domainLength: hostname.length,
      pathLength: urlObj.pathname.length,

      // 2. 구조 피처
      subdomainCount: Math.max(0, hostname.split('.').length - 2),  // 음수 방지
      hasHttps: urlObj.protocol === 'https:' ? 1 : 0,
      hasPort: urlObj.port !== '' ? 1 : 0,
      paramCount: urlObj.searchParams.size,

      // 3. 문자 분석
      digitRatio: fullUrl.length > 0 ? (fullUrl.match(/\d/g) || []).length / fullUrl.length : 0,
      specialCharRatio: fullUrl.length > 0 ? (fullUrl.match(/[@\-_\.]/g) || []).length / fullUrl.length : 0,
      hasIpAddress: /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/.test(hostname) ? 1 : 0,

      // 4. 의심 패턴
      hasSuspiciousKeyword: suspiciousKeywords.some(kw => 
        fullUrl.toLowerCase().includes(kw)
      ) ? 1 : 0,
      
      hasSuspiciousTld: suspiciousTlds.some(tld => 
        hostname.toLowerCase().endsWith(tld)
      ) ? 1 : 0,

      // 5. 엔트로피 (랜덤성)
      entropy: calculateEntropy(hostname)
    };

    return features;
  } catch (error) {
    console.error('Error extracting URL features:', error.message);
    console.warn('Returning DEFAULT_FEATURES due to extraction error');
    return { ...DEFAULT_FEATURES };
  }
}

/**
 * 전체 URL 분석 파이프라인 (null-safe 버전)
 * @param {string} url - 입력 URL
 * @returns {Promise<Object>} {originalUrl, normalizedUrl, features}
 */
async function analyzeUrl(url) {
  // URL 유효성 검사
  if (!url || typeof url !== 'string') {
    console.error('Invalid URL provided to analyzeUrl');
    return {
      originalUrl: url || '',
      expandedUrl: url || '',
      normalizedUrl: url || '',
      features: { ...DEFAULT_FEATURES },
      error: 'Invalid URL: URL must be a non-empty string'
    };
  }

  try {
    // Step 1: 단축 URL 해제
    const expandedUrl = await expandShortUrl(url);
    
    // Step 2: URL 정규화
    const normalizedUrl = normalizeUrl(expandedUrl);
    
    // Step 3: 특징 추출
    const features = extractUrlFeatures(normalizedUrl);

    return {
      originalUrl: url,
      expandedUrl,
      normalizedUrl,
      features,
      error: null
    };
  } catch (error) {
    console.error('Error in analyzeUrl:', error.message);
    return {
      originalUrl: url,
      expandedUrl: url,
      normalizedUrl: url,
      features: { ...DEFAULT_FEATURES },
      error: error.message
    };
  }
}

module.exports = {
  expandShortUrl,
  normalizeUrl,
  extractUrlFeatures,
  calculateEntropy,
  analyzeUrl,
  DEFAULT_FEATURES
};