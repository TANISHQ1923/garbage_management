// Unit tests must never inherit production endpoints or credentials.
process.env.NODE_ENV = 'test';
process.env.MONGODB_URI = 'mongodb://127.0.0.1:27017/phase0_unused';
process.env.JWT_SECRET = 'local-test-signing-key-only';
process.env.CLOUDINARY_CLOUD_NAME = '';
process.env.CLOUDINARY_API_KEY = '';
process.env.CLOUDINARY_API_SECRET = '';
process.env.AI_SERVICE_URL = 'http://127.0.0.1:1';
process.env.USE_SYNTHETIC_DATA = 'false';
