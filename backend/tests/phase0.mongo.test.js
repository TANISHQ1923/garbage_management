// Real MongoDB tests: never use MONGODB_URI or a non-loopback target.
// Opt in with PHASE0_TEST_MONGODB_URI=mongodb://127.0.0.1:27017/phase0_test?replicaSet=rs0
const mongoose = require('mongoose');
const uri = process.env.PHASE0_TEST_MONGODB_URI;
if (uri && !/^mongodb:\/\/(127\.0\.0\.1|localhost):\d+\/phase0_test(?:\?|$)/.test(uri)) {
  throw new Error('Integration tests require an explicit loopback phase0_test database.');
}
const run = uri ? describe : describe.skip;
run('REAL MongoDB Phase 0 transaction regression', () => {
  const Driver = require('../src/models/Driver');
  const Complaint = require('../src/models/Complaint');
  const { reserveAssignment } = require('../src/services/dispatchService');
  const env = require('../src/config/env');
  const prefix = `phase0-${Date.now()}-`;
  beforeAll(async () => {
    env.USE_SYNTHETIC_DATA = false; process.env.USE_SYNTHETIC_DATA = 'false';
    await mongoose.connect(uri, { autoIndex: false, autoCreate: false, serverSelectionTimeoutMS: 3000 });
    // Create collections before a transaction (also supports older MongoDB servers).
    for (const model of [Driver, Complaint, require('../src/models/User'), require('../src/models/Notification')]) {
      await model.createCollection();
    }
  });
  afterAll(async () => {
    if (mongoose.connection.readyState === 1) {
      await Driver.deleteMany({ driverId: new RegExp(`^${prefix}`) });
      await Complaint.deleteMany({ complaintId: new RegExp(`^${prefix}`) });
      await require('../src/models/Notification').deleteMany({ complaintId: new RegExp(`^${prefix}`) });
    }
    await mongoose.disconnect();
  });
  const makeDriver = suffix => Driver.create({ driverId: prefix + suffix, name: 'Integration Driver', phone: 'test',
    vehicleNumber: 'test', availability: 'AVAILABLE', currentLocation: { latitude: 18.5, longitude: 73.8, timestamp: Date.now(), source: 'DEVICE' } });
  const makeTask = suffix => Complaint.create({ complaintId: prefix + suffix, citizenId: prefix + 'citizen',
    citizenName: 'Test', locationText: 'Test', latitude: 18.5, longitude: 73.8, status: 'SUBMITTED' });
  test('concurrent pickups cannot reserve one driver twice', async () => {
    const d = await makeDriver('race-driver'); const a = await makeTask('a'); const b = await makeTask('b');
    const results = await Promise.allSettled([reserveAssignment(a.complaintId, d.driverId), reserveAssignment(b.complaintId, d.driverId)]);
    expect(results.filter(r => r.status === 'fulfilled')).toHaveLength(1);
    expect(await Complaint.countDocuments({ assignedDriverId: d.driverId, status: 'ASSIGNED' })).toBe(1);
  });
  test('a real transaction abort restores driver availability after task validation failure', async () => {
    const d = await makeDriver('rollback-driver'); const c = await makeTask('rollback-task');
    // Insert a legacy invalid field directly in the isolated test DB; save must fail after claiming driver.
    await Complaint.collection.updateOne({ complaintId: c.complaintId }, { $unset: { citizenName: '' } });
    await expect(reserveAssignment(c.complaintId, d.driverId)).rejects.toThrow();
    expect((await Driver.findOne({ driverId: d.driverId })).availability).toBe('AVAILABLE');
    expect((await Complaint.findOne({ complaintId: c.complaintId })).status).toBe('SUBMITTED');
  });
});
