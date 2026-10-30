// Unit/controller tests only. Sessions and database operations are mocked.
jest.mock('../src/config/db', () => ({ getDBStatus: jest.fn(() => ({ isConnected: true })) }));
jest.mock('../src/services/transactionService', () => ({ withTransaction: jest.fn(fn => fn({})) }));
const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const db = require('../src/config/db');
const User = require('../src/models/User');
const Driver = require('../src/models/Driver');
const Complaint = require('../src/models/Complaint');
const Notification = require('../src/models/Notification');
const synthetic = require('../src/services/syntheticDataService');
const dispatch = require('../src/services/dispatchService');
const { commitTransition } = require('../src/services/taskTransitionService');
const chain = value => ({ session: jest.fn().mockResolvedValue(value) });
const citizen = { id: '507f191e810c19729de860ea', name: 'Citizen', role: 'CITIZEN' };
const officer = { ...citizen, role: 'MUNICIPAL_OFFICER' };
const driverUser = { ...citizen, role: 'GARBAGE_DRIVER', driverId: 'DRV-1' };
const token = user => jwt.sign({ id: user.id }, env.JWT_SECRET);
const task = () => ({ complaintId: 'CMP-1', citizenId: citizen.id, citizenName: citizen.name,
  locationText: 'Bin site', latitude: 18.5, longitude: 73.8, status: 'SUBMITTED', statusHistory: [], save: jest.fn().mockResolvedValue() });
const driver = () => ({ driverId: 'DRV-1', name: 'Driver', vehicleNumber: 'TEST', availability: 'AVAILABLE',
  currentLocation: { latitude: 18.5, longitude: 73.8, timestamp: Date.now(), source: 'DEVICE' } });
