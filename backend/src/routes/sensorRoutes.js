const router = require('express').Router();
const rateLimit = require('express-rate-limit');
const { authenticateDevice } = require('../middleware/deviceAuthMiddleware');
const { beginSession, ingest } = require('../services/sensorService');
router.use((req, res, next) => {
  if (require('../services/syntheticDataService').isSyntheticMode()) return res.status(409).json({ success: false, message: 'Smart bins require real database mode.' });
  next();
});
router.use(rateLimit({ windowMs: 60000, max: 120, standardHeaders: true, legacyHeaders: false }));
router.use(authenticateDevice);
router.post('/session', async (req, res, next) => {
  try { res.json({ success: true, data: await beginSession(req.deviceId) }); } catch (e) { next(e); }
});
router.post('/readings', async (req, res, next) => {
  try { res.json({ success: true, data: await ingest(req.deviceId, req.body) }); } catch (e) { next(e); }
});
module.exports = router;
