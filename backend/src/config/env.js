const dotenv = require('dotenv');
const path = require('path');

// Load environment variables from .env
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const env = {
  NODE_ENV: process.env.NODE_ENV || 'development',
  PORT: parseInt(process.env.PORT || '5000', 10),
  MONGODB_URI: process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/garbage_management',
  JWT_SECRET: process.env.JWT_SECRET || 'dev_fallback_secret_key_smart_garbage_2026',
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',
  CLIENT_ORIGIN: process.env.CLIENT_ORIGIN || '*',
  // Cloudinary Cloud Media Storage
  CLOUDINARY_CLOUD_NAME: process.env.CLOUDINARY_CLOUD_NAME || '',
  CLOUDINARY_API_KEY: process.env.CLOUDINARY_API_KEY || '',
  CLOUDINARY_API_SECRET: process.env.CLOUDINARY_API_SECRET || '',
  // Media Size Limits (in MB)
  MAX_IMAGE_SIZE_MB: parseInt(process.env.MAX_IMAGE_SIZE_MB || '10', 10),
  MAX_VIDEO_SIZE_MB: parseInt(process.env.MAX_VIDEO_SIZE_MB || '50', 10),
  // Python FastAPI AI Analysis Service
  AI_SERVICE_URL: process.env.AI_SERVICE_URL || 'http://localhost:8000',
  AI_SERVICE_TIMEOUT_MS: parseInt(process.env.AI_SERVICE_TIMEOUT_MS || '5000', 10)
};

module.exports = env;
