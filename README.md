# RAN CRM - Contact & Call Log Sync System

A comprehensive CRM system for synchronizing contacts and call logs across multiple users in an organization. Built with a Node.js backend and Kotlin Android frontend.

## Project Structure

```
RAN-CRM-Node-PSQL-Kotlin/
├── backend/          # Node.js + PostgreSQL backend API
├── android/          # Kotlin Android app
├── kotlin.md         # Android app PRD and requirements
├── MOBILE_INTEGRATION.md  # Mobile integration guide
└── README.md         # This file
```

## Components

### Backend (Node.js + PostgreSQL)
- RESTful API with JWT authentication
- Contact and call log synchronization
- Phone number normalization and duplicate prevention
- PostgreSQL database with optimized indexes
- Docker containerization support

### Android App (Kotlin + Jetpack Compose)
- Material 3 UI with Jetpack Compose
- Offline-first architecture with Room database
- Background sync with WorkManager
- Phone number normalization using libphonenumber
- Runtime permissions for contacts and call logs

## Features

- **Authentication**: JWT-based login system
- **Contact Sync**: Two-way synchronization with conflict resolution
- **Call Log Sync**: Automatic upload of device call history
- **Phone Normalization**: E.164 standardization with Iranian number support
- **Duplicate Prevention**: Backend constraints prevent duplicate phone numbers
- **Background Sync**: Configurable intervals (15min, 30min, 1hour)
- **Offline Support**: Full functionality without internet connection

## Quick Start

### Prerequisites

- Docker and Docker Compose (for backend)
- Android Studio (for Android app)
- Node.js 22+ (optional, for backend development)
- PostgreSQL (optional, for backend development)

### Backend Setup

1. **Using Docker (Recommended)**
   ```bash
   cd backend
   docker-compose up -d
   ```

2. **Manual Setup**
   ```bash
   cd backend
   npm install
   # Set up PostgreSQL database
   # Configure .env file
   npm run dev
   ```

Backend will be available at `http://localhost:3000`

### Android Setup

1. **Open in Android Studio**
   ```bash
   cd android
   # Open the android folder in Android Studio
   ```

2. **Configure Backend URL**
   - Edit `android/app/src/main/java/com/ran/crm/data/remote/ApiClient.kt`
   - Change `BASE_URL` if needed (default: emulator localhost)

3. **Run the App**
   - Connect Android device or start emulator
   - Build and run from Android Studio

## API Endpoints

### Authentication
- `POST /auth/login` - User login
- `GET /auth/me` - Get current user

### Contacts
- `GET /contacts` - List contacts with pagination
- `POST /contacts` - Create contact
- `POST /contacts/batch` - Batch create contacts

### Call Logs
- `GET /calls` - List call logs with pagination
- `POST /calls` - Upload call logs

## Database Schema

### Users
- id (UUID), username, name, is_admin, timestamps

### Contacts
- id (UUID), name, phone_raw, phone_normalized (unique), created_by, timestamps

### Call Logs
- id (UUID), user_id, contact_id, direction, duration_seconds, timestamp

### Sync Audit
- id (UUID), user_id, synced_contacts, synced_calls, timestamp

## Sync Logic

### Contact Synchronization
- Client normalizes phone numbers before sending
- Server prevents duplicates via unique constraint on phone_normalized
- "Older contact wins" for conflict resolution
- Batch operations for efficiency

### Call Log Synchronization
- Upload in batches of max 1000 records
- Link to contacts by phone number matching
- Delta sync based on timestamps

## Development

### Backend Development
```bash
cd backend
npm run dev  # Development with nodemon
npm test     # Run tests
```

### Android Development
- Open `android/` folder in Android Studio
- Use the standard Android development workflow
- Tests: `./gradlew testDebugUnitTest`

### Debugging

**View App Logs (Only Errors):**
```bash
# Windows (PowerShell)
adb logcat *:E | findstr "com.ran.crm"

# Linux/Mac
adb logcat *:E | grep "com.ran.crm"
```

**View All App Logs:**
```bash
# Windows (PowerShell)
adb logcat | findstr "com.ran.crm"
```

### Build & Run Commands

**Debug Mode:**
```bash
# Build and Install on connected device/emulator
./gradlew installDebug

# Build APK only (output: app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

**Release Mode:**
```bash
# Build Signed Release APK
./gradlew assembleRelease

# Build Signed Release Bundle (AAB)
./gradlew bundleRelease
```

**ABI Filtering (ARM64 Only):**
To speed up builds or target specific hardware, you can filter ABIs in `app/build.gradle.kts`:
```kotlin
android {
    // ...
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a") // Include only arm64-v8a
            isUniversalApk = false
        }
    }
}
```
Or use `ndk.abiFilters` in `defaultConfig`:
```kotlin
defaultConfig {
    // ...
    ndk {
        abiFilters.add("arm64-v8a")
    }
}
```

## Deployment

### Backend
```bash
cd backend
docker build -t ran-crm-backend .
docker run -p 3000:3000 ran-crm-backend
```

### Android
- Build signed APK/AAB in Android Studio
- Configure signing config in `app/build.gradle.kts`
- Deploy to Google Play or distribute APK

## Configuration

### Backend Environment Variables
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ran_crm
DB_USER=postgres
DB_PASSWORD=password
JWT_SECRET=your-secret-key
JWT_EXPIRES_IN=24h
PORT=3000
```

### Android Configuration
- Backend URL in `ApiClient.kt`
- Minimum API level: 24 (Android 7.0)
- Target API level: 35 (Android 15)

## Testing

### Backend Tests
```bash
cd backend
npm test
```

### Android Tests
```bash
cd android
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Security

- JWT tokens with expiration
- Password hashing with bcrypt
- HTTPS enforcement in production
- Input validation and sanitization
- SQL injection prevention with prepared statements

## Performance

- Database indexes on frequently queried columns
- Pagination for large datasets
- Batch operations for bulk inserts
- Connection pooling
- Background sync constraints (network + battery)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a pull request

## License

See LICENSE file for details.

## Support

- Backend: Check `backend/README.md` and `backend/Instruction.md`
- Android: Check `android/README.md`
- API Integration: See `MOBILE_INTEGRATION.md`
- Requirements: See `kotlin.md`
