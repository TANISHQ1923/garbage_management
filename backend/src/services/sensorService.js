const { createHash, randomUUID } = require('crypto');
const Bin = require('../models/Bin');
const Device = require('../models/Device');
const SensorReading = require('../models/SensorReading');
const Complaint = require('../models/Complaint');
const { withTransaction } = require('./transactionService');
const { evaluate, distanceValid, healthFor } = require('./binEvaluationService');
const { z } = require('zod');
const failure = (message, statusCode = 400) => Object.assign(new Error(message), { statusCode });
const readingSchema = z.object({ sessionId: z.string().uuid(), sequence: z.number().int().min(0).max(4294967295),
  uptimeMs: z.number().int().min(0).max(4294967295), capturedAtMs: z.number().int().positive(),
  distanceCm: z.number().finite().nullable(), valid: z.boolean(), buffered: z.boolean(),
  reason: z.string().max(80).optional() }).strict();

async function initializeSensorStorage() {
  for (const model of [Bin, Device, SensorReading, Complaint]) { await model.createCollection(); await model.createIndexes(); }
}
async function assertSensorStorage() {
  if (!await Complaint.collection.indexExists('one_active_sensor_task_per_bin') ||
      !await SensorReading.collection.indexExists('unique_device_reading')) {
    throw failure('Sensor indexes are not initialized. Provision a bin before ingestion.', 503);
  }
}
async function beginSession(deviceId) {
  await assertSensorStorage();
  return withTransaction(async session => {
    const device = await Device.findOne({ deviceId, enabled: true }).session(session);
    if (!device) throw failure('Device disabled or unavailable.', 401);
    device.sessionId = randomUUID(); device.sessionStartedAt = Date.now();
    device.lastSequence = -1; device.lastUptimeMs = -1;
    await device.save({ session });
    const bin = await Bin.findOne({ binId: device.binId }).session(session);
    if (!bin) throw failure('Device bin is unavailable.', 409);
    bin.state = { ...bin.state, samples: [], candidate: null, count: 0, since: null, lastAt: null };
    await bin.save({ session });
    return { sessionId: device.sessionId, serverTimeMs: device.sessionStartedAt };
  });
}

