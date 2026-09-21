const cloudinary = require('cloudinary').v2;
const { Readable } = require('stream');
const env = require('../config/env');

/**
 * Configure Cloudinary instance if credentials are present
 */
const isCloudinaryConfigured = () => {
  return Boolean(
    env.CLOUDINARY_CLOUD_NAME &&
    env.CLOUDINARY_API_KEY &&
    env.CLOUDINARY_API_SECRET
  );
};

if (isCloudinaryConfigured()) {
  cloudinary.config({
    cloud_name: env.CLOUDINARY_CLOUD_NAME,
    api_key: env.CLOUDINARY_API_KEY,
    api_secret: env.CLOUDINARY_API_SECRET,
    secure: true
  });
}

/**
 * Allowed MIME Types
 */
const ALLOWED_IMAGE_MIMES = ['image/jpeg', 'image/png', 'image/webp'];
const ALLOWED_VIDEO_MIMES = ['video/mp4', 'video/quicktime', 'video/webm'];

/**
 * Allowed logical folders
 */
const FOLDER_MAP = {
  complaints: 'smart-garbage/complaints',
  'before-cleaning': 'smart-garbage/before-cleaning',
  'after-cleaning': 'smart-garbage/after-cleaning',
  videos: 'smart-garbage/videos'
};

/**
 * Validate media file MIME type and file size
 * @param {Object} file - Multer file object with buffer, mimetype, size
 * @returns {{ isValid: boolean, resourceType: 'image' | 'video' }}
 */
const validateMedia = (file) => {
  if (!file) {
    const error = new Error('No media file provided for upload.');
    error.statusCode = 400;
    throw error;
  }

  const mime = file.mimetype.toLowerCase();
  let resourceType = null;

  if (ALLOWED_IMAGE_MIMES.includes(mime)) {
    resourceType = 'image';
    const maxBytes = (env.MAX_IMAGE_SIZE_MB || 10) * 1024 * 1024;
    if (file.size > maxBytes) {
      const error = new Error(
        `Image file exceeds the maximum allowed size of ${env.MAX_IMAGE_SIZE_MB}MB (received ${(file.size / (1024 * 1024)).toFixed(2)}MB).`
      );
      error.statusCode = 400;
      throw error;
    }
  } else if (ALLOWED_VIDEO_MIMES.includes(mime)) {
    resourceType = 'video';
    const maxBytes = (env.MAX_VIDEO_SIZE_MB || 50) * 1024 * 1024;
    if (file.size > maxBytes) {
      const error = new Error(
        `Video file exceeds the maximum allowed size of ${env.MAX_VIDEO_SIZE_MB}MB (received ${(file.size / (1024 * 1024)).toFixed(2)}MB).`
      );
      error.statusCode = 400;
      throw error;
    }
  } else {
    const error = new Error(
      `Unsupported media format '${mime}'. Supported images: JPG, PNG, WEBP. Supported videos: MP4, MOV, WEBM.`
    );
    error.statusCode = 400;
    throw error;
  }

  return { isValid: true, resourceType };
};

/**
 * Upload a media buffer stream to Cloudinary
 * @param {Object} file - Multer file object with buffer
 * @param {Object} options - Upload options (folder, complaintId, resourceType)
 * @returns {Promise<{ success: boolean, url: string, publicId: string, resourceType: string }>}
 */
const uploadMedia = async (file, options = {}) => {
  if (!isCloudinaryConfigured()) {
    const error = new Error(
      'Cloudinary media storage is not configured. Please set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET in backend/.env'
    );
    error.statusCode = 503;
    throw error;
  }

  const { resourceType } = validateMedia(file);

  // Resolve target folder hierarchy
  let targetFolder = 'smart-garbage/complaints';
  if (options.folder && FOLDER_MAP[options.folder]) {
    targetFolder = FOLDER_MAP[options.folder];
  } else if (resourceType === 'video') {
    targetFolder = 'smart-garbage/videos';
  }

  const uploadOptions = {
    folder: targetFolder,
    resource_type: resourceType,
    use_filename: false,
    unique_filename: true
  };

  if (options.complaintId) {
    const sanitizedId = options.complaintId.replace(/[^a-zA-Z0-9_-]/g, '');
    uploadOptions.public_id = `${sanitizedId}_${Date.now()}`;
  }

  return new Promise((resolve, reject) => {
    const uploadStream = cloudinary.uploader.upload_stream(uploadOptions, (error, result) => {
      if (error) {
        return reject(new Error(`Cloudinary upload failed: ${error.message}`));
      }
      resolve({
        success: true,
        url: result.secure_url,
        publicId: result.public_id,
        resourceType: result.resource_type || resourceType
      });
    });

    Readable.from(file.buffer).pipe(uploadStream);
  });
};

/**
 * Safely delete an asset from Cloudinary
 * @param {string} publicId - Cloudinary asset public ID
 * @param {string} resourceType - 'image' | 'video'
 * @returns {Promise<Object>}
 */
const deleteAsset = async (publicId, resourceType = 'image') => {
  if (!publicId || !isCloudinaryConfigured()) {
    return { result: 'skipped' };
  }

  try {
    return await cloudinary.uploader.destroy(publicId, {
      resource_type: resourceType
    });
  } catch (err) {
    // Non-blocking cleanup error log
    console.warn(`[Cloudinary] Asset deletion warning for '${publicId}':`, err.message);
    return { result: 'error', error: err.message };
  }
};

module.exports = {
  isCloudinaryConfigured,
  validateMedia,
  uploadMedia,
  deleteAsset,
  ALLOWED_IMAGE_MIMES,
  ALLOWED_VIDEO_MIMES,
  FOLDER_MAP
};
