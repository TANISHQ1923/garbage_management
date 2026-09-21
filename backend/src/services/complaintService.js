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
  const timestamp = Date.now().toString().slice(-6);
  const random = Math.floor(1000 + Math.random() * 9000);
  return `CMP-${timestamp}-${random}`;
};

module.exports = {
  ALLOWED_OFFICER_TRANSITIONS,
  ALLOWED_DRIVER_TRANSITIONS,
  canTransition,
  canDriverTransition,
  generateComplaintId
};
