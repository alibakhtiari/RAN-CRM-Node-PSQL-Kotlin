📘 Final PRD — Kotlin Android App

Project: Shared Contact & Call-Log Sync CRM
Backend: Node.js + PostgreSQL (already implemented)
Platform: Android (Kotlin, Jetpack Compose)
Version: 2025.11-stable


1. Goal

Internal Android CRM app that synchronizes shared contacts and call logs across multiple users in one organization.
All contacts and call logs are stored in a private app storage (not system contacts).
Duplicate phone numbers are prevented by backend constraint.
App operates fully offline with background sync, retries, and delta updates.


2. Core Features

2.1 Authentication
	•	Login via /auth/login (username/password).
	•	JWT stored securely; appended to all API requests.
	•	Auto logout when token expires (HTTP 403).

2.2 Contacts
	•	Two-way sync between device and backend.
	•	Contacts stored locally in Room (contacts table).
	•	Imported from all sources (phone, WhatsApp, Google).
	•	Duplicates removed using normalized phone numbers (E.164).
	•	“Older contact wins” if conflict detected.
	•	Backend endpoints support:
	•	GET /contacts?page&limit&updated_since
	•	POST /contacts/batch
	•	Local paging (infinite scroll).
	•	Advanced search (T9 + fuzzy match).
	•	Contact actions: Call, Message, WhatsApp, Edit.

2.3 Call Logs
	•	Read device call logs (incoming, outgoing, missed).
	•	Store locally; upload new logs to backend in batches.
	•	Display call history for each contact: time, user, direction, duration.
	•	Backend endpoints:
	•	GET /calls?page&limit&updated_since
	•	POST /calls or /calls/batch

2.4 Sync
	•	Delta sync: Only changed records since lastSync.
	•	Batch upload:
	•	Contacts → 100 per request.
	•	Call logs → up to 1000 per request.
	•	Pagination: For download requests (page, limit, pagination object).
	•	Conflict handling:
	•	On 409 → replace local duplicate with server record.
	•	Local caching: All operations read from Room first.
	•	Background sync:
	•	Implemented with WorkManager (PeriodicWorkRequest).
	•	Configurable interval (15 m, 30 m, 1 h, manual).
	•	Persistent notification (optional) for long-running background tasks.
	•	Shortcut to disable battery optimization (opens Settings).

3. UI Structure

Screen	Function
Login	Authenticates and stores JWT
Contacts	Lists contacts (paged), add/edit/delete, search
Contact Detail	Shows call history + actions
Call Logs	Displays global history
Settings	Sync interval, persistent notification toggle, sync logs, logout

UI should remain responsive — data always comes from local Room DB, while background sync updates silently.


4. Local Database (Room Models)

Identical to definitions in MOBILE_INTEGRATION.md
	•	User
	•	Contact
	•	CallLog
	•	SyncAudit

All fields follow ISO 8601 timestamps; IDs are UUID.

5. Sync Workflow

5.1 Initial Sync
	1.	Login → store JWT.
	2.	Fetch contacts (paginated).
	3.	Fetch call logs (paginated).
	4.	Store locally.
	5.	Save lastSync timestamp.

5.2 Incremental Sync
	1.	Upload local queued contacts via /contacts/batch.
	2.	Upload new call logs via /calls or /calls/batch.
	3.	Fetch remote updates with updated_since=<lastSync>.
	4.	Merge (upsert) into Room.
	5.	Update lastSync.

5.3 Conflict Handling
	•	409 Conflict → remove local record, insert server version.
	•	If contact created outside app causes 409 → delete locally.

5.4 Background Operation
	•	WorkManager jobs with:

setRequiredNetworkType(NetworkType.CONNECTED)
setRequiresBatteryNotLow(true)


	•	Optionally run as foreground service with persistent notification.


6. Technical Stack (Latest Stable – Nov 2025)

Component	Latest Stable	Notes
Kotlin	2.2.21	JetBrains 2025 LTS
Android Gradle Plugin	8.13.0	Compatible with Gradle 8.13
Gradle	8.13	Required for AGP 8.13
Jetpack Compose BOM	2025.10	Compose 1.8.x compatible with Kotlin 2.2
Room	2.7.0	Full KSP support
WorkManager	2.10.0	Jetpack 2025 release
Retrofit	2.12.0	Latest stable
OkHttp	5.2.0	Stable with HTTP/2 + TLS1.3
Coroutines	1.10.1	Kotlin 2.2 compatible
Navigation Compose	2.9.0	Kotlin 2.2 compatible
libphonenumber	8.13.50	2025 release

All dependencies locked in Gradle using version catalog; verify CI for upgrades.

7. Performance & UX Optimizations
	•	Batch uploads reduce API overhead.
	•	Delta fetch minimizes network cost.
	•	Local paging keeps scrolling smooth.
	•	Advanced search improves usability for large contact sets.
	•	Foreground service optional to maintain reliability.
	•	Background constraints reduce wake-ups.
	•	Progress indicators for imports to prevent UI freeze.
	•	Offline-first design ensures instant data access.

8. Testing Checklist
	•	✅ Login, token storage, re-auth on 403.
	•	✅ Contact sync both ways; duplicates handled.
	•	✅ Call logs upload and fetch.
	•	✅ Batch + delta logic verified.
	•	✅ Pagination works properly.
	•	✅ Background sync persists after reboot.
	•	✅ Persistent notification toggle works.
	•	✅ Large imports don’t block UI.
	•	✅ Search and paging performant on 10 k contacts.