async function ingest(deviceId, input, now = Date.now()) {
  const parsed = readingSchema.safeParse(input);
  if (!parsed.success) throw failure('Malformed sensor reading. Check the bridge protocol.');
  const reading = parsed.data;
  await assertSensorStorage();
  const key = { deviceId, sessionId: reading.sessionId, sequence: reading.sequence };
  const payloadHash = createHash('sha256').update(JSON.stringify(reading)).digest('hex');
  const run = async () => withTransaction(async session => {
    const existing = await SensorReading.findOne(key).session(session);
    if (existing) {
      if (existing.payloadHash !== payloadHash) throw failure('Reading identity reused with different content.', 409);
      return { duplicate: true, eligible: existing.eligible, reason: existing.reason };
    }
    const device = await Device.findOne({ deviceId, enabled: true }).session(session);
    if (!device) throw failure('Device disabled or unavailable.', 401);
    const bin = await Bin.findOne({ binId: device.binId }).session(session);
    if (!bin) throw failure('Device bin is unavailable.', 409);
    // Every ingestion writes the device and bin documents, serializing concurrent readings.
    device.lastContactAt = now; bin.lastContactAt = now;
    const currentSession = reading.sessionId === device.sessionId;
    const ordered = reading.sequence > device.lastSequence && reading.uptimeMs > device.lastUptimeMs;
    const fresh = !reading.buffered && reading.capturedAtMs <= now && now - reading.capturedAtMs <= bin.config.maxReadingAgeMs;
    const increasingTime = bin.state.lastAt == null || reading.capturedAtMs > bin.state.lastAt;
    const eligible = currentSession && ordered && fresh && increasingTime;
    let reason = !currentSession ? 'OLD_SESSION' : !ordered || !increasingTime ? 'OUT_OF_ORDER' : !fresh ? 'BUFFERED_OR_STALE' : 'OK';
    const valid = reading.valid && distanceValid(reading.distanceCm, bin.config);
    if (eligible) {
      device.lastSequence = reading.sequence; device.lastUptimeMs = reading.uptimeMs;
      const evaluation = evaluate(bin.state, { ...reading, valid }, bin.config);
      bin.state = evaluation.state; bin.lastMeasurementAt = reading.capturedAtMs;
      bin.lastReadingValid = evaluation.valid;
      bin.lastReadingReason = evaluation.valid ? 'OK' : (reading.valid ? 'OUT_OF_RANGE' : (reading.reason || 'INVALID'));
      reason = bin.lastReadingReason;
      if (evaluation.valid) {
        bin.lastValidAt = reading.capturedAtMs; bin.lastDistanceCm = reading.distanceCm;
        bin.fillPercent = evaluation.fillPercent;
      }
      if (evaluation.empty) {
        bin.sensorEmptyConfirmedAt = reading.capturedAtMs;
        if (bin.lastPickupTaskId) await Complaint.updateOne({ complaintId: bin.lastPickupTaskId, source: 'SENSOR',
          activeSensorTask: true, sensorEmptyConfirmedAt: null },
          { $set: { sensorEmptyConfirmedAt: reading.capturedAtMs } }, { session });
      }
      if (evaluation.full && bin.state.armed) {
        const active = await Complaint.exists({ binId: bin.binId, source: 'SENSOR', activeSensorTask: true }).session(session);
        if (!active) {
          const complaintId = `BIN-${randomUUID()}`;
          await Complaint.create([{ complaintId, source: 'SENSOR', binId: bin.binId, activeSensorTask: true,
            garbageType: 'OVERFLOWING_BIN', description: `Sustained full threshold at ${bin.label}.`,
            latitude: bin.latitude, longitude: bin.longitude, locationText: bin.locationText,
            status: 'SUBMITTED', dispatchRetry: { state: 'PENDING_DRIVER', nextAttemptAt: now, attempts: 0, reason: 'NEW_SENSOR_PICKUP' },
            statusHistory: [{ status: 'SUBMITTED', timestamp: now, changedBy: 'Bin sensor', role: 'SYSTEM', note: 'Sustained fill threshold confirmed.' }]
          }], { session });
          bin.lastPickupTaskId = complaintId; bin.state = { ...bin.state, armed: false };
        }
      }
    }
    await SensorReading.create([{ ...key, binId: device.binId, uptimeMs: reading.uptimeMs,
      capturedAtMs: reading.capturedAtMs, receivedAt: now, distanceCm: reading.distanceCm, valid, eligible, reason, payloadHash }], { session });
    await device.save({ session }); await bin.save({ session });
    return { duplicate: false, eligible, valid, reason, binId: bin.binId, fillPercent: bin.fillPercent, pickupTaskId: bin.lastPickupTaskId };
  });
  // A duplicate-key race aborts its transaction; retry in a fresh snapshot to read the committed winner.
  for (let attempt = 0; ; attempt++) {
    try { return await run(); } catch (error) { if (error.code !== 11000 || attempt >= 2) throw error; }
  }
}

async function listBins(now = Date.now()) {
  const bins = await Bin.find().sort({ binId: 1 }).lean();
  const tasks = await Complaint.find({ source: 'SENSOR', complaintId: { $in: bins.map(b => b.lastPickupTaskId).filter(Boolean) } }).lean();
  return bins.map(b => { const t = tasks.find(t => t.complaintId === b.lastPickupTaskId);
    return { binId: b.binId, label: b.label, latitude: b.latitude, longitude: b.longitude, locationText: b.locationText,
      fillPercent: b.fillPercent, lastContactAt: b.lastContactAt, lastMeasurementAt: b.lastMeasurementAt, lastValidAt: b.lastValidAt,
      health: healthFor(b, now), readingReason: b.lastReadingReason, sensorEmptyConfirmedAt: b.sensorEmptyConfirmedAt,
      pickupTaskId: b.lastPickupTaskId, pickupStatus: t?.status || null,
      dispatchState: t?.dispatchRetry?.state || null, dispatchReason: t?.dispatchRetry?.reason || null,
      nextRetryAt: t?.dispatchRetry?.nextAttemptAt || null, assignedDriverName: t?.assignedDriverName || null };
  });
}
module.exports = { readingSchema, initializeSensorStorage, assertSensorStorage, beginSession, ingest, listBins };
