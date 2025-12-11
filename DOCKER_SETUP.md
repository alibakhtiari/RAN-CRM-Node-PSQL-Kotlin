# Backend Docker Setup & Testing Guide

## 🐳 Docker Backend Instructions

### 1. First Time Setup (Fresh Database)

If you're starting fresh, just run:

```bash
cd backend
docker-compose up --build
```

This will:
- Build the Node.js backend container
- Start PostgreSQL database
- Run `schema.sql` automatically (creates tables with new sync_audit schema)
- Run `init-admin.sql` (creates admin and user accounts)
- Start the backend server on `http://localhost:3000`

**Default Accounts**:
- Admin: `admin` / `admin123`
- User: `user` / `user123`

---

### 2. Existing Database (Migration Required)

If your database already exists, you need to migrate the `sync_audit` table:

**Option A: Via Docker**
```bash
cd backend
docker-compose exec postgres psql -U postgres -d ran_crm -f /docker-entrypoint-initdb.d/migrations/001_update_sync_audit.sql
```

**Option B: Manually**
```bash
# Connect to the database
docker-compose exec postgres psql -U postgres -d ran_crm

# Then run:
DROP TABLE IF EXISTS sync_audit CASCADE;

CREATE TABLE sync_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    sync_type VARCHAR(20) CHECK(sync_type IN ('contacts', 'calls', 'full')),
    status VARCHAR(20) CHECK(status IN ('success', 'error')) NOT NULL,
    error_message TEXT,
    synced_contacts INT DEFAULT 0,
    synced_calls INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_sync_audit_user ON sync_audit(user_id, created_at DESC);

-- Exit
\q
```

**Option C: Reset Database (Nuclear)**
```bash
# Stop containers
docker-compose down -v

# Restart fresh (this DELETES ALL DATA)
docker-compose up --build
```

---

### 3. Common Docker Commands

**Start backend**:
```bash
docker-compose up
```

**Start in background**:
```bash
docker-compose up -d
```

**Stop backend**:
```bash
docker-compose down
```

**View logs**:
```bash
docker-compose logs -f
```

**Restart just the backend** (after code changes):
```bash
docker-compose restart backend
```

**Rebuild everything**:
```bash
docker-compose down
docker-compose up --build
```

---

## 🧪 Testing Backend Changes

### 1. Test Health Check
```bash
curl http://localhost:3000/health
```

Expected response:
```json
{
  "status": "OK",
  "timestamp": "2024-01-15T10:00:00.000Z"
}
```

---

### 2. Test Login
```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Expected response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "username": "admin",
    "name": "Admin User",
    "is_admin": true
  }
}
```

**Save the token for next requests!**

---

### 3. Test Call Logs (with user filtering)

**As Regular User** (should see only their own logs):
```bash
TOKEN="your_user_token_here"

curl http://localhost:3000/calls \
  -H "Authorization: Bearer $TOKEN"
```

**As Admin** (should see all logs with usernames):
```bash
TOKEN="your_admin_token_here"

curl http://localhost:3000/calls \
  -H "Authorization: Bearer $TOKEN"
```

Expected response (admin):
```json
{
  "data": [
    {
      "id": "uuid",
      "user_id": "uuid",
      "username": "john_doe",
      "user_name": "John Doe",
      "contact_name": "Jane Smith",
      "direction": "outgoing",
      "duration_seconds": 180,
      "timestamp": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 10,
    "total": 1,
    "totalPages": 1
  }
}
```

---

### 4. Test Clear Call Logs (Admin Only)

```bash
TOKEN="your_admin_token_here"

curl -X DELETE http://localhost:3000/calls \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "message": "All call logs cleared successfully",
  "deleted_count": 42
}
```

**As non-admin** (should fail with 403):
```bash
TOKEN="your_user_token_here"

curl -X DELETE http://localhost:3000/calls \
  -H "Authorization: Bearer $TOKEN"
```

Expected error:
```json
{
  "error": "Admin access required"
}
```

---

### 5. Test Sync Audit

