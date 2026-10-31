const { uploadMedia, validateMedia } = require('../services/mediaService');
const syntheticDataService = require('../services/syntheticDataService');
const { getDBStatus } = require('../config/db');

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

    const evidence = ['before-cleaning', 'after-cleaning'].includes(folder);
    if (evidence) {
      let complaint = null;
      if (syntheticDataService.isSyntheticMode()) {
        complaint = syntheticDataService.getSyntheticComplaintById(complaintId);
      }
      if (!complaint && getDBStatus().isConnected) {
        complaint = complaintId && await require('../models/Complaint').findOne({ complaintId });
      }
      const driverId = req.user.driverId || req.user.id || req.user._id?.toString();
      const effectiveDriverId = syntheticDataService.resolveDriverId ? syntheticDataService.resolveDriverId(driverId) : driverId;
      const effectiveAssigned = syntheticDataService.resolveDriverId ? syntheticDataService.resolveDriverId(complaint?.assignedDriverId) : complaint?.assignedDriverId;

      if (!complaint || req.user.role !== 'GARBAGE_DRIVER' || (complaint.assignedDriverId !== driverId && effectiveAssigned !== effectiveDriverId && complaint.assignedDriverId !== effectiveDriverId)) {
        return res.status(403).json({ success: false, message: 'Photo evidence may only be uploaded by the assigned driver for this task.' });
      }
      if (!['ASSIGNED', 'PICKUP_IN_PROGRESS'].includes(complaint.status)) {
        return res.status(409).json({ success: false, message: 'This task no longer accepts pickup evidence uploads.' });
      }
      if (!req.file.mimetype?.startsWith('image/')) return res.status(400).json({ success: false, message: 'Pickup evidence must be an image.' });
    }

    const result = await uploadMedia(req.file, { folder, complaintId });
    if (evidence && getDBStatus().isConnected) {
      await require('../models/StoredMedia').create({ publicId: result.publicId, url: result.url,
        resourceType: result.resourceType, complaintId, folder, uploadedBy: String(req.user.id || req.user._id) });
    }

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
