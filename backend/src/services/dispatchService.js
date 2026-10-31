const Driver = require('../models/Driver');
const Complaint = require('../models/Complaint');
const User = require('../models/User');
const { createNotification } = require('./notificationService');
const syntheticDataService = require('./syntheticDataService');

/**
 * Converts degrees to radians.
 */
const toRadians = (degrees) => (degrees * Math.PI) / 180;

/**
 * Calculates straight-line distance in kilometers between two lat/lon coordinates
 * using the standard Haversine formula.
 *
 * @param {number} lat1 Latitude of first point
 * @param {number} lon1 Longitude of first point
 * @param {number} lat2 Latitude of second point
 * @param {number} lon2 Longitude of second point
 * @returns {number} Distance in kilometers
 */
const calculateHaversineDistanceKm = (lat1, lon1, lat2, lon2) => {
  const R = 6371; // Earth radius in kilometers
  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
};

/**
 * Validates that latitude and longitude are finite, non-null numbers within valid spherical ranges.
 */
const isValidCoordinate = (lat, lon) => {
  if (lat === null || lat === undefined || lon === null || lon === undefined) {
    return false;
  }
  if (typeof lat !== 'number' || typeof lon !== 'number') return false;
  const nLat = Number(lat);
  const nLon = Number(lon);
  if (!Number.isFinite(nLat) || !Number.isFinite(nLon)) {
    return false;
  }
  return nLat >= -90 && nLat <= 90 && nLon >= -180 && nLon <= 180;
};

const env = require('../config/env');
const { withTransaction } = require('./transactionService');
const ACTIVE_STATUSES = ['ASSIGNED', 'PICKUP_IN_PROGRESS'];
const conflict = (message) => Object.assign(new Error(message), { statusCode: 409 });

const hasFreshLocation = (driver, now = Date.now()) => {
  const loc = driver.currentLocation;
  if (!loc || !isValidCoordinate(loc.latitude, loc.longitude)) return false;
  if (loc.source !== 'DEVICE') return false;
  if (syntheticDataService.isSyntheticMode() && process.env.NODE_ENV !== 'test') {
    return true;
  }
  return Number.isFinite(loc.timestamp) && loc.timestamp <= now
    && now - loc.timestamp <= env.DRIVER_LOCATION_MAX_AGE_MS;
};

const rankCandidateDrivers = (coords, drivers, now = Date.now()) => {
  if (!isValidCoordinate(coords.latitude, coords.longitude)) return [];
  return drivers.filter(d => d.availability === 'AVAILABLE' && hasFreshLocation(d, now))
    .map(driver => ({ driver, distanceKm: calculateHaversineDistanceKm(coords.latitude, coords.longitude,
      driver.currentLocation.latitude, driver.currentLocation.longitude) }))
    .filter(c => c.distanceKm <= env.DRIVER_SERVICE_RADIUS_KM)
    .sort((a, b) => a.distanceKm - b.distanceKm || (b.driver.currentLocation?.timestamp || 0) - (a.driver.currentLocation?.timestamp || 0) || a.driver.driverId.localeCompare(b.driver.driverId));
};

const resultFor = (complaint, distanceKm = null, idempotent = false) => ({
  assigned: true, assignmentStatus: 'ASSIGNED', assignedDriverId: complaint.assignedDriverId,
  assignedDriverName: complaint.assignedDriverName, assignedVehicleNumber: complaint.assignedVehicleNumber,
  distanceKm, idempotent, assignedAt: complaint.assignedAt, message: 'Pickup assigned successfully.'
});

