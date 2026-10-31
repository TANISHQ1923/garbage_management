const StoredMedia = require('../models/StoredMedia');
const syntheticDataService = require('./syntheticDataService');
const { getDBStatus } = require('../config/db');

const badEvidence = message => Object.assign(new Error(message), { statusCode: 400 });
async function requireStoredEvidence(complaint, user, url, folder) {
  if (typeof url !== 'string' || (!url.startsWith('https://') && !url.startsWith('http://'))) {
    throw badEvidence('Photo must be uploaded successfully before it can be used as evidence. Local previews are not stored evidence.');
  }
  if (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected) {
    return {
      publicId: `synthetic-evidence-${Date.now()}`,
      url,
      resourceType: 'image'
    };
  }
  const media = await StoredMedia.findOne({ url, complaintId: complaint.complaintId, folder,
    uploadedBy: String(user.id || user._id), resourceType: 'image' });
  if (!media?.publicId) throw badEvidence('Stored photo reference was not found for this task and driver. Select the photo again and retry its upload.');
  return media;
}
module.exports = { requireStoredEvidence };
