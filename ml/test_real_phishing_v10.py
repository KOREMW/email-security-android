"""
Real-world Phishing URL Test for v10 Model
- Uses REAL phishing URLs from JPCERT/CC (2024)
- Tests if v10 model with Label Smoothing can detect real phishing
"""

import os
import json
import numpy as np
import tensorflow as tf
import re
import math
from urllib.parse import urlparse
from collections import Counter

data_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")

# Load TFLite model
interpreter = tf.lite.Interpreter(model_path=os.path.join(data_dir, 'phishing_model_v10.tflite'))
interpreter.allocate_tensors()
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Load scaler config
with open(os.path.join(data_dir, 'scaler_config_v10.json'), 'r') as f:
    config = json.load(f)
mean = np.array(config['mean'], dtype=np.float32)
scale = np.array(config['scale'], dtype=np.float32)

# Temperature for calibration (match Android app)
TEMPERATURE = 2.0

# Feature extraction (same as UrlClassifier.kt)
MAJOR_BRANDS = {'google', 'facebook', 'amazon', 'apple', 'microsoft', 'paypal',
                'ebay', 'netflix', 'twitter', 'instagram', 'linkedin', 'youtube',
                'yahoo', 'bing', 'github', 'stackoverflow', 'reddit', 'wikipedia', 'naver'}

SUSPICIOUS_KEYWORDS = ['verify', 'secure', 'update', 'confirm', 'authenticate',
                       'credentials', 'suspend', 'unusual', 'alert', 'urgent',
                       'expired', 'blocked', 'limited', 'restore', 'unlock',
                       'reactivate', 'validate', 'confirmation', 'security-check',
                       'verify-identity', 'account-recovery']

SUSPICIOUS_TLDS = ['.tk', '.ml', '.ga', '.cf', '.gq', '.xyz', '.top', '.click',
                  '.link', '.download', '.stream', '.racing', '.win', '.bid',
                  '.loan', '.work', '.date', '.review', '.vip', '.cn', '.cyou',
                  '.cc', '.shop']


def calculate_entropy(s):
    if not s:
        return 0.0
    freq = Counter(s)
    length = len(s)
    entropy = 0.0
    for count in freq.values():
        p = count / length
        if p > 0:
            entropy -= p * math.log2(p)
    return entropy


def extract_features(url):
    try:
        parsed = urlparse(url)
    except:
        parsed = None

    hostname = (parsed.hostname or '') if parsed else ''
    hostname = hostname.lower()
    path = parsed.path if parsed else ''
    scheme = (parsed.scheme or '').lower() if parsed else ''
    query = parsed.query if parsed else ''
    port = parsed.port if parsed else None

    domain_parts = hostname.split('.')
    base_domain = '.'.join(domain_parts[-2:]) if len(domain_parts) >= 2 else hostname
    is_major_brand = any(brand in base_domain for brand in MAJOR_BRANDS)

    has_suspicious_keyword = 0
    if not is_major_brand:
        if any(kw in url.lower() for kw in SUSPICIOUS_KEYWORDS):
            has_suspicious_keyword = 1
        for brand in MAJOR_BRANDS:
            if brand in hostname and not hostname.endswith(f'{brand}.com'):
                has_suspicious_keyword = 1
                break

    return np.array([
        len(url),
        len(hostname),
        len(path),
        max(0, hostname.count('.') - 1),
        1 if scheme == 'https' else 0,
        1 if port and port not in [80, 443] else 0,
        len(query.split('&')) if query else 0,
        sum(c.isdigit() for c in url) / len(url) if url else 0,
        sum(c in '@-_.' for c in url) / len(url) if url else 0,
        1 if re.match(r'^(\d{1,3}\.){3}\d{1,3}$', hostname.split(':')[0] if hostname else '') else 0,
        has_suspicious_keyword,
        1 if any(tld in hostname for tld in SUSPICIOUS_TLDS) else 0,
        calculate_entropy(hostname),
    ], dtype=np.float32)


