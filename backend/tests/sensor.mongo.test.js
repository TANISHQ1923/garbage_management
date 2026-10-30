// REAL constraint and transaction tests; opt-in local isolated database only.
const mongoose = require('mongoose');
const { randomUUID } = require('crypto');
const uri = process.env.PHASE1_TEST_MONGODB_URI;
if (uri && !/^mongodb:\/\/(127\.0\.0\.1|localhost):\d+\/phase1_test(?:\?|$)/.test(uri)) {
  throw new Error('Use an explicit loopback phase1_test database for sensor integration tests.');
}
(uri ? describe : describe.skip)('REAL MongoDB smart-bin integration', () => {
  const Bin = require('../src/models/Bin'), Device = require('../src/models/Device');
  const Reading = require('../src/models/SensorReading'), Complaint = require('../src/models/Complaint');
  const Driver = require('../src/models/Driver');
  const { initializeSensorStorage, ingest } = require('../src/services/sensorService');
  const { DEFAULT_CONFIG } = require('../src/services/binEvaluationService');
  const prefix = 'sensor-test-' + randomUUID();
  const sessionId = randomUUID();
  let binId, deviceId;
  beforeAll(async () => {
    await mongoose.connect(uri, { autoIndex: false, autoCreate: false, serverSelectionTimeoutMS: 3000 });
    await initializeSensorStorage();
    for (const model of [Driver, require('../src/models/User'), require('../src/models/Notification')]) await model.createCollection();
  });
  beforeEach(async () => {
    binId = prefix + '-' + randomUUID(); deviceId = prefix + '-' + randomUUID();
    await Bin.create({ binId, label: 'Test', latitude: 18.5, longitude: 73.8, locationText: 'Test',
      config: { ...DEFAULT_CONFIG, filterSamples: 1, confirmReadings: 2, confirmDurationMs: 2000 } });
    await Device.create({ deviceId, binId, sessionId, tokenHash: 'unused-in-service-test' });
  });
  afterAll(async () => {
    if (mongoose.connection.readyState === 1) {
      for (const model of [Bin, Device, Reading, Complaint]) await model.deleteMany({ binId: new RegExp('^' + prefix) });
    }
    await mongoose.disconnect();
  });
  const frame = (sequence, distanceCm = 3) => ({ sessionId, sequence, uptimeMs: sequence * 2000,
    capturedAtMs: 1000000 + sequence * 2000, distanceCm, valid: true, buffered: false });
  const send = r => ingest(deviceId, r, r.capturedAtMs);
  test('concurrent duplicate full readings create one task and one reading identity', async () => {
    await send(frame(1));
    await Promise.all([send(frame(2)), send(frame(2)), send(frame(3))]);
    expect(await Complaint.countDocuments({ binId, activeSensorTask: true })).toBe(1);
    expect(await Reading.countDocuments({ deviceId, sessionId, sequence: 2 })).toBe(1);
    const task = await Complaint.findOne({ binId });
    expect(task.citizenId).toBeUndefined(); expect(task.dispatchRetry.state).toBe('PENDING_DRIVER');
  });
  test('database independently rejects a second active sensor pickup for one bin', async () => {
    const row = { binId, source: 'SENSOR', activeSensorTask: true, locationText: 'Test' };
    await Complaint.create({ ...row, complaintId: prefix + '-one' });
    await expect(Complaint.create({ ...row, complaintId: prefix + '-two' })).rejects.toMatchObject({ code: 11000 });
    // Citizen tasks remain unaffected by the partial unique index.
    await Complaint.create({ complaintId: prefix + '-citizen', citizenId: 'citizen-test', citizenName: 'Test', locationText: 'Test', binId });
  });
  test('a task-write failure aborts the reading, device cursor and bin disarm together', async () => {
    await send(frame(1));
    const original = Complaint.create;
    const spy = jest.spyOn(Complaint, 'create').mockRejectedValue(new Error('Injected task failure'));
    try { await expect(send(frame(2))).rejects.toThrow('Injected task failure'); }
    finally { spy.mockRestore(); }
    expect(Complaint.create).toBe(original);
    expect(await Reading.countDocuments({ deviceId, sequence: 2 })).toBe(0);
    expect((await Device.findOne({ deviceId })).lastSequence).toBe(1);
    expect((await Bin.findOne({ binId })).state.armed).toBe(true);
    await send(frame(2)); expect(await Complaint.countDocuments({ binId })).toBe(1);
  });
});
