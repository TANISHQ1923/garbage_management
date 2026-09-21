const express = require('express');
const multer = require('multer');
const { requireAuth } = require('../middleware/authMiddleware');
const { handleUpload } = require('../controllers/mediaController');
const env = require('../config/env');

const router = express.Router();

// Memory storage so file buffer streams directly to Cloudinary without local disk writes
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    // Upper bound on multipart payload (max video limit)
    fileSize: Math.max(env.MAX_VIDEO_SIZE_MB, env.MAX_IMAGE_SIZE_MB, 50) * 1024 * 1024
  }
});

// Protected upload endpoint
router.post('/upload', requireAuth, upload.single('file'), handleUpload);

module.exports = router;
