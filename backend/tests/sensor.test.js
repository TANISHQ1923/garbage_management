// Pure/repository/controller tests; MongoDB operations are mocked. No atomicity claim.
jest.mock('../src/config/db', () => ({ getDBStatus: jest.fn(() => ({ isConnected: true })) }));
jest.mock('../src/services/transactionService', () => ({ withTransaction: jest.fn(fn => fn({ testSession: true })) }));
const request = require('supertest');
const jwt = require('jsonwebtoken');
const { createHash } = require('crypto');
const app = require('../src/app');
const env = require('../src/config/env');
const Bin = require('../src/models/Bin');
const Device = require('../src/models/Device');
const Reading = require('../src/models/SensorReading');
const Complaint = require('../src/models/Complaint');
const User = require('../src/models/User');
const { DEFAULT_CONFIG, evaluate, fillFor, healthFor, validConfig } = require('../src/services/binEvaluationService');
const { ingest } = require('../src/services/sensorService');
const chain = value => ({ session: jest.fn(async () => value), select: jest.fn(async () => value) });
const sessionId = '11111111-1111-4111-8111-111111111111';
let bin, device, readings, tasks;
const frame = (sequence, distanceCm = 3, valid = true) => ({ sessionId, sequence, uptimeMs: sequence * 2000,
  capturedAtMs: 1000000 + sequence * 2000, distanceCm, valid, buffered: false, reason: valid ? 'OK' : 'NO_ECHO' });
