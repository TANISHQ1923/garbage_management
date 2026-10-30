jest.mock('../src/config/db', () => ({ getDBStatus: () => ({ isConnected: true }) }));
jest.mock('../src/services/syntheticDataService', () => ({ isSyntheticMode: () => false }));
jest.mock('../src/services/sensorService', () => ({ assertSensorStorage: jest.fn() }));
jest.mock('../src/services/dispatchService', () => ({ autoDispatchComplaint: jest.fn() }));
const Complaint = require('../src/models/Complaint');
const Bin = require('../src/models/Bin');
const { autoDispatchComplaint } = require('../src/services/dispatchService');
const { retrySensorPickup } = require('../src/workers/sensorDispatchWorker');
beforeEach(() => {
  jest.restoreAllMocks();
  jest.spyOn(Complaint, 'findOneAndUpdate').mockResolvedValue({ complaintId: 'BIN-TASK', binId: 'BIN1', dispatchRetry: { attempts: 1 } });
  jest.spyOn(Bin, 'findOne').mockResolvedValue({ config: { retryDelayMs: 15000 } });
  jest.spyOn(Complaint, 'updateOne').mockResolvedValue({});
});
test('no driver persists pending reason, retry time and clears lease', async () => {
  autoDispatchComplaint.mockResolvedValue({ assigned: false, reason: 'NO_AVAILABLE_DRIVER' });
  await retrySensorPickup(100000);
  expect(Complaint.updateOne).toHaveBeenCalledWith(expect.objectContaining({ status: 'SUBMITTED' }), {
    $set: { 'dispatchRetry.state': 'PENDING_DRIVER', 'dispatchRetry.reason': 'NO_AVAILABLE_DRIVER',
      'dispatchRetry.nextAttemptAt': 115000, 'dispatchRetry.leaseUntil': 0 }
  });
});
test('a dispatcher failure remains retryable and does not drop the task', async () => {
  autoDispatchComplaint.mockRejectedValue(new Error('transaction unavailable'));
  await retrySensorPickup(100000);
  expect(Complaint.updateOne.mock.calls[0][1].$set['dispatchRetry.reason']).toBe('DISPATCH_UNAVAILABLE');
});
test('successful assignment is not overwritten by pending bookkeeping', async () => {
  autoDispatchComplaint.mockResolvedValue({ assigned: true });
  await retrySensorPickup(100000);
  expect(Complaint.updateOne.mock.calls[0][0].status).toBe('SUBMITTED');
});
