# RAN CRM Backend

Contact & Call Log Sync CRM — Node.js API + React Admin Panel.

## Tech Stack

- **Runtime**: Node.js 22+
- **Framework**: Express.js
- **Database**: MariaDB 10.6+
- **Process Manager**: PM2
- **Admin Panel**: React + Vite + Tailwind CSS

## Development

```bash
# Install dependencies (backend + admin panel)
npm install
cd admin-panel && npm install && cd ..

# Run both API and Admin Panel together
npm run dev:all

# Or run separately
npm run dev          # API only (port 3000)
npm run admin:dev    # Admin panel only (port 5173)
```

**Default Accounts:**
- Admin: `admin` / `admin123`
- User: `user` / `user123`

## Production Deployment

### 1. Set Up MariaDB

```bash
# On your MariaDB server
mysql -u root -p

CREATE DATABASE ran_crm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ran_user'@'%' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON ran_crm.* TO 'ran_user'@'%';
FLUSH PRIVILEGES;
EXIT;

# Import schema and default users
mysql -u ran_user -p ran_crm < database/schema.sql
mysql -u ran_user -p ran_crm < database/init-admin.sql
```

### 2. Configure Environment

```bash
cp .env.example .env
```

Edit `.env`:

```env
DB_HOST=your-mariadb-host
DB_PORT=3306
DB_NAME=ran_crm
DB_USER=ran_user
DB_PASSWORD=STRONG_PASSWORD_HERE
DB_SSL=false

JWT_SECRET=GENERATE_WITH_openssl_rand_-base64_48
NODE_ENV=production
PORT=3000
```

### 3. Build & Start

```bash
# Install production dependencies
npm install --omit=dev

# Build admin panel
npm run admin:build

# Install PM2 globally (once)
npm install -g pm2

# Start
npm run pm2:start

# Verify
curl http://localhost:3000/health
```

### 4. PM2 Commands

```bash
npm run pm2:start     # Start in production mode
npm run pm2:stop      # Stop
npm run pm2:restart   # Restart
npm run pm2:logs      # View logs

pm2 startup           # Auto-start on server reboot (run once)
pm2 save              # Save current process list
```

## API Reference

All endpoints require `Authorization: Bearer <token>` except login.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login (username + password) |
| `GET` | `/auth/me` | Current user info |
| `GET` | `/contacts?page=&limit=&q=` | List/search contacts |
| `GET` | `/contacts/search?q=` | Search contacts |
| `GET` | `/contacts/export` | Export contacts as CSV |
| `POST` | `/contacts` | Create contact |
| `POST` | `/contacts/batch` | Bulk upsert contacts |
| `PUT` | `/contacts/:id` | Update contact |
| `DELETE` | `/contacts/:id` | Soft-delete contact |
| `GET` | `/calls?page=&limit=` | Call history |
| `GET` | `/calls/:contact_id` | Calls for contact |
| `POST` | `/calls` | Bulk upload calls |
| `GET` | `/sync` | Sync records |
| `POST` | `/sync` | Record sync |
| `GET` | `/sync-audit` | Sync audit (admin) |
| `POST` | `/sync-audit` | Create audit entry |
| `GET` | `/users` | List users (admin) |
| `POST` | `/users` | Create user (admin) |
| `PATCH` | `/users/:id` | Update user (admin) |
| `DELETE` | `/users/:id` | Delete user (admin) |

## Environment Variables

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `DB_HOST` | `localhost` | ✅ prod | MariaDB host |
| `DB_PORT` | `3306` | | MariaDB port |
| `DB_NAME` | `ran_crm` | ✅ prod | Database name |
| `DB_USER` | `root` | ✅ prod | Database user |
| `DB_PASSWORD` | `password` | ✅ prod | Database password |
| `DB_SSL` | `false` | | Enable SSL for remote DB |
| `JWT_SECRET` | dev fallback | ✅ prod | JWT signing key (app crashes without it) |
| `JWT_EXPIRES_IN` | `30d` | | Token expiration |
| `PORT` | `3000` | | Server port |
| `NODE_ENV` | `development` | | `production` for prod |

## Project Structure

```
backend/
├── src/
│   ├── server.js          # Entry point
│   ├── app.js             # Express config
│   ├── config/            # DB + JWT config
│   ├── routes/            # API routes
│   ├── repositories/      # Data access layer
│   ├── middleware/         # Auth, validation, errors
│   ├── schemas/           # Zod validation schemas
│   └── utils/             # Logger, pagination, phone
├── admin-panel/           # React admin dashboard
├── database/              # SQL schema + seed data
├── ecosystem.config.js    # PM2 config
└── knexfile.js            # Knex DB config
```
