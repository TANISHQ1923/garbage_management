const Complaint = require('../models/Complaint');
const { withTransaction } = require('./transactionService');
const { releaseDriverIfIdle } = require('./dispatchService');

// Re-read inside the transaction: a stale controller document cannot overwrite an assignment.
const commitTransition = async (snapshot, expectedStatus, driverId) => withTransaction(async session => {
  const current = await Complaint.findOne({ complaintId: snapshot.complaintId }).session(session);
  if (!current || current.status !== expectedStatus || (driverId && current.assignedDriverId !== driverId)) {
    throw Object.assign(new Error('Task changed. Refresh before retrying.'), { statusCode: 409 });
  }
  const fields = ['status', 'statusHistory', 'beforeCleaningImageUrl', 'beforeCleaningImagePublicId',
    'afterCleaningImageUrl', 'afterCleaningImagePublicId'];
  for (const field of fields) current[field] = snapshot[field];
  if (current.source === 'SENSOR' && ['VERIFIED', 'REJECTED'].includes(current.status)) {
    current.activeSensorTask = false;
    current.dispatchRetry.state = 'CLOSED'; current.dispatchRetry.nextAttemptAt = null;
  }
  await current.save({ session });
  if (['CLEANED', 'REJECTED', 'VERIFIED'].includes(current.status)) {
    await releaseDriverIfIdle(current.assignedDriverId, session);
  }
  return current;
});
module.exports = { commitTransition };
