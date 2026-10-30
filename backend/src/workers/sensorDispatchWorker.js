const Complaint = require('../models/Complaint');
const Bin = require('../models/Bin');
const { autoDispatchComplaint } = require('../services/dispatchService');
const { assertSensorStorage } = require('../services/sensorService');
const { getDBStatus } = require('../config/db');
const { isSyntheticMode } = require('../services/syntheticDataService');

async function retrySensorPickup(now = Date.now()) {
  if (!getDBStatus().isConnected || isSyntheticMode()) return false;
  await assertSensorStorage();
  // A crashed worker's lease expires, leaving a durable retryable record.
  const task = await Complaint.findOneAndUpdate({ source: 'SENSOR', activeSensorTask: true, status: 'SUBMITTED',
    'dispatchRetry.state': 'PENDING_DRIVER', 'dispatchRetry.nextAttemptAt': { $lte: now },
    'dispatchRetry.leaseUntil': { $lte: now } }, {
    $set: { 'dispatchRetry.leaseUntil': now + 60000 }, $inc: { 'dispatchRetry.attempts': 1 }
  }, { new: true, sort: { 'dispatchRetry.nextAttemptAt': 1 } });
  if (!task) return false;
  let result;
  try { result = await autoDispatchComplaint(task); }
  catch (_) { result = { assigned: false, reason: 'DISPATCH_UNAVAILABLE' }; }
  const bin = await Bin.findOne({ binId: task.binId });
  const delay = Math.min(300000, (bin?.config.retryDelayMs || 15000) * Math.min(task.dispatchRetry.attempts, 10));
  // A manual or concurrent assignment must not be overwritten by the retry bookkeeping.
  await Complaint.updateOne({ complaintId: task.complaintId, status: 'SUBMITTED', source: 'SENSOR' }, { $set: {
    'dispatchRetry.state': 'PENDING_DRIVER', 'dispatchRetry.reason': result.reason || 'NO_AVAILABLE_DRIVER',
    'dispatchRetry.nextAttemptAt': now + delay, 'dispatchRetry.leaseUntil': 0
  } });
  return true;
}
function startSensorDispatchWorker() {
  let running = false;
  const tick = async () => {
    if (running) return;
    running = true;
    try { for (let i = 0; i < 10 && await retrySensorPickup(); i++); }
    catch (_) { /* Database/index failures leave pending work durable. Provisioning enables later ticks. */ }
    finally { running = false; }
  };
  const interval = setInterval(tick, 2000); interval.unref();
  tick();
  return () => clearInterval(interval);
}
module.exports = { retrySensorPickup, startSensorDispatchWorker };
