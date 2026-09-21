const express = require('express');
const {
  getAllComplaints,
  getComplaintById,
  getDrivers,
  assignDriver,
  updateStatus,
  addOfficerNote
} = require('../controllers/officerController');
const { authenticate } = require('../middleware/authMiddleware');
const { requireRole } = require('../middleware/roleMiddleware');

const router = express.Router();

// Strict officer role protection
router.use(authenticate);
router.use(requireRole('MUNICIPAL_OFFICER'));

router.get('/complaints', getAllComplaints);
router.get('/complaints/:id', getComplaintById);
router.get('/drivers', getDrivers);
router.put('/complaints/:id/assign', assignDriver);
router.put('/complaints/:id/status', updateStatus);
router.post('/complaints/:id/notes', addOfficerNote);

module.exports = router;
