const express = require('express');
const {
  getMyTasks,
  getTaskById,
  startCollection,
  completeCollection,
  updateEvidence,
  updateLocation,
  getLocation,
  getHistory
} = require('../controllers/driverController');
const { authenticate } = require('../middleware/authMiddleware');
const { requireRole } = require('../middleware/roleMiddleware');

const router = express.Router();

// Strict driver role protection
router.use(authenticate);
router.use(requireRole('GARBAGE_DRIVER'));

router.get('/complaints', getMyTasks);
router.get('/complaints/:id', getTaskById);
router.put('/complaints/:id/start', startCollection);
router.put('/complaints/:id/complete', completeCollection);
router.put('/complaints/:id/evidence', updateEvidence);
router.put('/location', updateLocation);
router.get('/location', getLocation);
router.get('/history', getHistory);

module.exports = router;
