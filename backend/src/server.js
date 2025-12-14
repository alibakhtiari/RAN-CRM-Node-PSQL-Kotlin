const app = require('./app');

const PORT = process.env.PORT || 3000;

const server = app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
});

const pool = require('./config/database');
const knex = require('./config/knex');

const gracefulShutdown = async () => {
  console.log('Received kill signal, shutting down gracefully');
  server.close(async () => {
    console.log('Closed out remaining connections');
    try {
      await pool.end();
      console.log('PostgreSQL pool closed');
      await knex.destroy();
      console.log('Knex connection destroyed');
    } catch (err) {
      console.error('Error during database disconnection', err);
    }
    process.exit(0);
  });

  // Force close after 10s
  setTimeout(() => {
    console.error('Could not close connections in time, forcefully shutting down');
    process.exit(1);
  }, 10000);
};

process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);
