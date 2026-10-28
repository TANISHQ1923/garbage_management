const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const syntheticDataService = require('../src/services/syntheticDataService');

describe('Citizen Cleanup Feedback API Test Suite', () => {
  let citizenToken;
  let otherCitizenToken;
  let officerToken;
  let driverToken;

  const mockCitizenUser = {
    _id: '507f191e810c19729de860ea',
    id: '507f191e810c19729de860ea',
    name: 'Aarav Sharma',
    email: 'citizen@example.com',
    role: 'CITIZEN'
  };

  const mockOtherCitizenUser = {
    _id: '507f191e810c19729de860ff',
    id: '507f191e810c19729de860ff',
    name: 'Pooja Verma',
    email: 'other_citizen@example.com',
    role: 'CITIZEN'
  };

  const mockOfficerUser = {
    _id: '507f191e810c19729de860eb',
    id: '507f191e810c19729de860eb',
    name: 'Vikram Patel',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER'
  };

  const mockDriverUser = {
    _id: '507f191e810c19729de860ec',
    id: '507f191e810c19729de860ec',
    name: 'Ramesh Kumar',
    email: 'driver@example.com',
    role: 'GARBAGE_DRIVER'
  };

  beforeAll(() => {
    citizenToken = jwt.sign(
      { id: mockCitizenUser.id, email: mockCitizenUser.email, role: mockCitizenUser.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    otherCitizenToken = jwt.sign(
      { id: mockOtherCitizenUser.id, email: mockOtherCitizenUser.email, role: mockOtherCitizenUser.role },
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

  beforeEach(() => {
    jest.clearAllMocks();
    // Default auth lookup
    jest.spyOn(User, 'findById').mockImplementation(async (id) => {
      const idStr = id ? id.toString() : '';
      if (idStr === mockCitizenUser.id) return mockCitizenUser;
      if (idStr === mockOtherCitizenUser.id) return mockOtherCitizenUser;
      if (idStr === mockOfficerUser.id) return mockOfficerUser;
      if (idStr === mockDriverUser.id) return mockDriverUser;
      return null;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // 1. Valid feedback submission
  it('1. should allow citizen to submit valid feedback on a VERIFIED complaint in MongoDB mode', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-FEEDBACK-001',
      citizenId: mockCitizenUser.id,
      citizenName: mockCitizenUser.name,
      status: 'VERIFIED',
      feedback: null,
      save: jest.fn().mockImplementation(function () {
        return Promise.resolve(this);
      }),
      toJSON: function () {
        return {
          id: this.complaintId,
          complaintId: this.complaintId,
          citizenId: this.citizenId,
          status: this.status,
          feedback: this.feedback
        };
      }
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .post('/api/complaints/CMP-FEEDBACK-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({
        rating: 5,
        comment: 'Excellent cleanup, very prompt service!'
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.feedback).toBeDefined();
    expect(res.body.data.feedback.rating).toBe(5);
    expect(res.body.data.feedback.comment).toBe('Excellent cleanup, very prompt service!');
    expect(res.body.data.feedback.citizenId).toBe(mockCitizenUser.id);
    expect(res.body.data.feedback.submittedAt).toBeGreaterThan(0);
    expect(mockComplaint.save).toHaveBeenCalled();
  });

  // 2. Rating below 1
  it('2. should reject feedback with rating below 1 with 400 Bad Request', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 0, comment: 'Too low' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Rating must be an integer between 1 and 5/i);
  });

  // 3. Rating above 5
  it('3. should reject feedback with rating above 5 with 400 Bad Request', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 6, comment: 'Too high' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Rating must be an integer between 1 and 5/i);
  });

  // 4. Non-integer rating
  it('4. should reject non-integer rating (e.g. 3.5) with 400 Bad Request', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 3.5, comment: 'Decimal rating' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Rating must be an integer between 1 and 5/i);
  });

  // 5. Feedback on complaint not yet VERIFIED
  it('5. should reject feedback on a complaint not yet VERIFIED with 400 Bad Request', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-IN-PROGRESS-001',
      citizenId: mockCitizenUser.id,
      status: 'PICKUP_IN_PROGRESS',
      feedback: null
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .post('/api/complaints/CMP-IN-PROGRESS-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 4, comment: 'Clean it up' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Feedback can only be submitted for verified cleanups');
  });

  // 6. Feedback on a REJECTED complaint
  it('6. should reject feedback on a REJECTED complaint with 400 Bad Request', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-REJECTED-001',
      citizenId: mockCitizenUser.id,
      status: 'REJECTED',
      feedback: null
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .post('/api/complaints/CMP-REJECTED-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 1, comment: 'Why rejected?' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Feedback cannot be submitted for rejected complaints');
  });

  // 7. Citizen attempting to submit feedback for another citizen's complaint
  it('7. should block citizen from submitting feedback for another citizen complaint with 403 Forbidden', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-OTHER-001',
      citizenId: mockOtherCitizenUser.id,
      status: 'VERIFIED',
      feedback: null
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .post('/api/complaints/CMP-OTHER-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 5, comment: 'Good job' });

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Access denied');
  });

  // 8. Citizen attempting duplicate feedback
  it('8. should prevent duplicate feedback submission with 400 Bad Request', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-ALREADY-RATED-001',
      citizenId: mockCitizenUser.id,
      status: 'VERIFIED',
      feedback: {
        rating: 4,
        comment: 'First rating',
        submittedAt: Date.now() - 10000,
        citizenId: mockCitizenUser.id
      }
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .post('/api/complaints/CMP-ALREADY-RATED-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 5, comment: 'Second attempt' });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Feedback has already been submitted for this complaint');
  });

  // 9. Unauthenticated request
  it('9. should reject unauthenticated feedback requests with 401 Unauthorized', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .send({ rating: 5, comment: 'No token' });

    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });

  // 10. Officer or driver attempting to submit citizen feedback
  it('10a. should block municipal officer from submitting cleanup feedback with 403 Forbidden', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${officerToken}`)
      .send({ rating: 5, comment: 'Officer trying to give feedback' });

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Forbidden');
  });

  it('10b. should block garbage driver from submitting cleanup feedback with 403 Forbidden', async () => {
    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ rating: 5, comment: 'Driver trying to give feedback' });

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Forbidden');
  });

  // 11. Invalid or excessively long comment
  it('11. should reject feedback with comment exceeding 500 characters with 400 Bad Request', async () => {
    const longComment = 'A'.repeat(501);

    const res = await request(app)
      .post('/api/complaints/CMP-001/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 4, comment: longComment });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toContain('Comment cannot exceed 500 characters');
  });

  // 12. Existing complaint workflows remain unaffected & Synthetic Mode Feedback
  it('12a. should support in-memory feedback submission safely in synthetic demo mode', async () => {
    const mockSyntheticComplaint = {
      complaintId: 'CMP-SYN-VERIFIED-01',
      id: 'CMP-SYN-VERIFIED-01',
      citizenId: mockCitizenUser.id,
      citizenName: mockCitizenUser.name,
      status: 'VERIFIED',
      feedback: null
    };

    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(true);
    jest.spyOn(syntheticDataService, 'getSyntheticComplaints').mockReturnValue([mockSyntheticComplaint]);

    const res = await request(app)
      .post('/api/complaints/CMP-SYN-VERIFIED-01/feedback')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({ rating: 4, comment: 'Quick response in synthetic demo mode' });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.feedback).toBeDefined();
    expect(res.body.data.feedback.rating).toBe(4);
    expect(res.body.data.feedback.comment).toBe('Quick response in synthetic demo mode');
    expect(mockSyntheticComplaint.feedback.rating).toBe(4);
  });

  it('12b. should preserve existing complaint details fetching workflow', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockComplaint = {
      complaintId: 'CMP-EXISTING-001',
      citizenId: mockCitizenUser.id,
      status: 'VERIFIED',
      feedback: {
        rating: 5,
        comment: 'Great work',
        submittedAt: 1700000000000
      },
      toJSON: function () {
        return {
          id: this.complaintId,
          complaintId: this.complaintId,
          citizenId: this.citizenId,
          status: this.status,
          feedback: this.feedback
        };
      }
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .get('/api/complaints/CMP-EXISTING-001')
      .set('Authorization', `Bearer ${citizenToken}`);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.feedback.rating).toBe(5);
  });
});
