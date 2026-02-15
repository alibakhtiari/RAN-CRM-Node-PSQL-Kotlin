require('dotenv').config();

module.exports = {
    development: {
        client: 'mysql2',
        connection: {
            host: process.env.DB_HOST || 'localhost',
            port: parseInt(process.env.DB_PORT) || 3306,
            database: process.env.DB_NAME || 'ran_crm',
            user: process.env.DB_USER || 'root',
            password: process.env.DB_PASSWORD || 'password',
            charset: 'utf8mb4',
        },
        pool: {
            min: 2,
            max: 10
        },
    },
    production: {
        client: 'mysql2',
        connection: {
            host: process.env.DB_HOST,
            port: parseInt(process.env.DB_PORT) || 3306,
            database: process.env.DB_NAME,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            charset: 'utf8mb4',
            ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
        },
        pool: {
            min: 2,
            max: 20
        },
    }
};
