const { uploadMedia, validateMedia } = require('../services/mediaService');

/**
 * Upload image or video to Cloudinary
 * POST /api/media/upload
 */
const handleUpload = async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file was uploaded. Please include a file in form-data under key \'file\'.'
      });
    }

    const folder = req.body.folder || 'complaints';
    const complaintId = req.body.complaintId || null;

    const result = await uploadMedia(req.file, { folder, complaintId });

    return res.status(200).json({
      success: true,
      message: 'Media uploaded successfully',
      data: {
        url: result.url,
        publicId: result.publicId,
        resourceType: result.resourceType
      }
    });
  } catch (error) {
    if (error.statusCode) {
      return res.status(error.statusCode).json({
        success: false,
        message: error.message
      });
    }
    next(error);
  }
};

module.exports = {
  handleUpload
};
