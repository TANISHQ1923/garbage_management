const mongoose = require('mongoose');

// Never downgrade to independent writes: standalone MongoDB must fail closed.
const withTransaction = async (operation) => {
  const session = await mongoose.startSession();
  try {
    return await session.withTransaction(() => operation(session));
  } catch (error) {
    if (error.code === 20 || /replica set|Transaction numbers|transactions are not supported/i.test(error.message)) {
      const unavailable = new Error('This operation requires a MongoDB replica set or sharded cluster with transaction support.');
      unavailable.statusCode = 503;
      throw unavailable;
    }
    throw error;
  } finally {
    await session.endSession();
  }
};
module.exports = { withTransaction };
