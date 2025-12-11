// ============================================
// 에러 로깅 및 모니터링 유틸리티
// - 상세한 에러 정보 기록
// - 스택 트레이스 포함
// - Firestore에 에러 로그 저장
// ============================================

const functions = require('firebase-functions');

/**
 * 에러 심각도 레벨
 */
const ErrorLevel = {
  DEBUG: 'DEBUG',
  INFO: 'INFO',
  WARN: 'WARN',
  ERROR: 'ERROR',
  CRITICAL: 'CRITICAL'
};

/**
 * 에러 카테고리
 */
const ErrorCategory = {
  VALIDATION: 'VALIDATION',      // 입력값 검증 오류
  API_EXTERNAL: 'API_EXTERNAL',  // 외부 API 오류 (VT, OpenAI)
  API_INTERNAL: 'API_INTERNAL',  // 내부 API 오류
  DATABASE: 'DATABASE',          // Firestore 오류
  NETWORK: 'NETWORK',            // 네트워크 오류
  TIMEOUT: 'TIMEOUT',            // 타임아웃
  AUTH: 'AUTH',                  // 인증/권한 오류
  UNKNOWN: 'UNKNOWN'             // 알 수 없는 오류
};

/**
 * 에러 정보를 상세하게 추출
 * @param {Error} error - 에러 객체
 * @returns {Object} 상세 에러 정보
 */
function extractErrorDetails(error) {
  if (!error) {
    return { message: 'Unknown error (null)', stack: null };
  }

  return {
    name: error.name || 'Error',
    message: error.message || String(error),
    stack: error.stack || null,
    code: error.code || null,
    statusCode: error.statusCode || error.status || null,
    // Axios 에러 상세
    axiosError: error.isAxiosError ? {
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data
    } : null
  };
}

/**
 * 에러 카테고리 자동 분류
 * @param {Error} error - 에러 객체
 * @returns {string} 에러 카테고리
 */
function categorizeError(error) {
  if (!error) return ErrorCategory.UNKNOWN;

  const message = (error.message || '').toLowerCase();
  const code = error.code || '';

  // 네트워크/타임아웃
  if (message.includes('timeout') || code === 'ETIMEDOUT' || code === 'ESOCKETTIMEDOUT') {
    return ErrorCategory.TIMEOUT;
  }
  if (message.includes('network') || code === 'ENETUNREACH' || code === 'ECONNREFUSED') {
    return ErrorCategory.NETWORK;
  }

  // 인증
  if (message.includes('unauthorized') || message.includes('forbidden') || 
      error.statusCode === 401 || error.statusCode === 403) {
    return ErrorCategory.AUTH;
  }

  // 외부 API
  if (message.includes('virustotal') || message.includes('openai') || error.isAxiosError) {
    return ErrorCategory.API_EXTERNAL;
  }

  // 데이터베이스
  if (message.includes('firestore') || message.includes('firebase') || code.startsWith('firestore')) {
    return ErrorCategory.DATABASE;
  }

  // 검증
  if (message.includes('invalid') || message.includes('required') || message.includes('validation')) {
    return ErrorCategory.VALIDATION;
  }

  return ErrorCategory.UNKNOWN;
}

/**
 * 통합 로거 클래스
 */
class Logger {
  constructor(context = 'General') {
    this.context = context;
    this.logs = [];
  }

  /**
   * 디버그 로그
   */
  debug(message, data = null) {
    this._log(ErrorLevel.DEBUG, message, data);
  }

  /**
   * 정보 로그
   */
  info(message, data = null) {
    this._log(ErrorLevel.INFO, message, data);
  }

  /**
   * 경고 로그
   */
  warn(message, data = null) {
    this._log(ErrorLevel.WARN, message, data);
  }

