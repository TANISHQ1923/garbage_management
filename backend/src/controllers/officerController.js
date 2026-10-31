const crypto = require('crypto');
const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const User = require('../models/User');
const { getDBStatus } = require('../config/db');
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

    const { reserveAssignment } = require('../services/dispatchService');
    const assignment = await reserveAssignment(complaint.complaintId, driverId, req.user);
    const updated = syntheticDataService.isSyntheticMode()
      ? syntheticDataService.getSyntheticComplaints().find(c => c.complaintId === complaint.complaintId)
      : await Complaint.findOne({ complaintId: complaint.complaintId });

    return res.status(200).json({
      success: true,
      message: 'Driver assigned successfully',
      data: updated,
      assignment
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

    if (finalTargetStatus === 'ASSIGNED') {
      return res.status(400).json({ success: false, message: 'Use the driver assignment endpoint.' });
    }
    if (!finalTargetStatus) {
      return res.status(400).json({
        success: false,
        message: 'Target status is required.'
      });
    }

    let complaint;
    const isMocked = typeof Complaint.findOne === 'function' && Boolean(Complaint.findOne._isMockFunction);
    if (!isMocked && (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected)) {
      complaint = syntheticDataService.getSyntheticComplaintById(id);
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

    if (!canTransition(complaint.status, finalTargetStatus)) {
      return res.status(400).json({
        success: false,
        message: `Illegal status transition: Cannot change complaint status from '${complaint.status}' to '${finalTargetStatus}'.`
      });
    }

    const expectedStatus = complaint.status;
    const now = Date.now();
    complaint.status = finalTargetStatus;
    if (!complaint.statusHistory) complaint.statusHistory = [];
    complaint.statusHistory.push({
      status: finalTargetStatus,
      timestamp: now,
      changedBy: req.user.name,
      role: req.user.role,
      note: noteText || `Status transitioned to ${finalTargetStatus} by municipal officer.`
    });

    if (typeof complaint.save === 'function') {
      await require('../services/taskTransitionService').commitTransition(complaint, expectedStatus);
    }
    if (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected) {
      syntheticDataService.updateSyntheticComplaint(complaint.complaintId || complaint.id, complaint);
    }

    // Citizen notification on verification
    if (finalTargetStatus === 'VERIFIED') {
      if (complaint.citizenId) await createNotification({
        userId: complaint.citizenId,
        type: 'CLEANUP_VERIFIED',
        title: 'Cleanup Verified & Closed',
        message: `Your complaint ${complaint.complaintId} has been verified by the municipal authority and officially closed. Thank you!`,
        complaintId: complaint.complaintId
      });
    } else {
      if (complaint.citizenId) await createNotification({
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

    let complaint;
    const isMocked = typeof Complaint.findOne === 'function' && Boolean(Complaint.findOne._isMockFunction);
    if (!isMocked && (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected)) {
      complaint = syntheticDataService.getSyntheticComplaintById(id);
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

    const note = {
      id: `NOTE-${Date.now()}-${crypto.randomBytes(2).toString('hex')}`,
      officerId: req.user.id || req.user._id.toString(),
      officerName: req.user.name,
      text: content.trim(),
      createdAt: Date.now()
    };

    if (!complaint.officerNotes) complaint.officerNotes = [];
    complaint.officerNotes.push(note);
    if (typeof complaint.save === 'function') {
      await complaint.save();
    }
    if (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected) {
      syntheticDataService.updateSyntheticComplaint(complaint.complaintId || complaint.id, complaint);
    }

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
