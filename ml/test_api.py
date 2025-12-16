"""
Test Firebase Cloud Functions API
- Test shortened URL expansion
- Test phishing URL detection
"""

import requests
import json

# Firebase Cloud Functions URL (us-central1)
BASE_URL = "https://us-central1-email-security-phishing-1.cloudfunctions.net"

def test_health_check():
    """Test health check endpoint"""
    print("=" * 60)
    print("1. Health Check")
    print("=" * 60)

    try:
        response = requests.get(f"{BASE_URL}/healthCheck", timeout=10)
        data = response.json()

        print(f"Status: {data.get('status')}")
        print(f"Version: {data.get('version')}")

        services = data.get('services', {})
        print(f"Firestore: {services.get('firestore', {}).get('status')}")
        print(f"VirusTotal: {services.get('virusTotal', {}).get('status')}")
        print(f"OpenAI: {services.get('openai', {}).get('status')}")

        return True
    except Exception as e:
        print(f"Error: {e}")
        return False


def test_analyze_url(url, description="", local_score=0.5):
    """Test analyzeSuspiciousUrl endpoint"""
    print()
    print("-" * 60)
    print(f"Testing: {description}")
    print(f"URL: {url}")
    print(f"Local ML Score: {local_score:.0%}")
    print("-" * 60)

    try:
        response = requests.post(
            f"{BASE_URL}/analyzeSuspiciousUrl",
            json={
                "url": url,
                "source": "test",
                "localRiskScore": local_score,
                "forceRefresh": True
            },
            timeout=60
        )

        if response.status_code != 200:
            print(f"Error: HTTP {response.status_code}")
            print(response.text)
            return None

        data = response.json()

        # Basic info
        print(f"Original URL: {data.get('originalUrl')}")
        print(f"Expanded URL: {data.get('expandedUrl')}")
        print(f"Normalized URL: {data.get('normalizedUrl')}")

        # Analysis result
        print()
        print(f"Suspicious: {data.get('suspicious')}")
        print(f"Risk Score: {data.get('finalRiskScore'):.1%}")
        print(f"Level: {data.get('suspiciousLevel')}")

        # VirusTotal
        vt = data.get('virusTotal', {})
        print()
        print(f"VirusTotal: malicious={vt.get('malicious', 0)}, harmless={vt.get('harmless', 0)}, status={vt.get('status')}")

        # Timing
        print()
        print(f"Cached: {data.get('cached')}")
        print(f"Processing Time: {data.get('processingTime')}ms")

        return data

    except Exception as e:
        print(f"Error: {e}")
        return None


def main():
    print("=" * 60)
    print("Firebase Cloud Functions API Test")
    print("=" * 60)
    print()

    # 1. Health check
    if not test_health_check():
        print("\n[FAIL] Health check failed. API may be down.")
        return

    print()
    print("=" * 60)
    print("2. URL Analysis Tests")
    print("=" * 60)

    # Test URLs with simulated ML model scores
    # (url, description, local_ml_score)
    test_cases = [
        # Legitimate URLs - low local ML scores (as the app would predict)
        ("https://www.google.com", "Legitimate - Google", 0.27),
        ("https://www.amazon.co.jp", "Legitimate - Amazon JP", 0.30),
        ("https://www.naver.com", "Legitimate - Naver", 0.28),

        # Shortened URLs - unknown, neutral score
        ("https://bit.ly/3abc123", "Shortened URL - bit.ly (zoom)", 0.50),
        ("https://bit.ly/google", "Shortened URL - bit.ly (google)", 0.50),
        ("https://tinyurl.com/y6p9z5ru", "Shortened URL - tinyurl", 0.50),

        # Phishing URLs - high local ML scores (as the app would predict)
        ("https://amazon.co.jp.verify-account.xyz/", "Phishing - Amazon fake", 0.77),
        ("https://paypal-secure-login.net/verify", "Phishing - PayPal fake", 0.79),
        ("https://rakuten-card-update.com/member", "Phishing - Rakuten fake", 0.84),

        # IP-based - high score
        ("http://192.168.1.1/amazon/login.php", "Phishing - IP based", 0.70),

        # Suspicious TLD - high score
        ("https://card-verify.xyz/login", "Phishing - Suspicious TLD", 0.72),
    ]

    results = []

    for url, desc, local_score in test_cases:
        result = test_analyze_url(url, desc, local_score)
        if result:
            results.append({
                "url": url,
                "desc": desc,
                "suspicious": result.get("suspicious"),
                "score": result.get("finalRiskScore"),
                "expanded": result.get("expandedUrl")
            })

    # Summary
    print()
    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)

    for r in results:
        status = "PHISHING" if r["suspicious"] else "SAFE"
        expanded = f" -> {r['expanded']}" if r["expanded"] else ""
        print(f"[{status:8}] {r['score']:.0%} - {r['desc']}{expanded}")

    # Check shortened URL expansion
    print()
    print("=" * 60)
    print("Shortened URL Expansion Check")
    print("=" * 60)

    for r in results:
        if "bit.ly" in r["url"] or "tinyurl" in r["url"]:
            if r["expanded"] and r["expanded"] != r["url"]:
                print(f"[OK] {r['url']} -> {r['expanded']}")
            else:
                print(f"[FAIL] {r['url']} - NOT EXPANDED!")


if __name__ == "__main__":
    main()
