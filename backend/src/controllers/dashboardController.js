/**
 * Dashboard Controller
 * Smart Garbage Management Application
 *
 * Serves aggregated dashboard endpoints for:
 * 1. Citizen Dashboard (GET /api/dashboard/citizen)
 * 2. Municipal Officer Dashboard (GET /api/dashboard/officer)
 * 3. Garbage Driver Dashboard (GET /api/dashboard/driver)
 *
 * Supports dual-mode operation:
 * - Synthetic Demo Mode (USE_SYNTHETIC_DATA=true): Served from synthetic dataset.
 * - Standard Database Mode (USE_SYNTHETIC_DATA=false): Served from MongoDB collections.
 */

const Complaint = require('../models/Complaint');
const Driver = require('../models/Driver');
const Notification = require('../models/Notification');
const { getDBStatus } = require('../config/db');
const syntheticDataService = require('../services/syntheticDataService');

/**
 * Get Citizen Dashboard Data
 * GET /api/dashboard/citizen
 * Requires Role: CITIZEN
 */
const getCitizenDashboard = async (req, res, next) => {
  try {
    const citizenId = req.user.id || req.user._id.toString();

    // 1. Synthetic Data Mode
    if (syntheticDataService.isSyntheticMode()) {
      const syntheticPayload = syntheticDataService.getCitizenDashboardData(citizenId);
      return res.status(200).json({
        success: true,
        message: 'Citizen dashboard data fetched successfully (Synthetic Demo Mode)',
        data: syntheticPayload
      });
    }

    // 2. Standard Database Mode (MongoDB)
    const dbStatus = getDBStatus();
    if (!dbStatus.isConnected) {
      return res.status(503).json({ success: false, message: 'Database unavailable. Please retry later.' });
    }

    const complaints = await Complaint.find({ citizenId }).sort({ createdAt: -1 });

    const activeComplaints = complaints.filter(
      (c) =>
        c.status === 'SUBMITTED' ||
        c.status === 'ASSIGNED' ||
        c.status === 'PICKUP_IN_PROGRESS'
    );
    const pendingRequests = complaints.filter((c) => c.status === 'SUBMITTED');
    const emergencyRequests = complaints.filter(
      (c) => c.emergency && c.status !== 'CLEANED' && c.status !== 'VERIFIED'
    );
    const completedComplaints = complaints.filter(
      (c) => c.status === 'CLEANED' || c.status === 'VERIFIED'
    );
    const rejectedComplaints = complaints.filter((c) => c.status === 'REJECTED');

    const recentComplaints = complaints.slice(0, 5);

    const notifications = await Notification.find({ userId: citizenId }).sort({ createdAt: -1 });
    const unreadCount = notifications.filter((n) => !n.isRead).length;

    return res.status(200).json({
      success: true,
      data: {
        citizenId,
        citizenName: req.user.name,
        metrics: {
          totalComplaints: complaints.length,
          activeComplaints: activeComplaints.length,
          pendingRequests: pendingRequests.length,
          emergencyRequests: emergencyRequests.length,
          completedComplaints: completedComplaints.length,
          rejectedComplaints: rejectedComplaints.length
        },
        recentComplaints,
        complaints,
        notifications,
        unreadNotificationsCount: unreadCount,
        mode: 'MONGODB'
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get Municipal Officer Dashboard Data
 * GET /api/dashboard/officer
 * Requires Role: MUNICIPAL_OFFICER
 */
const getOfficerDashboard = async (req, res, next) => {
  try {
    // 1. Synthetic Data Mode
    if (syntheticDataService.isSyntheticMode()) {
      const syntheticPayload = syntheticDataService.getOfficerDashboardData();
      return res.status(200).json({
        success: true,
        message: 'Officer dashboard data fetched successfully (Synthetic Demo Mode)',
        data: syntheticPayload
      });
    }

    // 2. Standard Database Mode (MongoDB)
    const dbStatus = getDBStatus();
    if (!dbStatus.isConnected) {
      return res.status(503).json({ success: false, message: 'Database unavailable. Please retry later.' });
    }

    const complaints = await Complaint.find().sort({ createdAt: -1 });
    const drivers = await Driver.find().sort({ availability: 1, name: 1 });

    const submitted = complaints.filter((c) => c.status === 'SUBMITTED');
    const assigned = complaints.filter((c) => c.status === 'ASSIGNED');
    const inProgress = complaints.filter((c) => c.status === 'PICKUP_IN_PROGRESS');
    const cleaned = complaints.filter((c) => c.status === 'CLEANED');
    const verified = complaints.filter((c) => c.status === 'VERIFIED');
    const rejected = complaints.filter((c) => c.status === 'REJECTED');
    const emergency = complaints.filter((c) => c.emergency);

    const priorityComplaints = complaints
      .filter((c) => c.status !== 'CLEANED' && c.status !== 'VERIFIED' && c.status !== 'REJECTED')
      .sort((a, b) => (b.emergency ? 1 : 0) - (a.emergency ? 1 : 0) || new Date(a.createdAt) - new Date(b.createdAt))
      .slice(0, 8);

    const pendingAssignments = submitted.slice(0, 8);
    const recentComplaints = complaints.slice(0, 10);

    const driverAvailability = {
      total: drivers.length,
      available: drivers.filter((d) => d.availability === 'AVAILABLE').length,
      busy: drivers.filter((d) => d.availability === 'BUSY').length,
      offline: drivers.filter((d) => d.availability === 'OFFLINE').length,
      drivers
    };

    const aiMetrics = {
      totalAnalyzed: complaints.filter((c) => c.aiAnalysisStatus === 'COMPLETED').length,
      pending: complaints.filter((c) => c.aiAnalysisStatus === 'PENDING').length,
      failed: complaints.filter((c) => c.aiAnalysisStatus === 'FAILED').length,
      unavailable: complaints.filter((c) => c.aiAnalysisStatus === 'AI_UNAVAILABLE').length
    };

    return res.status(200).json({
      success: true,
      data: {
        officer: {
          id: req.user.id || req.user._id.toString(),
          name: req.user.name,
          employeeId: req.user.employeeId,
          ward: req.user.ward
        },
        metrics: {
          total: complaints.length,
          submitted: submitted.length,
          assigned: assigned.length,
          inProgress: inProgress.length,
          cleaned: cleaned.length,
          verified: verified.length,
          rejected: rejected.length,
          emergency: emergency.length,
          activeComplaints: submitted.length + assigned.length + inProgress.length
        },
        driverAvailability,
        priorityComplaints,
        pendingAssignments,
        recentComplaints,
        aiMetrics,
        complaints,
        mode: 'MONGODB'
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get Garbage Driver Dashboard Data
 * GET /api/dashboard/driver
 * Requires Role: GARBAGE_DRIVER
 */
const getDriverDashboard = async (req, res, next) => {
  try {
    const driverId = req.user.driverId || req.user.id || req.user._id.toString();

    // 1. Synthetic Data Mode
    if (syntheticDataService.isSyntheticMode()) {
      const syntheticPayload = syntheticDataService.getDriverDashboardData(driverId);
      return res.status(200).json({
        success: true,
        message: 'Driver dashboard data fetched successfully (Synthetic Demo Mode)',
        data: syntheticPayload
      });
    }

    // 2. Standard Database Mode (MongoDB)
    const dbStatus = getDBStatus();
    if (!dbStatus.isConnected) {
      return res.status(503).json({ success: false, message: 'Database unavailable. Please retry later.' });
    }

    const tasks = await Complaint.find({ assignedDriverId: driverId }).sort({ emergency: -1, createdAt: -1 });
    const driverDoc = await Driver.findOne({ driverId });

    const assignedTasks = tasks.filter((c) => c.status === 'ASSIGNED');
    const inProgressTasks = tasks.filter((c) => c.status === 'PICKUP_IN_PROGRESS');
    const completedTasks = tasks.filter((c) => c.status === 'CLEANED' || c.status === 'VERIFIED');
    const emergencyTasks = tasks.filter(
      (c) => c.emergency && c.status !== 'CLEANED' && c.status !== 'VERIFIED'
    );

    const activeTask = inProgressTasks[0] || null;
    const collectionHistory = [...completedTasks].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

    const notifications = await Notification.find({ userId: driverId }).sort({ createdAt: -1 });
    const unreadCount = notifications.filter((n) => !n.isRead).length;

    return res.status(200).json({
      success: true,
      data: {
        driverId,
        driverInfo: driverDoc || {
          driverId,
          name: req.user.name,
          vehicleNumber: req.user.vehicleNumber || 'DL-01-GB-XXXX',
          vehicleType: req.user.vehicleType || 'Heavy Compactor Truck',
          availability: req.user.availability || 'AVAILABLE'
        },
        metrics: {
          totalAssignedTasks: tasks.length,
          assignedTasks: assignedTasks.length,
          inProgressTasks: inProgressTasks.length,
          completedTasks: completedTasks.length,
          emergencyTasks: emergencyTasks.length
        },
        activeTask,
        assignedTasks,
        collectionHistory,
        notifications,
        unreadNotificationsCount: unreadCount,
        mode: 'MONGODB'
      }
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getCitizenDashboard,
  getOfficerDashboard,
  getDriverDashboard
};