  /**
   * 에러 로그
   */
  error(message, error = null, additionalData = null) {
    const errorDetails = error ? extractErrorDetails(error) : null;
    const category = error ? categorizeError(error) : ErrorCategory.UNKNOWN;

    const logEntry = {
      level: ErrorLevel.ERROR,
      context: this.context,
      message,
      category,
      error: errorDetails,
      data: additionalData,
      timestamp: new Date().toISOString()
    };

    this.logs.push(logEntry);
    functions.logger.error(`[${this.context}] ${message}`, {
      category,
      error: errorDetails,
      data: additionalData
    });

    return logEntry;
  }

  /**
   * 치명적 에러 로그
   */
  critical(message, error = null, additionalData = null) {
    const errorDetails = error ? extractErrorDetails(error) : null;
    const category = error ? categorizeError(error) : ErrorCategory.UNKNOWN;

    const logEntry = {
      level: ErrorLevel.CRITICAL,
      context: this.context,
      message,
      category,
      error: errorDetails,
      data: additionalData,
      timestamp: new Date().toISOString()
    };

    this.logs.push(logEntry);
    functions.logger.error(`🚨 [CRITICAL][${this.context}] ${message}`, {
      category,
      error: errorDetails,
      data: additionalData
    });

    return logEntry;
  }

  /**
   * API 요청 로깅
   */
  logApiRequest(endpoint, method, params, duration = null) {
    const message = `API Request: ${method} ${endpoint}`;
    this._log(ErrorLevel.INFO, message, {
      endpoint,
      method,
      params: this._sanitizeParams(params),
      duration: duration ? `${duration}ms` : null
    });
  }

  /**
   * API 응답 로깅
   */
  logApiResponse(endpoint, statusCode, success, duration = null) {
    const level = success ? ErrorLevel.INFO : ErrorLevel.WARN;
    const message = `API Response: ${endpoint} - ${statusCode}`;
    this._log(level, message, {
      endpoint,
      statusCode,
      success,
      duration: duration ? `${duration}ms` : null
    });
  }

  /**
   * 내부 로그 메서드
   */
  _log(level, message, data = null) {
    const logEntry = {
      level,
      context: this.context,
      message,
      data,
      timestamp: new Date().toISOString()
    };

    this.logs.push(logEntry);

    switch (level) {
      case ErrorLevel.DEBUG:
        functions.logger.debug(`[${this.context}] ${message}`, data);
        break;
      case ErrorLevel.INFO:
        functions.logger.info(`[${this.context}] ${message}`, data);
        break;
      case ErrorLevel.WARN:
        functions.logger.warn(`[${this.context}] ${message}`, data);
        break;
      case ErrorLevel.ERROR:
      case ErrorLevel.CRITICAL:
        functions.logger.error(`[${this.context}] ${message}`, data);
        break;
    }

    return logEntry;
  }

  /**
   * 민감 정보 제거
   */
  _sanitizeParams(params) {
    if (!params) return null;
    
    const sanitized = { ...params };
    const sensitiveKeys = ['api_key', 'apiKey', 'password', 'token', 'secret'];
    
    for (const key of sensitiveKeys) {
      if (sanitized[key]) {
        sanitized[key] = '[REDACTED]';
      }
    }
    
    return sanitized;
  }

  /**
   * 모든 로그 반환
   */
  getLogs() {
    return this.logs;
  }

  /**
   * 에러 로그만 반환
   */
  getErrors() {
    return this.logs.filter(log => 
      log.level === ErrorLevel.ERROR || log.level === ErrorLevel.CRITICAL
    );
  }

  /**
   * 로그 초기화
   */
  clear() {
    this.logs = [];
  }
}

/**
 * 로거 인스턴스 생성 팩토리
 */
function createLogger(context) {
  return new Logger(context);
}

/**
 * 성능 측정 유틸리티
 */
function measureTime(fn) {
  return async (...args) => {
    const start = Date.now();
    try {
      const result = await fn(...args);
      const duration = Date.now() - start;
      return { result, duration };
    } catch (error) {
      const duration = Date.now() - start;
      error.duration = duration;
      throw error;
    }
  };
}

module.exports = {
  ErrorLevel,
  ErrorCategory,
  Logger,
  createLogger,
  extractErrorDetails,
  categorizeError,
  measureTime
};
