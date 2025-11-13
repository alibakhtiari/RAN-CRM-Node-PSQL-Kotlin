# RAN CRM Android App

Contact & Call Log Sync CRM Android application built with Kotlin and Jetpack Compose.

## Features

- JWT-based authentication
- Contact synchronization with phone number normalization
- Call log syncing
- Background sync with WorkManager
- Offline-first architecture
- Material 3 UI with Jetpack Compose

## Tech Stack

- **Language**: Kotlin 2.2.21
- **UI**: Jetpack Compose BOM 2025.10
- **Database**: Room 2.7.0
- **Networking**: Retrofit 2.12.0 + OkHttp 5.2.0
- **Background Work**: WorkManager 2.10.0
- **Phone Number Handling**: libphonenumber 8.13.50

## Setup

### Prerequisites

- Android Studio Iguana (2023.2.1) or later
- JDK 17 or later
- Android SDK API 35

### Backend Configuration

The app is configured to connect to a backend server. By default, it uses `http://10.0.2.2:3000` (Android emulator localhost).

To change the backend URL:

1. Open `android/app/src/main/java/com/ran/crm/data/remote/ApiClient.kt`
2. Modify the `BASE_URL` constant:

```kotlin
private const val BASE_URL = "https://your-production-api.com/"
```

### Building and Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd <repository>/android
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open" and navigate to the `android` folder
   - Wait for Gradle sync to complete

3. **Run the app**
   - Connect an Android device or start an emulator (API 24+)
   - Click the "Run" button or press Shift+F10

### Backend Setup

Ensure the backend server is running. See the main README.md in the root directory for backend setup instructions.

Default login credentials:
- Username: `admin`
- Password: `admin123`

## Architecture

### Data Layer
- **Entities**: Room database models (User, Contact, CallLog, SyncAudit)
- **DAOs**: Data Access Objects for database operations
- **Repositories**: Business logic and data synchronization

### Network Layer
- **API Service**: Retrofit interface for backend communication
- **Models**: Request/Response DTOs
- **Auth Interceptor**: Automatic JWT token injection

### UI Layer
- **Screens**: Jetpack Compose composables (Login, Contacts, etc.)
- **Theme**: Material 3 theming with dynamic colors

### Background Sync
- **WorkManager**: Scheduled background synchronization
- **SyncWorker**: Handles contact and call log syncing

## Permissions

The app requires the following permissions:

- `INTERNET`: For API communication
- `READ_CONTACTS`: For importing device contacts
- `READ_CALL_LOG`: For syncing call history

Permissions are requested at runtime when needed.

## Sync Logic

### Contact Sync
- Normalizes phone numbers using libphonenumber
- Handles duplicate prevention via backend constraints
- "Older contact wins" conflict resolution

### Call Log Sync
- Uploads new call logs in batches (max 1000 per request)
- Downloads recent call history
- Links calls to contacts by phone number

### Background Sync
- Configurable intervals: 15min, 30min, 1hour, manual
- Requires network connectivity and sufficient battery
- Exponential backoff on failures

## Testing

### Prerequisites for Testing
- **Backend Server**: Ensure the Node.js backend is running (see main README.md)
- **Test Device/Emulator**: API 24+ with Google Play Services
- **Network Connection**: Required for integration tests

### Unit Tests
Run local unit tests (no device/emulator needed):
```bash
./gradlew testDebugUnitTest
```

**Test Coverage:**
- PhoneUtils: Iranian phone number normalization
- AuthRepository: Login/logout functionality
- ContactRepository: Sync logic and conflict handling
- PermissionsManager: Permission checking logic

### Integration Tests
Run tests that interact with the real backend API:
```bash
./gradlew connectedDebugAndroidTest
```

**Test Coverage:**
- User authentication flow
- Contact retrieval and pagination
- Batch contact upload
- Call log fetching
- Error handling (401, invalid credentials)
- Token-based authorization

### Running Specific Tests
```bash
# Run only unit tests
./gradlew testDebugUnitTest --tests="*Test"

# Run only integration tests
./gradlew connectedDebugAndroidTest --tests="*Test"

# Run specific test class
./gradlew testDebugUnitTest --tests="com.ran.crm.utils.PhoneUtilsTest"

# Run specific test method
./gradlew testDebugUnitTest --tests="com.ran.crm.utils.PhoneUtilsTest.normalizePhoneNumber should handle Iranian mobile numbers correctly"
```

