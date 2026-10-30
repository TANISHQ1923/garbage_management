// Read-only live diagnostics for the fixed, isolated pilot DB. Never loads .env.
const mongoose = require('mongoose');
(async () => {
  await mongoose.connect('mongodb://127.0.0.1:27018/phase1_pilot?replicaSet=rs0&directConnection=true',
    { autoIndex: false, autoCreate: false, serverSelectionTimeoutMS: 3000 });
  console.log('Read-only Uno monitor. Keep Serial Monitor closed. Ctrl+C stops this display only.');
  let stopped = false;
  process.on('SIGINT', () => { stopped = true; });
  while (!stopped) {
    const bin = await mongoose.connection.db.collection('bins').findOne({ binId: 'UNO-BIN-001' });
    const reading = await mongoose.connection.db.collection('sensorreadings').find({ binId: 'UNO-BIN-001' }).sort({ receivedAt: -1 }).limit(1).next();
    const tasks = await mongoose.connection.db.collection('complaints').countDocuments({ source: 'SENSOR', binId: 'UNO-BIN-001', activeSensorTask: true });
    if (bin && reading) {
      const age = Math.max(0, Math.round((Date.now() - reading.capturedAtMs) / 1000));
      console.log(`${new Date().toLocaleTimeString()} | distance=${reading.distanceCm == null ? 'NO_ECHO' : reading.distanceCm.toFixed(2) + 'cm'} | ${reading.reason} | age=${age}s | last filtered fill=${bin.fillPercent?.toFixed(1) ?? '?'}% | streak=${bin.state.candidate || 'NONE'} ${bin.state.count || 0} | open tasks=${tasks}`);
    } else console.log('No bin/reading yet.');
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  await mongoose.disconnect();
})().catch(() => { console.error('Local monitor unavailable. Check the isolated MongoDB container.'); process.exitCode = 1; });
