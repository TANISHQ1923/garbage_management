/**
 * Synthetic Demo Dataset Seeder
 * Smart Garbage Management Application
 *
 * Idempotently seeds the synthetic dataset (Users, Drivers, Complaints, Notifications)
 * into MongoDB using Mongoose findOneAndUpdate with upsert: true.
 *
 * SYNTHETIC DEMO DATA ONLY - FOR TESTING AND DEMONSTRATION PURPOSES
 */

const fs = require('fs');
const path = require('path');
const mongoose = require('mongoose');
const env = require('../config/env');
const User = require('../models/User');
const Driver = require('../models/Driver');
const Complaint = require('../models/Complaint');
const Notification = require('../models/Notification');

/**
 * Seed synthetic demo dataset into MongoDB.
 *
 * @param {boolean} [disconnectOnFinish=true] Whether to disconnect mongoose on completion.
 * @returns {Promise<{success: boolean, counts: object}>}
 */
async function seedSyntheticData(disconnectOnFinish = true) {
  console.log('====================================================');
  console.log('[SyntheticSeed] Starting synthetic demo data seeding...');
  console.log('[SyntheticSeed] SYNTHETIC DEMO DATA ONLY');
  console.log('====================================================');

  const datasetPath = path.join(__dirname, '..', 'data', 'synthetic', 'synthetic-dashboard-data.json');
  if (!fs.existsSync(datasetPath)) {
    throw new Error(`Synthetic dataset file not found at: ${datasetPath}`);
  }

  const dataset = JSON.parse(fs.readFileSync(datasetPath, 'utf-8'));

  // Ensure DB connection if not already connected
  const isAlreadyConnected = mongoose.connection.readyState === 1;
  if (!isAlreadyConnected) {
    console.log(`[SyntheticSeed] Connecting to MongoDB: ${env.MONGODB_URI.replace(/\/\/.*@/, '//***:***@')}`);
    await mongoose.connect(env.MONGODB_URI, { serverSelectionTimeoutMS: 5000 });
    console.log('[SyntheticSeed] Connected to MongoDB.');
  }

  const results = {
    usersUpserted: 0,
    driversUpserted: 0,
    complaintsUpserted: 0,
    notificationsUpserted: 0
  };

  // 1. Seed Users (Citizens, Officers, Drivers)
  console.log(`[SyntheticSeed] Seeding ${dataset.users.length} synthetic users...`);
  for (const user of dataset.users) {
    const userDoc = { ...user };
    // MongoDB _id mapping
    if (userDoc.id && !userDoc._id) {
      userDoc._id = userDoc.id;
    }
    delete userDoc.id;

    await User.findOneAndUpdate(
      { email: user.email },
      userDoc,
      { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    results.usersUpserted++;
  }
  console.log(`[SyntheticSeed] Successfully upserted ${results.usersUpserted} synthetic users.`);

  // 2. Seed Driver Records
  console.log(`[SyntheticSeed] Seeding ${dataset.drivers.length} synthetic driver records...`);
  for (const drv of dataset.drivers) {
    await Driver.findOneAndUpdate(
      { driverId: drv.driverId },
      drv,
      { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    results.driversUpserted++;
  }
  console.log(`[SyntheticSeed] Successfully upserted ${results.driversUpserted} synthetic driver records.`);

  // 3. Seed Complaints
  console.log(`[SyntheticSeed] Seeding ${dataset.complaints.length} synthetic complaints across 6 lifecycle stages...`);
  for (const c of dataset.complaints) {
    await Complaint.findOneAndUpdate(
      { complaintId: c.complaintId },
      c,
      { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    results.complaintsUpserted++;
  }
  console.log(`[SyntheticSeed] Successfully upserted ${results.complaintsUpserted} synthetic complaints.`);

  // 4. Seed Notifications
  console.log(`[SyntheticSeed] Seeding ${dataset.notifications.length} synthetic notifications...`);
  for (const n of dataset.notifications) {
    await Notification.findOneAndUpdate(
      { notificationId: n.notificationId },
      n,
      { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    results.notificationsUpserted++;
  }
  console.log(`[SyntheticSeed] Successfully upserted ${results.notificationsUpserted} synthetic notifications.`);

  console.log('====================================================');
  console.log('[SyntheticSeed] Seeding summary:');
  console.log(`  - Users: ${results.usersUpserted}`);
  console.log(`  - Drivers: ${results.driversUpserted}`);
  console.log(`  - Complaints: ${results.complaintsUpserted}`);
  console.log(`  - Notifications: ${results.notificationsUpserted}`);
  console.log('[SyntheticSeed] Seed operation is 100% idempotent (safe to rerun).');
  console.log('====================================================');

  if (disconnectOnFinish && !isAlreadyConnected) {
    await mongoose.disconnect();
    console.log('[SyntheticSeed] Disconnected from MongoDB.');
  }

  return { success: true, counts: results };
}

if (require.main === module) {
  seedSyntheticData(true)
    .then(() => process.exit(0))
    .catch((err) => {
      console.error(`[SyntheticSeed] Error during seeding: ${err.message}`);
      process.exit(1);
    });
}

module.exports = { seedSyntheticData };
