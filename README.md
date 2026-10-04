# FraudGuard

FraudGuard is an Android anti-fraud/security application using:

- Kotlin for Android integration
- Python for fraud intelligence
- Chaquopy for embedded Python
- Android CallScreeningService for incoming calls
- Android SMS role for incoming SMS
- Local URL analysis
- Local fraud history
- Android notification alerts
- Privacy/security audit

## Important

FraudGuard does not claim that an unknown phone number is definitely
fraudulent. It calculates a risk level from available evidence.

External phone-number reputation is not included by default because
reputation services require their own API/service configuration.

## Build

### GitHub Actions

Push the project to GitHub.

The workflow:

    .github/workflows/android.yml

builds:

    app/build/outputs/apk/debug/app-debug.apk

and uploads:

    FraudGuard-debug-apk

as a GitHub Actions artifact.

### Local

Use Android Studio with JDK 17.

If Gradle 9.4.1 is installed:

    gradle assembleDebug

## First-run configuration

Open FraudGuard and:

1. Enable Call Protection.
2. Enable SMS Protection.
3. Grant requested permissions.
4. Grant notification permission.
5. Run Privacy & Security Audit.

## Call protection

Android calls the CallScreeningService for screening.

The application performs fast local analysis and responds to Android
within the required call-screening time.

Automatic blocking is OFF by default.

It can be enabled from the application.

## SMS protection

For complete incoming-SMS interception on modern Android, FraudGuard
must be selected as the default SMS application.

This is an Android platform requirement.

## URL protection

FraudGuard analyzes the structure of URLs without opening them.

It checks:

- HTTP
- IP addresses
- punycode
- URL shorteners
- suspicious TLDs
- long domains
- many subdomains
- account/payment keywords
- executable downloads
- suspicious URL structure

## Privacy audit

The application can report:

- applications requesting camera permission
- applications requesting microphone permission
- applications requesting location permission
- enabled accessibility services
- active device administrators
- active VPN transport

Android itself remains authoritative for actual camera/microphone
privacy indicators and the Android Privacy Dashboard.

## No signing configuration

This project intentionally contains no release signing or Play Store
publishing configuration.
