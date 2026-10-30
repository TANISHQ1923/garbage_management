// Local operator-only command. Reads JSON from stdin, never prints passwords or tokens.
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const { randomUUID } = require('crypto');
const User = require('../models/User');
const Driver = require('../models/Driver');
const { withTransaction } = require('../services/transactionService');
require('../config/env');

async function provisionStaff(input) {
  if (!['MUNICIPAL_OFFICER', 'GARBAGE_DRIVER'].includes(input.role)) throw new Error('Invalid staff role.');
  if (!input.name || !input.email || !input.employeeId || !input.password || input.password.length < 12) {
    throw new Error('Name, email, employee ID, and a password of at least 12 characters are required.');
  }
  if (input.role === 'GARBAGE_DRIVER' && (!input.phone || !input.vehicleNumber)) throw new Error('Driver phone and vehicle are required.');
  const driverId = input.role === 'GARBAGE_DRIVER' ? `DRV-${randomUUID()}` : null;
  const passwordHash = await bcrypt.hash(input.password, 12);
  return withTransaction(async session => {
    if (await User.exists({ employeeId: input.employeeId }).session(session)) throw new Error('Employee ID already provisioned.');
    const [user] = await User.create([{ name: input.name, email: input.email, role: input.role,
      employeeId: input.employeeId, passwordHash, driverId, phone: input.phone, ward: input.ward,
      vehicleNumber: input.vehicleNumber }], { session });
    if (driverId) await Driver.create([{ driverId, employeeId: input.employeeId, name: input.name,
      phone: input.phone, vehicleNumber: input.vehicleNumber, availability: 'AVAILABLE', currentLocation: null }], { session });
    return { userId: String(user._id), driverId };
  });
}
if (require.main === module) (async () => {
  let raw = ''; for await (const chunk of process.stdin) raw += chunk;
  if (!process.env.STAFF_PROVISIONING_URI) throw new Error('Set STAFF_PROVISIONING_URI explicitly.');
  await mongoose.connect(process.env.STAFF_PROVISIONING_URI, { autoIndex: false });
  try { console.log(JSON.stringify(await provisionStaff(JSON.parse(raw)))); }
  finally { await mongoose.disconnect(); }
})().catch(() => { console.error('Staff provisioning failed. Check input, duplicates, and transaction support.'); process.exitCode = 1; });
module.exports = { provisionStaff };