### Test Reports
View detailed test reports in:
- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/index.html`

## Running the App

### Development Mode (Debug)
1. **Connect Device or Start Emulator**
   ```bash
   # List available devices
   adb devices

   # Start emulator (if using Android Studio)
   emulator -avd <emulator_name>
   ```

2. **Run from Android Studio**
   - Open project in Android Studio
   - Click "Run" button (green play icon)
   - Select target device/emulator

3. **Run from Command Line**
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.ran.crm/.MainActivity
   ```

### Backend Connection
The app connects to `http://10.0.2.2:3000` (Android emulator localhost).

**To change backend URL:**
1. Open `app/src/main/java/com/ran/crm/data/remote/ApiClient.kt`
2. Modify the `BASE_URL` constant:
   ```kotlin
   private const val BASE_URL = "https://your-production-api.com/"
   ```

### Default Credentials
- **Username**: `admin`
- **Password**: `admin123`

## Building for Release

### 1. Configure Signing
Create `app/keystore.properties` (don't commit to git):
```properties
storeFile=../keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Update `app/build.gradle.kts`:
```kotlin
android {
    // ... existing config ...

    // Load signing config
    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 2. Build Commands

#### Universal APK (all architectures)
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

#### App Bundle (AAB) - Recommended for Google Play
```bash
# Debug AAB
./gradlew bundleDebug

# Release AAB
./gradlew bundleRelease
```

#### Specific Architecture APKs
```bash
# ARM64-v8a only
./gradlew assembleDebug -PabiFilters=arm64-v8a

# ARM32 + x86
./gradlew assembleDebug -PabiFilters=armeabi-v7a,x86

# All architectures
./gradlew assembleDebug
```

### 3. Build Variants
Available build variants:
- `debug` - Development build with debugging enabled
- `release` - Production build with optimizations

### 4. Output Locations
- **APKs**: `app/build/outputs/apk/`
- **AABs**: `app/build/outputs/bundle/`
- **Mapping files**: `app/build/outputs/mapping/release/mapping.txt` (for crash reporting)

### 5. ProGuard Configuration
Add rules to `app/proguard-rules.pro`:
```proguard
# Retrofit specific classes
-keepattributes Signature, InnerClasses, Enums
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# A coroutine-specific rule for Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    private java.util.Map<java.lang.String,java.lang.Object> __dbRefsMap;
}
```

## Build Configuration

### Supported Architectures
```gradle
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
}
```

### Build Optimization
```gradle
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

## Deployment

### Google Play Store
1. **Generate Bundle**: `./gradlew bundleRelease`
2. **Upload**: Use Google Play Console
3. **Supported devices**: API 24+ (Android 7.0)

### Direct APK Distribution
1. **Generate APK**: `./gradlew assembleRelease`
2. **Sign manually** if needed:
   ```bash
   apksigner sign --ks keystore.jks --out app-release-signed.apk app-release-unsigned.apk
   ```

### CI/CD Integration
Example GitHub Actions workflow:
```yaml
name: Build Android
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - uses: android-actions/setup-android@v2

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Run tests
      run: ./gradlew testDebugUnitTest

    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: apk
        path: app/build/outputs/apk/debug/
```

## Troubleshooting

### Common Issues

1. **Backend connection fails**
   - Verify backend server is running
   - Check BASE_URL in ApiClient.kt
   - Ensure device/emulator has internet access

2. **Sync not working**
   - Check WorkManager status in Android Studio profiler
   - Verify network permissions
   - Check backend logs for errors

3. **Build fails**
   - Clean and rebuild: `./gradlew clean build`
   - Invalidate caches: File > Invalidate Caches / Restart
   - Update Gradle wrapper: `./gradlew wrapper --gradle-version 8.13`

### Debug Logging

Network requests are logged in debug builds. Check Logcat with filter `CRM_API` for detailed logging.

## Contributing

1. Follow Kotlin coding standards
2. Write tests for new features
3. Update documentation
4. Use meaningful commit messages

## License

See LICENSE file in the root directory.
