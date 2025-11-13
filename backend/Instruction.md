PRD — Backend Only (Optimized)
Project Name: Contact & Call Log Sync CRM (Backend)
Stack: Node.js + Express + PostgreSQL
Scale: ~10 users, ~20k contacts, call logs growing
Purpose: Provide fast, stable API for Android app sync of contacts & call logs, prevent duplicates, keep owner/creator logic, minimal complexity.
Core Requirements
	•	Authentication: JWT based, admin creates users manually, login/logout endpoints.
	•	Contacts:
	•	Fields: id (UUID), name, phone_normalized (unique), phone_raw, created_by, created_at, updated_at.
	•	Unique constraint on phone_normalized.
	•	Conflict handling: Keep the first/older contact for a phone number; newer attempts either adopt server version or skip.
	•	Call Logs:
	•	Fields: id (UUID), user_id, contact_id (nullable), direction (incoming/outgoing/missed), duration_seconds, timestamp.
	•	Upload from device; query from UI for history.
	•	Sync Logs: Record summary of each sync for auditing.
	•	Pagination: All list endpoints support page + limit or cursor + limit for performance.
	•	Performance & Stability:
	•	No external caching layer (Redis) required for this scale.
	•	Use proper indexing on phone, timestamp, contact_id.
	•	Batch inserts for call logs.
	•	Connection pooling.
	•	Simple architecture: single backend + PostgreSQL.
	•	Security & Maintenance:
	•	Use prepared statements / ORM.
	•	Store password hashes (bcrypt).
	•	HTTPS enforced.
	•	Admin endpoints protected.
	•	Backup and monitoring in place.
Database Schema (PostgreSQL)

users
	•	id UUID PK (default gen_random_uuid())
	•	name TEXT
	•	email TEXT UNIQUE
	•	password_hash TEXT
	•	is_admin BOOLEAN DEFAULT false
	•	created_at TIMESTAMPTZ DEFAULT now()
	•	updated_at TIMESTAMPTZ DEFAULT now()

contacts
	•	id UUID PK
	•	name TEXT NOT NULL
	•	phone_raw TEXT NOT NULL
	•	phone_normalized TEXT NOT NULL UNIQUE
	•	created_by UUID FK users(id)
	•	created_at TIMESTAMPTZ DEFAULT now()
	•	updated_at TIMESTAMPTZ DEFAULT now()

Indexes:

CREATE UNIQUE INDEX idx_contacts_phone ON contacts(phone_normalized);
CREATE INDEX idx_contacts_name_trgm ON contacts USING gin (name gin_trgm_ops);

call_logs
	•	id UUID PK
	•	user_id UUID FK users(id)
	•	contact_id UUID FK contacts(id) — nullable
	•	direction VARCHAR(10) CHECK(direction IN (‘incoming’,’outgoing’,’missed’))
	•	duration_seconds INT
	•	timestamp TIMESTAMPTZ DEFAULT now()

Indexes:

CREATE INDEX idx_call_logs_contact ON call_logs(contact_id);
CREATE INDEX idx_call_logs_timestamp ON call_logs(timestamp DESC);

sync_audit
	•	id UUID PK
	•	user_id UUID FK users(id)
	•	synced_contacts INT
	•	synced_calls INT
	•	created_at TIMESTAMPTZ DEFAULT now()
API Endpoints

Auth
	•	POST /auth/login → login, returns { token, user }
	•	GET /auth/me → returns current user

Users (Admin only)
	•	GET /users?page=&limit=
	•	POST /users → create user
	•	DELETE /users/:id

Contacts
	•	GET /contacts?page=&limit= → list contacts
	•	GET /contacts/search?q=<name>&page=&limit= → search by name
	•	POST /contacts → create contact (client sends name, phone_raw, phone_normalized, created_at)
	•	PUT /contacts/:id → update contact (owner only)
	•	DELETE /contacts/:id → delete contact (owner only)

Conflict handling in POST /contacts:
	•	Attempt insert; on unique violation of phone_normalized, fetch existing row; if incoming created_at is older, update existing; else respond with existing to client.

Call Logs
	•	GET /calls?page=&limit= → global list
	•	GET /calls/:contact_id?page=&limit= → call history for contact
	•	POST /calls → bulk upload array of calls { user_id, contact_id?, direction, timestamp, duration_seconds, phone_normalized }

Sync Logs
	•	GET /sync?page=&limit= → recent sync records
Implementation Notes
	•	Use Node.js v24 LTS (or latest even-numbered LTS) for stability. ([turn0search19])
	•	Use PostgreSQL 17 or 18 for production; PostgreSQL 18 released Sept 2025. ([turn0search10])
	•	Use pg module or ORM (Prisma/TypeORM) with UUID support (gen_random_uuid())
	•	Batch inserts for call logs; wrap in transactions
	•	Use server-side phone normalization (libphonenumber-js) though client pre-normalizes
	•	Use OFFSET/LIMIT for pagination or keyset if future scale grows
	•	Monitor DB size: call logs could grow large; consider partitioning or archiving older logs after X months
	•	Backups: nightly dumps or managed backups
What Mobile Team Needs

Provide mobile developers with:
	•	API endpoints list + expected request/response JSON
	•	Field names/types for contacts and call_logs (so Room/Kotlin models match)
	•	Auth flow (login → receive token → include Authorization: Bearer <token>)
	•	Sync logic: explain no duplicate phone numbers, older wins rule, upload only new call logs, pagination for lists
Suggested Future Enhancements
	•	Soft-delete for contacts (add is_deleted flag) so call logs remain linked
	•	Partitioning of call_logs by month if millions of records
	•	Role-based permissions beyond admin/user (e.g., viewer only)
	•	Export feature (CSV/Excel) for contacts or call logs
	•	Notification push (server → mobile) for new contact additions