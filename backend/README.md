# RAN CRM Backend

Contact & Call Log Sync CRM — Node.js API with React Admin Panel.

## Quick Start (Development)

```bash
npm install
cd admin-panel && npm install && cd ..
npm run dev:all
```

Opens API on `http://localhost:3000` and Admin Panel on `http://localhost:5173/admin/`.

**Default Accounts:** `admin` / `admin123` · `user` / `user123`

---

## Production Deployment

### 1. Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE ran_crm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ran_user'@'%' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ran_crm.* TO 'ran_user'@'%';
FLUSH PRIVILEGES;
```

```bash
mysql -u ran_user -p ran_crm < database/setup.sql
```

### 2. Environment

```bash
cp .env.example .env
```

```env
DB_HOST=your-mariadb-host
DB_PORT=3306
DB_NAME=ran_crm
DB_USER=ran_user
DB_PASSWORD=YOUR_STRONG_PASSWORD
JWT_SECRET=CHANGE_ME_openssl_rand_-base64_48
NODE_ENV=production
PORT=3000
```

### 3. Build & Start

```bash
npm install --omit=dev
cd admin-panel && npm install && npm run build && cd ..
npm install -g pm2
npm run pm2:start
```

The admin panel builds to `public/admin/` and is served by Express at `/admin/`. **One PM2 process runs everything** — API + Admin Panel.

### 4. Verify

```
GET http://your-server:3000/health    → API
GET http://your-server:3000/admin/    → Admin Panel
```

### 5. PM2 Management

```bash
npm run pm2:start       # Start
npm run pm2:stop        # Stop
npm run pm2:restart     # Restart
npm run pm2:logs        # View logs
pm2 startup             # Auto-start on reboot (run once)
pm2 save                # Save process list
```

---

## NPM Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | API only (nodemon) |
| `npm run dev:all` | API + Admin Panel together |
| `npm run admin:build` | Build admin panel for production |
| `npm run pm2:start` | Start production server |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MariaDB host |
| `DB_PORT` | `3306` | MariaDB port |
| `DB_NAME` | `ran_crm` | Database name |
| `DB_USER` | `root` | Database user |
| `DB_PASSWORD` | — | Database password |
| `DB_SSL` | `false` | SSL for remote DB |
| `JWT_SECRET` | — | **Required in production** |
| `JWT_EXPIRES_IN` | `30d` | Token expiration |
| `PORT` | `3000` | Server port |

## Project Structure

```
backend/
├── src/                   # Express API
├── admin-panel/           # React admin (Vite)
│   └── dist → public/admin/  # Build output
├── database/setup.sql     # Schema + seed data
├── ecosystem.config.js    # PM2 config
└── knexfile.js            # DB connection config
```