const notifyAssignment = async (complaint) => {
  const driverUser = syntheticDataService.isSyntheticMode()
    ? syntheticDataService.getSyntheticUserById(complaint.assignedDriverId)
    : await User.findOne({ driverId: complaint.assignedDriverId, role: 'GARBAGE_DRIVER' });
  if (complaint.citizenId) await createNotification({ userId: complaint.citizenId, type: 'PICKUP_ASSIGNED',
    title: 'Garbage Pickup Assigned', message: `Complaint ${complaint.complaintId} assigned to ${complaint.assignedDriverName}.`,
    complaintId: complaint.complaintId });
  await createNotification({ userId: driverUser?.id || driverUser?._id || complaint.assignedDriverId,
    type: 'DRIVER_ASSIGNMENT', title: 'New Collection Assignment',
    message: `Complaint ${complaint.complaintId} assigned to your vehicle.`, complaintId: complaint.complaintId });
};

// Manual and automatic callers share all eligibility, capacity, and idempotency rules.
const reserveAssignment = async (complaintId, driverId, actor = {}, now = Date.now()) => {
  if (syntheticDataService.isSyntheticMode()) {
    const c = syntheticDataService.getSyntheticComplaints().find(c => c.complaintId === complaintId || c.id === complaintId);
    const effectiveDriverId = syntheticDataService.resolveDriverId ? syntheticDataService.resolveDriverId(driverId) : driverId;
    const d = syntheticDataService.getSyntheticDrivers().find(d => d.driverId === driverId || d.driverId === effectiveDriverId);
    if (!c || !d) throw conflict('Complaint or driver not found.');
    if (ACTIVE_STATUSES.includes(c.status) && (c.assignedDriverId === driverId || c.assignedDriverId === effectiveDriverId)) return resultFor(c, null, true);
    if (c.status !== 'SUBMITTED') throw conflict('Only submitted complaints may be assigned.');
    if (!rankCandidateDrivers(c, [d], now).length || syntheticDataService.getSyntheticComplaints()
      .some(t => (t.assignedDriverId === driverId || t.assignedDriverId === effectiveDriverId) && ACTIVE_STATUSES.includes(t.status))) throw conflict('Driver unavailable or location stale/outside service radius.');
    applyAssignment(c, d, actor, now);
    syntheticDataService.updateSyntheticComplaint(complaintId, c);
    syntheticDataService.updateSyntheticDriverAvailability(d.driverId, 'BUSY');
    if (driverId !== d.driverId) {
      syntheticDataService.updateSyntheticDriverAvailability(driverId, 'BUSY');
    }
    await notifyAssignment(c);
    return resultFor(c);
  }
  const outcome = await withTransaction(async session => {
    const c = await Complaint.findOne({ complaintId }).session(session);
    if (!c) throw Object.assign(new Error('Complaint not found.'), { statusCode: 404 });
    if (ACTIVE_STATUSES.includes(c.status) && c.assignedDriverId === driverId) return { c, idempotent: true };
    if (c.status !== 'SUBMITTED') throw conflict('Only submitted complaints may be assigned.');
    const d = await Driver.findOne({ driverId }).session(session);
    if (!d || !rankCandidateDrivers(c, [d], now).length) throw conflict('Driver unavailable or location stale/outside service radius.');
    if (await Complaint.exists({ assignedDriverId: driverId, status: { $in: ACTIVE_STATUSES } }).session(session)) {
      throw conflict('Driver already has an active pickup.');
    }
    // Both concurrent assignments write this driver document; MongoDB retries write conflicts.
    const claimed = await Driver.findOneAndUpdate({ driverId, availability: 'AVAILABLE' },
      { $set: { availability: 'BUSY' } }, { new: true, session });
    if (!claimed) throw conflict('Driver was reserved by another pickup.');
    applyAssignment(c, claimed, actor, now);
    await c.save({ session });
    return { c, idempotent: false };
  });
  // Notifications are after commit. Delivery failure must not turn a committed assignment into an HTTP failure.
  if (!outcome.idempotent) {
    try { await notifyAssignment(outcome.c); } catch (_) { console.warn('[Dispatch] Assignment committed; notification delivery failed.'); }
  }
  return resultFor(outcome.c, null, outcome.idempotent);
};

