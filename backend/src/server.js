const app = require('./app');
const env = require('./config/env');
const { connectDB } = require('./config/db');

const startServer = async () => {
  if (!env.JWT_SECRET) throw new Error('JWT_SECRET must be configured before starting the server.');
  for (const key of ['DRIVER_LOCATION_MAX_AGE_MS', 'DRIVER_SERVICE_RADIUS_KM']) {
    if (!Number.isFinite(env[key]) || env[key] <= 0) throw new Error(key + ' must be a positive number.');
  }
  // Connect to MongoDB Atlas or local MongoDB instance
  await connectDB();
  const stopSensorWorker = require('./workers/sensorDispatchWorker').startSensorDispatchWorker();

  const server = app.listen(env.PORT, '0.0.0.0', () => {
    console.log('====================================================');
    console.log(` Smart Garbage Management API Service`);
    console.log(` Environment: ${env.NODE_ENV}`);
    console.log(` Server running on: http://localhost:${env.PORT}`);
    console.log(` Health check URL: http://localhost:${env.PORT}/api/health`);
    console.log(` Android Emulator URL: http://10.0.2.2:${env.PORT}/api/`);
    console.log('====================================================');
  });

  // Graceful shutdown handling
  const handleShutdown = (signal) => {
    stopSensorWorker();
    console.log(`[Server] Received ${signal}. Shutting down gracefully...`);
    server.close(() => {
      console.log('[Server] HTTP server closed.');
      process.exit(0);
    });
  };

  process.on('SIGTERM', () => handleShutdown('SIGTERM'));
  process.on('SIGINT', () => handleShutdown('SIGINT'));
};

startServer();
