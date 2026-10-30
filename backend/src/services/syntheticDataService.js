/**
 * Synthetic Data Service
 * Smart Garbage Management Application
 *
 * Provides in-memory access and dynamic dashboard aggregations from
 * synthetic-dashboard-data.json when USE_SYNTHETIC_DATA=true.
 *
 * SYNTHETIC DEMO DATA ONLY
 */

const fs = require('fs');
const path = require('path');
const env = require('../config/env');
const { getDBStatus } = require('../config/db');
const { DEMO_USERS } = require('../data/demoUsers');
const { buildPriorityQueue } = require('./priorityService');

let cachedDataset = null;
let lastLoadedTime = 0;

/**
 * Returns true if synthetic demo data mode is enabled.
 */
const isSyntheticMode = () => {
  return (
    env.USE_SYNTHETIC_DATA === true ||
    process.env.USE_SYNTHETIC_DATA === 'true'
  );
};

/**
 * Loads the synthetic dataset from disk (cached in-memory).
 */
const loadDataset = (forceReload = false) => {
  if (cachedDataset && !forceReload) {
    return cachedDataset;
  }

  const datasetPath = path.join(__dirname, '..', 'data', 'synthetic', 'synthetic-dashboard-data.json');
  const runtimePath = path.join(__dirname, '..', 'data', 'synthetic', 'runtime-data.json');
  if (!fs.existsSync(datasetPath)) {
    console.warn(`[SyntheticDataService] Synthetic dataset not found at ${datasetPath}. Using fallback empty dataset.`);
    return {
      metadata: { title: 'EMPTY_DATASET' },
      users: [],
      drivers: [],
      complaints: [],
      notifications: [],
      dashboards: {}
    };
  }

  try {
    const raw = fs.readFileSync(datasetPath, 'utf-8');
    cachedDataset = JSON.parse(raw);
    lastLoadedTime = Date.now();

    // Merge persistent runtime data (user-created complaints, updates, notifications)
    if (process.env.NODE_ENV !== 'test' && fs.existsSync(runtimePath)) {
      try {
        const runtimeRaw = fs.readFileSync(runtimePath, 'utf-8');
        const runtimeData = JSON.parse(runtimeRaw);
        if (Array.isArray(runtimeData.complaints)) {
          for (const rc of runtimeData.complaints) {
            const idx = (cachedDataset.complaints || []).findIndex(
              (c) => c.complaintId === rc.complaintId || c.id === rc.complaintId
            );
            if (idx !== -1) {
              cachedDataset.complaints[idx] = { ...cachedDataset.complaints[idx], ...rc };
            } else {
              if (!cachedDataset.complaints) cachedDataset.complaints = [];
              cachedDataset.complaints.unshift(rc);
            }
          }
        }
        if (Array.isArray(runtimeData.notifications)) {
          for (const rn of runtimeData.notifications) {
            const idx = (cachedDataset.notifications || []).findIndex(
              (n) => n.id === rn.id || (n.title === rn.title && n.complaintId === rn.complaintId)
            );
            if (idx !== -1) {
              cachedDataset.notifications[idx] = { ...cachedDataset.notifications[idx], ...rn };
            } else {
              if (!cachedDataset.notifications) cachedDataset.notifications = [];
              cachedDataset.notifications.unshift(rn);
            }
          }
        }
        if (runtimeData.driverAvailability && typeof runtimeData.driverAvailability === 'object') {
          for (const [drvId, avail] of Object.entries(runtimeData.driverAvailability)) {
            const driver = (cachedDataset.drivers || []).find((d) => d.driverId === drvId);
            if (driver) driver.availability = avail;
            const user = (cachedDataset.users || []).find((u) => u.driverId === drvId);
            if (user) user.availability = avail;
          }
        }
      } catch (runtimeErr) {
        console.warn(`[SyntheticDataService] Error merging runtime data: ${runtimeErr.message}`);
      }
    }

    return cachedDataset;
  } catch (err) {
    console.error(`[SyntheticDataService] Error parsing synthetic dataset: ${err.message}`);
    return {
      metadata: { title: 'ERROR_DATASET' },
      users: [],
      drivers: [],
      complaints: [],
      notifications: [],
      dashboards: {}
    };
  }
};

/**
 * Get all synthetic users
 */
const getSyntheticUsers = () => {
  const data = loadDataset();
  return [...(data.users || []), ...DEMO_USERS];
};

/**
 * Find synthetic user by email.
 * Supports:
 * - Direct email match (e.g. driver1@demo.local)
 * - Domain aliasing (@demo.local <-> @example.com)
 * - name.surname@demo.local format for all drivers (e.g. ganesh.gaikwad@demo.local, ramesh.shinde@demo.local)
 */