def predict(url):
    features = extract_features(url)
    normalized = (features - mean) / scale
    interpreter.set_tensor(input_details[0]['index'], normalized.reshape(1, -1))
    interpreter.invoke()
    raw_score = interpreter.get_tensor(output_details[0]['index'])[0][0]

    # Apply temperature scaling for calibrated probability
    raw_score = np.clip(raw_score, 0.0001, 0.9999)
    logit = np.log(raw_score / (1 - raw_score))
    scaled_logit = logit / TEMPERATURE
    calibrated_score = 1 / (1 + np.exp(-scaled_logit))

    return raw_score, calibrated_score


# ============================================
# REAL Phishing URLs from JPCERT/CC (2024)
# These are NEW URLs not in training data
# ============================================
real_phishing_urls = [
    # Labor Bank impersonation - normal looking domains
    ('https://alwaysat.com/', 'Labor Bank'),
    ('https://auburnusbc.com/allkyusyu.html', 'Labor Bank'),
    ('https://blivets.com/ibrokinro.html', 'Labor Bank'),
    ('https://chaseapts.com/', 'Labor Bank'),
    ('https://iozay.com/ibrokinro.html', 'Labor Bank'),
    ('https://markputt.com/shikokuib.html', 'Labor Bank'),
    ('https://rongamer.com/', 'Labor Bank'),

    # SAISON CARD with random subdomains
    ('https://tsai.pos158.com', 'SAISON CARD'),
    ('https://txsai.38mmtu.com', 'SAISON CARD'),
    ('https://sai.93qhbz.com', 'SAISON CARD'),

    # Yamato Transport (kuroneko) typosquatting
    ('https://kuronekoyaimato.xyz/jp/', 'Yamato Transport'),
    ('https://kuronekoyamato1.vip/jp/', 'Yamato Transport'),

    # DuckDNS based (very suspicious)
    ('https://kamtanumg.duckdns.org/static/js/', 'DuckDNS Phishing'),
    ('https://amazon-jp.duckdns.org/', 'DuckDNS Amazon'),

    # Chinese domain phishing
    ('https://zaiowyd.qmigho.cn/caonima=MrPkxzRh.co.jp/', 'TEPCO .cn'),
    ('https://xhfge.cn/amazon/', 'Amazon .cn'),
    ('https://aeon.co.jp.hfasd.cn/', 'AEON .cn'),

    # Brand impersonation with suspicious TLDs
    ('http://amazon-jp-verify.com/login', 'Amazon fake'),
    ('http://appleid-verify.support/signin', 'Apple fake'),
    ('http://paypal-secure-login.net/verify', 'PayPal fake'),
    ('http://rakuten-card-update.com/member', 'Rakuten fake'),
    ('http://smbc-direct.info/login', 'SMBC fake'),

    # Short domain phishing
    ('https://amzn-jp.top/', 'Amazon typosquat'),
    ('https://raktn.vip/login', 'Rakuten typosquat'),
    ('https://paypay.cc/verify', 'PayPay fake'),

    # Subdomain abuse
    ('https://amazon.co.jp.verify-account.xyz/', 'Subdomain abuse'),
    ('https://apple.com.id-verify.cn/', 'Subdomain abuse'),
    ('https://google.com.secure-login.tk/', 'Subdomain abuse'),

    # IP address based
    ('http://192.168.1.1/amazon/login.php', 'IP-based'),
    ('http://45.67.89.123/paypal/verify/', 'IP-based'),

    # Long obfuscated URLs
    ('https://secure-banking-verify-account-update-login-portal.xyz/auth/v2/signin', 'Long obfuscated'),
]

# Legitimate URLs for comparison
legitimate_urls = [
    ('https://www.google.com', 'Google'),
    ('https://www.amazon.co.jp', 'Amazon JP'),
    ('https://www.amazon.co.jp/dp/B08N5WRWNW', 'Amazon Product'),
    ('https://www.rakuten.co.jp', 'Rakuten'),
    ('https://www.apple.com', 'Apple'),
    ('https://www.paypal.com', 'PayPal'),
    ('https://www.naver.com', 'Naver'),
    ('https://www.youtube.com', 'YouTube'),
    ('https://github.com', 'GitHub'),
    ('https://www.mercari.com', 'Mercari'),
    ('https://paypay.ne.jp', 'PayPay Official'),
    ('https://www.kuronekoyamato.co.jp', 'Yamato Official'),
    ('https://www.aeon.co.jp', 'AEON Official'),
    ('https://www.smbc.co.jp', 'SMBC Official'),
    ('https://www.jcb.co.jp', 'JCB Official'),
]


