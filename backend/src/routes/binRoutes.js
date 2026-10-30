const express = require('express');
const { z } = require('zod');
const { randomBytes, createHash } = require('crypto');
const Bin = require('../models/Bin');
const Device = require('../models/Device');
const { authenticate } = require('../middleware/authMiddleware');
const { requireRole } = require('../middleware/roleMiddleware');
const { withTransaction } = require('../services/transactionService');
const { DEFAULT_CONFIG, validConfig } = require('../services/binEvaluationService');
const { initializeSensorStorage, listBins } = require('../services/sensorService');
const router = express.Router();
router.use(authenticate, requireRole('MUNICIPAL_OFFICER'));
router.use((req, res, next) => {
  if (require('../services/syntheticDataService').isSyntheticMode()) return res.status(409).json({ success: false, message: 'Smart bins require real database mode.' });
  next();
});
const configSchema = z.object(Object.fromEntries(Object.keys(DEFAULT_CONFIG).map(k => [k, z.number().finite().optional()]))).strict();
const provisionSchema = z.object({ binId: z.string().regex(/^[A-Za-z0-9_-]{1,64}$/),
  deviceId: z.string().regex(/^[A-Za-z0-9_-]{1,64}$/), label: z.string().min(1).max(100),
  locationText: z.string().min(1).max(300), latitude: z.number().min(-90).max(90), longitude: z.number().min(-180).max(180),
  config: configSchema.optional() }).strict();
router.get('/', async (req, res, next) => { try { res.json({ success: true, data: await listBins() }); } catch (e) { next(e); } });
router.post('/', async (req, res, next) => {
  try {
    const parsed = provisionSchema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({ success: false, message: 'Invalid bin registration.' });
    const input = parsed.data, config = { ...DEFAULT_CONFIG, ...input.config };
    if (!validConfig(config)) return res.status(400).json({ success: false, message: 'Invalid sensor calibration or threshold configuration.' });
    await initializeSensorStorage();
    const secret = randomBytes(32).toString('base64url');
    await withTransaction(async session => {
      await Bin.create([{ binId: input.binId, label: input.label, locationText: input.locationText,
        latitude: input.latitude, longitude: input.longitude, config }], { session });
      await Device.create([{ deviceId: input.deviceId, binId: input.binId,
        tokenHash: createHash('sha256').update(secret).digest('hex') }], { session });
    });
    res.set('Cache-Control', 'no-store').status(201).json({ success: true,
      data: { binId: input.binId, deviceId: input.deviceId, deviceToken: `${input.deviceId}.${secret}`, config } });
  } catch (e) { next(e); }
});
router.patch('/:binId/config', async (req, res, next) => {
  try {
    const parsed = configSchema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({ success: false, message: 'Invalid configuration fields.' });
    await withTransaction(async session => {
      const bin = await Bin.findOne({ binId: req.params.binId }).session(session);
      if (!bin) throw Object.assign(new Error('Bin not found.'), { statusCode: 404 });
      const config = { ...bin.config, ...parsed.data };
      if (!validConfig(config)) throw Object.assign(new Error('Invalid calibration or thresholds.'), { statusCode: 400 });
      bin.config = config;
      bin.state = { ...bin.state, samples: [], candidate: null, count: 0, since: null, lastAt: null };
      bin.fillPercent = null; bin.lastReadingValid = false; bin.lastReadingReason = 'RECALIBRATING';
      await bin.save({ session });
    });
    res.json({ success: true });
  } catch (e) { next(e); }
});
router.post('/devices/:deviceId/rotate', async (req, res, next) => {
  try {
    const secret = randomBytes(32).toString('base64url');
    const device = await Device.findOneAndUpdate({ deviceId: req.params.deviceId }, {
      $set: { tokenHash: createHash('sha256').update(secret).digest('hex'), sessionId: null, enabled: true }
    }, { new: true });
    if (!device) return res.status(404).json({ success: false, message: 'Device not found.' });
    res.set('Cache-Control', 'no-store').json({ success: true, data: { deviceToken: `${device.deviceId}.${secret}` } });
  } catch (e) { next(e); }
});
module.exports = router;