**Record a sync event**:
```bash
TOKEN="your_token_here"

curl -X POST http://localhost:3000/sync-audit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sync_type": "contacts",
    "status": "success",
    "synced_contacts": 50,
    "synced_calls": 0,
    "error_message": null
  }'
```

Expected response:
```json
{
  "sync_audit": {
    "id": "uuid",
    "user_id": "uuid",
    "sync_type": "contacts",
    "status": "success",
    "created_at": "2024-01-15T10:00:00Z"
  }
}
```

**Get sync audit** (admin only):
```bash
TOKEN="your_admin_token_here"

curl http://localhost:3000/sync-audit \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "sync_audits": [
    {
      "user_id": "uuid",
      "username": "john_doe",
      "user_name": "John Doe",
      "contacts_status": "success",
      "contacts_last_sync": "2024-01-15T10:00:00Z",
      "synced_contacts": 50,
      "contacts_error": null,
      "calls_status": null,
      "calls_last_sync": null,
      "synced_calls": 0,
      "calls_error": null
    }
  ]
}
```

---

## 🌐 Test Admin Panel

1. Open browser: `http://localhost:3000/admin`
2. Login with `admin` / `admin123`
3. Navigate to each tab:
   - **Users**: Should show user list
   - **Contacts**: Should show contacts
   - **Call Logs**: Should show call logs with usernames and "Clear All" button
   - **Sync Audit**: Should show sync status grid with colored indicators

---

## 📱 Test Android App Integration

### 1. Update Android API URL

In `android/app/src/main/java/com/ran/crm/data/remote/ApiClient.kt`, ensure:
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/" // For emulator
// or
private const val BASE_URL = "http://YOUR_IP:3000/" // For physical device
```

### 2. Run a Sync from Android

1. Open the app
2. Go to Settings
3. Press "Sync"
4. Check Logcat for:
   ```
   SyncLogger: === STARTING FULL SYNC ===
   SyncLogger: Sync audit recorded: type=full, status=success
   ```

### 3. Verify in Admin Panel

1. Refresh Admin Panel → Sync Audit tab
2. Should see new entry with:
   - Your username
   - Success status (green)
   - Contact and call counts
   - Timestamp

---

## 🐛 Troubleshooting

### Port 3000 Already in Use
```bash
# Find process using port 3000
lsof -ti:3000 | xargs kill -9  # macOS/Linux
netstat -ano | findstr :3000   # Windows

# Or change port in docker-compose.yml:
ports:
  - "3001:3000"  # Change 3000 to 3001
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Schema Migration Failed
```bash
# Drop and recreate database
docker-compose down -v
docker-compose up --build
```

### Backend Not Reflecting Code Changes
```bash
# Hot reload should work, but if not:
docker-compose restart backend

# Or rebuild:
docker-compose down
docker-compose up --build
```

---

## 📊 Admin Panel Features Checklist

- [ ] **Call Logs Tab**
  - [ ] Username displayed for each log
  - [ ] Phone number shown for unknown contacts
  - [ ] Timestamp displayed
  - [ ] "Clear All Call Logs" button visible (admin only)
  - [ ] Non-admin users see only their own logs
  
- [ ] **Sync Audit Tab**
  - [ ] Shows all users' sync status
  - [ ] Separate columns for Contacts and Calls
  - [ ] Color-coded status (green=success, red=error)
  - [ ] Last sync timestamp
  - [ ] Error messages displayed
  - [ ] Synced item counts shown

---

## 🎉 Success Checklist

✅ Docker backend running on port 3000  
✅ Database schema updated (sync_audit table)  
✅ Admin panel accessible at `/admin`  
✅ Call logs show usernames  
✅ User filtering works (non-admin sees only own logs)  
✅ Clear call logs works (admin only)  
✅ Sync audit tracking works  
✅ Android app can record sync events  
✅ Admin panel displays sync status correctly  

---

## 📝 Notes

- **Sync Audit**: Automatically recorded by Android app after each sync
- **Admin Panel**: Real-time updates require page refresh
- **Call Log Filtering**: Based on JWT token's `user_id` and `is_admin` fields
- **Database**: PostgreSQL data persists in Docker volumes
- **Logs**: Check `docker-compose logs -f` for backend errors
