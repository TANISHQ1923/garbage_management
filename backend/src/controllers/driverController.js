const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const { canDriverTransition } = require('../services/complaintService');
const { createNotification } = require('../services/notificationService');
const { deleteAsset } = require('../services/mediaService');

/**
 * Helper to resolve driverId from authenticated user
 */
const getDriverId = (user) => {
  return user.driverId || user.id || user._id.toString();
};

/**
 * Get all tasks assigned to the authenticated driver
 * GET /api/driver/complaints
 */
const getMyTasks = async (req, res, next) => {
  try {
    const driverId = getDriverId(req.user);
    const complaints = await Complaint.find({ assignedDriverId: driverId }).sort({
      emergency: -1,
      createdAt: -1
    });

    return res.status(200).json({
      success: true,
      count: complaints.length,
      data: complaints
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get assigned task details
 * GET /api/driver/complaints/:id
 */
const getTaskById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const driverId = getDriverId(req.user);

    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    if (complaint.assignedDriverId !== driverId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied: This task is not assigned to your vehicle.'
      });
    }

    return res.status(200).json({
      success: true,
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Start collection for an assigned complaint
 * PUT /api/driver/complaints/:id/start
 */
const startCollection = async (req, res, next) => {
  try {
    const { id } = req.params;
    const {
      beforeCleaningImageUrl,
      beforeCleaningUri,
      beforeCleaningImagePublicId
    } = req.body;
    const driverId = getDriverId(req.user);

    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    if (complaint.assignedDriverId !== driverId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied: This task is not assigned to you.'
      });
    }

    if (!canDriverTransition(complaint.status, 'PICKUP_IN_PROGRESS')) {
      return res.status(400).json({
        success: false,
        message: `Cannot start collection: Complaint is currently in '${complaint.status}' status.`
      });
    }

    const beforeImage = beforeCleaningImageUrl || beforeCleaningUri;
    const oldBeforePublicId = complaint.beforeCleaningImagePublicId;

    if (beforeImage) {
      complaint.beforeCleaningImageUrl = beforeImage;
    }
    if (beforeCleaningImagePublicId) {
      complaint.beforeCleaningImagePublicId = beforeCleaningImagePublicId;
    }

    const now = Date.now();
    complaint.status = 'PICKUP_IN_PROGRESS';
    complaint.statusHistory.push({
      status: 'PICKUP_IN_PROGRESS',
      timestamp: now,
      changedBy: req.user.name,
      role: 'GARBAGE_DRIVER',
      note: 'Driver initiated on-site waste collection.'
    });

    await complaint.save();

    // Safely delete old Cloudinary asset if replaced
    if (oldBeforePublicId && beforeCleaningImagePublicId && oldBeforePublicId !== beforeCleaningImagePublicId) {
      deleteAsset(oldBeforePublicId, 'image').catch(() => {});
    }

    // Mark driver availability as BUSY
    await Driver.findOneAndUpdate({ driverId }, { availability: 'BUSY' });

    // Emit notification to citizen
    await createNotification({
      userId: complaint.citizenId,
      type: 'PICKUP_STARTED',
      title: 'Garbage Collection Started',
      message: `Collection has started for your complaint ${complaint.complaintId}. The team is on-site.`,
      complaintId: complaint.complaintId
    });

    return res.status(200).json({
      success: true,
      message: 'Collection marked in-progress',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Complete collection for an in-progress complaint
 * PUT /api/driver/complaints/:id/complete
 */
const completeCollection = async (req, res, next) => {
  try {
    const { id } = req.params;
    const {
      afterCleaningImageUrl,
      afterCleaningUri,
      afterCleaningImagePublicId,
      beforeCleaningImageUrl,
      beforeCleaningUri,
      beforeCleaningImagePublicId
    } = req.body;

    const finalAfterImage = afterCleaningImageUrl || afterCleaningUri;

    // MANDATORY after-cleaning photo requirement
    if (!finalAfterImage || finalAfterImage.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'After-cleaning photo evidence is strictly required before marking collection completed.'
      });
    }

    const driverId = getDriverId(req.user);

    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    if (complaint.assignedDriverId !== driverId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied: This task is not assigned to you.'
      });
    }

    if (!canDriverTransition(complaint.status, 'CLEANED')) {
      return res.status(400).json({
        success: false,
        message: `Cannot complete collection: Complaint is in '${complaint.status}' status (must be PICKUP_IN_PROGRESS).`
      });
    }

    const oldBeforePublicId = complaint.beforeCleaningImagePublicId;
    const oldAfterPublicId = complaint.afterCleaningImagePublicId;

    const finalBeforeImage = beforeCleaningImageUrl || beforeCleaningUri;
    if (finalBeforeImage) {
      complaint.beforeCleaningImageUrl = finalBeforeImage;
    }
    if (beforeCleaningImagePublicId) {
      complaint.beforeCleaningImagePublicId = beforeCleaningImagePublicId;
    }

    complaint.afterCleaningImageUrl = finalAfterImage;
    if (afterCleaningImagePublicId) {
      complaint.afterCleaningImagePublicId = afterCleaningImagePublicId;
    }

    const now = Date.now();
    complaint.status = 'CLEANED';
    complaint.statusHistory.push({
      status: 'CLEANED',
      timestamp: now,
      changedBy: req.user.name,
      role: 'GARBAGE_DRIVER',
      note: 'Waste pickup completed with on-site photo verification.'
    });

    await complaint.save();

    // Safely delete replaced assets
    if (oldBeforePublicId && beforeCleaningImagePublicId && oldBeforePublicId !== beforeCleaningImagePublicId) {
      deleteAsset(oldBeforePublicId, 'image').catch(() => {});
    }
    if (oldAfterPublicId && afterCleaningImagePublicId && oldAfterPublicId !== afterCleaningImagePublicId) {
      deleteAsset(oldAfterPublicId, 'image').catch(() => {});
    }

    // Reset driver availability to AVAILABLE
    await Driver.findOneAndUpdate({ driverId }, { availability: 'AVAILABLE' });

    // Emit notification to citizen
    await createNotification({
      userId: complaint.citizenId,
      type: 'CLEANUP_COMPLETED',
      title: 'Garbage Cleanup Completed',
      message: `The reported waste for complaint ${complaint.complaintId} has been successfully cleared. Awaiting final verification.`,
      complaintId: complaint.complaintId
    });

    return res.status(200).json({
      success: true,
      message: 'Collection marked completed',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Update collection evidence photos
 * PUT /api/driver/complaints/:id/evidence
 */
const updateEvidence = async (req, res, next) => {
  try {
    const { id } = req.params;
    const {
      beforeCleaningImageUrl,
      beforeCleaningUri,
      beforeCleaningImagePublicId,
      afterCleaningImageUrl,
      afterCleaningUri,
      afterCleaningImagePublicId
    } = req.body;

    const driverId = getDriverId(req.user);

    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    if (complaint.assignedDriverId !== driverId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied.'
      });
    }

    const beforeImg = beforeCleaningImageUrl || beforeCleaningUri;
    const afterImg = afterCleaningImageUrl || afterCleaningUri;

    const oldBeforePublicId = complaint.beforeCleaningImagePublicId;
    const oldAfterPublicId = complaint.afterCleaningImagePublicId;

    if (beforeImg) complaint.beforeCleaningImageUrl = beforeImg;
    if (beforeCleaningImagePublicId) complaint.beforeCleaningImagePublicId = beforeCleaningImagePublicId;

    if (afterImg) complaint.afterCleaningImageUrl = afterImg;
    if (afterCleaningImagePublicId) complaint.afterCleaningImagePublicId = afterCleaningImagePublicId;

    await complaint.save();

    // Safely delete replaced assets
    if (oldBeforePublicId && beforeCleaningImagePublicId && oldBeforePublicId !== beforeCleaningImagePublicId) {
      deleteAsset(oldBeforePublicId, 'image').catch(() => {});
    }
    if (oldAfterPublicId && afterCleaningImagePublicId && oldAfterPublicId !== afterCleaningImagePublicId) {
      deleteAsset(oldAfterPublicId, 'image').catch(() => {});
    }

    return res.status(200).json({
      success: true,
      message: 'Evidence photos updated',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Update driver's manual GPS location
 * PUT /api/driver/location
 */
const updateLocation = async (req, res, next) => {
  try {
    const { latitude, longitude } = req.body;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Latitude and longitude coordinates are required.'
      });
    }

    const driverId = getDriverId(req.user);
    const now = Date.now();

    const driver = await Driver.findOneAndUpdate(
      { driverId },
      {
        currentLocation: {
          latitude: Number(latitude),
          longitude: Number(longitude),
          timestamp: now
        }
      },
      { new: true, upsert: true }
    );

    return res.status(200).json({
      success: true,
      message: 'Driver coordinates updated',
      data: {
        driverId,
        latitude: driver.currentLocation.latitude,
        longitude: driver.currentLocation.longitude,
        timestamp: driver.currentLocation.timestamp
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get driver's current location
 * GET /api/driver/location
 */
const getLocation = async (req, res, next) => {
  try {
    const driverId = req.query.driverId || getDriverId(req.user);
    const driver = await Driver.findOne({ driverId });

    if (!driver || !driver.currentLocation) {
      return res.status(200).json({
        success: true,
        data: {
          driverId,
          latitude: 28.6139,
          longitude: 77.2090,
          timestamp: Date.now()
        }
      });
    }

    return res.status(200).json({
      success: true,
      data: {
        driverId,
        latitude: driver.currentLocation.latitude,
        longitude: driver.currentLocation.longitude,
        timestamp: driver.currentLocation.timestamp
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get completed collection history for driver
 * GET /api/driver/history
 */
const getHistory = async (req, res, next) => {
  try {
    const driverId = getDriverId(req.user);
    const history = await Complaint.find({
      assignedDriverId: driverId,
      status: { $in: ['CLEANED', 'VERIFIED'] }
    }).sort({ updatedAt: -1 });

    return res.status(200).json({
      success: true,
      count: history.length,
      data: history
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getMyTasks,
  getTaskById,
  startCollection,
  completeCollection,
  updateEvidence,
  updateLocation,
  getLocation,
  getHistory
};