const getSyntheticUserByEmail = (email) => {
  if (!email) return null;
  const cleanEmail = email.trim().toLowerCase();
  const users = getSyntheticUsers();

  // 1. Direct match on email or @demo.local <-> @example.com
  const direct = users.find(
    (u) =>
      u.email &&
      (u.email.toLowerCase() === cleanEmail ||
        u.email.toLowerCase().replace('@demo.local', '@example.com') === cleanEmail ||
        (u.secondaryEmail && u.secondaryEmail.toLowerCase() === cleanEmail))
  );
  if (direct) return direct;

  // 2. Name-based match: name.surname@demo.local or name.surname@example.com
  for (const u of users) {
    if (u.name) {
      const cleanName = u.name.toLowerCase().replace(/\s*\(driver\)|\s*\(citizen\)|\s*\(officer\)/gi, '').trim();
      const dotEmailDemo = cleanName.replace(/\s+/g, '.') + '@demo.local';
      const dotEmailExample = cleanName.replace(/\s+/g, '.') + '@example.com';
      if (cleanEmail === dotEmailDemo || cleanEmail === dotEmailExample) {
        return {
          ...u,
          loginEmail: cleanEmail
        };
      }
    }
  }

  return null;
};

/**
 * Find synthetic user by id, _id, driverId, or employeeId
 */
const getSyntheticUserById = (id) => {
  if (!id) return null;
  const targetId = id.toString();
  const users = getSyntheticUsers();
  return (
    users.find(
      (u) =>
        u.id === targetId ||
        u._id === targetId ||
        u.driverId === targetId ||
        u.employeeId === targetId ||
        (targetId === 'mock-citizen-001' && u.email === 'citizen@example.com') ||
        (targetId === 'mock-citizen-002' && u.email === 'citizen2@example.com') ||
        (targetId === 'mock-citizen-003' && u.email === 'citizen3@example.com') ||
        (targetId === 'mock-officer-001' && u.email === 'officer@example.com') ||
        (targetId === 'mock-driver-001' && u.email === 'driver@example.com')
    ) || null
  );
};

/**
 * Get all synthetic drivers
 */
const getSyntheticDrivers = () => {
  const data = loadDataset();
  return data.drivers || [];
};

/**
 * Get all synthetic complaints
 */
const getSyntheticComplaints = () => {
  const data = loadDataset();
  return data.complaints || [];
};

/**
 * Get synthetic notifications for a specific user ID
 */
const getSyntheticNotifications = (userId) => {
  const data = loadDataset();
  const allNotifs = data.notifications || [];
  if (!userId) return allNotifs;
  const target = userId.toString();
  const synUser = getSyntheticUserById(target);
  const targetIds = new Set([target]);
  if (synUser) {
    if (synUser.id) targetIds.add(synUser.id.toString());
    if (synUser._id) targetIds.add(synUser._id.toString());
    if (synUser.driverId) targetIds.add(synUser.driverId.toString());
    if (synUser.employeeId) targetIds.add(synUser.employeeId.toString());
  }
  const filtered = allNotifs.filter((n) => targetIds.has(n.userId ? n.userId.toString() : ''));
  if (filtered.length > 0) return filtered;
  // Fallback to primary demo citizen notifications for demo/mock citizen accounts
  if (
    target === '67b300000000000000000001' ||
    target === 'mock-citizen-001' ||
    target.startsWith('mock-citizen')
  ) {
    return allNotifs.filter((n) => n.userId === '65f100000000000000000001');
  }
  return filtered;
};

/**
 * Generate Citizen Dashboard payload for a given citizen ID
 */
