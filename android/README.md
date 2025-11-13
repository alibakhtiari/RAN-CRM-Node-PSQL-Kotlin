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

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Integration Tests
```bash
./gradlew connectedDebugAndroidTest
```

## Building for Release

1. **Generate signed APK/AAB**
   - In Android Studio: Build > Generate Signed Bundle/APK
   - Create a new keystore or use existing one

2. **Configure signing**
   ```gradle
   // app/build.gradle.kts
   android {
       signingConfigs {
           create("release") {
               storeFile = file("path/to/keystore.jks")
               storePassword = "store_password"
               keyAlias = "key_alias"
               keyPassword = "key_password"
           }
       }
       
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ... other config
           }
       }
   }
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
