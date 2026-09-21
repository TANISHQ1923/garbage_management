const mongoose = require('mongoose');
const env = require('./env');

const connectDB = async () => {
  if (!env.MONGODB_URI) {
    console.error('CRITICAL ERROR: MONGODB_URI is not defined in environment variables.');
    console.error('Please configure MONGODB_URI in your .env file or environment.');
    if (env.NODE_ENV !== 'test') {
      process.exit(1);
    }
    return null;
  }

  try {
    const conn = await mongoose.connect(env.MONGODB_URI, {
      serverSelectionTimeoutMS: 5000,
    });
    console.log(`[MongoDB] Connected successfully to host: ${conn.connection.host}`);
    return conn;
  } catch (error) {
    console.error(`[MongoDB] Connection error: ${error.message}`);
    if (env.NODE_ENV === 'test') {
      // In tests, don't crash the runner immediately if testing disconnected states
      return null;
    }
    console.error('[MongoDB] Running without an active database connection. Some APIs will return 503 until MongoDB is available.');
    return null;
  }
};

const getDBStatus = () => {
  const states = ['disconnected', 'connected', 'connecting', 'disconnecting'];
  const stateCode = mongoose.connection.readyState;
  return {
    state: states[stateCode] || 'unknown',
    isConnected: stateCode === 1
  };
};

module.exports = {
  connectDB,
  getDBStatus
};
