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
  const nLat = Number(lat);
  const nLon = Number(lon);
  if (!Number.isFinite(nLat) || !Number.isFinite(nLon)) {
    return false;
  }
  return nLat >= -90 && nLat <= 90 && nLon >= -180 && nLon <= 180;
};

/**
 * In-memory mutex promise chain to serialize concurrent dispatch in synthetic demo mode.
 */
let dispatchLock = Promise.resolve();

const withDispatchLock = (fn) => {
  const result = dispatchLock.then(fn, fn);
  dispatchLock = result.catch(() => {});
  return result;
};

/**
 * Ranks available drivers by distance to complaint coordinates, with stable driver ID tie-breaking.
 *
 * @param {{latitude: number, longitude: number}} complaintCoords
 * @param {Array<Object>} availableDrivers
 * @returns {Array<{driver: Object, distanceKm: number}>}
 */
const rankCandidateDrivers = (complaintCoords, availableDrivers) => {
  const { latitude: cLat, longitude: cLon } = complaintCoords;
  if (!isValidCoordinate(cLat, cLon)) {
    return [];
  }

  const candidates = [];
  for (const driver of availableDrivers) {
    if (driver.availability !== 'AVAILABLE') {
      continue;
    }
    const dLoc = driver.currentLocation;
    if (!dLoc || !isValidCoordinate(dLoc.latitude, dLoc.longitude)) {
      continue;
    }

    const distanceKm = calculateHaversineDistanceKm(
      Number(cLat),
      Number(cLon),
      Number(dLoc.latitude),
      Number(dLoc.longitude)
    );

    candidates.push({
      driver,
      distanceKm
    });
  }

  candidates.sort((a, b) => {
    const diff = a.distanceKm - b.distanceKm;
    if (Math.abs(diff) > 1e-6) {
      return diff;
    }
    return String(a.driver.driverId || '').localeCompare(String(b.driver.driverId || ''));
  });

  return candidates;
};

/**
 * Attempts automatic driver assignment for a complaint based on proximity and availability.
 * Supports both MongoDB mode and Synthetic Demo mode.
 *
 * @param {Object} complaint The newly created complaint
 * @param {Object} options Optional settings { now }
 * @returns {Promise<Object>} Dispatch result object
 */
