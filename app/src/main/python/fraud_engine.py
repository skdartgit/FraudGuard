import json
import re
from urllib.parse import urlparse


URL_RE = re.compile(
    r"(?i)\b(?:https?://|www\.)[^\s<>\"]+"
)

IP_HOST_RE = re.compile(
    r"^\d{1,3}(?:\.\d{1,3}){3}$"
)

PHONE_RE = re.compile(
    r"(?<!\d)(?:\+?\d[\d\s().-]{6,}\d)(?!\d)"
)


URGENT_TERMS = [
    "urgent",
    "immediately",
    "act now",
    "within 24 hours",
    "within 12 hours",
    "last warning",
    "final warning",
    "expires today",
    "account will be blocked",
    "account blocked",
    "account suspended",
    "verify immediately",
]

FINANCIAL_TERMS = [
    "bank",
    "bank account",
    "upi",
    "payment",
    "transaction",
    "credit card",
    "debit card",
    "card",
    "kyc",
    "refund",
    "cashback",
    "loan",
    "investment",
    "trading",
    "crypto",
    "wallet",
]

SECRET_TERMS = [
    "otp",
    "one time password",
    "pin",
    "cvv",
    "password",
    "passcode",
    "mpin",
    "upi pin",
    "verification code",
]

IMPERSONATION_TERMS = [
    "sbi",
    "state bank",
    "hdfc",
    "icici",
    "axis bank",
    "rbi",
    "reserve bank",
    "uidai",
    "income tax",
    "incometax",
    "police",
    "cyber crime",
    "customs",
    "courier",
    "fedex",
    "dhl",
    "amazon",
    "flipkart",
    "paytm",
    "phonepe",
    "google pay",
    "gpay",
]

REMOTE_ACCESS_TERMS = [
    "anydesk",
    "teamviewer",
    "quicksupport",
    "quick support",
    "remote access",
    "screen sharing",
    "screen share",
    "share your screen",
    "install this app",
    "install apk",
]

SHORTENERS = {
    "bit.ly",
    "tinyurl.com",
    "t.co",
    "goo.gl",
    "ow.ly",
    "is.gd",
    "buff.ly",
    "cutt.ly",
    "shorturl.at",
    "rebrand.ly",
}

SUSPICIOUS_TLDS = {
    "zip",
    "mov",
    "click",
    "download",
    "work",
    "top",
    "xyz",
    "gq",
    "tk",
    "ml",
    "cf",
    "ga",
}

RISKY_FILE_TERMS = [
    ".apk",
    ".exe",
    ".scr",
    ".bat",
    ".cmd",
    ".msi",
]


def _contains(text, terms):
    text = text.lower()
    return [term for term in terms if term in text]


def _risk_level(score):
    if score >= 85:
        return "CRITICAL"
    if score >= 65:
        return "HIGH"
    if score >= 40:
        return "SUSPICIOUS"
    if score >= 20:
        return "LOW"
    return "SAFE"


def _unique(items):
    result = []

    for item in items:
        if item not in result:
            result.append(item)

    return result


def analyze_call(number, verification_status=-1):
    number = str(number or "").strip()

    reasons = []
    score = 0

    digits = re.sub(r"\D", "", number)

    if not digits:
        score += 60
        reasons.append("Caller number is unavailable or malformed.")

    elif len(digits) < 7:
        score += 35
        reasons.append("Unusually short phone number.")

    if digits and len(set(digits)) == 1:
        score += 45
        reasons.append("Number contains only one repeated digit.")

    if len(digits) >= 8:
        repeated_pairs = sum(
            1 for a, b in zip(digits, digits[1:])
            if a == b
        )

        if repeated_pairs >= len(digits) // 2:
            score += 25
            reasons.append(
                "Number has an unusually repetitive pattern."
            )

    if number.startswith("+"):
        if not number.startswith("+91"):
            reasons.append(
                "International number; verify the caller independently."
            )
            score += 10

    if verification_status == 2:
        score += 35
        reasons.append(
            "Network caller-number verification failed."
        )

    elif verification_status == 1:
        score = max(0, score - 10)
        reasons.append(
            "Network caller-number verification passed."
        )

    elif verification_status == 0:
        reasons.append(
            "Network could not verify the caller number."
        )

    if not reasons:
        reasons.append(
            "No strong local fraud signal was detected."
        )

    score = min(100, max(0, score))

    return json.dumps({
        "type": "CALL",
        "number": number,
        "risk_score": score,
        "risk_level": _risk_level(score),
        "reasons": _unique(reasons),
        "external_reputation": "NOT_CONFIGURED",
    })