const getCitizenDashboardData = (citizenId) => {
  const data = loadDataset();
  const allComplaints = data.complaints || [];
  const targetId = citizenId ? citizenId.toString() : '';

  // Filter complaints for this citizen; include demo citizen complaints for demo accounts
  const isDemoCitizen = (
    targetId === '67b300000000000000000001' ||
    targetId === 'mock-citizen-001' ||
    targetId === '65f100000000000000000001'
  );

  let citizenComplaints = allComplaints.filter((c) => {
    return c.citizenId === targetId || (isDemoCitizen && c.citizenId === '65f100000000000000000001');
  });
  let effectiveCitizenId = targetId;
  if (citizenComplaints.length === 0 && allComplaints.length > 0) {
    effectiveCitizenId = data.users.find((u) => u.role === 'CITIZEN')?.id || '65f100000000000000000001';
    citizenComplaints = allComplaints.filter((c) => c.citizenId === effectiveCitizenId);
  }
  // Sort newest complaints first so user's complaint is always top
  citizenComplaints.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  const activeComplaints = citizenComplaints.filter(
    (c) =>
      c.status === 'SUBMITTED' ||
      c.status === 'ASSIGNED' ||
      c.status === 'PICKUP_IN_PROGRESS'
  );

  const pendingRequests = citizenComplaints.filter((c) => c.status === 'SUBMITTED');

  const emergencyRequests = citizenComplaints.filter(
    (c) => c.emergency && c.status !== 'CLEANED' && c.status !== 'VERIFIED'
  );

  const completedComplaints = citizenComplaints.filter(
    (c) => c.status === 'CLEANED' || c.status === 'VERIFIED'
  );

  const rejectedComplaints = citizenComplaints.filter((c) => c.status === 'REJECTED');

  const recentComplaints = [...citizenComplaints]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 5);

  const notifications = getSyntheticNotifications(effectiveCitizenId);
  const unreadCount = notifications.filter((n) => !n.isRead).length;
  const citizenName =
    citizenComplaints[0]?.citizenName ||
    data.users.find((u) => u.id === effectiveCitizenId)?.name ||
    'Aarav Sharma';

  return {
    citizenId: effectiveCitizenId,
    citizenName,
    metrics: {
      totalComplaints: citizenComplaints.length,
      activeComplaints: activeComplaints.length,
      pendingRequests: pendingRequests.length,
      emergencyRequests: emergencyRequests.length,
      completedComplaints: completedComplaints.length,
      rejectedComplaints: rejectedComplaints.length
    },
    recentComplaints,
    complaints: citizenComplaints,
    notifications,
    unreadNotificationsCount: unreadCount,
    mode: 'SYNTHETIC_DEMO'
  };
};

/**
 * Generate Municipal Officer Dashboard payload
 */
const getOfficerDashboardData = () => {
  const data = loadDataset();
  const allComplaints = data.complaints || [];
  const allDrivers = data.drivers || [];

  const submitted = allComplaints.filter((c) => c.status === 'SUBMITTED');
  const assigned = allComplaints.filter((c) => c.status === 'ASSIGNED');
  const inProgress = allComplaints.filter((c) => c.status === 'PICKUP_IN_PROGRESS');
  const cleaned = allComplaints.filter((c) => c.status === 'CLEANED');
  const verified = allComplaints.filter((c) => c.status === 'VERIFIED');
  const rejected = allComplaints.filter((c) => c.status === 'REJECTED');
  const emergency = allComplaints.filter((c) => c.emergency);

  // Priority requests: active queue sorted by rule-based priority score
  const priorityComplaints = buildPriorityQueue(allComplaints).slice(0, 8);

  const pendingAssignments = submitted.slice(0, 8);

  const recentComplaints = [...allComplaints]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 10);

  const driverAvailability = {
    total: allDrivers.length,
    available: allDrivers.filter((d) => d.availability === 'AVAILABLE').length,
    busy: allDrivers.filter((d) => d.availability === 'BUSY').length,
    offline: allDrivers.filter((d) => d.availability === 'OFFLINE').length,
    drivers: allDrivers
  };

  const aiMetrics = {
    totalAnalyzed: allComplaints.filter((c) => c.aiAnalysisStatus === 'COMPLETED').length,
    pending: allComplaints.filter((c) => c.aiAnalysisStatus === 'PENDING').length,
    failed: allComplaints.filter((c) => c.aiAnalysisStatus === 'FAILED').length,
    unavailable: allComplaints.filter((c) => c.aiAnalysisStatus === 'AI_UNAVAILABLE').length
  };

  return {
    metrics: {
      total: allComplaints.length,
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
    complaints: allComplaints,
    mode: 'SYNTHETIC_DEMO'
  };
};

/**
 * Generate Garbage Driver Dashboard payload for a given driver ID
 */
