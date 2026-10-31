/**
 * Business rules engine governing complaint status transitions.
 * Perfectly mirrors Android StatusTransitionValidator.kt.
 */

const ALLOWED_OFFICER_TRANSITIONS = {
  SUBMITTED: ['ASSIGNED', 'REJECTED'],
  ASSIGNED: ['PICKUP_IN_PROGRESS', 'REJECTED'],
  PICKUP_IN_PROGRESS: ['CLEANED'],
  CLEANED: ['VERIFIED'],
  VERIFIED: [],
  REJECTED: []
};

const ALLOWED_DRIVER_TRANSITIONS = {
  ASSIGNED: ['PICKUP_IN_PROGRESS'],
  PICKUP_IN_PROGRESS: ['CLEANED']
};

const canTransition = (currentStatus, targetStatus) => {
  if (currentStatus === targetStatus) return false;
  const allowed = ALLOWED_OFFICER_TRANSITIONS[currentStatus] || [];
  return allowed.includes(targetStatus);
};

const canDriverTransition = (currentStatus, targetStatus) => {
  if (currentStatus === targetStatus) return false;
  const allowed = ALLOWED_DRIVER_TRANSITIONS[currentStatus] || [];
  return allowed.includes(targetStatus);
};

const generateComplaintId = () => {
  // Shorter, cleaner government service reference format: SGM-XXXXXX (6-digit numeric reference)
  // e.g. SGM-101542
  const min = 100000;
  const max = 999999;
  const num = Math.floor(min + Math.random() * (max - min + 1));
  return `SGM-${num}`;
};

module.exports = {
  ALLOWED_OFFICER_TRANSITIONS,
  ALLOWED_DRIVER_TRANSITIONS,
  canTransition,
  canDriverTransition,
  generateComplaintId
};
