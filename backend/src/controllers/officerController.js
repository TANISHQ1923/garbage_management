const crypto = require('crypto');
const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const User = require('../models/User');
const { canTransition } = require('../services/complaintService');
const { createNotification } = require('../services/notificationService');

/**
 * Get all complaints with search, status filtering, and sorting
 * GET /api/officer/complaints
 */
const getAllComplaints = async (req, res, next) => {
  try {
    const { search, status, emergency, sort } = req.query;

    const filter = {};

    if (status && status !== 'All') {
      filter.status = status;
    }

    if (emergency !== undefined) {
      filter.emergency = emergency === 'true' || emergency === true;
    }

    if (search && search.trim() !== '') {
      const searchRegex = new RegExp(search.trim(), 'i');
      filter.$or = [
        { complaintId: searchRegex },
        { citizenName: searchRegex },
        { locationText: searchRegex },
        { garbageType: searchRegex }
      ];
    }

    let sortCriteria = { createdAt: -1 };
    if (sort === 'oldest') {
      sortCriteria = { createdAt: 1 };
    } else if (sort === 'emergency') {
      sortCriteria = { emergency: -1, createdAt: -1 };
    }

    const complaints = await Complaint.find(filter).sort(sortCriteria);

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
 * Get complaint details for officer
 * GET /api/officer/complaints/:id
 */
const getComplaintById = async (req, res, next) => {
  try {
    const { id } = req.params;
    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
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
 * Get all available drivers for assignment
 * GET /api/officer/drivers
 */
const getDrivers = async (req, res, next) => {
  try {
    const drivers = await Driver.find().sort({ availability: 1, name: 1 });
    return res.status(200).json({
      success: true,
      count: drivers.length,
      data: drivers
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Assign a driver to a complaint
 * PUT /api/officer/complaints/:id/assign
 */
const assignDriver = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { driverId, driverName, vehicleNumber, noteText } = req.body;

    if (!driverId) {
      return res.status(400).json({
        success: false,
        message: 'Driver ID is required for assignment.'
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

    if (complaint.status === 'VERIFIED') {
      return res.status(400).json({
        success: false,
        message: 'Cannot assign driver: Complaint is already verified and closed.'
      });
    }

    if (complaint.status === 'REJECTED') {
      return res.status(400).json({
        success: false,
        message: 'Cannot assign driver: Complaint was rejected.'
      });
    }

    // Lookup driver information if name or vehicle number was omitted
    let finalDriverName = driverName;
    let finalVehicleNumber = vehicleNumber;
    let driverUserRecord = null;

    const driverDoc = await Driver.findOne({ driverId });
    if (driverDoc) {
      finalDriverName = finalDriverName || driverDoc.name;
      finalVehicleNumber = finalVehicleNumber || driverDoc.vehicleNumber;
    }

    // Find driver user account for notification delivery
    driverUserRecord = await User.findOne({
      $or: [{ driverId }, { employeeId: driverDoc?.employeeId }]
    });

    const now = Date.now();
    complaint.status = 'ASSIGNED';
    complaint.assignedDriverId = driverId;
    complaint.assignedDriverName = finalDriverName || driverId;
    complaint.assignedVehicleNumber = finalVehicleNumber || 'Pending';
    complaint.assignedAt = now;

    complaint.statusHistory.push({
      status: 'ASSIGNED',
      timestamp: now,
      changedBy: req.user.name,
      role: req.user.role,
      note: noteText || `Assigned to driver ${finalDriverName || driverId} (${finalVehicleNumber || 'Vehicle Assigned'}).`
    });

    await complaint.save();

    // Notify citizen
    await createNotification({
      userId: complaint.citizenId,
      type: 'PICKUP_ASSIGNED',
      title: 'Garbage Pickup Assigned',
      message: `Your complaint ${complaint.complaintId} has been assigned to driver ${finalDriverName || driverId} (${finalVehicleNumber || 'Truck'}).`,
      complaintId: complaint.complaintId
    });

    // Notify driver (using User._id or driverId)
    const driverTargetId = driverUserRecord ? driverUserRecord.id : driverId;
    await createNotification({
      userId: driverTargetId,
      type: 'DRIVER_ASSIGNMENT',
      title: 'New Collection Assignment',
      message: `Complaint ${complaint.complaintId} (${complaint.locationText}) has been assigned to your route.`,
      complaintId: complaint.complaintId
    });

    return res.status(200).json({
      success: true,
      message: 'Driver assigned successfully',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Update complaint status by officer
 * PUT /api/officer/complaints/:id/status
 */
const updateStatus = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status: targetStatus, newStatus, noteText } = req.body;
    const finalTargetStatus = targetStatus || newStatus;

    if (!finalTargetStatus) {
      return res.status(400).json({
        success: false,
        message: 'Target status is required.'
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

    if (!canTransition(complaint.status, finalTargetStatus)) {
      return res.status(400).json({
        success: false,
        message: `Illegal status transition: Cannot change complaint status from '${complaint.status}' to '${finalTargetStatus}'.`
      });
    }

    const now = Date.now();
    complaint.status = finalTargetStatus;
    complaint.statusHistory.push({
      status: finalTargetStatus,
      timestamp: now,
      changedBy: req.user.name,
      role: req.user.role,
      note: noteText || `Status transitioned to ${finalTargetStatus} by municipal officer.`
    });

    await complaint.save();

    // Citizen notification on verification
    if (finalTargetStatus === 'VERIFIED') {
      await createNotification({
        userId: complaint.citizenId,
        type: 'CLEANUP_VERIFIED',
        title: 'Cleanup Verified & Closed',
        message: `Your complaint ${complaint.complaintId} has been verified by the municipal authority and officially closed. Thank you!`,
        complaintId: complaint.complaintId
      });
    } else {
      await createNotification({
        userId: complaint.citizenId,
        type: 'COMPLAINT_STATUS_UPDATED',
        title: 'Complaint Status Updated',
        message: `Complaint ${complaint.complaintId} status updated to ${finalTargetStatus}.`,
        complaintId: complaint.complaintId
      });
    }

    return res.status(200).json({
      success: true,
      message: `Complaint status updated to ${finalTargetStatus}`,
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Add internal officer note
 * POST /api/officer/complaints/:id/notes
 */
const addOfficerNote = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { text, noteText } = req.body;
    const content = text || noteText;

    if (!content || content.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'Note text cannot be empty.'
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

    const note = {
      id: `NOTE-${Date.now()}-${crypto.randomBytes(2).toString('hex')}`,
      officerId: req.user.id || req.user._id.toString(),
      officerName: req.user.name,
      text: content.trim(),
      createdAt: Date.now()
    };

    complaint.officerNotes.push(note);
    await complaint.save();

    return res.status(200).json({
      success: true,
      message: 'Officer note appended successfully',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getAllComplaints,
  getComplaintById,
  getDrivers,
  assignDriver,
  updateStatus,
  addOfficerNote
};
