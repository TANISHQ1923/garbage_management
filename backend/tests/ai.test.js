const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const aiService = require('../src/services/aiService');
const notificationService = require('../src/services/notificationService');

describe('Step 9: AI Waste Classification and Severity Estimation Integration', () => {
  let citizenToken;
  let officerToken;

  const mockCitizen = {
    _id: '507f191e810c19729de860e1',
    id: '507f191e810c19729de860e1',
    name: 'Citizen Ananya',
    email: 'ananya@example.com',
    role: 'CITIZEN'
  };

  const mockOfficer = {
    _id: '507f191e810c19729de860e4',
    id: '507f191e810c19729de860e4',
    name: 'Officer Vikram',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER'
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
  });

  beforeEach(() => {
    jest.clearAllMocks();

    jest.spyOn(notificationService, 'createNotification').mockResolvedValue(true);

    jest.spyOn(User, 'findById').mockImplementation((id) => {
      const idStr = id ? id.toString() : '';
      if (idStr === mockCitizen.id) return Promise.resolve(mockCitizen);
      if (idStr === mockOfficer.id) return Promise.resolve(mockOfficer);
      return Promise.resolve(null);
    });
  });

  describe('Citizen Complaint Submission with AI Analysis', () => {
    test('1. Complaint submitted WITHOUT image remains functional and does not invoke AI', async () => {
      const aiSpy = jest.spyOn(aiService, 'analyzeGarbageImage');

      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => ({
        ...doc,
        _id: '607f191e810c19729de860a1',
        id: doc.complaintId,
        toJSON: () => doc
      }));

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'OVERFLOWING_BIN',
          description: 'No photo report',
          locationText: 'Near Sector 4 Market'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(aiSpy).not.toHaveBeenCalled();
      expect(res.body.data.aiAnalysis).toBeNull();
    });

    test('2. Complaint submitted WITH image triggers AI analysis service', async () => {
      const aiSpy = jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: true,
        status: 'COMPLETED',
        data: {
          classification: { category: 'PLASTIC', confidence: null },
          severity: { level: 'HIGH', confidence: null },
          modelStatus: 'DEVELOPMENT_BASELINE',
          processedAt: new Date().toISOString()
        }
      });

      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => ({
        ...doc,
        _id: '607f191e810c19729de860a2',
        id: doc.complaintId,
        toJSON: () => doc
      }));

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'ROADSIDE_GARBAGE',
          description: 'Plastic bottles scattered',
          imageUrl: 'https://res.cloudinary.com/test/image/upload/smart-garbage/complaints/plastic.jpg',
          locationText: 'Outer Ring Road Bus Stop'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(aiSpy).toHaveBeenCalledWith('https://res.cloudinary.com/test/image/upload/smart-garbage/complaints/plastic.jpg');
      expect(res.body.data.aiAnalysisStatus).toBe('COMPLETED');
      expect(res.body.data.aiAnalysis.classification.category).toBe('PLASTIC');
      expect(res.body.data.aiAnalysis.severity.level).toBe('HIGH');
      expect(res.body.data.aiAnalysis.modelStatus).toBe('DEVELOPMENT_BASELINE');
    });

    test('3. AI service UNAVAILABLE does not crash or delete complaint (sets AI_UNAVAILABLE)', async () => {
      jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: false,
        status: 'AI_UNAVAILABLE',
        error: 'AI service request timed out'
      });

      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => ({
        ...doc,
        _id: '607f191e810c19729de860a3',
        id: doc.complaintId,
        toJSON: () => doc
      }));

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'EVENT_WASTE',
          description: 'Fairground litter',
          imageUrl: 'https://res.cloudinary.com/test/image/upload/event.jpg',
          locationText: 'Community Park'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data.aiAnalysisStatus).toBe('AI_UNAVAILABLE');
      expect(res.body.data.status).toBe('SUBMITTED'); // Complaint preserved safely
    });

    test('4. AI analysis failure (e.g. invalid format) records FAILED status gracefully', async () => {
      jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: false,
        status: 'FAILED',
        error: 'Unsupported image format'
      });

      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => ({
        ...doc,
        _id: '607f191e810c19729de860a4',
        id: doc.complaintId,
        toJSON: () => doc
      }));

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'OVERFLOWING_BIN',
          description: 'Corrupted image test',
          imageUrl: 'https://example.com/bad.png',
          locationText: 'Station Road'
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data.aiAnalysisStatus).toBe('FAILED');
      expect(res.body.data.status).toBe('SUBMITTED');
    });

    test('5. AI analysis NEVER modifies official complaint status or auto-rejects', async () => {
      jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: true,
        status: 'COMPLETED',
        data: {
          classification: { category: 'OTHER', confidence: null },
          severity: { level: 'LOW', confidence: null },
          modelStatus: 'DEVELOPMENT_BASELINE'
        }
      });

      let savedStatus = null;
      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => {
        savedStatus = doc.status;
        return {
          ...doc,
          _id: '607f191e810c19729de860a5',
          id: doc.complaintId,
          toJSON: () => doc
        };
      });

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'OTHER_WASTE',
          description: 'Low severity waste',
          imageUrl: 'https://res.cloudinary.com/test/image/upload/other.jpg',
          locationText: 'Lane 2'
        });

      expect(res.status).toBe(201);
      expect(savedStatus).toBe('SUBMITTED');
      expect(res.body.data.status).toBe('SUBMITTED');
    });

    test('6. Client cannot tamper with or fabricate aiAnalysis in request body', async () => {
      jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: true,
        status: 'COMPLETED',
        data: {
          classification: { category: 'METAL', confidence: null },
          severity: { level: 'MEDIUM', confidence: null },
          modelStatus: 'DEVELOPMENT_BASELINE'
        }
      });

      jest.spyOn(Complaint, 'create').mockImplementation(async (doc) => ({
        ...doc,
        _id: '607f191e810c19729de860a6',
        id: doc.complaintId,
        toJSON: () => doc
      }));

      const res = await request(app)
        .post('/api/complaints')
        .set('Authorization', `Bearer ${citizenToken}`)
        .send({
          garbageType: 'ROADSIDE_GARBAGE',
          description: 'Attempting to inject fake 99% accuracy',
          imageUrl: 'https://res.cloudinary.com/test/image/upload/metal.jpg',
          locationText: 'Main Street',
          aiAnalysis: {
            classification: { category: 'GOLD', confidence: 0.99 },
            modelStatus: 'SUPER_AI'
          }
        });

      expect(res.status).toBe(201);
      // Injected values must be discarded in favor of backend AI service result
      expect(res.body.data.aiAnalysis.classification.category).toBe('METAL');
      expect(res.body.data.aiAnalysis.modelStatus).toBe('DEVELOPMENT_BASELINE');
    });
  });

  describe('Manual / Re-trigger AI Analysis Endpoint', () => {
    test('7. POST /api/complaints/:id/analyze-ai successfully updates AI analysis on existing complaint', async () => {
      const mockComplaintDoc = {
        complaintId: 'CMP-123456-7890',
        citizenId: mockCitizen.id,
        imageUrl: 'https://res.cloudinary.com/test/image/upload/sample.jpg',
        status: 'SUBMITTED',
        aiAnalysis: null,
        aiAnalysisStatus: 'PENDING',
        save: jest.fn().mockResolvedValue(true),
        toJSON: function() { return this; }
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaintDoc);
      jest.spyOn(aiService, 'analyzeGarbageImage').mockResolvedValue({
        success: true,
        status: 'COMPLETED',
        data: {
          classification: { category: 'GLASS', confidence: null },
          severity: { level: 'CRITICAL', confidence: null },
          modelStatus: 'DEVELOPMENT_BASELINE',
          processedAt: new Date().toISOString()
        }
      });

      const res = await request(app)
        .post('/api/complaints/CMP-123456-7890/analyze-ai')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(mockComplaintDoc.aiAnalysis.classification.category).toBe('GLASS');
      expect(mockComplaintDoc.aiAnalysis.severity.level).toBe('CRITICAL');
      expect(mockComplaintDoc.aiAnalysisStatus).toBe('COMPLETED');
      expect(mockComplaintDoc.save).toHaveBeenCalled();
    });

    test('8. POST /api/complaints/:id/analyze-ai returns 400 if complaint has no image', async () => {
      const mockComplaintDoc = {
        complaintId: 'CMP-123456-7891',
        citizenId: mockCitizen.id,
        imageUrl: null,
        status: 'SUBMITTED'
      };

      jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaintDoc);

      const res = await request(app)
        .post('/api/complaints/CMP-123456-7891/analyze-ai')
        .set('Authorization', `Bearer ${officerToken}`);

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('does not contain an image');
    });
  });
});
