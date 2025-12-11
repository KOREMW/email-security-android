// ============================================
// ML 예측 모듈
// - TensorFlow Lite 모델 로드
// - 단일 URL 추론
// - 배치 추론 (최대 100개)
// - null-safe 처리 적용
// ============================================

const tf = require('@tensorflow/tfjs');
require('@tensorflow/tfjs-node');
const tflite = require('@tensorflow/tfjs-tflite');
const { ML_MODEL } = require('./constants');

/**
 * 기본 URL 피처 (features가 null일 때 사용)
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
 * features 객체의 유효성 검사 및 기본값 적용
 * @param {Object|null} features - URL 피처 객체
 * @returns {Object} 유효한 피처 객체
 */
function sanitizeFeatures(features) {
  if (!features || typeof features !== 'object') {
    console.warn('Features is null or invalid, using DEFAULT_FEATURES');
    return { ...DEFAULT_FEATURES };
  }

  // 각 필드에 대해 기본값 적용
  return {
    urlLength: typeof features.urlLength === 'number' ? features.urlLength : DEFAULT_FEATURES.urlLength,
    domainLength: typeof features.domainLength === 'number' ? features.domainLength : DEFAULT_FEATURES.domainLength,
    pathLength: typeof features.pathLength === 'number' ? features.pathLength : DEFAULT_FEATURES.pathLength,
    subdomainCount: typeof features.subdomainCount === 'number' ? features.subdomainCount : DEFAULT_FEATURES.subdomainCount,
    hasHttps: typeof features.hasHttps === 'number' ? features.hasHttps : DEFAULT_FEATURES.hasHttps,
    hasPort: typeof features.hasPort === 'number' ? features.hasPort : DEFAULT_FEATURES.hasPort,
    paramCount: typeof features.paramCount === 'number' ? features.paramCount : DEFAULT_FEATURES.paramCount,
    digitRatio: typeof features.digitRatio === 'number' ? features.digitRatio : DEFAULT_FEATURES.digitRatio,
    specialCharRatio: typeof features.specialCharRatio === 'number' ? features.specialCharRatio : DEFAULT_FEATURES.specialCharRatio,
    hasIpAddress: typeof features.hasIpAddress === 'number' ? features.hasIpAddress : DEFAULT_FEATURES.hasIpAddress,
    hasSuspiciousKeyword: typeof features.hasSuspiciousKeyword === 'number' ? features.hasSuspiciousKeyword : DEFAULT_FEATURES.hasSuspiciousKeyword,
    hasSuspiciousTld: typeof features.hasSuspiciousTld === 'number' ? features.hasSuspiciousTld : DEFAULT_FEATURES.hasSuspiciousTld,
    entropy: typeof features.entropy === 'number' ? features.entropy : DEFAULT_FEATURES.entropy
  };
}

class MLPredictor {
  constructor() {
    this.model = null;
    this.isLoaded = false;
    this.loadTime = null;
    this.inferenceCount = 0;
  }

  /**
   * TFLite 모델 로드 (시작 시 1회)
   * @param {string} modelPath - 모델 파일 경로
   * @returns {Promise<void>}
   */
  async loadModel(modelPath = ML_MODEL.PATH) {
    try {
      if (this.isLoaded) {
        console.log('Model already loaded');
        return;
      }

      console.log(`Loading TFLite model from: ${modelPath}`);
      const startTime = Date.now();

      // 모델 로드 (시뮬레이션: 실제로는 tflite 라이브러리 사용)
      // this.model = await tflite.loadTFLiteModel(`file://${modelPath}`);
      
      // 현재는 간단한 로드 시뮬레이션
      this.model = {
        predict: (input) => this.mockPredict(input)
      };

      this.loadTime = Date.now() - startTime;
      this.isLoaded = true;

      console.log(`✓ Model loaded successfully (${this.loadTime}ms)`);
    } catch (error) {
      console.error('Error loading model:', error.message);
      throw error;
    }
  }

  /**
   * 모델 상태 확인
   * @returns {Object}
   */
  getModelStatus() {
    return {
      loaded: this.isLoaded,
      modelPath: ML_MODEL.PATH,
      loadTime: this.loadTime,
      inferenceCount: this.inferenceCount,
      memoryUsage: process.memoryUsage().heapUsed / 1024 / 1024  // MB
    };
  }

  /**
   * Mock 예측 (실제 모델 없을 때)
   * @param {Object} features - URL 특징
   * @returns {number} 위험도 점수 (0.0 ~ 1.0)
   */
  mockPredict(features) {
    // features 유효성 검사
    const safeFeatures = sanitizeFeatures(features);
    
    // 특징 기반 간단한 위험도 계산 (테스트용)
    let score = 0;

    if (safeFeatures.hasSuspiciousKeyword) score += 0.3;
    if (safeFeatures.hasSuspiciousTld) score += 0.25;
    if (safeFeatures.hasIpAddress) score += 0.2;
    if (safeFeatures.entropy > 4.5) score += 0.15;
    if (safeFeatures.digitRatio > 0.3) score += 0.1;
    if (safeFeatures.specialCharRatio > 0.3) score += 0.1;

    return Math.min(score, 1.0);
  }

