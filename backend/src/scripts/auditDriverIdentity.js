// Read-only inspection. Explicit target prevents accidentally auditing the configured production DB.
const mongoose = require('mongoose');
const User = require('../models/User');
const Driver = require('../models/Driver');

async function auditDriverIdentity() {
  const users = await User.find({ role: 'GARBAGE_DRIVER' }).select('_id driverId employeeId').lean();
  const drivers = await Driver.find().select('driverId employeeId').lean();
  return users.map(user => ({ userId: String(user._id), driverId: user.driverId || null,
    exactDriverMatches: drivers.filter(d => d.driverId === user.driverId).length,
    sameIdentityUsers: users.filter(u => user.driverId && u.driverId === user.driverId).length,
    employeeCandidateCount: user.employeeId ? drivers.filter(d => d.employeeId === user.employeeId).length : 0 }));
}
if (require.main === module) (async () => {
  if (!process.env.IDENTITY_AUDIT_URI) throw new Error('Set IDENTITY_AUDIT_URI explicitly.');
  await mongoose.connect(process.env.IDENTITY_AUDIT_URI, { autoIndex: false, autoCreate: false });
  try { console.log(JSON.stringify(await auditDriverIdentity(), null, 2)); }
  finally { await mongoose.disconnect(); }
})().catch(() => { console.error('Read-only identity audit failed. Check explicit target and access.'); process.exitCode = 1; });
module.exports = { auditDriverIdentity };
