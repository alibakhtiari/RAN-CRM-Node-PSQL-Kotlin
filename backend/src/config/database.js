const { Pool } = require('pg');
require('dotenv').config();

const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'ran_crm',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'password',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
};

console.log('Initializing DB Pool with config:', {
  ...dbConfig,
  password: '****'
});

const pool = new Pool(dbConfig);

pool.on('connect', (client) => {
  console.log('New client connected to the database');
});

pool.on('error', (err, client) => {
  console.error('Unexpected error on idle client', err);
  // Do NOT exit process, let it recover
});

module.exports = pool;
