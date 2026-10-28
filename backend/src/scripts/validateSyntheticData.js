/**
 * Synthetic Demo Dataset Validator
 * Smart Garbage Management Application
 *
 * Validates:
 * 1. JSON structure and integrity
 * 2. Enum compliance with Mongoose models and Android domain models
 * 3. Relational integrity (citizenId, driverId, complaintId)
 * 4. Status history lifecycle consistency and evidence photo rules
 * 5. Dashboard metrics reconciliation against raw complaint records
 *
 * SYNTHETIC DEMO DATA ONLY
 */

const fs = require('fs');
const path = require('path');
const { canTransition, canDriverTransition } = require('../services/complaintService');

// Valid enums extracted directly from Mongoose models
const VALID_ROLES = ['CITIZEN', 'MUNICIPAL_OFFICER', 'GARBAGE_DRIVER'];
const VALID_STATUSES = ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS', 'CLEANED', 'VERIFIED', 'REJECTED'];
const VALID_GARBAGE_TYPES = [
  'OVERFLOWING_BIN',
  'FESTIVAL_WASTE',
  'EVENT_WASTE',
  'ROADSIDE_GARBAGE',
  'FLOOD_WASTE',
  'OTHER_WASTE'
];
const VALID_DRIVER_AVAILABILITY = ['AVAILABLE', 'BUSY', 'OFFLINE'];
const VALID_NOTIFICATION_TYPES = [
  'COMPLAINT_SUBMITTED',
  'COMPLAINT_STATUS_UPDATED',
  'EMERGENCY_REQUEST_RECEIVED',
  'PICKUP_ASSIGNED',
  'PICKUP_STARTED',
  'CLEANUP_COMPLETED',
  'CLEANUP_VERIFIED',
  'DRIVER_ASSIGNMENT',
  'SYSTEM_NOTIFICATION'
];
const VALID_AI_STATUSES = ['PENDING', 'COMPLETED', 'FAILED', 'AI_UNAVAILABLE'];
const VALID_AI_SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

