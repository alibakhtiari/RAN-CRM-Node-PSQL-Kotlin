module.exports = {
    apps: [{
        name: 'ran-crm-backend',
        script: 'src/server.js',
        instances: 1,
        autorestart: true,
        watch: false,
        max_memory_restart: '512M',
        env: {
            NODE_ENV: 'development',
            PORT: 3000,
        },
        env_production: {
            NODE_ENV: 'production',
            PORT: 3000,
        },
        // Graceful shutdown
        kill_timeout: 10000,
        listen_timeout: 5000,
        // Logging
        error_file: './logs/pm2-error.log',
        out_file: './logs/pm2-out.log',
        merge_logs: true,
        log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
    }],
};