  /**
   * 단일 URL 추론 (null-safe 버전)
   * @param {Object} features - URL 특징
   * @returns {Promise<number>} 위험도 점수 (0.0 ~ 1.0)
   */
  async predict(features) {
    // 모델 로드 여부 확인
    if (!this.isLoaded) {
      throw new Error('Model not loaded. Call loadModel() first');
    }

    // features null 체크 - 기본값 반환
    if (!features) {
      console.warn('Features is null, returning neutral score (0.5)');
      return 0.5;
    }

    try {
      const startTime = Date.now();

      // features 유효성 검사 및 기본값 적용
      const safeFeatures = sanitizeFeatures(features);

      // 특징을 텐서로 변환
      const inputTensor = tf.tensor2d([
        [
          safeFeatures.urlLength,
          safeFeatures.domainLength,
          safeFeatures.pathLength,
          safeFeatures.subdomainCount,
          safeFeatures.hasHttps,
          safeFeatures.hasPort,
          safeFeatures.paramCount,
          safeFeatures.digitRatio,
          safeFeatures.specialCharRatio,
          safeFeatures.hasIpAddress,
          safeFeatures.hasSuspiciousKeyword,
          safeFeatures.hasSuspiciousTld,
          safeFeatures.entropy
        ]
      ]);

      // 모델 추론
      let prediction;
      if (this.model && this.model.predict) {
        prediction = this.model.predict(safeFeatures);
      } else {
        // Mock 사용
        prediction = this.mockPredict(safeFeatures);
      }

      // 결과 처리
      const riskScore = Array.isArray(prediction) ? prediction[0] : prediction;
      const inferenceTime = Date.now() - startTime;

      this.inferenceCount++;

      // 메모리 정리
      if (inputTensor && inputTensor.dispose) {
        inputTensor.dispose();
      }

      console.log(`Prediction time: ${inferenceTime}ms, Risk: ${riskScore.toFixed(3)}`);

      return Math.min(Math.max(riskScore, 0), 1.0);  // 0.0 ~ 1.0 범위 보장
    } catch (error) {
      console.error('Error in predict:', error.message);
      // 에러 발생 시에도 중립 점수 반환 (서비스 중단 방지)
      console.warn('Returning neutral score (0.5) due to prediction error');
      return 0.5;
    }
  }

  /**
   * 배치 추론 (여러 URL 동시 처리) - null-safe 버전
   * @param {Array<Object>} featuresList - 특징 배열
   * @returns {Promise<Array<number>>} 위험도 점수 배열
   */
  async predictBatch(featuresList) {
    // 모델 로드 여부 확인
    if (!this.isLoaded) {
      throw new Error('Model not loaded. Call loadModel() first');
    }

    // 빈 배열 체크
    if (!Array.isArray(featuresList) || featuresList.length === 0) {
      return [];
    }

    if (featuresList.length > ML_MODEL.BATCH_SIZE_MAX) {
      console.warn(`Batch size (${featuresList.length}) exceeds max (${ML_MODEL.BATCH_SIZE_MAX})`);
    }

    try {
      const startTime = Date.now();

      // 각 features에 대해 유효성 검사 및 기본값 적용
      const safeFeaturesList = featuresList.map(f => sanitizeFeatures(f));

      // 모든 특징을 배치로 변환
      const featureArrays = safeFeaturesList.map(f => [
        f.urlLength,
        f.domainLength,
        f.pathLength,
        f.subdomainCount,
        f.hasHttps,
        f.hasPort,
        f.paramCount,
        f.digitRatio,
        f.specialCharRatio,
        f.hasIpAddress,
        f.hasSuspiciousKeyword,
        f.hasSuspiciousTld,
        f.entropy
      ]);

      const inputTensor = tf.tensor2d(featureArrays);

      // 배치 추론
      let predictions;
      if (this.model && this.model.predict) {
        predictions = safeFeaturesList.map(f => this.mockPredict(f));
      } else {
        // Mock 사용
        predictions = safeFeaturesList.map(f => this.mockPredict(f));
      }

      // 배열로 변환
      const scores = Array.isArray(predictions) ? predictions : Array(featuresList.length).fill(0.5);

      const batchTime = Date.now() - startTime;
      this.inferenceCount += featuresList.length;

      // 메모리 정리
      if (inputTensor && inputTensor.dispose) {
        inputTensor.dispose();
      }

      console.log(`Batch prediction time: ${batchTime}ms for ${featuresList.length} URLs`);

      return scores.map(s => Math.min(Math.max(s, 0), 1.0));
    } catch (error) {
      console.error('Error in predictBatch:', error.message);
      // 에러 발생 시 모든 항목에 중립 점수 반환
      console.warn('Returning neutral scores (0.5) due to batch prediction error');
      return featuresList.map(() => 0.5);
    }
  }

  /**
   * 메모리 해제 (서버 종료 시)
   */
  unloadModel() {
    if (this.model && this.model.dispose) {
      this.model.dispose();
    }
    this.model = null;
    this.isLoaded = false;
    console.log('Model unloaded');
  }
}

// 싱글톤 인스턴스
let predictorInstance = null;

function getInstance() {
  if (!predictorInstance) {
    predictorInstance = new MLPredictor();
  }
  return predictorInstance;
}

module.exports = {
  MLPredictor,
  getInstance,
  DEFAULT_FEATURES,
  sanitizeFeatures
};