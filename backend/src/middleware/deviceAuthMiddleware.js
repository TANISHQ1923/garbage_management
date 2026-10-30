const { createHash, timingSafeEqual } = require('crypto');
const Device = require('../models/Device');
const authenticateDevice = async (req, res, next) => {
  try {
    const raw = req.headers.authorization || '';
    const match = /^Bearer ([A-Za-z0-9_-]{1,64})\.([A-Za-z0-9_-]{43})$/.exec(raw);
    if (!match) return res.status(401).json({ success: false, message: 'Device authentication required.' });
    const device = await Device.findOne({ deviceId: match[1], enabled: true }).select('+tokenHash');
    const hash = createHash('sha256').update(match[2]).digest();
    const expected = device && Buffer.from(device.tokenHash, 'hex');
    if (!expected || expected.length !== hash.length || !timingSafeEqual(expected, hash)) {
      return res.status(401).json({ success: false, message: 'Invalid device credential.' });
    }
    req.deviceId = device.deviceId; next();
  } catch (_) { res.status(503).json({ success: false, message: 'Device authentication unavailable.' }); }
};
module.exports = { authenticateDevice };