function applyAssignment(c, d, actor, now) {
  if (c.source === 'SENSOR') c.dispatchRetry = { ...(c.dispatchRetry?.toObject?.() || c.dispatchRetry || {}),
    state: 'ASSIGNED', reason: null, nextAttemptAt: null, leaseUntil: 0 };
  c.status = 'ASSIGNED'; c.assignedDriverId = d.driverId; c.assignedDriverName = d.name;
  c.assignedVehicleNumber = d.vehicleNumber; c.assignedAt = now;
  if (!c.statusHistory) c.statusHistory = [];
  c.statusHistory.push({ status: 'ASSIGNED', timestamp: now, changedBy: actor.name || 'System Auto-Dispatch',
    role: actor.role || 'SYSTEM', note: `Assigned to ${d.name}.` });
}

const pending = reason => ({ assigned: false, assignmentStatus: 'PENDING_DRIVER', reason,
  assignedDriverId: null, message: 'Complaint saved; awaiting eligible driver assignment.' });
const autoDispatchComplaint = async (complaint, options = {}) => {
  const now = options.now || Date.now();
  if (ACTIVE_STATUSES.includes(complaint.status) && complaint.assignedDriverId) return resultFor(complaint, null, true);
  if (!isValidCoordinate(complaint.latitude, complaint.longitude)) return pending('MISSING_COMPLAINT_COORDINATES');
  const drivers = syntheticDataService.isSyntheticMode() ? syntheticDataService.getSyntheticDrivers()
    : await Driver.find({ availability: 'AVAILABLE' });
  for (const candidate of rankCandidateDrivers(complaint, drivers, now)) {
    try {
      const result = await reserveAssignment(complaint.complaintId, candidate.driver.driverId, {}, now);
      Object.assign(complaint, { status: 'ASSIGNED', assignedDriverId: result.assignedDriverId,
        assignedDriverName: result.assignedDriverName, assignedVehicleNumber: result.assignedVehicleNumber, assignedAt: result.assignedAt });
      if (!result.idempotent && !complaint.statusHistory?.some(h => h.status === 'ASSIGNED' && h.timestamp === result.assignedAt)) {
        if (!complaint.statusHistory) complaint.statusHistory = [];
        complaint.statusHistory.push({ status: 'ASSIGNED', timestamp: result.assignedAt, changedBy: 'System Auto-Dispatch', role: 'SYSTEM', note: `Assigned to ${result.assignedDriverName}.` });
      }
      return { ...result, distanceKm: candidate.distanceKm };
    } catch (error) {
      if (error.statusCode !== 409) throw error;
    }
  }
  return pending('NO_AVAILABLE_DRIVER');
};

// Called in the same transaction as completion/rejection. Never release capacity with other active tasks.
const releaseDriverIfIdle = async (driverId, session) => {
  if (!driverId) return;
  const driver = await Driver.findOne({ driverId }).session(session);
  if (!driver) return;
  const active = await Complaint.exists({ assignedDriverId: driverId, status: { $in: ACTIVE_STATUSES } }).session(session);
  await Driver.updateOne({ driverId }, { $set: { availability: active ? 'BUSY' : (driver.availability === 'OFFLINE' ? 'OFFLINE' : 'AVAILABLE') } }, { session });
};
// Fresh availability/location is actionable: don't wait out an old no-driver backoff.
// This only advances the worker deadline; all reservation checks still run there.
const wakePendingSensorPickups = async (now = Date.now()) => {
  if (syntheticDataService.isSyntheticMode()) return;
  try {
    await Complaint.updateMany({ source: 'SENSOR', status: 'SUBMITTED', 'dispatchRetry.state': 'PENDING_DRIVER' },
      { $min: { 'dispatchRetry.nextAttemptAt': now } });
  } catch (_) { console.warn('[Dispatch] Pending pickups retain their existing retry deadlines.'); }
};
module.exports = { calculateHaversineDistanceKm, isValidCoordinate, hasFreshLocation, rankCandidateDrivers,
  reserveAssignment, autoDispatchComplaint, releaseDriverIfIdle, wakePendingSensorPickups, ACTIVE_STATUSES };
