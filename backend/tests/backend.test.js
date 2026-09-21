const request = require('supertest');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const Driver = require('../src/models/Driver');
const Notification = require('../src/models/Notification');
const {
  canTransition,
  canDriverTransition
} = require('../src/services/complaintService');

describe('Smart Garbage Management Backend API Test Suite', () => {
  let citizenToken;
  let officerToken;
  let driverToken;

  const mockCitizenUser = {
    _id: '507f191e810c19729de860ea',
    id: '507f191e810c19729de860ea',
    name: 'Aarav Sharma',
    email: 'citizen@example.com',
    role: 'CITIZEN',
    phone: '+91 9876543210',
    toJSON: function () {
      return {
        id: this.id,
        name: this.name,
        email: this.email,
        role: this.role,
        phone: this.phone
      };
    }
  };

  const mockOfficerUser = {
    _id: '507f191e810c19729de860eb',
    id: '507f191e810c19729de860eb',
    name: 'Vikram Patel',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER',
    employeeId: 'MC-OFF-2026',
    toJSON: function () {
      return {
        id: this.id,
        name: this.name,
        email: this.email,
        role: this.role,
        employeeId: this.employeeId
      };
    }
  };

  const mockDriverUser = {
    _id: '507f191e810c19729de860ec',
    id: '507f191e810c19729de860ec',
    name: 'Ramesh Kumar',
    email: 'driver@example.com',
    role: 'GARBAGE_DRIVER',
    driverId: 'DRV-882',
    vehicleNumber: 'DL-01-GB-4040',
    toJSON: function () {
      return {
        id: this.id,
        name: this.name,
        email: this.email,
        role: this.role,
        driverId: this.driverId,
        vehicleNumber: this.vehicleNumber
      };
    }
  };

  beforeAll(() => {
    // Generate valid tokens with matching secrets
    citizenToken = jwt.sign(
      { id: mockCitizenUser.id, email: mockCitizenUser.email, role: mockCitizenUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    officerToken = jwt.sign(
      { id: mockOfficerUser.id, email: mockOfficerUser.email, role: mockOfficerUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    driverToken = jwt.sign(
      { id: mockDriverUser.id, email: mockDriverUser.email, role: mockDriverUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
  });

  // =========================================================================
  // 1. HEALTH CHECK ENDPOINT
  // =========================================================================
  describe('GET /api/health', () => {
    it('should return 200 OK and valid health status object', async () => {
      const res = await request(app).get('/api/health');
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.message).toContain('Smart Garbage Management API is running');
      expect(res.body).toHaveProperty('database');
      expect(res.body).toHaveProperty('timestamp');
    });
  });

  // =========================================================================
  // 2. AUTHENTICATION & TOKEN VERIFICATION
  // =========================================================================
  describe('Authentication Endpoints', () => {
    it('should reject unauthenticated request on protected route with 401', async () => {
      const res = await request(app).get('/api/auth/me');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Authentication required');
    });

    it('should reject invalid JWT token with 401', async () => {
      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid.bogus.token');
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    it('should return safe info message on forgot-password', async () => {
      const res = await request(app)
        .post('/api/auth/forgot-password')
        .send({ email: 'citizen@example.com' });
      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.message).toContain('password reset instructions');
    });

    it('should register a new user successfully and never expose passwordHash', async () => {
      const testEmail = `test_${Date.now()}@example.com`;
      jest.spyOn(User, 'findOne').mockResolvedValueOnce(null);
      jest.spyOn(User, 'create').mockResolvedValueOnce({
        id: 'new-user-123',
        name: 'New Citizen',
        email: testEmail,
        role: 'CITIZEN',
        toJSON: () => ({ id: 'new-user-123', name: 'New Citizen', email: testEmail, role: 'CITIZEN' })
      });

      const res = await request(app)
        .post('/api/auth/register')
        .send({
          name: 'New Citizen',
          email: testEmail,
          password: 'password123',
          role: 'CITIZEN'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('token');
      expect(res.body.data.user).not.toHaveProperty('passwordHash');
      expect(res.body.data.user.email).toBe(testEmail);
    });

    it('should prevent duplicate email registration with 409 Conflict', async () => {
      jest.spyOn(User, 'findOne').mockResolvedValueOnce(mockCitizenUser);

      const res = await request(app)
        .post('/api/auth/register')
        .send({
          name: 'Aarav Sharma',
          email: 'citizen@example.com',
          password: 'password123',
          role: 'CITIZEN'
        });

      expect(res.status).toBe(409);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('already exists');
    });

    it('should authenticate valid user on login', async () => {
      const userWithHash = {
        ...mockCitizenUser,
        comparePassword: jest.fn().mockResolvedValue(true),
        toJSON: mockCitizenUser.toJSON
      };

      jest.spyOn(User, 'findOne').mockReturnValue({
        select: jest.fn().mockResolvedValue(userWithHash)
      });

      const res = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'citizen@example.com',
          password: 'password123',
          role: 'CITIZEN'
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data).toHaveProperty('token');
      expect(res.body.data.user.email).toBe('citizen@example.com');
      expect(res.body.data.user).not.toHaveProperty('passwordHash');
    });

    it('should reject login with wrong password with 401', async () => {
      const userWithHash = {
        ...mockCitizenUser,
        comparePassword: jest.fn().mockResolvedValue(false)
      };

      jest.spyOn(User, 'findOne').mockReturnValue({
        select: jest.fn().mockResolvedValue(userWithHash)
      });

      const res = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'citizen@example.com',
          password: 'wrongpassword'
        });

      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });
  });

  // =========================================================================
  // 3. ROLE AUTHORIZATION GUARDS
  // =========================================================================
  describe('Role Authorization Middleware', () => {
    it('should block citizen from accessing officer-only driver assignment with 403 Forbidden', async () => {
      jest.spyOn(User, 'findById').mockResolvedValue(mockCitizenUser);

      const res = await request(app)
        .put('/api/officer/complaints/CMP-001/assign')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({ driverId: 'DRV-882' });

      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Forbidden');
    });

    it('should block officer from accessing driver-only tasks with 403 Forbidden', async () => {
      jest.spyOn(User, 'findById').mockResolvedValue(mockOfficerUser);

      const res = await request(app)
        .get('/api/driver/complaints')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
    });
  });

  // =========================================================================
  // 4. CITIZEN COMPLAINT FLOW & ISOLATION
  // =========================================================================
  describe('Citizen Complaint APIs', () => {
    beforeEach(() => {
      jest.spyOn(User, 'findById').mockResolvedValue(mockCitizenUser);
    });

    it('should create a complaint with SUBMITTED status and initial status history', async () => {
      const mockCreated = {
        complaintId: 'CMP-2026-999',
        citizenId: mockCitizenUser.id,
        citizenName: mockCitizenUser.name,
        locationText: 'Sector 4 Market',
        status: 'SUBMITTED',
        statusHistory: [{ status: 'SUBMITTED', note: 'Complaint registered by citizen.' }],
        toJSON: () => ({
          id: 'CMP-2026-999',
          complaintId: 'CMP-2026-999',
          citizenId: mockCitizenUser.id,
          locationText: 'Sector 4 Market',
          status: 'SUBMITTED'
        })
      };

      jest.spyOn(Complaint, 'create').mockResolvedValueOnce(mockCreated);
      jest.spyOn(Notification, 'create').mockResolvedValueOnce({});

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'OVERFLOWING_BIN',
          locationDescription: 'Sector 4 Market',
          description: 'Public bin spilling over.'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data.status).toBe('SUBMITTED');
    });

    it('should prevent citizen from accessing another citizen’s complaint with 403', async () => {
      const otherCitizenComplaint = {
        complaintId: 'CMP-OTHER-001',
        citizenId: 'different-citizen-id-999',
        status: 'SUBMITTED'
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValueOnce(otherCitizenComplaint);

      const res = await request(app)
        .get('/api/complaints/CMP-OTHER-001')
        .set('Authorization', `Bearer ${citizenToken}`);

      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Access denied');
    });
  });

  // =========================================================================
  // 5. OFFICER ASSIGNMENT & STATUS TRANSITIONS
  // =========================================================================
  describe('Officer APIs & State Machine', () => {
    beforeEach(() => {
      jest.spyOn(User, 'findById').mockResolvedValue(mockOfficerUser);
    });

    it('should assign a driver and update status to ASSIGNED', async () => {
      const mockComplaintDoc = {
        complaintId: 'CMP-101',
        citizenId: mockCitizenUser.id,
        status: 'SUBMITTED',
        statusHistory: [],
        save: jest.fn().mockResolvedValue(true)
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValueOnce(mockComplaintDoc);
      jest.spyOn(Driver, 'findOne').mockResolvedValueOnce({
        driverId: 'DRV-882',
        name: 'Ramesh Kumar',
        vehicleNumber: 'DL-01-GB-4040'
      });
      jest.spyOn(User, 'findOne').mockResolvedValueOnce(mockDriverUser);
      jest.spyOn(Notification, 'create').mockResolvedValue({});

      const res = await request(app)
        .put('/api/officer/complaints/CMP-101/assign')
        .set('Authorization', `Bearer ${officerToken}`)
        .send({
          driverId: 'DRV-882',
          driverName: 'Ramesh Kumar',
          vehicleNumber: 'DL-01-GB-4040'
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(mockComplaintDoc.status).toBe('ASSIGNED');
      expect(mockComplaintDoc.assignedDriverId).toBe('DRV-882');
    });

    it('should reject invalid status transition (e.g. SUBMITTED -> CLEANED) with 400 Bad Request', async () => {
      const mockComplaintDoc = {
        complaintId: 'CMP-101',
        status: 'SUBMITTED'
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValueOnce(mockComplaintDoc);

      const res = await request(app)
        .put('/api/officer/complaints/CMP-101/status')
        .set('Authorization', `Bearer ${officerToken}`)
        .send({ newStatus: 'CLEANED' });

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Illegal status transition');
    });
  });

  // =========================================================================
  // 6. DRIVER COLLECTION WORKFLOW & PHOTO EVIDENCE
  // =========================================================================
  describe('Driver Collection APIs', () => {
    beforeEach(() => {
      jest.spyOn(User, 'findById').mockResolvedValue(mockDriverUser);
    });

    it('should transition ASSIGNED complaint to PICKUP_IN_PROGRESS on startCollection', async () => {
      const mockTask = {
        complaintId: 'CMP-202',
        citizenId: mockCitizenUser.id,
        assignedDriverId: 'DRV-882',
        status: 'ASSIGNED',
        statusHistory: [],
        save: jest.fn().mockResolvedValue(true)
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValueOnce(mockTask);
      jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValue({});
      jest.spyOn(Notification, 'create').mockResolvedValue({});

      const res = await request(app)
        .put('/api/driver/complaints/CMP-202/start')
        .set('Authorization', `Bearer ${driverToken}`)
        .send({});

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(mockTask.status).toBe('PICKUP_IN_PROGRESS');
    });

    it('should FAIL completeCollection when after-cleaning photo is missing (400)', async () => {
      const res = await request(app)
        .put('/api/driver/complaints/CMP-202/complete')
        .set('Authorization', `Bearer ${driverToken}`)
        .send({
          afterCleaningImageUrl: '' // Blank photo
        });

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('After-cleaning photo evidence is strictly required');
    });

    it('should SUCCEED completeCollection when after-cleaning photo is provided', async () => {
      const mockTask = {
        complaintId: 'CMP-202',
        citizenId: mockCitizenUser.id,
        assignedDriverId: 'DRV-882',
        status: 'PICKUP_IN_PROGRESS',
        statusHistory: [],
        save: jest.fn().mockResolvedValue(true)
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValueOnce(mockTask);
      jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValue({});
      jest.spyOn(Notification, 'create').mockResolvedValue({});

      const res = await request(app)
        .put('/api/driver/complaints/CMP-202/complete')
        .set('Authorization', `Bearer ${driverToken}`)
        .send({
          afterCleaningImageUrl: 'https://storage.example.com/cleaned-photo.jpg'
        });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(mockTask.status).toBe('CLEANED');
      expect(mockTask.afterCleaningImageUrl).toBe('https://storage.example.com/cleaned-photo.jpg');
    });

    it('should update and query driver GPS location coordinates', async () => {
      const mockDriverDoc = {
        driverId: 'DRV-882',
        currentLocation: {
          latitude: 28.625,
          longitude: 77.215,
          timestamp: Date.now()
        }
      };

      jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValueOnce(mockDriverDoc);

      const putRes = await request(app)
        .put('/api/driver/location')
        .set('Authorization', `Bearer ${driverToken}`)
        .send({ latitude: 28.625, longitude: 77.215 });

      expect(putRes.status).toBe(200);
      expect(putRes.body.success).toBe(true);
      expect(putRes.body.data.latitude).toBe(28.625);

      jest.spyOn(Driver, 'findOne').mockResolvedValueOnce(mockDriverDoc);

      const getRes = await request(app)
        .get('/api/driver/location')
        .set('Authorization', `Bearer ${driverToken}`);

      expect(getRes.status).toBe(200);
      expect(getRes.body.data.latitude).toBe(28.625);
    });
  });

  // =========================================================================
  // 7. STATUS TRANSITION ENGINE UNIT CHECKS
  // =========================================================================
  describe('Status Transition Engine Rules', () => {
    it('should adhere to strict municipal state transition rules', () => {
      expect(canTransition('SUBMITTED', 'ASSIGNED')).toBe(true);
      expect(canTransition('SUBMITTED', 'REJECTED')).toBe(true);
      expect(canTransition('SUBMITTED', 'CLEANED')).toBe(false);

      expect(canTransition('ASSIGNED', 'PICKUP_IN_PROGRESS')).toBe(true);
      expect(canTransition('ASSIGNED', 'REJECTED')).toBe(true);
      expect(canTransition('ASSIGNED', 'VERIFIED')).toBe(false);

      expect(canTransition('PICKUP_IN_PROGRESS', 'CLEANED')).toBe(true);
      expect(canTransition('PICKUP_IN_PROGRESS', 'REJECTED')).toBe(false);

      expect(canTransition('CLEANED', 'VERIFIED')).toBe(true);
      expect(canTransition('VERIFIED', 'SUBMITTED')).toBe(false); // Terminal
      expect(canTransition('REJECTED', 'ASSIGNED')).toBe(false); // Terminal
    });

    it('should enforce driver-specific transition rules', () => {
      expect(canDriverTransition('ASSIGNED', 'PICKUP_IN_PROGRESS')).toBe(true);
      expect(canDriverTransition('PICKUP_IN_PROGRESS', 'CLEANED')).toBe(true);
      expect(canDriverTransition('SUBMITTED', 'ASSIGNED')).toBe(false); // Only officer
      expect(canDriverTransition('CLEANED', 'VERIFIED')).toBe(false); // Only officer
    });
  });
});
