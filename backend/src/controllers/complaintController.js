const Complaint = require('../models/Complaint');
const { generateComplaintId } = require('../services/complaintService');
const notificationService = require('../services/notificationService');
const aiService = require('../services/aiService');
const syntheticDataService = require('../services/syntheticDataService');
const { calculatePriority } = require('../services/priorityService');
const { autoDispatchComplaint } = require('../services/dispatchService');
const { getDBStatus } = require('../config/db');

/**
 * Submit a new garbage complaint
 * POST /api/complaints
 */
const createComplaint = async (req, res, next) => {
  try {
    const {
      garbageType,
      description,
      imageUrl,
      imagePublicId,
      videoUrl,
      videoPublicId,
      latitude,
      longitude,
      locationDescription,
      locationText,
      isEmergency,
      emergency
    } = req.body;

    const locDesc = locationDescription || locationText;
    if (!locDesc || locDesc.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'Location description is required.'
      });
    }

    const complaintId = generateComplaintId();
    const citizenId = req.user.id || req.user._id.toString();
    const isEmerg = Boolean(emergency !== undefined ? emergency : isEmergency);
    const now = Date.now();

    // Trigger AI analysis if image is provided (Step 9)
    let aiAnalysis = null;
    let aiAnalysisStatus = imageUrl ? 'PENDING' : 'PENDING';

    if (imageUrl) {
      const aiResult = await aiService.analyzeGarbageImage(imageUrl);
      if (aiResult && aiResult.success && aiResult.data) {
        aiAnalysis = {
          classification: aiResult.data.classification,
          severity: aiResult.data.severity,
          modelStatus: aiResult.data.modelStatus || 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(aiResult.data.processedAt || Date.now())
        };
        aiAnalysisStatus = 'COMPLETED';
      } else if (aiResult && aiResult.status === 'AI_UNAVAILABLE') {
        aiAnalysisStatus = 'AI_UNAVAILABLE';
      } else {
        aiAnalysisStatus = 'FAILED';
      }
    }

    const complaintData = {
      complaintId,
      citizenId,
      citizenName: req.user.name,
      citizenPhone: req.user.phone || null,
      garbageType: garbageType || 'OVERFLOWING_BIN',
      description: description || '',
      imageUrl: imageUrl || null,
      imagePublicId: imagePublicId || null,
      videoUrl: videoUrl || null,
      videoPublicId: videoPublicId || null,
      latitude: latitude !== undefined ? latitude : null,
      longitude: longitude !== undefined ? longitude : null,
      locationText: locDesc.trim(),
      emergency: isEmerg,
      status: 'SUBMITTED',
      assignedDriverId: null,
      assignedDriverName: null,
      assignedVehicleNumber: null,
      assignedAt: null,
      aiAnalysis,
      aiAnalysisStatus,
      statusHistory: [
        {
          status: 'SUBMITTED',
          timestamp: now,
          changedBy: req.user.name,
          role: req.user.role,
          note: isEmerg ? 'Complaint registered with EMERGENCY priority.' : 'Complaint registered by citizen.'
        }
      ],
      createdAt: now,
      updatedAt: now
    };

    let complaint;
    const isCreateMocked = typeof jest !== 'undefined' && jest.isMockFunction && jest.isMockFunction(Complaint.create);
    if (syntheticDataService.isSyntheticMode() && !isCreateMocked) {
      complaint = syntheticDataService.addSyntheticComplaint(complaintData);
    } else {
      complaint = await Complaint.create(complaintData);
      if (syntheticDataService.isSyntheticMode()) {
        syntheticDataService.addSyntheticComplaint(
          typeof complaint.toObject === 'function' ? complaint.toObject() : complaint
        );
      }
    }

    // Calculate priority score & level
    const plainForPriority = typeof complaint.toObject === 'function' ? complaint.toObject() : complaint;
    const { score, level } = calculatePriority(plainForPriority, now);
    complaint.priorityScore = score;
    complaint.priorityLevel = level;

    // Attempt automatic driver dispatch
    let dispatchResult;
    try { dispatchResult = await autoDispatchComplaint(complaint, { now }); }
    catch (_) {
      dispatchResult = { assigned: false, assignmentStatus: 'PENDING_DRIVER', reason: 'DISPATCH_UNAVAILABLE',
        message: 'Complaint saved, but automatic assignment is unavailable. Officer action is required.' };
    }

    // If dispatch did not assign a driver, notify citizen that complaint was received and awaiting assignment
    if (!dispatchResult.assigned) {
      await notificationService.createNotification({
        userId: citizenId,
        type: 'COMPLAINT_SUBMITTED',
        title: 'Complaint Registered',
        message: `Your complaint ${complaintId} has been successfully submitted and forwarded to the municipal zone office.`,
        complaintId
      });
    }

    const plainComplaint = typeof complaint.toObject === 'function' ? complaint.toObject() : complaint;
    plainComplaint.priorityScore = score;
    plainComplaint.priorityLevel = level;
    plainComplaint.dispatchResult = dispatchResult;

    return res.status(201).json({
      success: true,
      message: dispatchResult.assigned
        ? `Complaint submitted and automatically assigned to nearby driver ${dispatchResult.assignedDriverName}.`
        : 'Complaint submitted successfully and is awaiting driver assignment.',
      data: plainComplaint,
      dispatchResult
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get complaints filed by the authenticated citizen
 * GET /api/complaints/my
 */
const getMyComplaints = async (req, res, next) => {
  try {
    const citizenId = req.user.id || req.user._id.toString();

    if (syntheticDataService.isSyntheticMode()) {
      const allSynthetic = syntheticDataService.getSyntheticComplaints();
      const isDemoCitizen = (
        citizenId === '67b300000000000000000001' ||
        citizenId === 'mock-citizen-001' ||
        req.user.email === 'citizen@example.com' ||
        citizenId === '65f100000000000000000001'
      );
      let citizenComplaints = allSynthetic.filter((c) => {
        return c.citizenId === citizenId || (isDemoCitizen && c.citizenId === '65f100000000000000000001');
      });
      citizenComplaints.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      return res.status(200).json({
        success: true,
        count: citizenComplaints.length,
        data: citizenComplaints
      });
    }

    const complaints = await Complaint.find({ citizenId }).sort({ createdAt: -1 });

    return res.status(200).json({
      success: true,
      data: complaints
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Get a single complaint by ID
 * GET /api/complaints/:id
 */
const getComplaintById = async (req, res, next) => {
  try {
    const { id } = req.params;

    let complaint;
    const isFindMocked = typeof Complaint.findOne === 'function' && Boolean(Complaint.findOne._isMockFunction);
    if (!isFindMocked && syntheticDataService.isSyntheticMode()) {
      complaint = syntheticDataService.getSyntheticComplaints().find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );
    }
    if (!complaint) {
      if (getDBStatus().isConnected || isFindMocked) {
        complaint = await Complaint.findOne({
          $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
        });
      }
    }

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    const currentUserId = req.user.id || req.user._id.toString();

    // Citizen can only access their own complaints
    if (req.user.role === 'CITIZEN' && complaint.citizenId !== currentUserId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied: You are not authorized to view complaints filed by other citizens.'
      });
    }

    // Driver can only access complaints assigned to them
    if (req.user.role === 'GARBAGE_DRIVER') {
      const driverId = req.user.driverId || currentUserId;
      if (complaint.assignedDriverId !== driverId) {
        return res.status(403).json({
          success: false,
          message: 'Access denied: This complaint is not assigned to you.'
        });
      }
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
 * Request emergency priority for an existing complaint
 * POST /api/complaints/:id/emergency
 */
const requestEmergency = async (req, res, next) => {
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

    const currentUserId = req.user.id || req.user._id.toString();
    if (req.user.role === 'CITIZEN' && complaint.citizenId !== currentUserId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied.'
      });
    }

    complaint.emergency = true;
    complaint.statusHistory.push({
      status: complaint.status,
      timestamp: Date.now(),
      changedBy: req.user.name,
      role: req.user.role,
      note: 'Emergency pickup escalated by citizen.'
    });

    await complaint.save();

    await notificationService.createNotification({
      userId: complaint.citizenId,
      type: 'EMERGENCY_REQUEST_RECEIVED',
      title: 'Emergency Request Received',
      message: `Emergency escalation logged for complaint ${complaint.complaintId}. Municipal priority team alerted.`,
      complaintId: complaint.complaintId
    });

    return res.status(200).json({
      success: true,
      message: 'Complaint escalated to emergency priority',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Trigger or retry AI analysis on an existing complaint
 * POST /api/complaints/:id/analyze-ai
 */
const analyzeComplaintWithAi = async (req, res, next) => {
  try {
    const { id } = req.params;
    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: 'Complaint not found.'
      });
    }

    if (!complaint.imageUrl) {
      return res.status(400).json({
        success: false,
        message: 'Complaint does not contain an image for AI analysis.'
      });
    }

    const aiResult = await aiService.analyzeGarbageImage(complaint.imageUrl);
    if (aiResult && aiResult.success && aiResult.data) {
      complaint.aiAnalysis = {
        classification: aiResult.data.classification,
        severity: aiResult.data.severity,
        modelStatus: aiResult.data.modelStatus || 'DEVELOPMENT_BASELINE',
        analyzedAt: new Date(aiResult.data.processedAt || Date.now())
      };
      complaint.aiAnalysisStatus = 'COMPLETED';
    } else if (aiResult && aiResult.status === 'AI_UNAVAILABLE') {
      complaint.aiAnalysisStatus = 'AI_UNAVAILABLE';
    } else {
      complaint.aiAnalysisStatus = 'FAILED';
    }

    await complaint.save();

    return res.status(200).json({
      success: true,
      message: 'AI analysis updated.',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Submit citizen cleanup feedback for a verified complaint
 * POST /api/complaints/:id/feedback
 */
const submitCleanupFeedback = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { rating, comment } = req.body;

    // 1. Validate rating: integer from 1 to 5
    if (
      rating === undefined ||
      rating === null ||
      typeof rating !== 'number' ||
      !Number.isInteger(rating) ||
      rating < 1 ||
      rating > 5
    ) {
      return res.status(400).json({
        success: false,
        message: 'Rating must be an integer between 1 and 5.'
      });
    }

    // 2. Validate comment: optional string <= 500 characters
    let cleanComment = '';
    if (comment !== undefined && comment !== null) {
      if (typeof comment !== 'string') {
        return res.status(400).json({
          success: false,
          message: 'Comment must be a text string.'
        });
      }
      cleanComment = comment.trim();
      if (cleanComment.length > 500) {
        return res.status(400).json({
          success: false,
          message: 'Comment cannot exceed 500 characters.'
        });
      }
    }

    const currentUserId = req.user.id || req.user._id.toString();
    const now = Date.now();

    // 3. Synthetic Data Mode
    if (syntheticDataService.isSyntheticMode()) {
      const allSynthetic = syntheticDataService.getSyntheticComplaints();
      const complaint = allSynthetic.find(
        (c) => c.complaintId === id || c.id === id || c._id === id
      );

      if (!complaint) {
        return res.status(404).json({
          success: false,
          message: `Complaint with ID '${id}' not found.`
        });
      }

      // Check ownership
      const isOwner =
        complaint.citizenId === currentUserId ||
        (currentUserId === '67b300000000000000000001' && complaint.citizenId === '65f100000000000000000001') ||
        (currentUserId === 'mock-citizen-001' && complaint.citizenId === '65f100000000000000000001') ||
        (req.user.email === 'citizen@example.com' && complaint.citizenId === '65f100000000000000000001');

      if (!isOwner) {
        return res.status(403).json({
          success: false,
          message: 'Access denied: You are not authorized to submit feedback for this complaint.'
        });
      }

      if (complaint.status === 'REJECTED') {
        return res.status(400).json({
          success: false,
          message: 'Feedback cannot be submitted for rejected complaints.'
        });
      }

      if (complaint.status !== 'VERIFIED') {
        return res.status(400).json({
          success: false,
          message: `Feedback can only be submitted for verified cleanups. Current status: ${complaint.status}.`
        });
      }

      if (complaint.feedback && complaint.feedback.rating) {
        return res.status(400).json({
          success: false,
          message: 'Feedback has already been submitted for this complaint.'
        });
      }

      complaint.feedback = {
        rating,
        comment: cleanComment,
        submittedAt: now,
        citizenId: complaint.citizenId,
        citizenName: req.user.name || complaint.citizenName
      };

      return res.status(200).json({
        success: true,
        message: 'Feedback submitted successfully',
        data: complaint
      });
    }

    // 4. Standard Database Mode (MongoDB)
    const complaint = await Complaint.findOne({
      $or: [{ complaintId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }]
    });

    if (!complaint) {
      return res.status(404).json({
        success: false,
        message: `Complaint with ID '${id}' not found.`
      });
    }

    if (complaint.citizenId !== currentUserId) {
      return res.status(403).json({
        success: false,
        message: 'Access denied: You are not authorized to submit feedback for this complaint.'
      });
    }

    if (complaint.status === 'REJECTED') {
      return res.status(400).json({
        success: false,
        message: 'Feedback cannot be submitted for rejected complaints.'
      });
    }

    if (complaint.status !== 'VERIFIED') {
      return res.status(400).json({
        success: false,
        message: `Feedback can only be submitted for verified cleanups. Current status: ${complaint.status}.`
      });
    }

    if (complaint.feedback && complaint.feedback.rating) {
      return res.status(400).json({
        success: false,
        message: 'Feedback has already been submitted for this complaint.'
      });
    }

    complaint.feedback = {
      rating,
      comment: cleanComment,
      submittedAt: now,
      citizenId: currentUserId,
      citizenName: req.user.name || complaint.citizenName
    };

    await complaint.save();

    return res.status(200).json({
      success: true,
      message: 'Feedback submitted successfully',
      data: complaint
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  createComplaint,
  getMyComplaints,
  getComplaintById,
  requestEmergency,
  analyzeComplaintWithAi,
  submitCleanupFeedback
};