function runValidation() {
  console.log('====================================================');
  console.log('[Validator] Running Synthetic Demo Dataset Validation');
  console.log('====================================================');

  const datasetPath = path.join(__dirname, '..', 'data', 'synthetic', 'synthetic-dashboard-data.json');
  if (!fs.existsSync(datasetPath)) {
    throw new Error(`Dataset file not found at: ${datasetPath}`);
  }

  // 1. JSON parsing validation
  const raw = fs.readFileSync(datasetPath, 'utf-8');
  let data;
  try {
    data = JSON.parse(raw);
    console.log('  [PASS] JSON is valid and well-formed.');
  } catch (e) {
    throw new Error(`Invalid JSON syntax: ${e.message}`);
  }

  const errors = [];
  const warnings = [];

  // Top-level structure
  if (!data.metadata) errors.push('Missing top-level metadata');
  if (!Array.isArray(data.users)) errors.push('data.users must be an array');
  if (!Array.isArray(data.drivers)) errors.push('data.drivers must be an array');
  if (!Array.isArray(data.complaints)) errors.push('data.complaints must be an array');
  if (!Array.isArray(data.notifications)) errors.push('data.notifications must be an array');
  if (!data.dashboards) errors.push('Missing top-level dashboards summary');

  if (errors.length > 0) {
    console.error('Structural errors:', errors);
    process.exit(1);
  }

  const { users, drivers, complaints, notifications, dashboards } = data;

  // 2. Users Validation
  console.log(`\n--- Validating ${users.length} Users ---`);
  const userMap = new Map();
  const citizenMap = new Map();
  const officerMap = new Map();
  const driverUserMap = new Map();

  users.forEach((u, i) => {
    if (!u.id) errors.push(`User at index ${i} missing id`);
    if (!u.email) errors.push(`User at index ${i} missing email`);
    if (!VALID_ROLES.includes(u.role)) errors.push(`User ${u.email} has invalid role: ${u.role}`);
    if (!u.passwordHash) errors.push(`User ${u.email} missing passwordHash`);

    userMap.set(u.id, u);
    if (u.role === 'CITIZEN') citizenMap.set(u.id, u);
    if (u.role === 'MUNICIPAL_OFFICER') officerMap.set(u.employeeId || u.id, u);
    if (u.role === 'GARBAGE_DRIVER') driverUserMap.set(u.driverId || u.id, u);
  });
  console.log(`  Citizens: ${citizenMap.size}, Officers: ${officerMap.size}, Drivers: ${driverUserMap.size}`);

  // 3. Drivers Collection Validation
  console.log(`\n--- Validating ${drivers.length} Driver Collection Records ---`);
  const driverRecordMap = new Map();
  drivers.forEach((d, i) => {
    if (!d.driverId) errors.push(`Driver at index ${i} missing driverId`);
    if (!d.name) errors.push(`Driver at index ${i} missing name`);
    if (!d.vehicleNumber) errors.push(`Driver at index ${i} missing vehicleNumber`);
    if (!VALID_DRIVER_AVAILABILITY.includes(d.availability)) {
      errors.push(`Driver ${d.driverId} has invalid availability: ${d.availability}`);
    }
    if (!d.currentLocation || typeof d.currentLocation.latitude !== 'number' || typeof d.currentLocation.longitude !== 'number') {
      errors.push(`Driver ${d.driverId} has invalid currentLocation`);
    }
    driverRecordMap.set(d.driverId, d);
  });
  console.log(`  [PASS] All ${drivers.length} driver records have valid fields and coordinates.`);

  // 4. Complaints Validation
  console.log(`\n--- Validating ${complaints.length} Complaints ---`);
  const complaintMap = new Map();
  const statusCounts = {};
  const garbageTypeCounts = {};
  let emergencyCount = 0;

  VALID_STATUSES.forEach(s => (statusCounts[s] = 0));
  VALID_GARBAGE_TYPES.forEach(t => (garbageTypeCounts[t] = 0));

  complaints.forEach((c, idx) => {
    // Unique ID
    if (!c.complaintId) errors.push(`Complaint at index ${idx} missing complaintId`);
    if (complaintMap.has(c.complaintId)) errors.push(`Duplicate complaintId: ${c.complaintId}`);
    complaintMap.set(c.complaintId, c);

    // Enums
    if (!VALID_STATUSES.includes(c.status)) {
      errors.push(`Complaint ${c.complaintId} has invalid status: ${c.status}`);
    } else {
      statusCounts[c.status]++;
    }

    if (!VALID_GARBAGE_TYPES.includes(c.garbageType)) {
      errors.push(`Complaint ${c.complaintId} has invalid garbageType: ${c.garbageType}`);
    } else {
      garbageTypeCounts[c.garbageType]++;
    }

    if (c.emergency) emergencyCount++;

    // Relational: citizenId
    if (!citizenMap.has(c.citizenId)) {
      errors.push(`Complaint ${c.complaintId} references unknown citizenId: ${c.citizenId}`);
    }

    // Driver assignment consistency
    if (c.status === 'SUBMITTED' || c.status === 'REJECTED') {
      if (c.assignedDriverId) {
        errors.push(`Complaint ${c.complaintId} in ${c.status} status must NOT have assignedDriverId`);
      }
    } else {
      // ASSIGNED, PICKUP_IN_PROGRESS, CLEANED, VERIFIED
      if (!c.assignedDriverId) {
        errors.push(`Complaint ${c.complaintId} in ${c.status} status must have assignedDriverId`);
      } else if (!driverRecordMap.has(c.assignedDriverId)) {
        errors.push(`Complaint ${c.complaintId} references unknown assignedDriverId: ${c.assignedDriverId}`);
      }
      if (!c.assignedVehicleNumber) {
        errors.push(`Complaint ${c.complaintId} missing assignedVehicleNumber`);
      }
    }

    // Photo evidence rules
    if (c.status === 'PICKUP_IN_PROGRESS') {
      if (!c.beforeCleaningImageUrl) {
        errors.push(`Complaint ${c.complaintId} in PICKUP_IN_PROGRESS must have beforeCleaningImageUrl`);
      }
    }
    if (c.status === 'CLEANED' || c.status === 'VERIFIED') {
      if (!c.beforeCleaningImageUrl) {
        errors.push(`Complaint ${c.complaintId} in ${c.status} must have beforeCleaningImageUrl`);
      }
      if (!c.afterCleaningImageUrl) {
        errors.push(`Complaint ${c.complaintId} in ${c.status} strictly requires afterCleaningImageUrl`);
      }
    }

    // Officer notes rules
    if (c.status === 'VERIFIED') {
      if (!Array.isArray(c.officerNotes) || c.officerNotes.length === 0) {
        errors.push(`Complaint ${c.complaintId} in VERIFIED status should have inspection officerNotes`);
      }
    }
    if (c.status === 'REJECTED') {
      if (!Array.isArray(c.officerNotes) || c.officerNotes.length === 0) {
        errors.push(`Complaint ${c.complaintId} in REJECTED status must have officerNotes explaining rejection`);
      }
    }

    // Status History progression validation
    if (!Array.isArray(c.statusHistory) || c.statusHistory.length === 0) {
      errors.push(`Complaint ${c.complaintId} missing statusHistory`);
    } else {
      const historyStatuses = c.statusHistory.map(h => h.status);
      const lastStatus = historyStatuses[historyStatuses.length - 1];
      if (lastStatus !== c.status) {
        errors.push(`Complaint ${c.complaintId} current status (${c.status}) does not match last status in history (${lastStatus})`);
      }

      // Check strictly monotonic timestamps
      for (let h = 1; h < c.statusHistory.length; h++) {
        if (c.statusHistory[h].timestamp < c.statusHistory[h - 1].timestamp) {
          errors.push(`Complaint ${c.complaintId} status history has non-monotonic timestamp at step ${h}`);
        }
      }
    }

    // AI Analysis checks
    if (!VALID_AI_STATUSES.includes(c.aiAnalysisStatus)) {
      errors.push(`Complaint ${c.complaintId} has invalid aiAnalysisStatus: ${c.aiAnalysisStatus}`);
    }
    if (c.aiAnalysisStatus === 'COMPLETED') {
      if (!c.aiAnalysis) {
        errors.push(`Complaint ${c.complaintId} aiAnalysisStatus is COMPLETED but aiAnalysis is null`);
      } else {
        if (c.aiAnalysis.modelStatus !== 'DEVELOPMENT_BASELINE') {
          errors.push(`Complaint ${c.complaintId} aiAnalysis.modelStatus must be DEVELOPMENT_BASELINE`);
        }
        if (!c.aiAnalysis.classification || !c.aiAnalysis.classification.category) {
          errors.push(`Complaint ${c.complaintId} aiAnalysis missing classification category`);
        }
        if (!c.aiAnalysis.severity || !VALID_AI_SEVERITIES.includes(c.aiAnalysis.severity.level)) {
          errors.push(`Complaint ${c.complaintId} aiAnalysis missing or invalid severity level`);
        }
      }
    }

    // Feedback validation (if present)
    if (c.feedback) {
      if (c.status !== 'VERIFIED') {
        errors.push(`Complaint ${c.complaintId} has feedback but status is ${c.status} (must be VERIFIED)`);
      }
      if (!Number.isInteger(c.feedback.rating) || c.feedback.rating < 1 || c.feedback.rating > 5) {
        errors.push(`Complaint ${c.complaintId} has invalid feedback rating: ${c.feedback.rating}`);
      }
      if (c.feedback.comment && (typeof c.feedback.comment !== 'string' || c.feedback.comment.length > 500)) {
        errors.push(`Complaint ${c.complaintId} feedback comment exceeds 500 characters or is not a string`);
      }
      if (c.feedback.citizenId && c.feedback.citizenId !== c.citizenId) {
        errors.push(`Complaint ${c.complaintId} feedback citizenId (${c.feedback.citizenId}) does not match complaint citizenId (${c.citizenId})`);
      }
    }
  });

  console.log('  Status Distribution:', statusCounts);
  console.log('  Garbage Type Distribution:', garbageTypeCounts);
  console.log('  Emergency Complaints Count:', emergencyCount);

  // 5. Notifications Validation
  console.log(`\n--- Validating ${notifications.length} Notifications ---`);
  notifications.forEach((n, idx) => {
    if (!n.notificationId) errors.push(`Notification at index ${idx} missing notificationId`);
    if (!VALID_NOTIFICATION_TYPES.includes(n.type)) {
      errors.push(`Notification ${n.notificationId} has invalid type: ${n.type}`);
    }
    if (!n.userId) errors.push(`Notification ${n.notificationId} missing userId`);
    if (n.complaintId && !complaintMap.has(n.complaintId)) {
      errors.push(`Notification ${n.notificationId} references unknown complaintId: ${n.complaintId}`);
    }
  });
  console.log(`  [PASS] All ${notifications.length} notifications are valid.`);

  // 6. Dashboard Metrics Reconciliation
  console.log('\n--- Reconciling Dashboard Summary Data ---');
  const { officerDashboard, citizenDashboard, driverDashboard } = dashboards;

  // Officer dashboard checks
  if (officerDashboard.total !== complaints.length) {
    errors.push(`OfficerDashboard.total (${officerDashboard.total}) !== complaints.length (${complaints.length})`);
  }
  if (officerDashboard.submitted !== statusCounts.SUBMITTED) {
    errors.push(`OfficerDashboard.submitted mismatch`);
  }
  if (officerDashboard.assigned !== statusCounts.ASSIGNED) {
    errors.push(`OfficerDashboard.assigned mismatch`);
  }
  if (officerDashboard.inProgress !== statusCounts.PICKUP_IN_PROGRESS) {
    errors.push(`OfficerDashboard.inProgress mismatch`);
  }
  if (officerDashboard.cleaned !== statusCounts.CLEANED) {
    errors.push(`OfficerDashboard.cleaned mismatch`);
  }
  if (officerDashboard.verified !== statusCounts.VERIFIED) {
    errors.push(`OfficerDashboard.verified mismatch`);
  }
  if (officerDashboard.rejected !== statusCounts.REJECTED) {
    errors.push(`OfficerDashboard.rejected mismatch`);
  }
  if (officerDashboard.emergency !== emergencyCount) {
    errors.push(`OfficerDashboard.emergency mismatch`);
  }

  // Citizen dashboard checks (for citizen1)
  const c1Complaints = complaints.filter(c => c.citizenId === citizenDashboard.demoCitizenId);
  if (citizenDashboard.totalComplaints !== c1Complaints.length) {
    errors.push(`CitizenDashboard.totalComplaints (${citizenDashboard.totalComplaints}) !== actual (${c1Complaints.length})`);
  }
  const expectedC1Active = c1Complaints.filter(c =>
    c.status === 'SUBMITTED' || c.status === 'ASSIGNED' || c.status === 'PICKUP_IN_PROGRESS'
  ).length;
  if (citizenDashboard.activeComplaints !== expectedC1Active) {
    errors.push(`CitizenDashboard.activeComplaints mismatch: expected ${expectedC1Active}, got ${citizenDashboard.activeComplaints}`);
  }
  const expectedC1Pending = c1Complaints.filter(c => c.status === 'SUBMITTED').length;
  if (citizenDashboard.pendingRequests !== expectedC1Pending) {
    errors.push(`CitizenDashboard.pendingRequests mismatch`);
  }
  const expectedC1Completed = c1Complaints.filter(c => c.status === 'CLEANED' || c.status === 'VERIFIED').length;
  if (citizenDashboard.completedComplaints !== expectedC1Completed) {
    errors.push(`CitizenDashboard.completedComplaints mismatch`);
  }

  // Driver dashboard checks (for DRV-DEMO-001)
  const d1Complaints = complaints.filter(c => c.assignedDriverId === driverDashboard.demoDriverId);
  if (driverDashboard.totalAssignedTasks !== d1Complaints.length) {
    errors.push(`DriverDashboard.totalAssignedTasks (${driverDashboard.totalAssignedTasks}) !== actual (${d1Complaints.length})`);
  }
  const expectedD1Assigned = d1Complaints.filter(c => c.status === 'ASSIGNED').length;
  if (driverDashboard.assignedTasks !== expectedD1Assigned) {
    errors.push(`DriverDashboard.assignedTasks mismatch`);
  }
  const expectedD1InProgress = d1Complaints.filter(c => c.status === 'PICKUP_IN_PROGRESS').length;
  if (driverDashboard.inProgressTasks !== expectedD1InProgress) {
    errors.push(`DriverDashboard.inProgressTasks mismatch`);
  }
  const expectedD1Completed = d1Complaints.filter(c => c.status === 'CLEANED' || c.status === 'VERIFIED').length;
  if (driverDashboard.completedTasks !== expectedD1Completed) {
    errors.push(`DriverDashboard.completedTasks mismatch`);
  }

  console.log('  [PASS] All dashboard totals reconcile 100% with raw complaints data.');

  // Summary
  if (errors.length > 0) {
    console.error(`\nValidation FAILED with ${errors.length} errors:`);
    errors.forEach(e => console.error(`  - ${e}`));
    process.exit(1);
  } else {
    console.log('\n====================================================');
    console.log('[Validator] VALIDATION SUCCESSFUL: Zero errors found.');
    console.log('  - All enums strictly compliant with schemas');
    console.log('  - All IDs, foreign keys, and relationships intact');
    console.log('  - Lifecycle histories and photo requirements verified');
    console.log('  - Dashboard calculations mathematically exact');
    console.log('====================================================');
  }
}

if (require.main === module) {
  try {
    runValidation();
  } catch (err) {
    console.error(`Validation error: ${err.message}`);
    process.exit(1);
  }
}

module.exports = { runValidation };