const autoDispatchComplaint = async (complaint, options = {}) => {
  const now = options.now || Date.now();
  const cLat = complaint.latitude;
  const cLon = complaint.longitude;

  // 1. If complaint coordinates are missing or invalid:
  if (!isValidCoordinate(cLat, cLon)) {
    return {
      assigned: false,
      assignmentStatus: 'PENDING_DRIVER',
      assignedDriverId: null,
      assignedDriverName: null,
      assignedVehicleNumber: null,
      distanceKm: null,
      reason: 'MISSING_COMPLAINT_COORDINATES',
      message: 'Complaint registered. Awaiting driver assignment because valid GPS coordinates were not provided.'
    };
  }

  // 2. Synthetic Demo Mode execution
  if (syntheticDataService.isSyntheticMode()) {
    return withDispatchLock(async () => {
      const allDrivers = syntheticDataService.getSyntheticDrivers();

      // Filter eligible drivers: availability AVAILABLE and valid coordinates
      const eligibleDrivers = allDrivers.filter(
        (d) => d.availability === 'AVAILABLE'
      );

      const ranked = rankCandidateDrivers({ latitude: cLat, longitude: cLon }, eligibleDrivers);

      if (ranked.length === 0) {
        return {
          assigned: false,
          assignmentStatus: 'PENDING_DRIVER',
          assignedDriverId: null,
          assignedDriverName: null,
          assignedVehicleNumber: null,
          distanceKm: null,
          reason: 'NO_AVAILABLE_DRIVER',
          message: 'Complaint registered. No nearby drivers are currently available for automatic dispatch.'
        };
      }

      const selected = ranked[0];
      const driver = selected.driver;
      const distKm = Math.round(selected.distanceKm * 100) / 100;

      // Atomically update driver availability in synthetic data
      syntheticDataService.updateSyntheticDriverAvailability(driver.driverId, 'BUSY');

      // Update complaint in memory
      complaint.status = 'ASSIGNED';
      complaint.assignedDriverId = driver.driverId;
      complaint.assignedDriverName = driver.name;
      complaint.assignedVehicleNumber = driver.vehicleNumber || 'Vehicle Assigned';
      complaint.assignedAt = now;
      if (!complaint.statusHistory) complaint.statusHistory = [];
      complaint.statusHistory.push({
        status: 'ASSIGNED',
        timestamp: now,
        changedBy: 'System Auto-Dispatch',
        role: 'SYSTEM',
        note: `Automatically assigned to closest available driver ${driver.name} (${driver.vehicleNumber || 'Vehicle Assigned'}) [${distKm} km].`
      });

      // Send in-app notifications
      // 1. Citizen notification
      await createNotification({
        userId: complaint.citizenId,
        type: 'PICKUP_ASSIGNED',
        title: 'Garbage Pickup Assigned',
        message: `Your complaint ${complaint.complaintId} has been automatically assigned to driver ${driver.name} (${driver.vehicleNumber || 'Collection Vehicle'}).`,
        complaintId: complaint.complaintId
      });

      // 2. Driver notification (lookup user account if available)
      const driverUser = syntheticDataService.getSyntheticUserById(driver.driverId);
      const driverUserId = driverUser ? driverUser.id || driverUser._id : driver.driverId;
      await createNotification({
        userId: driverUserId,
        type: 'DRIVER_ASSIGNMENT',
        title: 'New Collection Assignment',
        message: `Complaint ${complaint.complaintId} (${complaint.locationText || 'Location'}) has been automatically assigned to your route.`,
        complaintId: complaint.complaintId
      });

      return {
        assigned: true,
        assignmentStatus: 'ASSIGNED',
        assignedDriverId: driver.driverId,
        assignedDriverName: driver.name,
        assignedVehicleNumber: driver.vehicleNumber || 'Vehicle Assigned',
        distanceKm: distKm,
        message: `Complaint submitted and automatically assigned to nearby driver ${driver.name} (${distKm} km).`
      };
    });
  }

  // 3. MongoDB Mode execution
  // Query AVAILABLE drivers with non-null coordinates
  const eligibleDrivers = await Driver.find({
    availability: 'AVAILABLE',
    'currentLocation.latitude': { $ne: null },
    'currentLocation.longitude': { $ne: null }
  });

  const ranked = rankCandidateDrivers({ latitude: cLat, longitude: cLon }, eligibleDrivers);

  if (ranked.length === 0) {
    return {
      assigned: false,
      assignmentStatus: 'PENDING_DRIVER',
      assignedDriverId: null,
      assignedDriverName: null,
      assignedVehicleNumber: null,
      distanceKm: null,
      reason: 'NO_AVAILABLE_DRIVER',
      message: 'Complaint registered. No nearby drivers are currently available for automatic dispatch.'
    };
  }

  // Iterate over candidates in distance order and attempt atomic claim
  for (const candidate of ranked) {
    const candidateDriver = candidate.driver;
    const claimedDriver = await Driver.findOneAndUpdate(
      { driverId: candidateDriver.driverId, availability: 'AVAILABLE' },
      { availability: 'BUSY' },
      { new: true }
    );

    if (!claimedDriver) {
      // Race condition: another request claimed this driver, try next candidate
      continue;
    }

    const distKm = Math.round(candidate.distanceKm * 100) / 100;

    // Assignment succeeds: update complaint
    complaint.status = 'ASSIGNED';
    complaint.assignedDriverId = claimedDriver.driverId;
    complaint.assignedDriverName = claimedDriver.name;
    complaint.assignedVehicleNumber = claimedDriver.vehicleNumber || 'Vehicle Assigned';
    complaint.assignedAt = now;
    if (!complaint.statusHistory) complaint.statusHistory = [];
    complaint.statusHistory.push({
      status: 'ASSIGNED',
      timestamp: now,
      changedBy: 'System Auto-Dispatch',
      role: 'SYSTEM',
      note: `Automatically assigned to closest available driver ${claimedDriver.name} (${claimedDriver.vehicleNumber || 'Vehicle Assigned'}) [${distKm} km].`
    });

    if (typeof complaint.save === 'function') {
      await complaint.save();
    }

    // Send citizen notification
    await createNotification({
      userId: complaint.citizenId,
      type: 'PICKUP_ASSIGNED',
      title: 'Garbage Pickup Assigned',
      message: `Your complaint ${complaint.complaintId} has been automatically assigned to driver ${claimedDriver.name} (${claimedDriver.vehicleNumber || 'Collection Vehicle'}).`,
      complaintId: complaint.complaintId
    });

    let driverTargetId = claimedDriver.driverId;
    try {
      const driverUser = await User.findOne({
        $or: [{ driverId: claimedDriver.driverId }, { employeeId: claimedDriver.employeeId }]
      });
      if (driverUser) {
        driverTargetId = driverUser.id || driverUser._id || claimedDriver.driverId;
      }
    } catch (e) {
      // Gracefully fallback to driverId
    }

    await createNotification({
      userId: driverTargetId,
      type: 'DRIVER_ASSIGNMENT',
      title: 'New Collection Assignment',
      message: `Complaint ${complaint.complaintId} (${complaint.locationText || 'Location'}) has been automatically assigned to your route.`,
      complaintId: complaint.complaintId
    });

    return {
      assigned: true,
      assignmentStatus: 'ASSIGNED',
      assignedDriverId: claimedDriver.driverId,
      assignedDriverName: claimedDriver.name,
      assignedVehicleNumber: claimedDriver.vehicleNumber || 'Vehicle Assigned',
      distanceKm: distKm,
      message: `Complaint submitted and automatically assigned to nearby driver ${claimedDriver.name} (${distKm} km).`
    };
  }

  // All candidate claims failed due to concurrent acquisition
  return {
    assigned: false,
    assignmentStatus: 'PENDING_DRIVER',
    assignedDriverId: null,
    assignedDriverName: null,
    assignedVehicleNumber: null,
    distanceKm: null,
    reason: 'NO_AVAILABLE_DRIVER',
    message: 'Complaint registered. No nearby drivers are currently available for automatic dispatch.'
  };
};

module.exports = {
  calculateHaversineDistanceKm,
  isValidCoordinate,
  rankCandidateDrivers,
  autoDispatchComplaint
};
