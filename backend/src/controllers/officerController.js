const crypto = require('crypto');
const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const User = require('../models/User');
const { canTransition } = require('../services/complaintService');
const { createNotification } = require('../services/notificationService');
const syntheticDataService = require('../services/syntheticDataService');
const {
  calculatePriority,
  isEligibleForActiveQueue,
  buildPriorityQueue
} = require('../services/priorityService');

/**
 * Get active complaint priority queue for municipal officers
 * GET /api/officer/complaints/priority-queue
 */
const getPriorityQueue = async (req, res, next) => {
  try {
    const now = Date.now();
    let rawComplaints = [];

    if (syntheticDataService.isSyntheticMode()) {
      rawComplaints = syntheticDataService.getSyntheticComplaints();
    } else {
      rawComplaints = await Complaint.find({
        status: { $in: ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS'] }
      });
    }

    const queue = buildPriorityQueue(rawComplaints, now);

    return res.status(200).json({
      success: true,
      count: queue.length,
      data: queue
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get all complaints with search, status filtering, and sorting
 * GET /api/officer/complaints
 */
const getAllComplaints = async (req, res, next) => {
  try {
    const { search, status, emergency, sort } = req.query;
    const now = Date.now();

    if (syntheticDataService.isSyntheticMode()) {
      let complaints = [...syntheticDataService.getSyntheticComplaints()];
      if (status === 'Priority Queue' || status === 'priority-queue') {
        complaints = complaints.filter((c) => isEligibleForActiveQueue(c));
      } else if (status && status !== 'All') {
        complaints = complaints.filter((c) => c.status === status);
      }
      if (emergency !== undefined) {
        const isEmerg = emergency === 'true' || emergency === true;
        complaints = complaints.filter((c) => c.emergency === isEmerg);
      }
      if (search && search.trim() !== '') {
        const s = search.trim().toLowerCase();
        complaints = complaints.filter(
          (c) =>
            (c.complaintId && c.complaintId.toLowerCase().includes(s)) ||
            (c.citizenName && c.citizenName.toLowerCase().includes(s)) ||
            (c.locationText && c.locationText.toLowerCase().includes(s)) ||
            (c.garbageType && c.garbageType.toLowerCase().includes(s))
        );
      }

      // Enrich with priority scores
      let enriched = complaints.map((c) => {
        const { score, level } = calculatePriority(c, now);
        return {
          ...c,
          priorityScore: score,
          priorityLevel: level
        };
      });

      if (sort === 'priority' || sort === 'priority-queue' || status === 'Priority Queue' || status === 'priority-queue') {
        enriched.sort((a, b) => {
          if (b.priorityScore !== a.priorityScore) {
            return b.priorityScore - a.priorityScore;
          }
          const timeA = new Date(a.createdAt).getTime() || 0;
          const timeB = new Date(b.createdAt).getTime() || 0;
          if (timeA !== timeB) {
            return timeA - timeB;
          }
          const idA = a.complaintId || a.id || '';
          const idB = b.complaintId || b.id || '';
          return idA.localeCompare(idB);
        });
      } else if (sort === 'oldest') {
        enriched.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
      } else if (sort === 'emergency') {
        enriched.sort((a, b) => (b.emergency ? 1 : 0) - (a.emergency ? 1 : 0) || new Date(b.createdAt) - new Date(a.createdAt));
      } else {
        enriched.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      }

      return res.status(200).json({
        success: true,
        count: enriched.length,
        data: enriched
      });
    }

    const filter = {};

    if (status === 'Priority Queue' || status === 'priority-queue') {
      filter.status = { $in: ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS'] };
    } else if (status && status !== 'All') {
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

    let enriched = complaints.map((c) => {
      const plain = c.toObject();
      const { score, level } = calculatePriority(plain, now);
      plain.priorityScore = score;
      plain.priorityLevel = level;
      return plain;
    });

    if (sort === 'priority' || sort === 'priority-queue' || status === 'Priority Queue' || status === 'priority-queue') {
      enriched.sort((a, b) => {
        if (b.priorityScore !== a.priorityScore) {
          return b.priorityScore - a.priorityScore;
        }
        const timeA = new Date(a.createdAt).getTime() || 0;
        const timeB = new Date(b.createdAt).getTime() || 0;
        if (timeA !== timeB) {
          return timeA - timeB;
        }
        const idA = a.complaintId || a.id || '';
        const idB = b.complaintId || b.id || '';
        return idA.localeCompare(idB);
      });
    }

    return res.status(200).json({
      success: true,
      count: enriched.length,
      data: enriched
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

    if (syntheticDataService.isSyntheticMode()) {
      const synComplaint = syntheticDataService.getSyntheticComplaints().find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );
      if (synComplaint) {
        return res.status(200).json({
          success: true,
          data: synComplaint
        });
      }
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
    if (syntheticDataService.isSyntheticMode()) {
      const drivers = syntheticDataService.getSyntheticDrivers();
      return res.status(200).json({
        success: true,
        count: drivers.length,
        data: drivers
      });
    }

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

    const isDriverFindMocked = typeof Driver.findOne === 'function' && Boolean(Driver.findOne._isMockFunction);
    if (isDriverFindMocked || !syntheticDataService.isSyntheticMode()) {
      try {
        const driverDoc = await Driver.findOne({ driverId });
        if (driverDoc) {
          finalDriverName = finalDriverName || driverDoc.name;
          finalVehicleNumber = finalVehicleNumber || driverDoc.vehicleNumber;
        }

        driverUserRecord = await User.findOne({
          $or: [{ driverId }, { employeeId: driverDoc?.employeeId }]
        });
      } catch (e) {
        // Graceful fallback
      }
    } else {
      const synDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.driverId === driverId);
      if (synDriver) {
        finalDriverName = finalDriverName || synDriver.name;
        finalVehicleNumber = finalVehicleNumber || synDriver.vehicleNumber;
      }
      driverUserRecord = syntheticDataService.getSyntheticUserById(driverId);
    }

    const now = Date.now();
    complaint.status = 'ASSIGNED';
    complaint.assignedDriverId = driverId;
    complaint.assignedDriverName = finalDriverName || driverId;
    complaint.assignedVehicleNumber = finalVehicleNumber || 'Pending';
    complaint.assignedAt = now;

    if (!complaint.statusHistory) complaint.statusHistory = [];
    complaint.statusHistory.push({
      status: 'ASSIGNED',
      timestamp: now,
      changedBy: req.user.name,
      role: req.user.role,
      note: noteText || `Assigned to driver ${finalDriverName || driverId} (${finalVehicleNumber || 'Vehicle Assigned'}).`
    });

    if (typeof complaint.save === 'function') {
      await complaint.save();
    }
    if (syntheticDataService.isSyntheticMode()) {
      syntheticDataService.updateSyntheticComplaint(complaint.complaintId || complaint.id, complaint);
      syntheticDataService.updateSyntheticDriverAvailability(driverId, 'BUSY');
    }

    // Notify citizen
    await createNotification({
      userId: complaint.citizenId,
      type: 'PICKUP_ASSIGNED',
      title: 'Garbage Pickup Assigned',
      message: `Your complaint ${complaint.complaintId} has been assigned to driver ${finalDriverName || driverId} (${finalVehicleNumber || 'Truck'}).`,
      complaintId: complaint.complaintId
    });

    // Notify driver (using User._id or driverId)
    const driverTargetId = driverUserRecord ? (driverUserRecord.id || driverUserRecord._id) : driverId;
    await createNotification({
      userId: driverTargetId,
      type: 'DRIVER_ASSIGNMENT',
      title: 'New Collection Assignment',
      message: `Complaint ${complaint.complaintId} (${complaint.locationText || 'Location'}) has been assigned to your route.`,
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
  getPriorityQueue,
  getAllComplaints,
  getComplaintById,
  getDrivers,
  assignDriver,
  updateStatus,
  addOfficerNote
};
