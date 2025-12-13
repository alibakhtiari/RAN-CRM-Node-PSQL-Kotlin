const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'ran_crm',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'password',
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000, // Increased to 10s
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
});

pool.on('connect', (client) => {
  console.log('New client connected to the database');
});

pool.on('error', (err, client) => {
  console.error('Unexpected error on idle client', err);
  // Do NOT exit process, let it recover
});

module.exports = pool;
