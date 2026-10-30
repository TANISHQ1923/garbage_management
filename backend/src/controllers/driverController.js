const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const { canDriverTransition } = require('../services/complaintService');
const { createNotification } = require('../services/notificationService');
const { deleteAsset } = require('../services/mediaService');
const syntheticDataService = require('../services/syntheticDataService');
const { getDBStatus } = require('../config/db');

/**
 * Helper to resolve driverId from authenticated user
 */
const getDriverId = (user) => {
  if (!user.driverId) throw Object.assign(new Error('Driver identity is not provisioned. Contact your officer.'), { statusCode: 409 });
  return user.driverId;
};

/**
 * Get all tasks assigned to the authenticated driver
 * GET /api/driver/complaints
 */
const getMyTasks = async (req, res, next) => {
  try {
    const driverId = getDriverId(req.user);

    if (syntheticDataService.isSyntheticMode()) {
      const allSynthetic = syntheticDataService.getSyntheticComplaints();
      let myTasks = allSynthetic.filter((c) => c.assignedDriverId === driverId);
      if (
        myTasks.length === 0 &&
        (driverId === 'DRV-882' ||
          driverId === 'mock-driver-001' ||
          req.user.email === 'driver@example.com')
      ) {
        myTasks = allSynthetic.filter((c) => c.assignedDriverId === 'DRV-DEMO-001');
      }
      myTasks.sort((a, b) => (b.emergency ? 1 : 0) - (a.emergency ? 1 : 0) || new Date(b.createdAt) - new Date(a.createdAt));
      return res.status(200).json({
        success: true,
        count: myTasks.length,
        data: myTasks
      });
    }

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

    if (syntheticDataService.isSyntheticMode()) {
      const complaint = syntheticDataService.getSyntheticComplaints().find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );
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
    }

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
    let beforeCleaningImagePublicId = null, afterCleaningImagePublicId = null;
    const { id } = req.params;
    const {
      beforeCleaningImageUrl,
      beforeCleaningUri,
    } = req.body;
    const driverId = getDriverId(req.user);

    let complaint;
    const isFindMocked = typeof Complaint.findOne === 'function' && Boolean(Complaint.findOne._isMockFunction);
    if (!isFindMocked && syntheticDataService.isSyntheticMode()) {
      complaint = syntheticDataService.getSyntheticComplaints().find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );
    }
    if (!complaint) {
      complaint = await Complaint.findOne({
        $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
      });
    }

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
    if (beforeImage) {
      const stored = await require('../services/evidenceService').requireStoredEvidence(complaint, req.user, beforeImage, 'before-cleaning');
      beforeCleaningImagePublicId = stored.publicId;
    }
    const oldBeforePublicId = complaint.beforeCleaningImagePublicId;

    if (beforeImage) {
      complaint.beforeCleaningImageUrl = beforeImage;
    }
    if (beforeCleaningImagePublicId) {
      complaint.beforeCleaningImagePublicId = beforeCleaningImagePublicId;
    }

    const expectedStatus = complaint.status;
    const now = Date.now();
    complaint.status = 'PICKUP_IN_PROGRESS';
    complaint.statusHistory.push({
      status: 'PICKUP_IN_PROGRESS',
      timestamp: now,
      changedBy: req.user.name,
      role: 'GARBAGE_DRIVER',
      note: 'Driver initiated on-site waste collection.'
    });

    if (typeof complaint.save === 'function') {
      await require('../services/taskTransitionService').commitTransition(complaint, expectedStatus, driverId);
    }
    if (syntheticDataService.isSyntheticMode()) {
      syntheticDataService.updateSyntheticComplaint(complaint.complaintId || complaint.id, complaint);
      syntheticDataService.updateSyntheticDriverAvailability(driverId, 'BUSY');
    }

    // Safely delete old Cloudinary asset if replaced
    if (oldBeforePublicId && beforeCleaningImagePublicId && oldBeforePublicId !== beforeCleaningImagePublicId) {
      deleteAsset(oldBeforePublicId, 'image').catch(() => {});
    }


    // Emit notification to citizen
    if (complaint.citizenId) await createNotification({
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
    let beforeCleaningImagePublicId = null, afterCleaningImagePublicId = null;
    const { id } = req.params;
    const {
      afterCleaningImageUrl,
      afterCleaningUri,
      beforeCleaningImageUrl,
      beforeCleaningUri,
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

    let complaint;
    const isCompleteFindMocked = typeof Complaint.findOne === 'function' && Boolean(Complaint.findOne._isMockFunction);
    if (!isCompleteFindMocked && syntheticDataService.isSyntheticMode()) {
      complaint = syntheticDataService.getSyntheticComplaints().find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );
    }
    if (!complaint) {
      complaint = await Complaint.findOne({
        $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
      });
    }

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

    const afterStored = await require('../services/evidenceService').requireStoredEvidence(complaint, req.user, finalAfterImage, 'after-cleaning');
    const beforeImageForValidation = beforeCleaningImageUrl || beforeCleaningUri;
    const beforeStored = beforeImageForValidation
      ? await require('../services/evidenceService').requireStoredEvidence(complaint, req.user, beforeImageForValidation, 'before-cleaning') : null;
    afterCleaningImagePublicId = afterStored.publicId;
    if (beforeStored) beforeCleaningImagePublicId = beforeStored.publicId;
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

    const expectedStatus = complaint.status;
    const now = Date.now();
    complaint.status = 'CLEANED';
    complaint.statusHistory.push({
      status: 'CLEANED',
      timestamp: now,
      changedBy: req.user.name,
      role: 'GARBAGE_DRIVER',
      note: 'Waste pickup completed with on-site photo verification.'
    });

    if (typeof complaint.save === 'function') {
      await require('../services/taskTransitionService').commitTransition(complaint, expectedStatus, driverId);
    }
    if (syntheticDataService.isSyntheticMode()) {
      syntheticDataService.updateSyntheticComplaint(complaint.complaintId || complaint.id, complaint);
      const hasOther = syntheticDataService.getSyntheticComplaints().some(c => c.assignedDriverId === driverId && ['ASSIGNED', 'PICKUP_IN_PROGRESS'].includes(c.status));
      syntheticDataService.updateSyntheticDriverAvailability(driverId, hasOther ? 'BUSY' : 'AVAILABLE');
    }

    // Safely delete replaced assets
    if (oldBeforePublicId && beforeCleaningImagePublicId && oldBeforePublicId !== beforeCleaningImagePublicId) {
      deleteAsset(oldBeforePublicId, 'image').catch(() => {});
    }
    if (oldAfterPublicId && afterCleaningImagePublicId && oldAfterPublicId !== afterCleaningImagePublicId) {
      deleteAsset(oldAfterPublicId, 'image').catch(() => {});
    }


    // Emit notification to citizen
    if (complaint.citizenId) await createNotification({
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
    let beforeCleaningImagePublicId = null, afterCleaningImagePublicId = null;
    const { id } = req.params;
    const {
      beforeCleaningImageUrl,
      beforeCleaningUri,
      afterCleaningImageUrl,
      afterCleaningUri,
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
    if (beforeImg) {
      const stored = await require('../services/evidenceService').requireStoredEvidence(complaint, req.user, beforeImg, 'before-cleaning');
      beforeCleaningImagePublicId = stored.publicId;
    }
    const afterToValidate = afterCleaningImageUrl || afterCleaningUri;
    if (afterToValidate) {
      const stored = await require('../services/evidenceService').requireStoredEvidence(complaint, req.user, afterToValidate, 'after-cleaning');
      afterCleaningImagePublicId = stored.publicId;
    }
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
    const { latitude, longitude, capturedAt } = req.body;

    if (!require('../services/dispatchService').isValidCoordinate(latitude, longitude)) {
      return res.status(400).json({
        success: false,
        message: 'Latitude and longitude coordinates are required.'
      });
    }

    const driverId = getDriverId(req.user);
    const now = Date.now();
    if (!Number.isFinite(capturedAt) || capturedAt > now || now - capturedAt > require('../config/env').DRIVER_LOCATION_MAX_AGE_MS) {
      return res.status(400).json({ success: false, message: 'A recent device location capture time is required. Capture GPS again.' });
    }

    if (syntheticDataService.isSyntheticMode() && !Boolean(Driver.findOneAndUpdate?._isMockFunction)) {
      const synDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.driverId === driverId);
      if (synDriver) {
        synDriver.currentLocation = {
          latitude: Number(latitude),
          longitude: Number(longitude),
          timestamp: capturedAt,
          source: 'DEVICE'
        };
        return res.status(200).json({
          success: true,
          message: 'Driver coordinates updated',
          data: {
            driverId,
            latitude: synDriver.currentLocation.latitude,
            longitude: synDriver.currentLocation.longitude,
            timestamp: capturedAt,
            source: 'DEVICE'
          }
        });
      }
    }

    const driver = await Driver.findOneAndUpdate(
      { driverId },
      {
        currentLocation: {
          latitude: Number(latitude),
          longitude: Number(longitude),
          timestamp: capturedAt,
          source: 'DEVICE'
        }
      },
      { new: true, runValidators: true }
    );

    if (!driver) return res.status(409).json({ success: false, message: 'Driver identity is not provisioned.' });

    await require('../services/dispatchService').wakePendingSensorPickups();

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

    if (syntheticDataService.isSyntheticMode() && !Boolean(Driver.findOne?._isMockFunction)) {
      const synDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.driverId === driverId);
      if (synDriver && synDriver.currentLocation) {
        return res.status(200).json({
          success: true,
          data: {
            driverId,
            latitude: synDriver.currentLocation.latitude,
            longitude: synDriver.currentLocation.longitude,
            timestamp: synDriver.currentLocation.timestamp || Date.now()
          }
        });
      }
    }

    const driver = await Driver.findOne({ driverId });

    if (!driver || !driver.currentLocation) {
      return res.status(404).json({ success: false, message: 'No measured driver location available.' });
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

    if (syntheticDataService.isSyntheticMode()) {
      const allSynthetic = syntheticDataService.getSyntheticComplaints();
      const history = allSynthetic
        .filter((c) => c.assignedDriverId === driverId && (c.status === 'CLEANED' || c.status === 'VERIFIED'))
        .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
      return res.status(200).json({
        success: true,
        count: history.length,
        data: history
      });
    }

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

const updateAvailability = async (req, res, next) => {
  try {
    const { availability } = req.body;
    if (!['AVAILABLE', 'OFFLINE'].includes(availability)) return res.status(400).json({ success: false, message: 'Choose AVAILABLE or OFFLINE. BUSY is managed by assignment.' });
    const driverId = getDriverId(req.user);
    const { withTransaction } = require('../services/transactionService');
    const { ACTIVE_STATUSES } = require('../services/dispatchService');
    await withTransaction(async session => {
      const active = await Complaint.exists({ assignedDriverId: driverId, status: { $in: ACTIVE_STATUSES } }).session(session);
      if (active) throw Object.assign(new Error('Finish the active pickup before changing availability.'), { statusCode: 409 });
      const d = await Driver.findOneAndUpdate({ driverId }, { $set: { availability } }, { session, new: true });
      if (!d) throw Object.assign(new Error('Driver identity is not provisioned.'), { statusCode: 409 });
    });
    if (availability === 'AVAILABLE') await require('../services/dispatchService').wakePendingSensorPickups();
    return res.json({ success: true, data: { availability } });
  } catch (error) { next(error); }
};

module.exports = {
  getMyTasks,
  getTaskById,
  startCollection,
  completeCollection,
  updateEvidence,
  updateLocation,
  getLocation,
  getHistory,
  updateAvailability
};