def analyze_url(url):
    original = url.strip()

    if original.lower().startswith("www."):
        candidate = "https://" + original
    else:
        candidate = original

    reasons = []
    score = 0

    try:
        parsed = urlparse(candidate)
    except Exception:
        return {
            "url": original,
            "score": 70,
            "level": "HIGH",
            "reasons": ["URL could not be parsed safely."],
        }

    scheme = parsed.scheme.lower()
    host = (parsed.hostname or "").lower()
    path = (parsed.path or "").lower()

    if not host:
        score += 40
        reasons.append("URL has no normal domain name.")

    if scheme == "http":
        score += 10
        reasons.append("URL does not use HTTPS.")

    if "@" in candidate:
        score += 25
        reasons.append(
            "URL contains @, which can disguise the real destination."
        )

    if host.startswith("xn--") or ".xn--" in host:
        score += 30
        reasons.append(
            "Domain uses punycode and may imitate another domain."
        )

    if IP_HOST_RE.match(host):
        score += 35
        reasons.append(
            "URL uses an IP address instead of a normal domain."
        )

    if host in SHORTENERS:
        score += 25
        reasons.append(
            "URL uses a known URL-shortening service."
        )

    if len(host) > 45:
        score += 15
        reasons.append(
            "Domain name is unusually long."
        )

    if host.count(".") >= 4:
        score += 15
        reasons.append(
            "URL contains many subdomain levels."
        )

    tld = ""
    if "." in host:
        tld = host.rsplit(".", 1)[-1]

    if tld in SUSPICIOUS_TLDS:
        score += 15
        reasons.append(
            "Domain uses a TLD commonly seen in suspicious links."
        )

    if len(candidate) > 180:
        score += 15
        reasons.append(
            "URL is unusually long and complex."
        )

    lower = candidate.lower()

    for term in RISKY_FILE_TERMS:
        if term in lower:
            score += 35
            reasons.append(
                f"URL references a potentially executable file: {term}"
            )

    suspicious_words = [
        "verify",
        "login",
        "secure",
        "account",
        "kyc",
        "update",
        "refund",
        "payment",
        "wallet",
        "otp",
        "password",
        "claim",
        "prize",
        "bonus",
    ]

    found_words = [
        word for word in suspicious_words
        if word in lower
    ]

    if len(found_words) >= 2:
        score += 15
        reasons.append(
            "URL contains multiple high-risk account/payment terms."
        )

    if not reasons:
        reasons.append(
            "No strong structural URL warning was detected."
        )

    score = min(100, max(0, score))

    return {
        "url": original,
        "score": score,
        "level": _risk_level(score),
        "reasons": _unique(reasons),
        "host": host,
    }


def analyze_sms(sender, message):
    sender = str(sender or "").strip()
    message = str(message or "").strip()

    text = message.lower()

    reasons = []
    score = 0

    urgent = _contains(
        text,
        URGENT_TERMS
    )

    financial = _contains(
        text,
        FINANCIAL_TERMS
    )

    secrets = _contains(
        text,
        SECRET_TERMS
    )

    impersonation = _contains(
        text,
        IMPERSONATION_TERMS
    )

    remote_access = _contains(
        text,
        REMOTE_ACCESS_TERMS
    )

    if urgent:
        score += min(25, 8 + len(urgent) * 3)
        reasons.append(
            "Urgency or threat language detected."
        )

    if financial:
        score += min(20, 8 + len(financial) * 2)
        reasons.append(
            "Financial/account-related language detected."
        )

    if secrets:
        score += min(35, 15 + len(secrets) * 5)
        reasons.append(
            "Message requests or discusses sensitive authentication information."
        )

    if impersonation:
        score += min(20, 8 + len(impersonation) * 2)
        reasons.append(
            "Possible company/government impersonation language detected."
        )

    if remote_access:
        score += min(35, 20 + len(remote_access) * 4)
        reasons.append(
            "Remote-access or screen-sharing instructions detected."
        )

    urls = URL_RE.findall(message)

    url_results = []

    for url in urls[:10]:
        result = analyze_url(url)
        url_results.append(result)

        if result["score"] >= 70:
            score += 25
            reasons.append(
                "A linked URL has multiple high-risk characteristics."
            )

        elif result["score"] >= 40:
            score += 12
            reasons.append(
                "A linked URL has suspicious characteristics."
            )

    for term in RISKY_FILE_TERMS:
        if term in text:
            score += 30
            reasons.append(
                "Message contains a potentially executable download."
            )

    if (
        ("click" in text or "open" in text or "visit" in text)
        and urls
    ):
        score += 10
        reasons.append(
            "Message urges the recipient to open a link."
        )

    if (
        "share screen" in text
        or "screen share" in text
        or "remote access" in text
    ):
        score += 25
        reasons.append(
            "Message may be attempting to obtain remote device access."
        )

    if (
        "otp" in text
        and (
            "tell" in text
            or "share" in text
            or "send" in text
            or "provide" in text
        )
    ):
        score += 25
        reasons.append(
            "Message appears to request disclosure of an OTP."
        )

    score = min(100, max(0, score))

    if not reasons:
        reasons.append(
            "No strong local fraud signal was detected."
        )

    return json.dumps({
        "type": "SMS",
        "sender": sender,
        "risk_score": score,
        "risk_level": _risk_level(score),
        "reasons": _unique(reasons),
        "urls": url_results,
        "external_reputation": "NOT_CONFIGURED",
    })


if __name__ == "__main__":
    # Simple standalone self-test.
    tests = [
        analyze_call("+919876543210", 0),
        analyze_sms(
            "BANK",
            "URGENT! Your bank account will be blocked. "
            "Verify your KYC immediately: "
            "http://bit.ly/example"
        ),
    ]

    for item in tests:
        print(item)
