const mongoose = require('mongoose');
const schema = new mongoose.Schema({
  deviceId: { type: String, required: true }, binId: { type: String, required: true },
  sessionId: { type: String, required: true }, sequence: { type: Number, required: true },
  uptimeMs: { type: Number, required: true }, capturedAtMs: { type: Number, required: true },
  receivedAt: { type: Number, required: true }, distanceCm: { type: Number, default: null },
  valid: { type: Boolean, required: true }, reason: String, eligible: Boolean,
  payloadHash: { type: String, required: true }
}, { autoIndex: false, autoCreate: false });
schema.index({ deviceId: 1, sessionId: 1, sequence: 1 }, { unique: true, name: 'unique_device_reading' });
schema.index({ binId: 1, capturedAtMs: -1 });
module.exports = mongoose.model('SensorReading', schema);
