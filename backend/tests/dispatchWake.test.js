const Complaint = require('../src/models/Complaint');
const { wakePendingSensorPickups } = require('../src/services/dispatchService');
test('a fresh driver event advances only pending sensor retry deadlines', async () => {
  const update = jest.spyOn(Complaint, 'updateMany').mockResolvedValue({});
  try {
    await wakePendingSensorPickups(1000);
    expect(update).toHaveBeenCalledWith({ source: 'SENSOR', status: 'SUBMITTED', 'dispatchRetry.state': 'PENDING_DRIVER' },
      { $min: { 'dispatchRetry.nextAttemptAt': 1000 } });
  } finally { update.mockRestore(); }
});