const getDriverDashboardData = (driverId) => {
  const data = loadDataset();
  const allComplaints = data.complaints || [];
  const allDrivers = data.drivers || [];
  const targetId = driverId ? driverId.toString() : '';

  // Filter tasks assigned to this driver
  let driverTasks = allComplaints.filter((c) => c.assignedDriverId === targetId);
  let effectiveDriverId = targetId;

  if (driverTasks.length === 0 && allComplaints.length > 0) {
    // Fallback to first synthetic driver
    effectiveDriverId = 'DRV-DEMO-001';
    driverTasks = allComplaints.filter((c) => c.assignedDriverId === effectiveDriverId);
  }

  const driverRecord = allDrivers.find((d) => d.driverId === effectiveDriverId) || {
    driverId: effectiveDriverId,
    name: driverTasks[0]?.assignedDriverName || 'Demo Driver',
    vehicleNumber: driverTasks[0]?.assignedVehicleNumber || 'MH-DEMO-001',
    vehicleType: 'Heavy Compactor Truck',
    availability: 'AVAILABLE',
    currentLocation: { latitude: 18.4682, longitude: 73.8647, timestamp: Date.now() }
  };

  const assignedTasks = driverTasks.filter((c) => c.status === 'ASSIGNED');
  const inProgressTasks = driverTasks.filter((c) => c.status === 'PICKUP_IN_PROGRESS');
  const completedTasks = driverTasks.filter((c) => c.status === 'CLEANED' || c.status === 'VERIFIED');
  const emergencyTasks = driverTasks.filter(
    (c) => c.emergency && c.status !== 'CLEANED' && c.status !== 'VERIFIED'
  );

  const activeTask = inProgressTasks[0] || null;

  const collectionHistory = [...completedTasks].sort(
    (a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)
  );

  const notifications = getSyntheticNotifications(effectiveDriverId);
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return {
    driverId: effectiveDriverId,
    driverInfo: driverRecord,
    metrics: {
      totalAssignedTasks: driverTasks.length,
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
    mode: 'SYNTHETIC_DEMO'
  };
};

/**
 * Persist user-created and updated synthetic data to disk so it survives server restarts.
 */
const persistRuntimeData = () => {
  if (process.env.NODE_ENV === 'test' || !cachedDataset) return;
  try {
    const runtimePath = path.join(__dirname, '..', 'data', 'synthetic', 'runtime-data.json');
    const runtimeComplaints = (cachedDataset.complaints || []).filter(
      (c) => c && (!c.complaintId || !c.complaintId.startsWith('SGM-DEMO-') || c._isRuntimeModified)
    );
    const runtimeNotifications = (cachedDataset.notifications || []).filter(
      (n) => n && (!n.id || !n.id.startsWith('NOTIF-DEMO-') || n._isRuntimeModified)
    );
    const driverAvailability = {};
    for (const d of cachedDataset.drivers || []) {
      if (d.driverId) {
        driverAvailability[d.driverId] = d.availability;
      }
    }
    const payload = {
      complaints: runtimeComplaints,
      notifications: runtimeNotifications,
      driverAvailability
    };
    fs.writeFileSync(runtimePath, JSON.stringify(payload, null, 2), 'utf-8');
  } catch (err) {
    console.warn(`[SyntheticDataService] Failed to persist runtime data: ${err.message}`);
  }
};

/**
 * Add a complaint to in-memory synthetic dataset and persist to disk
 */
const addSyntheticComplaint = (complaint) => {
  const data = loadDataset();
  if (!data.complaints) data.complaints = [];
  complaint._isRuntimeModified = true;
  data.complaints.unshift(complaint);
  persistRuntimeData();
  return complaint;
};

/**
 * Update an in-memory synthetic complaint by ID and persist to disk
 */
const updateSyntheticComplaint = (id, updates) => {
  const data = loadDataset();
  const index = (data.complaints || []).findIndex(
    (c) => c.complaintId === id || c.id === id || c._id === id
  );
  if (index !== -1) {
    data.complaints[index] = { ...data.complaints[index], ...updates, _isRuntimeModified: true };
    persistRuntimeData();
    return data.complaints[index];
  }
  return null;
};

/**
 * Add an in-memory synthetic notification and persist to disk
 */
const addSyntheticNotification = (notification) => {
  const data = loadDataset();
  if (!data.notifications) data.notifications = [];
  notification._isRuntimeModified = true;
  data.notifications.unshift(notification);
  persistRuntimeData();
  return notification;
};

/**
 * Update synthetic driver availability in memory and persist to disk
 */
const updateSyntheticDriverAvailability = (driverId, availability) => {
  const data = loadDataset();
  const driver = (data.drivers || []).find((d) => d.driverId === driverId);
  if (driver) {
    driver.availability = availability;
    const user = (data.users || []).find((u) => u.driverId === driverId);
    if (user) {
      user.availability = availability;
    }
    persistRuntimeData();
    return driver;
  }
  return null;
};

/**
 * Reset in-memory dataset cache (useful for clean test setups)
 */
const resetSyntheticCache = () => {
  cachedDataset = null;
  return loadDataset(true);
};

module.exports = {
  isSyntheticMode,
  loadDataset,
  getSyntheticUsers,
  getSyntheticUserByEmail,
  getSyntheticUserById,
  getSyntheticDrivers,
  getSyntheticComplaints,
  getSyntheticNotifications,
  getCitizenDashboardData,
  getOfficerDashboardData,
  getDriverDashboardData,
  addSyntheticComplaint,
  updateSyntheticComplaint,
  addSyntheticNotification,
  updateSyntheticDriverAvailability,
  resetSyntheticCache
};
