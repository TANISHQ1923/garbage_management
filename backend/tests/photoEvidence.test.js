// HTTP/validation tests use mocks; actual upload and transaction verification are separate.
jest.mock('../src/config/db', () => ({ getDBStatus: () => ({ isConnected: true }) }));
jest.mock('../src/services/taskTransitionService', () => ({ commitTransition: jest.fn(async c => c) }));
jest.mock('../src/services/mediaService', () => ({
  uploadMedia: jest.fn(async () => ({ url: 'https://example.invalid/stored.jpg', publicId: 'test/actual-provider-id', resourceType: 'image' })),
  deleteAsset: jest.fn(async () => ({}))
}));
const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const StoredMedia = require('../src/models/StoredMedia');
const Notification = require('../src/models/Notification');
const { commitTransition } = require('../src/services/taskTransitionService');
const media = require('../src/services/mediaService');
const driver = { id: '507f191e810c19729de860ea', name: 'Driver', role: 'GARBAGE_DRIVER', driverId: 'DRV-1' };
let task;
const token = () => jwt.sign({ id: driver.id }, env.JWT_SECRET);
const complete = body => request(app).put('/api/driver/complaints/EXISTING/complete').set('Authorization', 'Bearer ' + token()).send(body);
beforeEach(() => {
  jest.restoreAllMocks(); commitTransition.mockReset(); commitTransition.mockImplementation(async c => c);
  task = { complaintId: 'EXISTING', source: 'SENSOR', assignedDriverId: 'DRV-1', status: 'PICKUP_IN_PROGRESS', statusHistory: [], save: jest.fn() };
  jest.spyOn(User, 'findById').mockResolvedValue(driver);
  jest.spyOn(Complaint, 'findOne').mockImplementation(async () => task);
  jest.spyOn(StoredMedia, 'findOne').mockResolvedValue(null);
  jest.spyOn(StoredMedia, 'create').mockResolvedValue({});
  jest.spyOn(Notification, 'create').mockResolvedValue({});
});
test('local preview URI cannot complete a task', async () => {
  const r = await complete({ afterCleaningImageUrl: 'content://photo/after' });
  expect(r.status).toBe(400); expect(task.status).toBe('PICKUP_IN_PROGRESS'); expect(commitTransition).not.toHaveBeenCalled();
});
test('arbitrary HTTPS photo without a task/driver upload record is rejected', async () => {
  const r = await complete({ afterCleaningImageUrl: 'https://example.invalid/fake.jpg' });
  expect(r.status).toBe(400); expect(commitTransition).not.toHaveBeenCalled();
});
test('completion checks assignment and lifecycle before evidence', async () => {
  task.assignedDriverId = 'OTHER'; expect((await complete({ afterCleaningImageUrl: 'https://example.invalid/after.jpg' })).status).toBe(403);
  task.assignedDriverId = 'DRV-1'; task.status = 'ASSIGNED';
  expect((await complete({ afterCleaningImageUrl: 'https://example.invalid/after.jpg' })).status).toBe(400);
  expect(StoredMedia.findOne).not.toHaveBeenCalled();
});
test('completion uses server-recorded public ID, never the submitted ID', async () => {
  StoredMedia.findOne.mockResolvedValue({ publicId: 'provider/recorded-id', url: 'https://example.invalid/after.jpg' });
  const r = await complete({ afterCleaningImageUrl: 'https://example.invalid/after.jpg', afterCleaningImagePublicId: 'forged-client-id' });
  expect(r.status).toBe(200); expect(task.afterCleaningImagePublicId).toBe('provider/recorded-id');
  expect(StoredMedia.findOne).toHaveBeenCalledWith({ url: 'https://example.invalid/after.jpg', complaintId: 'EXISTING', folder: 'after-cleaning', uploadedBy: driver.id, resourceType: 'image' });
});
test('transaction errors are returned and cannot report completion success', async () => {
  StoredMedia.findOne.mockResolvedValue({ publicId: 'provider/id' });
  commitTransition.mockRejectedValue(Object.assign(new Error('Transaction service unavailable. Retry later.'), { statusCode: 503 }));
  const r = await complete({ afterCleaningImageUrl: 'https://example.invalid/after.jpg' });
  expect(r.status).toBe(503); expect(r.body.success).toBe(false); expect(r.body.message).toContain('Transaction service unavailable');
});
test('assigned driver upload records a task-bound stored-media reference', async () => {
  const r = await request(app).post('/api/media/upload').set('Authorization', 'Bearer ' + token())
    .field('folder', 'after-cleaning').field('complaintId', 'EXISTING')
    .attach('file', Buffer.from('mocked image bytes'), { filename: 'after.jpg', contentType: 'image/jpeg' });
  expect(r.status).toBe(200);
  expect(StoredMedia.create).toHaveBeenCalledWith(expect.objectContaining({ complaintId: 'EXISTING', uploadedBy: driver.id, folder: 'after-cleaning', publicId: 'test/actual-provider-id' }));
});
test('another driver cannot upload evidence for this task', async () => {
  task.assignedDriverId = 'OTHER'; media.uploadMedia.mockClear();
  const r = await request(app).post('/api/media/upload').set('Authorization', 'Bearer ' + token())
    .field('folder', 'before-cleaning').field('complaintId', 'EXISTING')
    .attach('file', Buffer.from('mocked bytes'), { filename: 'before.jpg', contentType: 'image/jpeg' });
  expect(r.status).toBe(403); expect(media.uploadMedia).not.toHaveBeenCalled();
});
