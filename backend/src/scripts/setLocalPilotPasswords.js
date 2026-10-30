// Interactive local helper only; fixed database and exact pilot accounts, no role changes.
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const User = require('../models/User');
const { withTransaction } = require('../services/transactionService');
(async () => {
  const expected = 'mongodb://127.0.0.1:27018/phase1_pilot?replicaSet=rs0&directConnection=true';
  if (process.env.MONGODB_URI !== expected) throw new Error('Local pilot target required.');
  let raw = ''; for await (const chunk of process.stdin) raw += chunk;
  const input = JSON.parse(raw);
  if (![input.officer, input.driver].every(p => typeof p === 'string' && p.length >= 12)) throw new Error('Invalid local password.');
  await mongoose.connect(expected, { autoIndex: false, autoCreate: false });
  try {
    const officer = await bcrypt.hash(input.officer, 12), driver = await bcrypt.hash(input.driver, 12);
    await withTransaction(async session => {
      for (const [key, hash] of [['Officer', officer], ['Driver', driver]]) {
        const result = await User.updateOne({ email: key.toLowerCase() + '@phase1.local', employeeId: 'LOCAL-PILOT-' + key },
          { $set: { passwordHash: hash } }, { session });
        if (result.matchedCount !== 1) throw new Error('Local pilot account missing.');
      }
    });
  } finally { await mongoose.disconnect(); }
})().catch(() => { console.error('Local pilot password update failed.'); process.exitCode = 1; });
