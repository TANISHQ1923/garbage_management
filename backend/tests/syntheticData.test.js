const fs = require('fs');
const path = require('path');
const { runValidation } = require('../src/scripts/validateSyntheticData');

describe('Synthetic Demo Dataset Test Suite', () => {
  const datasetPath = path.join(__dirname, '..', 'src', 'data', 'synthetic', 'synthetic-dashboard-data.json');

  it('1. should exist in backend/src/data/synthetic/synthetic-dashboard-data.json', () => {
    expect(fs.existsSync(datasetPath)).toBe(true);
  });

  it('2. should pass all structural, enum, relational, and reconciliation validation rules', () => {
    expect(() => runValidation()).not.toThrow();
  });

  it('3. should verify synthetic metadata is marked as SYNTHETIC DEMO DATA', () => {
    const data = JSON.parse(fs.readFileSync(datasetPath, 'utf-8'));
    expect(data.metadata.title).toBe('SYNTHETIC DEMO DATASET');
    expect(data.metadata.label).toBe('SYNTHETIC DEMO DATA');
    expect(data.metadata.compliance.modelStatus).toBe('DEVELOPMENT_BASELINE');
    expect(data.metadata.compliance.noRealPII).toBe(true);
    expect(data.metadata.compliance.fictionalVehicles).toBe(true);
  });

  it('4. should verify user counts and demo email formats', () => {
    const data = JSON.parse(fs.readFileSync(datasetPath, 'utf-8'));
    expect(data.users.length).toBe(21);

    const citizens = data.users.filter(u => u.role === 'CITIZEN');
    const officers = data.users.filter(u => u.role === 'MUNICIPAL_OFFICER');
    const drivers = data.users.filter(u => u.role === 'GARBAGE_DRIVER');

    expect(citizens.length).toBe(10);
    expect(officers.length).toBe(3);
    expect(drivers.length).toBe(8);

    data.users.forEach(u => {
      expect(u.email).toMatch(/^[a-z0-9]+@demo\.local$/);
    });
  });

  it('5. should verify complaint baseline state and zero initial dummy complaints', () => {
    const data = JSON.parse(fs.readFileSync(datasetPath, 'utf-8'));
    expect(data.complaints.length).toBe(0);
    expect(data.notifications.length).toBe(0);
  });

  it('6. should simulate idempotent seeding (running twice produces identical document maps with zero duplicates)', () => {
    const data = JSON.parse(fs.readFileSync(datasetPath, 'utf-8'));

    // Simulated database collection tables
    const userStore = new Map();
    const driverStore = new Map();
    const complaintStore = new Map();
    const notificationStore = new Map();

    const simulateSeed = () => {
      data.users.forEach(u => userStore.set(u.email, { ...u }));
      data.drivers.forEach(d => driverStore.set(d.driverId, { ...d }));
      data.complaints.forEach(c => complaintStore.set(c.complaintId, { ...c }));
      data.notifications.forEach(n => notificationStore.set(n.notificationId, { ...n }));
    };

    // First seed run
    simulateSeed();
    expect(userStore.size).toBe(21);
    expect(driverStore.size).toBe(8);
    expect(complaintStore.size).toBe(0);
    expect(notificationStore.size).toBe(0);

    // Second seed run
    simulateSeed();
    expect(userStore.size).toBe(21);
    expect(driverStore.size).toBe(8);
    expect(complaintStore.size).toBe(0);
    expect(notificationStore.size).toBe(0);
  });
});
