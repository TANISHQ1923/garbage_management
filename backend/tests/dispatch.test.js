const request = require('supertest');
const jwt = require('jsonwebtoken');
const app = require('../src/app');
const env = require('../src/config/env');
const User = require('../src/models/User');
const Complaint = require('../src/models/Complaint');
const Driver = require('../src/models/Driver');
const Notification = require('../src/models/Notification');
const syntheticDataService = require('../src/services/syntheticDataService');
const {
  calculateHaversineDistanceKm,
  isValidCoordinate,
  rankCandidateDrivers,
  autoDispatchComplaint
} = require('../src/services/dispatchService');

describe('Step 2: Automatic Nearby Driver Assignment & Notifications Test Suite', () => {
  let citizenToken;
  let officerToken;
  let driverToken;

  const mockCitizen = {
    _id: '507f191e810c19729de860ea',
    id: '507f191e810c19729de860ea',
    name: 'Aarav Sharma',
    email: 'citizen@example.com',
    role: 'CITIZEN',
    phone: '+91 9876543210'
  };

  const mockOfficer = {
    _id: '507f191e810c19729de860eb',
    id: '507f191e810c19729de860eb',
    name: 'Vikram Patel',
    email: 'officer@example.com',
    role: 'MUNICIPAL_OFFICER',
    employeeId: 'MC-OFF-2026'
  };

  const mockDriver = {
    _id: '507f191e810c19729de860ec',
    id: '507f191e810c19729de860ec',
    name: 'Ramesh Kumar',
    email: 'driver@example.com',
    role: 'GARBAGE_DRIVER',
    driverId: 'DRV-882'
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
    driverToken = jwt.sign(
      { id: mockDriver.id, email: mockDriver.email, role: mockDriver.role },
      env.JWT_SECRET,
      { expiresIn: '1h' }
    );
  });

  beforeEach(() => {
    jest.restoreAllMocks();
    syntheticDataService.resetSyntheticCache();

    jest.spyOn(User, 'findById').mockImplementation((id) => {
      const idStr = id ? id.toString() : '';
      if (idStr === mockCitizen.id) return Promise.resolve(mockCitizen);
      if (idStr === mockOfficer.id) return Promise.resolve(mockOfficer);
      if (idStr === mockDriver.id) return Promise.resolve(mockDriver);
      return Promise.resolve(null);
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  // 1. Closest AVAILABLE driver selected
  test('1. selects closest AVAILABLE driver based on Haversine distance', () => {
    const complaintLoc = { latitude: 28.6139, longitude: 77.2090 };
    const drivers = [
      {
        driverId: 'DRV-FAR',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.7000, longitude: 77.3000 } // ~13 km
      },
      {
        driverId: 'DRV-CLOSE',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6150, longitude: 77.2100 } // ~0.16 km
      },
      {
        driverId: 'DRV-MID',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6300, longitude: 77.2200 } // ~2.1 km
      }
    ];

    const ranked = rankCandidateDrivers(complaintLoc, drivers);
    expect(ranked.length).toBe(3);
    expect(ranked[0].driver.driverId).toBe('DRV-CLOSE');
    expect(ranked[0].distanceKm).toBeLessThan(ranked[1].distanceKm);
    expect(ranked[1].driver.driverId).toBe('DRV-MID');
    expect(ranked[2].driver.driverId).toBe('DRV-FAR');
  });

  // 2. BUSY drivers excluded
  test('2. excludes BUSY drivers even if they are geographically closer', () => {
    const complaintLoc = { latitude: 28.6139, longitude: 77.2090 };
    const drivers = [
      {
        driverId: 'DRV-BUSY',
        availability: 'BUSY',
        currentLocation: { latitude: 28.6140, longitude: 77.2091 } // closest (~15 meters)
      },
      {
        driverId: 'DRV-AVAILABLE',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6250, longitude: 77.2180 } // ~1.5 km
      }
    ];

    const ranked = rankCandidateDrivers(complaintLoc, drivers);
    expect(ranked.length).toBe(1);
    expect(ranked[0].driver.driverId).toBe('DRV-AVAILABLE');
  });

  // 3. OFFLINE drivers excluded
  test('3. excludes OFFLINE drivers from dispatch consideration', () => {
    const complaintLoc = { latitude: 28.6139, longitude: 77.2090 };
    const drivers = [
      {
        driverId: 'DRV-OFFLINE',
        availability: 'OFFLINE',
        currentLocation: { latitude: 28.6140, longitude: 77.2091 }
      },
      {
        driverId: 'DRV-AVAILABLE',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6300, longitude: 77.2250 }
      }
    ];

    const ranked = rankCandidateDrivers(complaintLoc, drivers);
    expect(ranked.length).toBe(1);
    expect(ranked[0].driver.driverId).toBe('DRV-AVAILABLE');
  });

  // 4. Missing driver coordinates handled safely
  test('4. handles missing or invalid driver coordinates without error', () => {
    const complaintLoc = { latitude: 28.6139, longitude: 77.2090 };
    const drivers = [
      {
        driverId: 'DRV-NO-COORDS',
        availability: 'AVAILABLE',
        currentLocation: null
      },
      {
        driverId: 'DRV-NAN-COORDS',
        availability: 'AVAILABLE',
        currentLocation: { latitude: NaN, longitude: 77.2090 }
      },
      {
        driverId: 'DRV-VALID',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6200, longitude: 77.2150 }
      }
    ];

    const ranked = rankCandidateDrivers(complaintLoc, drivers);
    expect(ranked.length).toBe(1);
    expect(ranked[0].driver.driverId).toBe('DRV-VALID');
  });

  // 5. Missing complaint coordinates handled safely
  test('5. handles missing complaint coordinates safely and returns PENDING_DRIVER', async () => {
    const complaint = {
      complaintId: 'CMP-TEST-NO-COORD',
      citizenId: 'CIT-01',
      latitude: null,
      longitude: null,
      status: 'SUBMITTED'
    };

    const result = await autoDispatchComplaint(complaint);
    expect(result.assigned).toBe(false);
    expect(result.assignmentStatus).toBe('PENDING_DRIVER');
    expect(result.reason).toBe('MISSING_COMPLAINT_COORDINATES');
    expect(complaint.status).toBe('SUBMITTED');
  });

  // 6. No available driver leaves complaint unassigned
  test('6. leaves complaint unassigned when all drivers are busy or offline', async () => {
    const complaintLoc = { latitude: 28.6139, longitude: 77.2090 };
    const drivers = [
      { driverId: 'D1', availability: 'BUSY', currentLocation: complaintLoc },
      { driverId: 'D2', availability: 'OFFLINE', currentLocation: complaintLoc }
    ];

    const ranked = rankCandidateDrivers(complaintLoc, drivers);
    expect(ranked.length).toBe(0);
  });

  // 7. Successful assignment updates driver availability consistently
  test('7. updates driver availability to BUSY upon successful assignment', async () => {
    syntheticDataService.resetSyntheticCache();
    const availableDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.availability === 'AVAILABLE');
    expect(availableDriver).toBeDefined();

    const complaint = {
      complaintId: 'CMP-AUTO-001',
      citizenId: 'CIT-01',
      latitude: availableDriver.currentLocation.latitude,
      longitude: availableDriver.currentLocation.longitude,
      status: 'SUBMITTED',
      locationText: 'Test Location'
    };

    const result = await autoDispatchComplaint(complaint);
    expect(result.assigned).toBe(true);
    expect(result.assignmentStatus).toBe('ASSIGNED');
    expect(result.assignedDriverId).toBe(availableDriver.driverId);

    // Verify driver is now BUSY in synthetic memory
    const updatedDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.driverId === availableDriver.driverId);
    expect(updatedDriver.availability).toBe('BUSY');
  });

  // 8. Successful assignment creates a driver notification
  test('8. creates an in-app DRIVER_ASSIGNMENT notification for the assigned driver', async () => {
    syntheticDataService.resetSyntheticCache();
    const availableDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.availability === 'AVAILABLE');

    const complaint = {
      complaintId: 'CMP-NOTIF-DRV',
      citizenId: 'CIT-01',
      latitude: availableDriver.currentLocation.latitude,
      longitude: availableDriver.currentLocation.longitude,
      status: 'SUBMITTED',
      locationText: 'Park Street Market'
    };

    await autoDispatchComplaint(complaint);

    const driverNotifications = syntheticDataService.getSyntheticNotifications(availableDriver.driverId);
    const assignmentNotif = driverNotifications.find(
      (n) => n.complaintId === 'CMP-NOTIF-DRV' && n.type === 'DRIVER_ASSIGNMENT'
    );
    expect(assignmentNotif).toBeDefined();
    expect(assignmentNotif.title).toContain('Collection Assignment');
    expect(assignmentNotif.isRead).toBe(false);
  });

  // 9. Successful assignment creates citizen notification
  test('9. creates an in-app PICKUP_ASSIGNED notification for the citizen', async () => {
    syntheticDataService.resetSyntheticCache();
    const availableDriver = syntheticDataService.getSyntheticDrivers().find((d) => d.availability === 'AVAILABLE');

    const complaint = {
      complaintId: 'CMP-NOTIF-CIT',
      citizenId: 'mock-citizen-test-9',
      latitude: availableDriver.currentLocation.latitude,
      longitude: availableDriver.currentLocation.longitude,
      status: 'SUBMITTED',
      locationText: 'Ring Road Junction'
    };

    await autoDispatchComplaint(complaint);

    const citizenNotifications = syntheticDataService.getSyntheticNotifications('mock-citizen-test-9');
    const citizenNotif = citizenNotifications.find(
      (n) => n.complaintId === 'CMP-NOTIF-CIT' && n.type === 'PICKUP_ASSIGNED'
    );
    expect(citizenNotif).toBeDefined();
    expect(citizenNotif.title).toContain('Pickup Assigned');
    expect(citizenNotif.isRead).toBe(false);
  });

  // 10. Two concurrent complaints cannot assign the same driver
  test('10. prevents double-assignment under concurrent complaint submissions', async () => {
    syntheticDataService.resetSyntheticCache();
    // Get all initial AVAILABLE drivers
    const initialAvailable = syntheticDataService.getSyntheticDrivers().filter((d) => d.availability === 'AVAILABLE');
    expect(initialAvailable.length).toBeGreaterThanOrEqual(2);

    const complaintA = {
      complaintId: 'CMP-RACE-A',
      citizenId: 'CIT-A',
      latitude: 28.6185,
      longitude: 77.2135,
      status: 'SUBMITTED',
      locationText: 'Race Location A'
    };

    const complaintB = {
      complaintId: 'CMP-RACE-B',
      citizenId: 'CIT-B',
      latitude: 28.6185,
      longitude: 77.2135,
      status: 'SUBMITTED',
      locationText: 'Race Location B'
    };

    // Run both dispatches concurrently
    const [resultA, resultB] = await Promise.all([
      autoDispatchComplaint(complaintA),
      autoDispatchComplaint(complaintB)
    ]);

    expect(resultA.assigned).toBe(true);
    expect(resultB.assigned).toBe(true);
    // Crucial: The two complaints MUST NOT receive the same driver!
    expect(resultA.assignedDriverId).not.toBe(resultB.assignedDriverId);
  });

  // 11. Priority score and emergency flag remain intact
  test('11. preserves emergency flag and critical priority score upon automatic dispatch', async () => {
    syntheticDataService.resetSyntheticCache();

    const res = await request(app)
      .post('/api/complaints')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({
        garbageType: 'OVERFLOWING_BIN',
        description: 'Hospital perimeter bin overflowing onto road.',
        locationDescription: 'City General Hospital Gate 3',
        latitude: 28.6180,
        longitude: 77.2130,
        isEmergency: true
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.emergency).toBe(true);
    expect(res.body.data.priorityScore).toBeGreaterThanOrEqual(70);
    expect(res.body.data.priorityLevel).toBe('CRITICAL');
    expect(res.body.data.status).toBe('ASSIGNED');
    expect(res.body.dispatchResult.assignmentStatus).toBe('ASSIGNED');
  });

  // 12. Synthetic mode assignment works
  test('12. POST /api/complaints in synthetic demo mode dispatches to nearby driver', async () => {
    syntheticDataService.resetSyntheticCache();

    const res = await request(app)
      .post('/api/complaints')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({
        garbageType: 'ROADSIDE_GARBAGE',
        description: 'Debris left after road repair work.',
        locationDescription: 'Sector 5 Main Crossing',
        latitude: 28.6300,
        longitude: 77.2250,
        isEmergency: false
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.dispatchResult).toBeDefined();
    expect(res.body.dispatchResult.assignedDriverId).toBeDefined();
    expect(res.body.data.assignedDriverId).toBe(res.body.dispatchResult.assignedDriverId);
  });

  // 13. MongoDB mode behavior remains supported
  test('13. MongoDB mode queries Driver collection and atomically claims available driver', async () => {
    jest.spyOn(syntheticDataService, 'isSyntheticMode').mockReturnValue(false);

    const mockCandidateDriver = {
      driverId: 'DRV-MONGO-01',
      name: 'MongoDB Driver',
      vehicleNumber: 'DL-01-M-1111',
      availability: 'AVAILABLE',
      currentLocation: { latitude: 28.6140, longitude: 77.2091 }
    };

    jest.spyOn(Complaint, 'find').mockReturnValue({
      select: jest.fn().mockResolvedValue([])
    });

    jest.spyOn(Driver, 'find').mockResolvedValue([mockCandidateDriver]);
    jest.spyOn(Driver, 'findOneAndUpdate').mockResolvedValue({
      ...mockCandidateDriver,
      availability: 'BUSY'
    });
    jest.spyOn(User, 'findOne').mockResolvedValue({
      id: 'mock-driver-user-mongo'
    });
    jest.spyOn(Notification, 'create').mockResolvedValue({});

    const complaint = {
      complaintId: 'CMP-MONGO-001',
      citizenId: 'CIT-MONGO',
      latitude: 28.6139,
      longitude: 77.2090,
      status: 'SUBMITTED',
      locationText: 'MongoDB Test Site',
      save: jest.fn().mockResolvedValue(true)
    };

    const result = await autoDispatchComplaint(complaint);
    expect(result.assigned).toBe(true);
    expect(result.assignedDriverId).toBe('DRV-MONGO-01');
    expect(complaint.status).toBe('ASSIGNED');
    expect(Driver.findOneAndUpdate).toHaveBeenCalledWith(
      { driverId: 'DRV-MONGO-01', availability: 'AVAILABLE' },
      { availability: 'BUSY' },
      { new: true }
    );
  });

  // 14. Existing manual officer assignment still works
  test('14. officer manual driver assignment endpoint remains functional', async () => {
    syntheticDataService.resetSyntheticCache();

    // Use a pre-existing synthetic complaint ID from dataset
    const complaints = syntheticDataService.getSyntheticComplaints();
    const targetComplaint = complaints.find((c) => c.status === 'SUBMITTED');
    expect(targetComplaint).toBeDefined();

    const res = await request(app)
      .put(`/api/officer/complaints/${targetComplaint.complaintId}/assign`)
      .set('Authorization', `Bearer ${officerToken}`)
      .send({
        driverId: 'DRV-DEMO-003',
        driverName: 'Amitabh Yadav',
        vehicleNumber: 'MH-DEMO-003',
        noteText: 'Assigned manually by Municipal Officer Vikram.'
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.status).toBe('ASSIGNED');
    expect(res.body.data.assignedDriverId).toBe('DRV-DEMO-003');
  });

  // 15. Unauthenticated users cannot create complaints or trigger dispatch
  test('15. rejects unauthenticated complaint creation with 401', async () => {
    const res = await request(app)
      .post('/api/complaints')
      .send({
        locationDescription: 'Unauthorized Location',
        latitude: 28.6139,
        longitude: 77.2090
      });

    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });

  // 16. Other roles cannot bypass existing authorization
  test('16. rejects non-officers from assigning drivers via officer route with 403', async () => {
    const res = await request(app)
      .put('/api/officer/complaints/CMP-001/assign')
      .set('Authorization', `Bearer ${citizenToken}`)
      .send({
        driverId: 'DRV-ILLEGAL'
      });

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
  });
});
