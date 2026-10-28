/**
 * Complaint Priority Calculation Engine
 * Smart Garbage Management Application
 *
 * Deterministic rule-based priority scoring for municipal complaint triage.
 *
 * Scoring Policy:
 * 1. Emergency:
 *    - Emergency complaint: +50 points
 * 2. Complaint Age (mutually exclusive tiers):
 *    - >= 24 hours: +25 points
 *    - 12 to <24 hours: +15 points
 *    - 6 to <12 hours: +8 points
 *    - < 6 hours: +0 points
 * 3. Status:
 *    - SUBMITTED: +20 points
 *    - ASSIGNED: +10 points
 *    - PICKUP_IN_PROGRESS: +5 points
 *    - CLEANED, VERIFIED, REJECTED: 0 points (excluded from active dispatch queue)
 *
 * Priority Labels:
 * - >= 70: CRITICAL
 * - 50 - 69: HIGH
 * - 25 - 49: MEDIUM
 * - < 25: LOW
 *
 * Queue Sorting Policy:
 * 1. Highest priority score first
 * 2. Oldest complaint first when scores are equal
 * 3. Stable complaint ID ordering as final tie-breaker
 */

const ACTIVE_QUEUE_STATUSES = ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS'];

/**
 * Calculate deterministic priority score and label for a single complaint.
 *
 * @param {Object} complaint - Complaint document or JSON object
 * @param {number} [now=Date.now()] - Current epoch millisecond timestamp
 * @returns {Object} { score: number, level: string, ageHours: number, breakdown: Object }
 */
const calculatePriority = (complaint, now = Date.now()) => {
  if (!complaint) {
    return { score: 0, level: 'LOW', ageHours: 0, breakdown: { emergency: 0, age: 0, status: 0 } };
  }

  // 1. Emergency bonus
  const isEmergency = Boolean(complaint.emergency || complaint.isEmergency);
  const emergencyPoints = isEmergency ? 50 : 0;

  // 2. Age bonus (mutually exclusive)
  const createdTime = complaint.createdAt ? new Date(complaint.createdAt).getTime() : now;
  const ageMs = Math.max(0, now - createdTime);
  const ageHours = ageMs / (1000 * 60 * 60);

  let agePoints = 0;
  if (ageHours >= 24) {
    agePoints = 25;
  } else if (ageHours >= 12) {
    agePoints = 15;
  } else if (ageHours >= 6) {
    agePoints = 8;
  } else {
    agePoints = 0;
  }

  // 3. Status bonus
  let statusPoints = 0;
  switch (complaint.status) {
    case 'SUBMITTED':
      statusPoints = 20;
      break;
    case 'ASSIGNED':
      statusPoints = 10;
      break;
    case 'PICKUP_IN_PROGRESS':
      statusPoints = 5;
      break;
    default:
      statusPoints = 0;
  }

  const rawScore = emergencyPoints + agePoints + statusPoints;
  const score = Math.min(100, Math.max(0, rawScore));

  let level = 'LOW';
  if (score >= 70) {
    level = 'CRITICAL';
  } else if (score >= 50) {
    level = 'HIGH';
  } else if (score >= 25) {
    level = 'MEDIUM';
  } else {
    level = 'LOW';
  }

  return {
    score,
    level,
    ageHours: parseFloat(ageHours.toFixed(2)),
    breakdown: {
      emergency: emergencyPoints,
      age: agePoints,
      status: statusPoints
    }
  };
};

/**
 * Checks whether a complaint is eligible for the active dispatch queue.
 * Excludes: CLEANED (awaiting verification), VERIFIED (completed), REJECTED (terminal).
 *
 * @param {Object} complaint
 * @returns {boolean}
 */
const isEligibleForActiveQueue = (complaint) => {
  if (!complaint || !complaint.status) return false;
  return ACTIVE_QUEUE_STATUSES.includes(complaint.status);
};

/**
 * Builds and returns the sorted, deduplicated active priority queue.
 *
 * @param {Array<Object>} complaints
 * @param {number} [now=Date.now()]
 * @returns {Array<Object>}
 */
const buildPriorityQueue = (complaints, now = Date.now()) => {
  if (!Array.isArray(complaints)) return [];

  const seenIds = new Set();
  const eligibleEnriched = [];

  for (const c of complaints) {
    if (!isEligibleForActiveQueue(c)) continue;

    const id = c.complaintId || c.id || (c._id ? c._id.toString() : null);
    if (!id || seenIds.has(id)) continue;
    seenIds.add(id);

    const { score, level } = calculatePriority(c, now);

    // Support both plain objects and Mongoose documents
    const plain = typeof c.toObject === 'function' ? c.toObject() : { ...c };
    if (!plain.id) plain.id = id;

    plain.priorityScore = score;
    plain.priorityLevel = level;

    eligibleEnriched.push(plain);
  }

  // Multi-tier sort:
  // 1. Highest priority score first
  // 2. Oldest complaint first when scores are equal (ascending createdAt)
  // 3. Stable complaint ID ordering as tie-breaker
  eligibleEnriched.sort((a, b) => {
    if (b.priorityScore !== a.priorityScore) {
      return b.priorityScore - a.priorityScore;
    }
    const timeA = new Date(a.createdAt).getTime() || 0;
    const timeB = new Date(b.createdAt).getTime() || 0;
    if (timeA !== timeB) {
      return timeA - timeB;
    }
    const idA = a.complaintId || a.id || '';
    const idB = b.complaintId || b.id || '';
    return idA.localeCompare(idB);
  });

  return eligibleEnriched;
};

module.exports = {
  ACTIVE_QUEUE_STATUSES,
  calculatePriority,
  isEligibleForActiveQueue,
  buildPriorityQueue
};
