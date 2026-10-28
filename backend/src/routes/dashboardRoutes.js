const express = require('express');
const {
  getCitizenDashboard,
  getOfficerDashboard,
  getDriverDashboard
} = require('../controllers/dashboardController');
const { authenticate } = require('../middleware/authMiddleware');
const { requireRole } = require('../middleware/roleMiddleware');

const router = express.Router();

// All dashboard endpoints require valid authentication
router.use(authenticate);

// 1. Citizen Dashboard: Restricted to CITIZEN role
router.get('/citizen', requireRole('CITIZEN'), getCitizenDashboard);

// 2. Municipal Officer Dashboard: Restricted to MUNICIPAL_OFFICER role
router.get('/officer', requireRole('MUNICIPAL_OFFICER'), getOfficerDashboard);

// 3. Garbage Driver Dashboard: Restricted to GARBAGE_DRIVER role
router.get('/driver', requireRole('GARBAGE_DRIVER'), getDriverDashboard);

module.exports = router;