const send = async (sequence, distance = 3, valid = true) => { const r = frame(sequence, distance, valid); return ingest('DEV1', r, r.capturedAtMs); };
beforeEach(() => {
  jest.restoreAllMocks();
  readings = []; tasks = [];
  bin = { binId: 'BIN1', label: 'Test', latitude: 18.5, longitude: 73.8, locationText: 'Site', config: { ...DEFAULT_CONFIG },
    state: { armed: true, samples: [] }, save: jest.fn().mockResolvedValue() };
  device = { deviceId: 'DEV1', binId: 'BIN1', sessionId, enabled: true, lastSequence: -1, lastUptimeMs: -1,
    tokenHash: createHash('sha256').update('x'.repeat(43)).digest('hex'), save: jest.fn().mockResolvedValue() };
  jest.spyOn(Bin, 'findOne').mockImplementation(() => chain(bin));
  jest.spyOn(Device, 'findOne').mockImplementation(() => chain(device));
  jest.spyOn(Reading, 'findOne').mockImplementation(key => chain(readings.find(r => r.sequence === key.sequence && r.sessionId === key.sessionId) || null));
  jest.spyOn(Reading, 'create').mockImplementation(async rows => { readings.push(...rows); return rows; });
  jest.spyOn(Complaint, 'exists').mockImplementation(() => chain(tasks.find(t => t.activeSensorTask) || null));
  jest.spyOn(Complaint, 'create').mockImplementation(async rows => { tasks.push(...rows); return rows; });
  jest.spyOn(Complaint, 'updateOne').mockImplementation(async (filter, update) => {
    Object.assign(tasks.find(t => t.complaintId === filter.complaintId) || {}, update.$set); return {};
  });
  jest.spyOn(Complaint.collection, 'indexExists').mockResolvedValue(true);
  jest.spyOn(Reading.collection, 'indexExists').mockResolvedValue(true);
});
test('calibration produces 80% at 3.18 cm and validates threshold reachability', () => {
  expect(fillFor(3.18, DEFAULT_CONFIG)).toBeCloseTo(80);
  expect(fillFor(15.9, DEFAULT_CONFIG)).toBe(0);
  expect(validConfig(DEFAULT_CONFIG)).toBe(true);
  expect(validConfig({ ...DEFAULT_CONFIG, fullThreshold: 95 })).toBe(false); // inside HC-SR04 blind range
});
test.each([null, 0, -1, 1.9, 16, 500])('invalid distance %s never confirms full or empty', value => {
  const result = evaluate({ armed: false, samples: [] }, { ...frame(1), distanceCm: value, valid: true }, DEFAULT_CONFIG);
  expect(result.valid).toBe(false); expect(result.state.armed).toBe(false);
  expect(result.full).toBe(false); expect(result.empty).toBe(false);
});
test('missing/incorrect device token is rejected', async () => {
  expect((await request(app).post('/api/device/readings').send(frame(1))).status).toBe(401);
  expect((await request(app).post('/api/device/readings').set('Authorization', 'Bearer DEV1.' + 'y'.repeat(43)).send(frame(1))).status).toBe(401);
});
test('bin provisioning is restricted to authenticated officers', async () => {
  expect((await request(app).post('/api/bins').send({})).status).toBe(401);
  jest.spyOn(User, 'findById').mockResolvedValue({ id: '507f191e810c19729de860ea', role: 'CITIZEN' });
  const token = jwt.sign({ id: '507f191e810c19729de860ea' }, env.JWT_SECRET);
  expect((await request(app).post('/api/bins').set('Authorization', 'Bearer ' + token).send({})).status).toBe(403);
});
test('authenticated device resolves its bin; arbitrary payload binId is rejected', async () => {
  const r = frame(1);
  expect((await request(app).post('/api/device/readings').set('Authorization', 'Bearer DEV1.' + 'x'.repeat(43))
    .send({ ...r, binId: 'OTHER' })).status).toBe(400);
  const result = await ingest('DEV1', r, r.capturedAtMs);
  expect(result.binId).toBe('BIN1'); expect(readings[0].binId).toBe('BIN1');
});
test('no echo preserves last valid fill and breaks confirmation', async () => {
  await send(1, 15.9); const previous = bin.fillPercent;
  await send(2, null, false);
  expect(bin.fillPercent).toBe(previous); expect(bin.lastReadingValid).toBe(false);
  expect(bin.state.samples).toEqual([]); expect(bin.state.count).toBe(0);
});
test('a noise spike cannot create a task', async () => {
  for (let i = 1; i <= 10; i++) await send(i, i === 5 ? 3 : 15.9);
  expect(tasks).toHaveLength(0);
});
test('full requires filter warmup, count, and sustained time; creates one citizen-free task', async () => {
  for (let i = 1; i <= 7; i++) await send(i);
  expect(tasks).toHaveLength(0);
  await send(8); expect(tasks).toHaveLength(1);
  expect(tasks[0].source).toBe('SENSOR'); expect(tasks[0].citizenId).toBeUndefined();
  expect(tasks[0].dispatchRetry.state).toBe('PENDING_DRIVER');
  for (let i = 9; i <= 15; i++) await send(i);
  expect(tasks).toHaveLength(1);
});
test('duplicate retries do not increase streaks or mutate contact and conflicting content returns 409', async () => {
  await send(1); const count = bin.state.count;
  expect((await send(1)).duplicate).toBe(true); expect(bin.state.count).toBe(count); expect(readings).toHaveLength(1);
  const changed = frame(1, 10);
  await expect(ingest('DEV1', changed, changed.capturedAtMs)).rejects.toMatchObject({ statusCode: 409 });
});
test('buffered, old-session, out-of-order and stale readings are stored but cannot change fill', async () => {
  await send(4, 15.9);
  const r = frame(5);
  expect((await ingest('DEV1', { ...r, buffered: true }, r.capturedAtMs)).eligible).toBe(false);
  expect((await send(3)).eligible).toBe(false);
  expect((await ingest('DEV1', { ...frame(6), sessionId: '22222222-2222-4222-8222-222222222222' }, frame(6).capturedAtMs)).eligible).toBe(false);
  expect((await ingest('DEV1', frame(7), frame(7).capturedAtMs + 10001)).eligible).toBe(false);
  expect(bin.fillPercent).toBe(0); expect(tasks).toHaveLength(0);
});
test('gaps and invalid samples reset sustained thresholds', async () => {
  for (let i = 1; i < 7; i++) await send(i);
  await send(8, null, false); await send(9);
  expect(tasks).toHaveLength(0);
  await send(20); expect(bin.state.count).toBe(0);
});
test('emptying rearms only after confirmation and is separate from driver cleanup', async () => {
  for (let i = 1; i <= 8; i++) await send(i);
  expect(bin.state.armed).toBe(false);
  await send(9, 15.9); expect(bin.state.armed).toBe(false);
  for (let i = 10; i <= 18; i++) await send(i, 15.9);
  expect(bin.state.armed).toBe(true); expect(tasks[0].sensorEmptyConfirmedAt).toBeGreaterThan(0);
  expect(tasks[0].status).toBe('SUBMITTED');
  for (let i = 19; i <= 28; i++) await send(i);
  expect(tasks).toHaveLength(1); // existing task still open, even after empty/refill
  tasks[0].activeSensorTask = false; tasks[0].status = 'VERIFIED';
  await send(29); expect(tasks).toHaveLength(2);
});
test('missing storage constraints fail closed', async () => {
  Complaint.collection.indexExists.mockResolvedValue(false);
  await expect(send(1)).rejects.toMatchObject({ statusCode: 503 });
  expect(Reading.create).not.toHaveBeenCalled();
});
test('health distinguishes invalid, stale and offline without changing fill', () => {
  const b = { ...bin, lastContactAt: 100000, lastMeasurementAt: 100000, lastValidAt: 100000, lastReadingValid: true, fillPercent: 84 };
  expect(healthFor(b, 100000)).toBe('ONLINE');
  expect(healthFor({ ...b, lastReadingValid: false }, 100000)).toBe('FAULT');
  expect(healthFor({ ...b, lastContactAt: 120000 }, 120000)).toBe('STALE');
  expect(healthFor(b, 160001)).toBe('OFFLINE'); expect(b.fillPercent).toBe(84);
});
