const app = require('./app');
const logger = require('./utils/logger');

const PORT = process.env.PORT || 3000;

const server = app.listen(PORT, () => {
  logger.info(`Server running on port ${PORT}`);
  logger.info(`Health check: http://localhost:${PORT}/health`);
});

const knex = require('./config/knex');

const gracefulShutdown = async () => {
  logger.info('Received kill signal, shutting down gracefully');
  server.close(async () => {
    logger.info('Closed out remaining connections');
    try {
      await knex.destroy();
      logger.info('Knex connection destroyed');
    } catch (err) {
      logger.error('Error during database disconnection', err);
    }
    process.exit(0);
  });

  // Force close after 10s
  setTimeout(() => {
    logger.error('Could not close connections in time, forcefully shutting down');
    process.exit(1);
  }, 10000);
};

process.on('SIGTERM', gracefulShutdown);
process.on('SIGINT', gracefulShutdown);
