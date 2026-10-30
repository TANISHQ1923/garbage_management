jest.mock('../src/config/db', () => ({ getDBStatus: jest.fn(() => ({ isConnected: true, state: 'connected' })) }));
const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const syntheticDataService = require('../src/services/syntheticDataService');
const {
  calculatePriority,
  isEligibleForActiveQueue,
  buildPriorityQueue
} = require('../src/services/priorityService');

describe('Priority Queue and Scoring Test Suite', () => {
  let officerToken;
  let citizenToken;
  let driverToken;

  const mockOfficerUser = {
    _id: '507f191e810c19729de860eb',
    id: '507f191e810c19729de860eb',
    name: 'Vikram Patel',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER'
  };

  const mockCitizenUser = {
    _id: '507f191e810c19729de860ea',
    id: '507f191e810c19729de860ea',
    name: 'Aarav Sharma',
    email: 'citizen@example.com',
    role: 'CITIZEN'
  };

  const mockDriverUser = {
    _id: '507f191e810c19729de860ec',
    id: '507f191e810c19729de860ec',
    name: 'Ramesh Kumar',
    email: 'driver@example.com',
    role: 'GARBAGE_DRIVER'
  };

  beforeAll(() => {
    officerToken = jwt.sign(
      { id: mockOfficerUser.id, email: mockOfficerUser.email, role: mockOfficerUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    citizenToken = jwt.sign(
      { id: mockCitizenUser.id, email: mockCitizenUser.email, role: mockCitizenUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    driverToken = jwt.sign(
      { id: mockDriverUser.id, email: mockDriverUser.email, role: mockDriverUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
  });

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(User, 'findById').mockImplementation(async (id) => {
      const idStr = id ? id.toString() : '';
      if (idStr === mockOfficerUser.id) return mockOfficerUser;
      if (idStr === mockCitizenUser.id) return mockCitizenUser;
      if (idStr === mockDriverUser.id) return mockDriverUser;
      return null;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // 1. Emergency complaints receive the emergency bonus (+50 points)
  it('1. should award 50 bonus points to emergency complaints', () => {
    const fixedNow = 1700000000000;
    // Complaint created 2 hours ago (< 6h -> age bonus 0), status SUBMITTED -> status bonus 20
    const nonEmergency = {
      status: 'SUBMITTED',
      emergency: false,
      createdAt: fixedNow - 2 * 3600 * 1000
    };
    const emergency = {
      status: 'SUBMITTED',
      emergency: true,
      createdAt: fixedNow - 2 * 3600 * 1000
    };

    const resNonEmerg = calculatePriority(nonEmergency, fixedNow);
    const resEmerg = calculatePriority(emergency, fixedNow);

    expect(resNonEmerg.score).toBe(20); // 0 (emerg) + 0 (age) + 20 (status)
    expect(resEmerg.score).toBe(70);    // 50 (emerg) + 0 (age) + 20 (status)
    expect(resEmerg.breakdown.emergency).toBe(50);
    expect(resEmerg.level).toBe('CRITICAL');
  });

  // 2. Older complaints receive the correct age bonus
  it('2. should award the correct mutually exclusive age bonus points', () => {
    const fixedNow = 1700000000000;

    // >= 24h -> 25 points
    const c24h = { status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 25 * 3600 * 1000 };
    expect(calculatePriority(c24h, fixedNow).breakdown.age).toBe(25);

    // 12 to <24h -> 15 points
    const c18h = { status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 18 * 3600 * 1000 };
    expect(calculatePriority(c18h, fixedNow).breakdown.age).toBe(15);

    // 6 to <12h -> 8 points
    const c8h = { status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 8 * 3600 * 1000 };
    expect(calculatePriority(c8h, fixedNow).breakdown.age).toBe(8);

    // < 6h -> 0 points
    const c3h = { status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 3 * 3600 * 1000 };
    expect(calculatePriority(c3h, fixedNow).breakdown.age).toBe(0);
  });

  // 3. Status bonuses are calculated correctly
  it('3. should award correct status bonuses (SUBMITTED: 20, ASSIGNED: 10, PICKUP_IN_PROGRESS: 5)', () => {
    const fixedNow = 1700000000000;
    const base = { emergency: false, createdAt: fixedNow - 1 * 3600 * 1000 };

    const cSubmitted = { ...base, status: 'SUBMITTED' };
    const cAssigned = { ...base, status: 'ASSIGNED' };
    const cInProgress = { ...base, status: 'PICKUP_IN_PROGRESS' };

    expect(calculatePriority(cSubmitted, fixedNow).breakdown.status).toBe(20);
    expect(calculatePriority(cAssigned, fixedNow).breakdown.status).toBe(10);
    expect(calculatePriority(cInProgress, fixedNow).breakdown.status).toBe(5);
  });

  // 4. Multiple age bonuses are not added together
  it('4. should not accumulate multiple age tiers together', () => {
    const fixedNow = 1700000000000;
    // 30 hours old: should get strictly 25 points, NOT (25 + 15 + 8) = 48
    const cOld = { status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 30 * 3600 * 1000 };
    const res = calculatePriority(cOld, fixedNow);

    expect(res.breakdown.age).toBe(25);
    expect(res.score).toBe(45); // 0 (emerg) + 25 (age) + 20 (status) = 45
    expect(res.level).toBe('MEDIUM');
  });

  // 5. Higher scores appear first
  it('5. should rank higher priority complaints before lower priority complaints', () => {
    const fixedNow = 1700000000000;
    const complaints = [
      { complaintId: 'CMP-LOW', status: 'PICKUP_IN_PROGRESS', emergency: false, createdAt: fixedNow - 1 * 3600 * 1000 }, // 0 + 0 + 5 = 5
      { complaintId: 'CMP-CRIT', status: 'SUBMITTED', emergency: true, createdAt: fixedNow - 25 * 3600 * 1000 },         // 50 + 25 + 20 = 95
      { complaintId: 'CMP-MED', status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 15 * 3600 * 1000 }           // 0 + 15 + 20 = 35
    ];

    const queue = buildPriorityQueue(complaints, fixedNow);
    expect(queue.map((c) => c.complaintId)).toEqual(['CMP-CRIT', 'CMP-MED', 'CMP-LOW']);
    expect(queue[0].priorityScore).toBe(95);
    expect(queue[0].priorityLevel).toBe('CRITICAL');
  });

  // 6. Equal scores are sorted deterministically (oldest first, then ID)
  it('6. should sort equal scores deterministically by oldest first, then by ID', () => {
    const fixedNow = 1700000000000;
    const complaints = [
      { complaintId: 'CMP-B', status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 10 * 3600 * 1000 }, // Score 28, 10h ago
      { complaintId: 'CMP-A', status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 11 * 3600 * 1000 }, // Score 28, 11h ago (older)
      { complaintId: 'CMP-C', status: 'SUBMITTED', emergency: false, createdAt: fixedNow - 10 * 3600 * 1000 }  // Score 28, 10h ago (same time as CMP-B)
    ];

    const queue = buildPriorityQueue(complaints, fixedNow);
    // CMP-A is oldest (11h ago) -> 1st
    // CMP-B and CMP-C have same age (10h ago), tie-breaker is complaintId ('CMP-B' < 'CMP-C')
    expect(queue.map((c) => c.complaintId)).toEqual(['CMP-A', 'CMP-B', 'CMP-C']);
  });

  // 7. VERIFIED complaints are excluded
  it('7. should exclude VERIFIED complaints from the active dispatch queue', () => {
    const complaint = { complaintId: 'CMP-VERIFIED', status: 'VERIFIED', emergency: true };
    expect(isEligibleForActiveQueue(complaint)).toBe(false);

    const queue = buildPriorityQueue([complaint]);
    expect(queue).toHaveLength(0);
  });

  // 8. REJECTED complaints are excluded
  it('8. should exclude REJECTED complaints from the active dispatch queue', () => {
    const complaint = { complaintId: 'CMP-REJECTED', status: 'REJECTED', emergency: true };
    expect(isEligibleForActiveQueue(complaint)).toBe(false);

    const queue = buildPriorityQueue([complaint]);
    expect(queue).toHaveLength(0);
  });

  // 9. CLEANED complaints are excluded from the active queue
  it('9. should exclude CLEANED complaints awaiting inspection from active dispatch queue', () => {
    const complaint = { complaintId: 'CMP-CLEANED', status: 'CLEANED', emergency: true };
    expect(isEligibleForActiveQueue(complaint)).toBe(false);

    const queue = buildPriorityQueue([complaint]);
    expect(queue).toHaveLength(0);
  });

  // 10. Unauthorized users cannot access the officer queue
  it('10a. should reject unauthenticated requests to the priority queue with 401', async () => {
    const res = await request(app).get('/api/officer/complaints/priority-queue');
    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });

  it('10b. should block citizen from accessing officer priority queue with 403 Forbidden', async () => {
    const res = await request(app)
      .get('/api/officer/complaints/priority-queue')
      .set('Authorization', `Bearer ${citizenToken}`);

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Forbidden');
  });

  it('10c. should block garbage driver from accessing officer priority queue with 403 Forbidden', async () => {
    const res = await request(app)
      .get('/api/officer/complaints/priority-queue')
      .set('Authorization', `Bearer ${driverToken}`);

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Forbidden');
  });

  // 11. Synthetic demo mode returns queue data
  it('11. should return valid priority queue in synthetic demo mode for authorized officer', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(true);

    const res = await request(app)
      .get('/api/officer/complaints/priority-queue')
      .set('Authorization', `Bearer ${officerToken}`);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(Array.isArray(res.body.data)).toBe(true);
    expect(res.body.count).toBeGreaterThan(0);

    const queue = res.body.data;
    // Check all complaints are eligible statuses
    queue.forEach((c) => {
      expect(['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS']).toContain(c.status);
      expect(c.priorityScore).toBeDefined();
      expect(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']).toContain(c.priorityLevel);
    });

    // Check sorted in descending order of priorityScore
    for (let i = 1; i < queue.length; i++) {
      expect(queue[i - 1].priorityScore).toBeGreaterThanOrEqual(queue[i].priorityScore);
    }
  });

  // 12. Existing complaint and dashboard APIs continue working
  it('12. should preserve existing officer complaints listing and sorting', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(true);

    const res = await request(app)
      .get('/api/officer/complaints?sort=priority')
      .set('Authorization', `Bearer ${officerToken}`);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data[0].priorityScore).toBeDefined();
    expect(res.body.data[0].priorityLevel).toBeDefined();
  });
});
