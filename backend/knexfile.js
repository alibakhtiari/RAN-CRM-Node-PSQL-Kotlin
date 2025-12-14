require('dotenv').config();

module.exports = {
    development: {
        client: 'pg',
        connection: {
            host: process.env.DB_HOST || 'localhost',
            port: process.env.DB_PORT || 5432,
            database: process.env.DB_NAME || 'ran_crm',
            user: process.env.DB_USER || 'postgres',
            password: process.env.DB_PASSWORD || 'password',
            ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
        },
        pool: {
            min: 2,
            max: 10
        },
        migrations: {
            tableName: 'knex_migrations',
            directory: './src/database/migrations'
        },
        seeds: {
            directory: './src/database/seeds'
        }
    },
    production: {
        client: 'pg',
        connection: {
            host: process.env.DB_HOST,
            port: process.env.DB_PORT,
            database: process.env.DB_NAME,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
        },
        pool: {
            min: 2,
            max: 20
        },
        migrations: {
            tableName: 'knex_migrations',
            directory: './src/database/migrations'
        }
    }
};