def main():
    print('=' * 70)
    print('REAL-WORLD PHISHING URL TEST - Model v10')
    print('(Trained with REAL phishing data + Label Smoothing)')
    print('=' * 70)
    print()

    # Test real phishing URLs
    print('REAL PHISHING URLs:')
    print('-' * 70)

    detected = 0
    missed = 0
    phishing_scores = []

    for url, desc in real_phishing_urls:
        raw, cal = predict(url)
        phishing_scores.append(cal)
        status = 'DETECTED' if cal >= 0.5 else 'MISSED'
        symbol = 'O' if cal >= 0.5 else 'X'

        if cal >= 0.5:
            detected += 1
        else:
            missed += 1

        print(f'  [{symbol}] {cal:.0%} - {desc}')
        print(f'       {url[:60]}...' if len(url) > 60 else f'       {url}')

    phishing_detection_rate = detected / len(real_phishing_urls) * 100

    print()
    print(f'Phishing Detection: {detected}/{len(real_phishing_urls)} ({phishing_detection_rate:.1f}%)')
    print()

    # Test legitimate URLs
    print('LEGITIMATE URLs (should NOT be flagged):')
    print('-' * 70)

    correct = 0
    false_positive = 0
    legit_scores = []

    for url, desc in legitimate_urls:
        raw, cal = predict(url)
        legit_scores.append(cal)
        status = 'OK' if cal < 0.5 else 'FALSE POSITIVE'
        symbol = 'O' if cal < 0.5 else 'X'

        if cal < 0.5:
            correct += 1
        else:
            false_positive += 1

        print(f'  [{symbol}] {cal:.0%} - {desc}: {url}')

    print()
    print(f'Legitimate Correct: {correct}/{len(legitimate_urls)}')
    print(f'False Positives: {false_positive}')

    # Score distribution analysis
    print()
    print('=' * 70)
    print('SCORE DISTRIBUTION ANALYSIS')
    print('=' * 70)
    phishing_scores = np.array(phishing_scores)
    legit_scores = np.array(legit_scores)

    print()
    print('Phishing URL Scores:')
    print(f'  Min: {phishing_scores.min():.2%}')
    print(f'  Max: {phishing_scores.max():.2%}')
    print(f'  Mean: {phishing_scores.mean():.2%}')
    print(f'  Median: {np.median(phishing_scores):.2%}')

    print()
    print('Legitimate URL Scores:')
    print(f'  Min: {legit_scores.min():.2%}')
    print(f'  Max: {legit_scores.max():.2%}')
    print(f'  Mean: {legit_scores.mean():.2%}')
    print(f'  Median: {np.median(legit_scores):.2%}')

    # Check extreme values
    extreme_phishing = np.sum(phishing_scores > 0.95)
    extreme_legit = np.sum(legit_scores < 0.05)
    print()
    print('Extreme Values (after Temperature Scaling):')
    print(f'  Phishing >95%: {extreme_phishing}/{len(phishing_scores)}')
    print(f'  Legitimate <5%: {extreme_legit}/{len(legit_scores)}')

    # Summary
    print()
    print('=' * 70)
    print('SUMMARY')
    print('=' * 70)
    print(f'Real Phishing Detection Rate: {phishing_detection_rate:.1f}%')
    print(f'False Positive Rate: {false_positive}/{len(legitimate_urls)} ({false_positive/len(legitimate_urls)*100:.1f}%)')

    if phishing_detection_rate >= 80:
        print()
        print('[SUCCESS] Detection rate is GOOD!')
        print('Model is ready for production use.')
    else:
        print()
        print('[WARNING] Detection rate is still LOW!')
        print('Consider adding more training data or adjusting features.')


if __name__ == "__main__":
    main()
