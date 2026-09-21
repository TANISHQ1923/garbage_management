const express = require('express');
const {
  createComplaint,
  getMyComplaints,
  getComplaintById,
  requestEmergency,
  analyzeComplaintWithAi
} = require('../controllers/complaintController');
const { authenticate } = require('../middleware/authMiddleware');
const { requireRole } = require('../middleware/roleMiddleware');

const router = express.Router();

// All complaint routes require valid authentication
router.use(authenticate);

// Citizen filing and history
router.post('/', requireRole('CITIZEN'), createComplaint);
router.get('/my', requireRole('CITIZEN'), getMyComplaints);
router.get('/:id', getComplaintById);
router.post('/:id/emergency', requireRole('CITIZEN'), requestEmergency);
router.post('/:id/analyze-ai', analyzeComplaintWithAi);

module.exports = router;
