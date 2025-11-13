# RAN CRM Backend

Contact & Call Log Sync CRM Backend API built with Node.js, Express, and PostgreSQL.

## Features

- JWT-based authentication
- User management (admin only)
- Contact synchronization with conflict resolution
- Call log bulk upload and retrieval
- Sync audit logging
- Pagination support
- Docker containerization

## Tech Stack

- **Runtime**: Node.js 22+ (LTS)
- **Framework**: Express.js
- **Database**: PostgreSQL 17+
- **Authentication**: JWT
- **Container**: Docker

## Quick Start

### Using Docker Compose (Recommended)

1. **Clone and navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Start services**
   ```bash
   docker-compose up -d
   ```

3. **Check health**
   ```bash
   curl http://localhost:3000/health
   ```

The database schema will be automatically initialized.

### Manual Setup

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Set up PostgreSQL**
   - Create database: `ran_crm`
   - Run schema: `psql -d ran_crm -f database/schema.sql`

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your database credentials
   ```

4. **Start server**
   ```bash
   npm run dev  # Development
   npm start    # Production
   ```

## API Endpoints

### Authentication
- `POST /auth/login` - Login with email/password
- `GET /auth/me` - Get current user info

### Users (Admin Only)
- `GET /users?page=&limit=` - List users
- `POST /users` - Create user
- `DELETE /users/:id` - Delete user

### Contacts
- `GET /contacts?page=&limit=` - List contacts
- `GET /contacts/search?q=<name>&page=&limit=` - Search contacts
- `POST /contacts` - Create/update contact (with conflict handling)
- `PUT /contacts/:id` - Update contact (owner only)
- `DELETE /contacts/:id` - Delete contact (owner only)

### Call Logs
- `GET /calls?page=&limit=` - Global call history
- `GET /calls/:contact_id?page=&limit=` - Call history for contact
- `POST /calls` - Bulk upload calls

### Sync Audit
- `GET /sync?page=&limit=` - Recent sync records
- `POST /sync` - Record sync operation

## Request/Response Examples

### Login
```bash
POST /auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password"
}
```

Response:
```json
{
  "token": "jwt-token-here",
  "user": {
    "id": "uuid",
    "name": "Admin User",
    "email": "admin@example.com",
    "is_admin": true
  }
}
```

### Create Contact
```bash
POST /contacts
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "John Doe",
  "phone_raw": "+1 (555) 123-4567",
  "phone_normalized": "+15551234567",
  "created_at": "2023-01-01T00:00:00Z"
}
```

### Bulk Upload Calls
```bash
POST /calls
Authorization: Bearer <token>
Content-Type: application/json

{
  "calls": [
    {
      "contact_id": "uuid",
      "direction": "incoming",
      "duration_seconds": 120,
      "timestamp": "2023-01-01T12:00:00Z",
      "phone_normalized": "+15551234567"
    }
  ]
}
```

## Conflict Handling

For contacts, if a phone number already exists:
- Compare `created_at` timestamps
- **Older wins**: Update existing record with incoming data
- **Newer loses**: Return existing record without changes

## Pagination

All list endpoints support:
- `page`: Page number (default: 1)
- `limit`: Items per page (default: 10, max: 100)

Response format:
```json
{
  "data": [...],
  "pagination": {
    "currentPage": 1,
    "totalPages": 5,
    "totalItems": 50,
    "itemsPerPage": 10,
    "hasNext": true,
    "hasPrev": false
  }
}
```

## Docker Commands

```bash
# Build and run
docker-compose up --build

# Run in background
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f app

# Rebuild after code changes
docker-compose up --build --force-recreate

# Generate JWT secret for production
openssl rand -base64 32
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | ran_crm | Database name |
| `DB_USER` | postgres | Database user |
| `DB_PASSWORD` | password | Database password |
| `JWT_SECRET` | dev-secret | JWT signing secret |
| `JWT_EXPIRES_IN` | 24h | JWT expiration time |
| `PORT` | 3000 | Server port |

## Database Schema

See `database/schema.sql` for complete schema with indexes and constraints.

## Security

- Passwords hashed with bcrypt (12 rounds)
- JWT tokens for authentication
- Prepared statements prevent SQL injection
- Admin-only endpoints protected
- HTTPS enforced in production

## Performance

- Connection pooling (max 20 connections)
- Optimized indexes on frequently queried columns
- Batch inserts for call logs
- Pagination limits large result sets

## Development

```bash
# Install dependencies
npm install

# Run tests (when added)
npm test

# Development server with auto-reload
npm run dev

# Production build
npm run build
```

## Production Deployment

1. Set production environment variables
2. Use `docker-compose.prod.yml` or deploy to container orchestration
3. Set up database backups
4. Configure monitoring and logging
5. Use HTTPS with proper SSL certificates

## Mobile Integration

See [MOBILE_INTEGRATION.md](MOBILE_INTEGRATION.md) for comprehensive mobile development guide including:
- Kotlin/Room data models
- Sync logic implementation
- Network client setup
- Error handling strategies
- Testing guidelines
