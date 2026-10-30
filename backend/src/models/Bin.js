const mongoose = require('mongoose');
const schema = new mongoose.Schema({
  binId: { type: String, required: true, unique: true },
  label: { type: String, required: true },
  locationText: { type: String, required: true },
  latitude: { type: Number, required: true },
  longitude: { type: Number, required: true },
  config: { type: mongoose.Schema.Types.Mixed, required: true },
  state: { type: mongoose.Schema.Types.Mixed, default: () => ({ armed: true, samples: [] }) },
  lastContactAt: { type: Number, default: null },
  lastMeasurementAt: { type: Number, default: null },
  lastValidAt: { type: Number, default: null },
  lastDistanceCm: { type: Number, default: null },
  fillPercent: { type: Number, default: null },
  lastReadingValid: { type: Boolean, default: false },
  lastReadingReason: { type: String, default: 'NO_DATA' },
  lastPickupTaskId: { type: String, default: null },
  sensorEmptyConfirmedAt: { type: Number, default: null }
}, { timestamps: true, autoIndex: false, autoCreate: false });
module.exports = mongoose.model('Bin', schema);
