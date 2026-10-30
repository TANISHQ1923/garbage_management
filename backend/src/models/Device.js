const mongoose = require('mongoose');
const schema = new mongoose.Schema({
  deviceId: { type: String, required: true, unique: true },
  binId: { type: String, required: true, unique: true },
  tokenHash: { type: String, required: true, select: false },
  enabled: { type: Boolean, default: true },
  sessionId: { type: String, default: null },
  sessionStartedAt: { type: Number, default: null },
  lastContactAt: { type: Number, default: null },
  lastSequence: { type: Number, default: -1 },
  lastUptimeMs: { type: Number, default: -1 }
}, { timestamps: true, autoIndex: false, autoCreate: false });
module.exports = mongoose.model('Device', schema);
