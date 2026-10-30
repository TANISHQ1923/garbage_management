const mongoose = require('mongoose');
const schema = new mongoose.Schema({
  publicId: { type: String, required: true, unique: true },
  url: { type: String, required: true },
  resourceType: { type: String, required: true },
  complaintId: { type: String, required: true, index: true },
  uploadedBy: { type: String, required: true },
  folder: { type: String, enum: ['before-cleaning', 'after-cleaning'], required: true }
}, { timestamps: true });
module.exports = mongoose.model('StoredMedia', schema);