beforeEach(() => {
  jest.restoreAllMocks(); env.USE_SYNTHETIC_DATA = false; process.env.USE_SYNTHETIC_DATA = 'false';
  db.getDBStatus.mockReturnValue({ isConnected: true });
  jest.spyOn(User, 'findById').mockResolvedValue(citizen);
  jest.spyOn(User, 'findOne').mockResolvedValue(driverUser);
  jest.spyOn(Notification, 'create').mockResolvedValue({});
  jest.spyOn(Complaint, 'updateMany').mockResolvedValue({});
});
test('database outage returns 503 and does not opt into synthetic mode', async () => {
  db.getDBStatus.mockReturnValue({ isConnected: false });
  expect(synthetic.isSyntheticMode()).toBe(false);
  const r = await request(app).get('/api/dashboard/citizen').set('Authorization', `Bearer ${token(citizen)}`);
  expect(r.status).toBe(503);
});
test.each(['MUNICIPAL_OFFICER', 'GARBAGE_DRIVER', 'ADMIN'])('public signup rejects %s', async role => {
  const create = jest.spyOn(User, 'create');
  expect((await request(app).post('/api/auth/register').send({ name: 'Test', email: 'test@example.invalid', password: 'test-password', role })).status).toBe(403);
  expect(create).not.toHaveBeenCalled();
});
test('mock bearer token is rejected in normal mode', async () => {
  expect((await request(app).get('/api/auth/me').set('Authorization', 'Bearer mock-token-mock-citizen-001')).status).toBe(401);
});
test('normal login does not consult demo credentials on database errors', async () => {
  User.findOne.mockImplementation(() => ({ select: () => Promise.reject(new Error('Unavailable')) }));
  const demo = jest.spyOn(synthetic, 'getSyntheticUserByEmail');
  expect((await request(app).post('/api/auth/login').send({ email: 'test@example.invalid', password: 'test' })).status).toBe(500);
  expect(demo).not.toHaveBeenCalled();
});
test('unconfigured media is an error, never sample evidence', async () => {
  const old = env.CLOUDINARY_API_KEY; env.CLOUDINARY_API_KEY = '';
  try { await expect(require('../src/services/mediaService').uploadMedia({ mimetype: 'image/jpeg', size: 10, buffer: Buffer.alloc(10) })).rejects.toMatchObject({ statusCode: 503 }); }
  finally { env.CLOUDINARY_API_KEY = old; }
});
test('fresh measured location required, legacy fallback/stale/future/outside-radius rejected', () => {
  const d = driver(); const now = Date.now();
  expect(dispatch.rankCandidateDrivers(task(), [d], now)).toHaveLength(1);
  delete d.currentLocation.source; expect(dispatch.rankCandidateDrivers(task(), [d], now)).toHaveLength(0);
  d.currentLocation.source = 'DEVICE'; d.currentLocation.timestamp = now - env.DRIVER_LOCATION_MAX_AGE_MS - 1;
  expect(dispatch.hasFreshLocation(d, now)).toBe(false);
  d.currentLocation.timestamp = now + 1; expect(dispatch.hasFreshLocation(d, now)).toBe(false);
  d.currentLocation.timestamp = now; d.currentLocation.latitude = 0;
  expect(dispatch.rankCandidateDrivers(task(), [d], now)).toHaveLength(0);
});
test('reservation saves driver and task using the same session', async () => {
  const c = task(), d = driver();
  jest.spyOn(Complaint, 'findOne').mockReturnValue(chain(c));
  jest.spyOn(Complaint, 'exists').mockReturnValue(chain(null));
  jest.spyOn(Driver, 'findOne').mockReturnValue(chain(d));
  const claim = jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValue(d);
  const r = await dispatch.reserveAssignment(c.complaintId, d.driverId);
  expect(r.assigned).toBe(true); expect(c.status).toBe('ASSIGNED');
  expect(c.save.mock.calls[0][0].session).toBe(claim.mock.calls[0][2].session);
});
test('repeated same assignment is idempotent without another claim or notification', async () => {
  const c = { ...task(), status: 'ASSIGNED', assignedDriverId: 'DRV-1' };
  jest.spyOn(Complaint, 'findOne').mockReturnValue(chain(c));
  const claim = jest.spyOn(Driver, 'findOneAndUpdate');
  expect((await dispatch.reserveAssignment('CMP-1', 'DRV-1')).idempotent).toBe(true);
  expect(claim).not.toHaveBeenCalled(); expect(Notification.create).not.toHaveBeenCalled();
});
test('existing active task prevents another driver reservation', async () => {
  jest.spyOn(Complaint, 'findOne').mockReturnValue(chain(task()));
  jest.spyOn(Complaint, 'exists').mockReturnValue(chain({ complaintId: 'OTHER' }));
  jest.spyOn(Driver, 'findOne').mockReturnValue(chain(driver()));
  const claim = jest.spyOn(Driver, 'findOneAndUpdate');
  await expect(dispatch.reserveAssignment('CMP-1', 'DRV-1')).rejects.toMatchObject({ statusCode: 409 });
  expect(claim).not.toHaveBeenCalled();
});
test('task save failure escapes transaction; no success or notification', async () => {
  const c = task(); c.save.mockRejectedValue(new Error('save failed'));
  jest.spyOn(Complaint, 'findOne').mockReturnValue(chain(c));
  jest.spyOn(Complaint, 'exists').mockReturnValue(chain(null));
  jest.spyOn(Driver, 'findOne').mockReturnValue(chain(driver()));
  jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValue(driver());
  await expect(dispatch.reserveAssignment('CMP-1', 'DRV-1')).rejects.toThrow('save failed');
  expect(Notification.create).not.toHaveBeenCalled();
});
test('completion keeps driver BUSY when a legacy second active task exists', async () => {
  const current = { ...task(), status: 'PICKUP_IN_PROGRESS', assignedDriverId: 'DRV-1' };
  jest.spyOn(Complaint, 'findOne').mockReturnValue(chain(current));
  jest.spyOn(Driver, 'findOne').mockReturnValue(chain(driver()));
  jest.spyOn(Complaint, 'exists').mockReturnValue(chain({ complaintId: 'OTHER' }));
  const update = jest.spyOn(Driver, 'updateOne').mockResolvedValue({});
  await commitTransition({ ...current, status: 'CLEANED' }, 'PICKUP_IN_PROGRESS', 'DRV-1');
  expect(update.mock.calls[0][1].$set.availability).toBe('BUSY');
});
test('citizen submission is persisted even with no eligible driver', async () => {
  jest.spyOn(Complaint, 'create').mockImplementation(async data => ({ ...data, toObject: () => data }));
  jest.spyOn(Driver, 'find').mockResolvedValue([]);
  const r = await request(app).post('/api/complaints').set('Authorization', `Bearer ${token(citizen)}`)
    .send({ locationText: 'Site', latitude: 18.5, longitude: 73.8 });
  expect(r.status).toBe(201); expect(r.body.dispatchResult.assigned).toBe(false);
  expect(Complaint.create).toHaveBeenCalled();
});
test('emergency escalation calls the bound notification service', async () => {
  jest.spyOn(Complaint, 'findOne').mockResolvedValue(task());
  const r = await request(app).post('/api/complaints/CMP-1/emergency').set('Authorization', `Bearer ${token(citizen)}`);
  expect(r.status).toBe(200); expect(Notification.create).toHaveBeenCalled();
});
test('officer verification commits CLEANED to VERIFIED', async () => {
  User.findById.mockResolvedValue(officer);
  const c = { ...task(), status: 'CLEANED' };
  jest.spyOn(Complaint, 'findOne').mockReturnValueOnce(Promise.resolve(c)).mockReturnValueOnce(chain({ ...c }));
  const r = await request(app).put('/api/officer/complaints/CMP-1/status').set('Authorization', `Bearer ${token(officer)}`).send({ status: 'VERIFIED' });
  expect(r.status).toBe(200); expect(c.status).toBe('VERIFIED');
});
test('driver completion requires photo evidence before any state change', async () => {
  User.findById.mockResolvedValue(driverUser);
  const r = await request(app).put('/api/driver/complaints/CMP-1/complete').set('Authorization', `Bearer ${token(driverUser)}`).send({});
  expect(r.status).toBe(400);
});

