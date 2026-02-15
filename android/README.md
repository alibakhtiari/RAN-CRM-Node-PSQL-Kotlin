# RAN CRM Android Configuration & Release

This document covers configuration and release procedures for the Android application.

## 1. Backend Configuration

The API connection is managed in `ApiClient.kt`.

- **File**: `app/src/main/java/com/ran/crm/data/remote/ApiClient.kt`
- **Constant**: `BASE_URL`

```kotlin
// Change this for production
private const val BASE_URL = "https://your-api-domain.com/"
```

## 2. Local Environment Setup

Create or update `android/local.properties` (this file is git-ignored) to include the following configuration:

### Signing Credentials
```properties
storeFile=C:\\path\\to\\your\\keystore.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```

### Test Credentials
Used for automated integration tests:
```properties
TEST_USERNAME=admin
TEST_PASSWORD=admin123
```

## 3. Build & Release Commands

All commands should be run from the `android/` directory.

### Build APKs
- **Debug**: `./gradlew assembleDebug`
- **Release**: `./gradlew assembleRelease`
- **Specific Architecture (ARM64)**: `./gradlew assembleRelease -PabiFilters=arm64-v8a`

### Build App Bundles (AAB)
Recommended for Google Play Store:
- **Debug**: `./gradlew bundleDebug`
- **Release**: `./gradlew bundleRelease`

### Install on Device
- **Debug**: `./gradlew installDebug`
- **Release**: `./gradlew installRelease`
- **Specific Architecture (ARM64 Release)**: `./gradlew installRelease -PabiFilters=arm64-v8a`

### Cleanup
- **Clean Build**: `./gradlew clean`

## 4. Build Artifacts

Generated files are located at:
- **APKs**: `app/build/outputs/apk/release/`
- **AABs**: `app/build/outputs/bundle/release/`
- **Mapping Files**: `app/build/outputs/mapping/release/` (Keep these for de-obfuscating crash logs)

## 5. Supported Architectures

The app currently filters for:
- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`
