const request = require('supertest');
const jwt = require('jsonwebtoken');
const { Writable } = require('stream');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const cloudinary = require('cloudinary').v2;
const mediaService = require('../src/services/mediaService');

describe('Step 8: Cloudinary Media Storage & Evidence Integration', () => {
  let citizenToken;
  let driverToken;
  let officerToken;
  let otherCitizenToken;

  const mockCitizen = {
    _id: '507f191e810c19729de860e1',
    id: '507f191e810c19729de860e1',
    name: 'Citizen One',
    email: 'citizen1@example.com',
    role: 'CITIZEN'
  };

  const mockOtherCitizen = {
    _id: '507f191e810c19729de860e2',
    id: '507f191e810c19729de860e2',
    name: 'Citizen Two',
    email: 'citizen2@example.com',
    role: 'CITIZEN'
  };

  const mockDriver = {
    _id: '507f191e810c19729de860e3',
    id: '507f191e810c19729de860e3',
    name: 'Driver Ramesh',
    email: 'driver@example.com',
    role: 'GARBAGE_DRIVER',
    driverId: 'DRV-882'
  };

  const mockOfficer = {
    _id: '507f191e810c19729de860e4',
    id: '507f191e810c19729de860e4',
    name: 'Officer Vikram',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER'
  };

  beforeAll(() => {
    // Configure test environment credentials for Cloudinary
    env.CLOUDINARY_CLOUD_NAME = 'test_cloud';
    env.CLOUDINARY_API_KEY = 'test_key';
    env.CLOUDINARY_API_SECRET = 'test_secret';

    citizenToken = jwt.sign(
      { id: mockCitizen.id, email: mockCitizen.email, role: mockCitizen.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    otherCitizenToken = jwt.sign(
      { id: mockOtherCitizen.id, email: mockOtherCitizen.email, role: mockOtherCitizen.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
    driverToken = jwt.sign(
      { id: mockDriver.id, email: mockDriver.email, role: mockDriver.role },
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

    // Mock User.findById for auth middleware
    jest.spyOn(User, 'findById').mockImplementation((id) => {
      const idStr = id ? id.toString() : '';
      if (idStr === mockCitizen.id) return Promise.resolve(mockCitizen);
      if (idStr === mockOtherCitizen.id) return Promise.resolve(mockOtherCitizen);
      if (idStr === mockDriver.id) return Promise.resolve(mockDriver);
      if (idStr === mockOfficer.id) return Promise.resolve(mockOfficer);
      return Promise.resolve(null);
    });

    // Mock Cloudinary upload_stream using a simple writable stream
    jest.spyOn(cloudinary.uploader, 'upload_stream').mockImplementation((options, callback) => {
      return new Writable({
        write(chunk, encoding, next) {
          next();
        },
        final(next) {
          const resType = options.resource_type || 'image';
          callback(null, {
            secure_url: `https://res.cloudinary.com/test_cloud/${resType}/upload/${options.folder || 'complaints'}/test_asset.jpg`,
            public_id: options.public_id || `${options.folder || 'complaints'}/test_asset_123`,
            resource_type: resType
          });
          next();
        }
      });
    });

    // Mock Cloudinary destroy
    jest.spyOn(cloudinary.uploader, 'destroy').mockResolvedValue({ result: 'ok' });
  });

  // -------------------------------------------------------------------------
  // 1. Unauthenticated Media Upload Rejected
  // -------------------------------------------------------------------------
  it('1. should reject unauthenticated media upload with 401', async () => {
    const res = await request(app)
      .post('/api/media/upload')
      .attach('file', Buffer.from('fake image content'), {
        filename: 'test.jpg',
        contentType: 'image/jpeg'
      });

    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });

  // -------------------------------------------------------------------------
  // 2. Unsupported MIME Rejected
  // -------------------------------------------------------------------------
  it('2. should reject unsupported MIME type with 400 Bad Request', async () => {
    const res = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${citizenToken}`)
      .attach('file', Buffer.from('%PDF-1.4 test content'), {
        filename: 'document.pdf',
        contentType: 'application/pdf'
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Unsupported media format/i);
  });

  // -------------------------------------------------------------------------
  // 3. Oversized File Rejected
  // -------------------------------------------------------------------------
  it('3. should reject oversized file with 400 Bad Request', async () => {
    // Generate a buffer larger than 10MB (MAX_IMAGE_SIZE_MB = 10)
    const oversizedBuffer = Buffer.alloc(11 * 1024 * 1024);

    const res = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${citizenToken}`)
      .attach('file', oversizedBuffer, {
        filename: 'huge_photo.jpg',
        contentType: 'image/jpeg'
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/exceeds the maximum allowed size/i);
  });

  // -------------------------------------------------------------------------
  // 4. Authenticated Image Upload Flow
  // -------------------------------------------------------------------------
  it('4. should upload valid image and return secure URL and publicId with 200', async () => {
    const smallImageBuffer = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46]);

    const res = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${citizenToken}`)
      .field('folder', 'complaints')
      .field('complaintId', 'CMP-202609-001')
      .attach('file', smallImageBuffer, {
        filename: 'garbage_photo.jpg',
        contentType: 'image/jpeg'
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('url');
    expect(res.body.data.url).toMatch(/^https:\/\/res\.cloudinary\.com\//);
    expect(res.body.data).toHaveProperty('publicId');
    expect(res.body.data.resourceType).toBe('image');
    // Ensure secrets are never exposed in response
    expect(res.body).not.toHaveProperty('api_secret');
    expect(res.body).not.toHaveProperty('api_key');
  });

  // -------------------------------------------------------------------------
  // 5. Authenticated Video Upload Flow
  // -------------------------------------------------------------------------
  it('5. should upload valid video and return video resource type', async () => {
    const videoBuffer = Buffer.from('fake mp4 video bytes');

    const res = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${citizenToken}`)
      .field('folder', 'videos')
      .attach('file', videoBuffer, {
        filename: 'overflow_clip.mp4',
        contentType: 'video/mp4'
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.resourceType).toBe('video');
    expect(res.body.data.url).toContain('cloudinary.com');
  });

  // -------------------------------------------------------------------------
  // 6. Citizen Cannot Attach / Access Another Citizen's Complaint
  // -------------------------------------------------------------------------
  it("6. should forbid a citizen from viewing or escalating another citizen's complaint", async () => {
    const mockComplaint = {
      complaintId: 'CMP-OTHER-01',
      citizenId: mockCitizen.id, // belongs to mockCitizen
      status: 'SUBMITTED'
    };
    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .get('/api/complaints/CMP-OTHER-01')
      .set('Authorization', `Bearer ${otherCitizenToken}`);

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Access denied/i);
  });

  // -------------------------------------------------------------------------
  // 7. Driver Cannot Attach Evidence to Another Driver's Complaint
  // -------------------------------------------------------------------------
  it("7. should forbid driver from attaching evidence to another driver's task", async () => {
    const mockComplaint = {
      complaintId: 'CMP-DRV-999',
      assignedDriverId: 'DRV-DIFFERENT', // Not DRV-882
      status: 'PICKUP_IN_PROGRESS'
    };
    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);

    const res = await request(app)
      .put('/api/driver/complaints/CMP-DRV-999/evidence')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({
        beforeCleaningImageUrl: 'https://res.cloudinary.com/test/before.jpg'
      });

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Access denied/i);
  });

  // -------------------------------------------------------------------------
  // 8. After-Cleaning Evidence Required Before Completion
  // -------------------------------------------------------------------------
  it('8. should reject completeCollection if after-cleaning photo is missing', async () => {
    const res = await request(app)
      .put('/api/driver/complaints/CMP-DRV-001/complete')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({
        afterCleaningImageUrl: '' // Missing!
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/After-cleaning photo evidence is strictly required/i);
  });

  // -------------------------------------------------------------------------
  // 9. Complaint Stores Returned Media URL & Public ID
  // -------------------------------------------------------------------------
  it('9. should store imageUrl and imagePublicId when citizen creates a complaint', async () => {
    const fakeHttpsUrl = 'https://res.cloudinary.com/test_cloud/image/upload/smart-garbage/complaints/complaint_1.jpg';
    const fakePublicId = 'smart-garbage/complaints/complaint_1';

    let capturedSaveData = null;
    jest.spyOn(Complaint, 'create').mockImplementation(async (data) => {
      capturedSaveData = data;
      return {
        ...data,
        id: data.complaintId,
        toJSON: () => ({ ...data, id: data.complaintId })
      };
    });

    const res = await request(app)
      .post('/api/complaints')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({
        garbageType: 'OVERFLOWING_BIN',
        description: 'Bin overflowing near the main gate',
        imageUrl: fakeHttpsUrl,
        imagePublicId: fakePublicId,
        locationDescription: 'Sector 5 Market Gate 2',
        emergency: false
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(capturedSaveData).not.toBeNull();
    expect(capturedSaveData.imageUrl).toBe(fakeHttpsUrl);
    expect(capturedSaveData.imagePublicId).toBe(fakePublicId);
  });

  // -------------------------------------------------------------------------
  // 10. Replacement Deletes Old Media Only After New Update Succeeds
  // -------------------------------------------------------------------------
  it('10. should safely delete old Cloudinary asset when evidence photo is replaced', async () => {
    const oldPublicId = 'smart-garbage/before-cleaning/old_photo_123';
    const newPublicId = 'smart-garbage/before-cleaning/new_photo_456';
    const newUrl = 'https://res.cloudinary.com/test_cloud/image/upload/new_photo_456.jpg';

    const mockComplaint = {
      complaintId: 'CMP-TASK-001',
      assignedDriverId: 'DRV-882',
      beforeCleaningImageUrl: 'https://res.cloudinary.com/test_cloud/image/upload/old_photo_123.jpg',
      beforeCleaningImagePublicId: oldPublicId,
      save: jest.fn().mockResolvedValue(true)
    };

    jest.spyOn(Complaint, 'findOne').mockResolvedValue(mockComplaint);
    const destroySpy = jest.spyOn(cloudinary.uploader, 'destroy');

    const res = await request(app)
      .put('/api/driver/complaints/CMP-TASK-001/evidence')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({
        beforeCleaningImageUrl: newUrl,
        beforeCleaningImagePublicId: newPublicId
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(mockComplaint.beforeCleaningImageUrl).toBe(newUrl);
    expect(mockComplaint.beforeCleaningImagePublicId).toBe(newPublicId);
    expect(mockComplaint.save).toHaveBeenCalled();
    // Old asset deletion should be invoked with the OLD public ID
    expect(destroySpy).toHaveBeenCalledWith(oldPublicId, { resource_type: 'image' });
  });

  // -------------------------------------------------------------------------
  // 11. Configuration Error When Cloudinary Credentials Missing
  // -------------------------------------------------------------------------
  it('11. should return 503 configuration error only when media endpoint is used without Cloudinary config', async () => {
    // Temporarily unset cloud name
    const origCloudName = env.CLOUDINARY_CLOUD_NAME;
    env.CLOUDINARY_CLOUD_NAME = '';

    const res = await request(app)
      .post('/api/media/upload')
      .set('Authorization', `Bearer ${citizenToken}`)
      .attach('file', Buffer.from('photo'), {
        filename: 'test.jpg',
        contentType: 'image/jpeg'
      });

    expect(res.status).toBe(503);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/Cloudinary media storage is not configured/i);

    // Restore config
    env.CLOUDINARY_CLOUD_NAME = origCloudName;
  });
});
