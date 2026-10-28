const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const Driver = require('../src/models/Driver');
const Notification = require('../src/models/Notification');
const syntheticDataService = require('../src/services/syntheticDataService');

describe('Dashboard Endpoints & Synthetic Demo Mode Integration Test Suite', () => {
  let citizenToken;
  let officerToken;
  let driver1Token;
  let driver2Token;

  const mockCitizen = {
    id: '65f100000000000000000001',
    _id: '65f100000000000000000001',
    name: 'Aarav Sharma',
    email: 'citizen1@demo.local',
    role: 'CITIZEN'
  };

  const mockOfficer = {
    id: '65f200000000000000000001',
    _id: '65f200000000000000000001',
    name: 'Suresh Raman',
    email: 'officer1@demo.local',
    employeeId: 'MC-OFF-DEMO-001',
    ward: 'Ward 12 - Central Zone',
    role: 'MUNICIPAL_OFFICER'
  };

  const mockDriver1 = {
    id: '65f300000000000000000001',
    _id: '65f300000000000000000001',
    name: 'Ramesh Shinde',
    email: 'driver1@demo.local',
    driverId: 'DRV-DEMO-001',
    vehicleNumber: 'MH-DEMO-001',
    role: 'GARBAGE_DRIVER'
  };

  const mockDriver2 = {
    id: '65f300000000000000000002',
    _id: '65f300000000000000000002',
    name: 'Sunil Pawar',
    email: 'driver2@demo.local',
    driverId: 'DRV-DEMO-002',
    vehicleNumber: 'MH-DEMO-002',
    role: 'GARBAGE_DRIVER'
  };

  beforeAll(() => {
    citizenToken = jwt.sign(
      { id: mockCitizen.id, email: mockCitizen.email, role: mockCitizen.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    officerToken = jwt.sign(
      { id: mockOfficer.id, email: mockOfficer.email, role: mockOfficer.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    driver1Token = jwt.sign(
      { id: mockDriver1.id, email: mockDriver1.email, role: mockDriver1.role, driverId: mockDriver1.driverId },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    driver2Token = jwt.sign(
      { id: mockDriver2.id, email: mockDriver2.email, role: mockDriver2.role, driverId: mockDriver2.driverId },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
  });

  // =========================================================================
  // 1. AUTHENTICATION & TOKEN VALIDATION
  // =========================================================================
  describe('Authentication Enforcement', () => {
    it('should reject unauthenticated request with 401 on /api/dashboard/citizen', async () => {
      const res = await request(app).get('/api/dashboard/citizen');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Authentication required');
    });

    it('should reject unauthenticated request with 401 on /api/dashboard/officer', async () => {
      const res = await request(app).get('/api/dashboard/officer');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    it('should reject unauthenticated request with 401 on /api/dashboard/driver', async () => {
      const res = await request(app).get('/api/dashboard/driver');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    it('should reject invalid bearer token with 401', async () => {
      const res = await request(app)
        .get('/api/dashboard/citizen')
        .set('Authorization', 'Bearer invalid.mock.token');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });
  });

  // =========================================================================
  // 2. ROLE-BASED ACCESS CONTROL (RBAC) RESTRICTIONS
  // =========================================================================
  describe('Role-Based Access Control (RBAC)', () => {
    it('should block citizen from accessing officer dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/officer')
        .set('Authorization', `Bearer ${citizenToken}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Forbidden');
    });

    it('should block citizen from accessing driver dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/driver')
        .set('Authorization', `Bearer ${citizenToken}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });

    it('should block driver from accessing citizen dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/citizen')
        .set('Authorization', `Bearer ${driver1Token}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });

    it('should block driver from accessing officer dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/officer')
        .set('Authorization', `Bearer ${driver1Token}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });

    it('should block officer from accessing citizen dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/citizen')
        .set('Authorization', `Bearer ${officerToken}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });

    it('should block officer from accessing driver dashboard with 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/dashboard/driver')
        .set('Authorization', `Bearer ${officerToken}`);
      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });
  });

  // =========================================================================
  // 3. SYNTHETIC MODE ENABLED
  // =========================================================================
  describe('Synthetic Mode Enabled (USE_SYNTHETIC_DATA=true)', () => {
    beforeAll(() => {
      process.env.USE_SYNTHETIC_DATA = 'true';
      env.USE_SYNTHETIC_DATA = true;
    });

    afterAll(() => {
      process.env.USE_SYNTHETIC_DATA = 'false';
      env.USE_SYNTHETIC_DATA = false;
    });

    it('should return Citizen Dashboard data populated from synthetic dataset', async () => {
      const res = await request(app)
        .get('/api/dashboard/citizen')
        .set('Authorization', `Bearer ${citizenToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('metrics');
      expect(res.body.data).toHaveProperty('complaints');
      expect(res.body.data).toHaveProperty('recentComplaints');
      expect(res.body.data).toHaveProperty('notifications');

      const metrics = res.body.data.metrics;
      expect(metrics.totalComplaints).toBe(20);
      expect(metrics.activeComplaints).toBe(8);
      expect(metrics.pendingRequests).toBe(3);
      expect(metrics.emergencyRequests).toBe(3);
      expect(metrics.completedComplaints).toBe(10);
      expect(metrics.rejectedComplaints).toBe(2);

      // Verify citizen only gets their own complaints
      res.body.data.complaints.forEach((c) => {
        expect(c.citizenId).toBe(mockCitizen.id);
      });
    });

    it('should return Municipal Officer Dashboard data populated from synthetic dataset', async () => {
      const res = await request(app)
        .get('/api/dashboard/officer')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('metrics');
      expect(res.body.data).toHaveProperty('driverAvailability');
      expect(res.body.data).toHaveProperty('priorityComplaints');
      expect(res.body.data).toHaveProperty('recentComplaints');
      expect(res.body.data).toHaveProperty('aiMetrics');

      const metrics = res.body.data.metrics;
      expect(metrics.total).toBe(120);
      expect(metrics.submitted).toBe(22);
      expect(metrics.assigned).toBe(26);
      expect(metrics.inProgress).toBe(16);
      expect(metrics.cleaned).toBe(24);
      expect(metrics.verified).toBe(22);
      expect(metrics.rejected).toBe(10);
      expect(metrics.emergency).toBe(18);
      expect(metrics.activeComplaints).toBe(64);

      // Verify mathematical consistency: total = submitted + assigned + inProgress + cleaned + verified + rejected
      expect(metrics.submitted + metrics.assigned + metrics.inProgress + metrics.cleaned + metrics.verified + metrics.rejected).toBe(metrics.total);

      // Driver availability
      expect(res.body.data.driverAvailability.total).toBe(8);
      expect(res.body.data.driverAvailability.available).toBe(4);
      expect(res.body.data.driverAvailability.busy).toBe(2);
      expect(res.body.data.driverAvailability.offline).toBe(2);
    });

    it('should return Garbage Driver Dashboard data populated from synthetic dataset', async () => {
      const res = await request(app)
        .get('/api/dashboard/driver')
        .set('Authorization', `Bearer ${driver1Token}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('driverId', 'DRV-DEMO-001');
      expect(res.body.data).toHaveProperty('driverInfo');
      expect(res.body.data).toHaveProperty('metrics');
      expect(res.body.data).toHaveProperty('assignedTasks');
      expect(res.body.data).toHaveProperty('collectionHistory');

      const metrics = res.body.data.metrics;
      expect(metrics.totalAssignedTasks).toBe(12);
      expect(metrics.assignedTasks).toBe(4);
      expect(metrics.inProgressTasks).toBe(2);
      expect(metrics.completedTasks).toBe(6);
      expect(metrics.emergencyTasks).toBe(3);

      // Verify active task exists and has before-cleaning photo
      if (res.body.data.activeTask) {
        expect(res.body.data.activeTask.status).toBe('PICKUP_IN_PROGRESS');
        expect(res.body.data.activeTask.beforeCleaningImageUrl).toBeTruthy();
      }

      // Verify Driver 1 only receives tasks assigned to DRV-DEMO-001
      res.body.data.assignedTasks.forEach((t) => {
        expect(t.assignedDriverId).toBe('DRV-DEMO-001');
      });
      res.body.data.collectionHistory.forEach((t) => {
        expect(t.assignedDriverId).toBe('DRV-DEMO-001');
      });
    });

    it('should isolate data between different drivers (Driver 1 cannot see Driver 2 tasks)', async () => {
      const res1 = await request(app)
        .get('/api/dashboard/driver')
        .set('Authorization', `Bearer ${driver1Token}`);

      const res2 = await request(app)
        .get('/api/dashboard/driver')
        .set('Authorization', `Bearer ${driver2Token}`);

      expect(res1.body.data.driverId).toBe('DRV-DEMO-001');
      expect(res2.body.data.driverId).toBe('DRV-DEMO-002');

      const d1TaskIds = res1.body.data.assignedTasks.map((t) => t.complaintId);
      const d2TaskIds = res2.body.data.assignedTasks.map((t) => t.complaintId);

      // Verify no overlap in assigned tasks between distinct drivers
      d1TaskIds.forEach((id) => {
        expect(d2TaskIds).not.toContain(id);
      });
    });

    it('should serve synthetic complaints through existing GET /api/complaints/my in synthetic mode', async () => {
      const res = await request(app)
        .get('/api/complaints/my')
        .set('Authorization', `Bearer ${citizenToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.length).toBe(20);
    });

    it('should serve synthetic complaints through existing GET /api/officer/complaints in synthetic mode', async () => {
      const res = await request(app)
        .get('/api/officer/complaints')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.count).toBe(120);
    });

    it('should support search and status filtering on /api/officer/complaints in synthetic mode', async () => {
      const res = await request(app)
        .get('/api/officer/complaints?status=SUBMITTED')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.count).toBe(22);
      res.body.data.forEach((c) => expect(c.status).toBe('SUBMITTED'));
    });

    it('should serve synthetic tasks through existing GET /api/driver/complaints in synthetic mode', async () => {
      const res = await request(app)
        .get('/api/driver/complaints')
        .set('Authorization', `Bearer ${driver1Token}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.count).toBe(12);
      res.body.data.forEach((t) => expect(t.assignedDriverId).toBe('DRV-DEMO-001'));
    });
  });

  // =========================================================================
  // 4. MONGODB MODE (USE_SYNTHETIC_DATA=false)
  // =========================================================================
  describe('MongoDB Mode Compatibility (USE_SYNTHETIC_DATA=false)', () => {
    beforeAll(() => {
      process.env.USE_SYNTHETIC_DATA = 'false';
      env.USE_SYNTHETIC_DATA = false;
    });

    it('should execute MongoDB query paths without error when synthetic mode is disabled', async () => {
      // Mock MongoDB Complaint.find and Driver.find
      const mockDbComplaints = [
        {
          complaintId: 'SGM-DB-001',
          citizenId: mockCitizen.id,
          status: 'SUBMITTED',
          emergency: false,
          createdAt: new Date()
        },
        {
          complaintId: 'SGM-DB-002',
          citizenId: mockCitizen.id,
          status: 'CLEANED',
          emergency: true,
          createdAt: new Date()
        }
      ];

      jest.spyOn(Complaint, 'find').mockReturnValue({
        sort: jest.fn().mockResolvedValue(mockDbComplaints)
      });
      jest.spyOn(Notification, 'find').mockReturnValue({
        sort: jest.fn().mockResolvedValue([])
      });

      const res = await request(app)
        .get('/api/dashboard/citizen')
        .set('Authorization', `Bearer ${citizenToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('metrics');
    });
  });
});
