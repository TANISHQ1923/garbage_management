const Complaint = require('../models/Complaint');
const { generateComplaintId } = require('../services/complaintService');
const notificationService = require('../services/notificationService');
const aiService = require('../services/aiService');

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

    const complaint = await Complaint.create({
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
      ]
    });

    // Create confirmation notification for citizen
    await notificationService.createNotification({
      userId: citizenId,
      type: 'COMPLAINT_SUBMITTED',
      title: 'Complaint Registered',
      message: `Your complaint ${complaintId} has been successfully submitted and forwarded to the municipal zone office.`,
      complaintId
    });

    return res.status(201).json({
      success: true,
      message: 'Complaint submitted successfully',
      data: complaint
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

    await createNotification({
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

module.exports = {
  createComplaint,
  getMyComplaints,
  getComplaintById,
  requestEmergency,
  analyzeComplaintWithAi
};