test('public citizen registration discards injected staff identity fields', async () => {
  User.findOne.mockResolvedValue(null);
  const create = jest.spyOn(User, 'create').mockImplementation(async data => ({ ...citizen, toJSON: () => ({ ...citizen, email: data.email }) }));
  const r = await request(app).post('/api/auth/register').send({ name: 'Citizen', email: 'citizen@example.invalid', password: 'local-test-password', employeeId: 'injected', driverId: 'DRV-injected' });
  expect(r.status).toBe(201);
  expect(create.mock.calls[0][0]).toMatchObject({ role: 'CITIZEN', driverId: null, employeeId: null });
});
test('controlled provisioning generates exactly the same driver ID on both records', async () => {
  jest.spyOn(User, 'exists').mockReturnValue(chain(null));
  const userCreate = jest.spyOn(User, 'create').mockResolvedValue([{ _id: citizen.id }]);
  const driverCreate = jest.spyOn(Driver, 'create').mockResolvedValue([{}]);
  await require('../src/scripts/provisionStaff').provisionStaff({ name: 'Driver', email: 'driver@example.invalid', password: 'local-test-password', role: 'GARBAGE_DRIVER', employeeId: 'EMP-TEST', phone: 'test', vehicleNumber: 'test' });
  expect(userCreate.mock.calls[0][0][0].driverId).toBe(driverCreate.mock.calls[0][0][0].driverId);
  expect(driverCreate.mock.calls[0][0][0].currentLocation).toBeNull();
});
test('standalone transaction failure returns clear 503 and closes session', async () => {
  const mongoose = require('mongoose');
  const session = { withTransaction: jest.fn().mockRejectedValue(Object.assign(new Error('Transaction numbers are only allowed on a replica set'), { code: 20 })), endSession: jest.fn() };
  jest.spyOn(mongoose, 'startSession').mockResolvedValue(session);
  const actual = jest.requireActual('../src/services/transactionService');
  await expect(actual.withTransaction(async () => {})).rejects.toMatchObject({ statusCode: 503 });
  expect(session.endSession).toHaveBeenCalled();
});
test('driver cannot set AVAILABLE while a pickup is active', async () => {
  User.findById.mockResolvedValue(driverUser);
  jest.spyOn(Complaint, 'exists').mockReturnValue(chain({ complaintId: 'ACTIVE' }));
  const update = jest.spyOn(Driver, 'findOneAndUpdate');
  const r = await request(app).put('/api/driver/availability').set('Authorization', `Bearer ${token(driverUser)}`).send({ availability: 'AVAILABLE' });
  expect(r.status).toBe(409); expect(update).not.toHaveBeenCalled();
});
test('unprovisioned driver cannot manufacture a Driver record using location upload', async () => {
  User.findById.mockResolvedValue({ ...citizen, role: 'GARBAGE_DRIVER' });
  const update = jest.spyOn(Driver, 'findOneAndUpdate');
  const r = await request(app).put('/api/driver/location').set('Authorization', `Bearer ${token(driverUser)}`).send({ latitude: 18.5, longitude: 73.8, capturedAt: Date.now() });
  expect(r.status).toBe(409); expect(update).not.toHaveBeenCalled();
});
