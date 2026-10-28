/**
 * Synthetic Demo Dataset Generator
 * Smart Garbage Management Application
 *
 * Generates synthetic-dashboard-data.json with full schema compliance,
 * internal consistency, and zero real personal data.
 *
 * SYNTHETIC DEMO DATA ONLY
 */

const fs = require('fs');
const path = require('path');
const bcrypt = require('bcryptjs');

// Polyfill count helper on Array prototype for clean aggregation
if (!Array.prototype.count) {
  Array.prototype.count = function (predicate) {
    return this.filter(predicate).length;
  };
}

// Constants & Enums matching backend/src/models and android_app domain models
const ROLES = {
  CITIZEN: 'CITIZEN',
  OFFICER: 'MUNICIPAL_OFFICER',
  DRIVER: 'GARBAGE_DRIVER'
};

const COMPLAINT_STATUS = {
  SUBMITTED: 'SUBMITTED',
  ASSIGNED: 'ASSIGNED',
  PICKUP_IN_PROGRESS: 'PICKUP_IN_PROGRESS',
  CLEANED: 'CLEANED',
  VERIFIED: 'VERIFIED',
  REJECTED: 'REJECTED'
};

const GARBAGE_TYPES = [
  'OVERFLOWING_BIN',
  'ROADSIDE_GARBAGE',
  'FESTIVAL_WASTE',
  'EVENT_WASTE',
  'FLOOD_WASTE',
  'OTHER_WASTE'
];

const AI_CATEGORIES = [
  'WET_WASTE',
  'DRY_WASTE',
  'PLASTIC',
  'PAPER',
  'METAL',
  'GLASS',
  'MIXED_WASTE',
  'OTHER'
];

const AI_SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const SAMPLE_IMAGES = [
  'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
  'https://res.cloudinary.com/demo/image/upload/v1/cld-sample.jpg',
  'https://res.cloudinary.com/demo/image/upload/v1/cld-sample-2.jpg',
  'https://res.cloudinary.com/demo/image/upload/v1/cld-sample-4.jpg'
];

async function generateDataset() {
  console.log('[Generator] Building synthetic demo dataset...');

  // Hash demo password once
  const demoSalt = await bcrypt.genSalt(10);
  const passwordHash = await bcrypt.hash('password123', demoSalt);

  // 1. Synthetic Users
  const citizenDefs = [
    { name: 'Aarav Sharma', email: 'citizen1@demo.local', phone: '+91 9876543201', address: 'Flat 402, Green Meadows, Bibwewadi Main Road, Pune', ward: 'Ward 19 - Bibwewadi' },
    { name: 'Priya Patel', email: 'citizen2@demo.local', phone: '+91 9876543202', address: 'House 12, Chintamani Nagar, Bibwewadi, Pune', ward: 'Ward 19 - Bibwewadi' },
    { name: 'Rohan Mehta', email: 'citizen3@demo.local', phone: '+91 9876543203', address: 'Tower B, Mahavir Nagar, Bibwewadi, Pune', ward: 'Ward 19 - Bibwewadi' },
    { name: 'Neha Gupta', email: 'citizen4@demo.local', phone: '+91 9876543204', address: 'Upper Indira Nagar, Near KK Market, Bibwewadi, Pune', ward: 'Ward 19 - Bibwewadi' },
    { name: 'Vikram Reddy', email: 'citizen5@demo.local', phone: '+91 9876543205', address: 'B-104, Sunrise Heights, Sahakar Nagar, Pune', ward: 'Ward 19 - Bibwewadi' },
    { name: 'Ananya Deshmukh', email: 'citizen6@demo.local', phone: '+91 9876543206', address: '15 Riverview Residency, Padmavati, Pune-Satara Road', ward: 'Ward 20 - Dhankawadi' },
    { name: 'Kabir Joshi', email: 'citizen7@demo.local', phone: '+91 9876543207', address: '78 Pine Crest Avenue, Dhankawadi, Pune', ward: 'Ward 20 - Dhankawadi' },
    { name: 'Sneha Nair', email: 'citizen8@demo.local', phone: '+91 9876543208', address: 'Villa 9, Palm Court, Market Yard, Gultekdi, Pune', ward: 'Ward 20 - Dhankawadi' },
    { name: 'Aditya Verma', email: 'citizen9@demo.local', phone: '+91 9876543209', address: 'C-302, Silver Oak Society, Katraj-Kondhwa Road, Pune', ward: 'Ward 20 - Dhankawadi' },
    { name: 'Pooja Kulkarni', email: 'citizen10@demo.local', phone: '+91 9876543210', address: '56 Model Colony, Shivajinagar, Pune', ward: 'Ward 12 - Shivajinagar' }
  ];

  const officerDefs = [
    { name: 'Suresh Raman', email: 'officer1@demo.local', phone: '+91 9876543211', employeeId: 'MC-OFF-DEMO-001', ward: 'Ward 19 - Bibwewadi & Sahakar Nagar' },
    { name: 'Meenakshi Iyer', email: 'officer2@demo.local', phone: '+91 9876543212', employeeId: 'MC-OFF-DEMO-002', ward: 'Ward 20 - Dhankawadi & Katraj' },
    { name: 'Rajeshwari Rao', email: 'officer3@demo.local', phone: '+91 9876543213', employeeId: 'MC-OFF-DEMO-003', ward: 'Ward 12 - Shivajinagar & Central Pune' }
  ];

  const driverDefs = [
    { name: 'Ramesh Shinde', email: 'driver1@demo.local', phone: '+91 9876543221', driverId: 'DRV-DEMO-001', employeeId: 'EMP-DEMO-001', vehicleNumber: 'MH-DEMO-001', vehicleType: 'Heavy Compactor Truck', availability: 'BUSY' },
    { name: 'Sunil Pawar', email: 'driver2@demo.local', phone: '+91 9876543222', driverId: 'DRV-DEMO-002', employeeId: 'EMP-DEMO-002', vehicleNumber: 'MH-DEMO-002', vehicleType: 'Heavy Compactor Truck', availability: 'BUSY' },
    { name: 'Amitabh Yadav', email: 'driver3@demo.local', phone: '+91 9876543223', driverId: 'DRV-DEMO-003', employeeId: 'EMP-DEMO-003', vehicleNumber: 'MH-DEMO-003', vehicleType: 'Medium Tipper Truck', availability: 'AVAILABLE' },
    { name: 'Santosh Patil', email: 'driver4@demo.local', phone: '+91 9876543224', driverId: 'DRV-DEMO-004', employeeId: 'EMP-DEMO-004', vehicleNumber: 'MH-DEMO-004', vehicleType: 'Medium Tipper Truck', availability: 'AVAILABLE' },
    { name: 'Ganesh Gaikwad', email: 'driver5@demo.local', phone: '+91 9876543225', driverId: 'DRV-DEMO-005', employeeId: 'EMP-DEMO-005', vehicleNumber: 'MH-DEMO-005', vehicleType: 'Electric Mini Van', availability: 'AVAILABLE' },
    { name: 'Manoj Kale', email: 'driver6@demo.local', phone: '+91 9876543226', driverId: 'DRV-DEMO-006', employeeId: 'EMP-DEMO-006', vehicleNumber: 'MH-DEMO-006', vehicleType: 'Hydraulic Lift Loader', availability: 'AVAILABLE' },
    { name: 'Dilip Bhosale', email: 'driver7@demo.local', phone: '+91 9876543227', driverId: 'DRV-DEMO-007', employeeId: 'EMP-DEMO-007', vehicleNumber: 'MH-DEMO-007', vehicleType: 'Electric Mini Van', availability: 'OFFLINE' },
    { name: 'Vijay Jagtap', email: 'driver8@demo.local', phone: '+91 9876543228', driverId: 'DRV-DEMO-008', employeeId: 'EMP-DEMO-008', vehicleNumber: 'MH-DEMO-008', vehicleType: 'Hydraulic Lift Loader', availability: 'OFFLINE' }
  ];

  const users = [];

  // Generate Citizens
  citizenDefs.forEach((c, idx) => {
    users.push({
      id: `65f1000000000000000000${(idx + 1).toString().padStart(2, '0')}`,
      name: c.name,
      email: c.email,
      passwordHash,
      role: ROLES.CITIZEN,
      phone: c.phone,
      address: c.address,
      ward: c.ward,
      employeeId: null,
      driverId: null,
      vehicleNumber: null,
      vehicleType: null,
      availability: null
    });
  });

  // Generate Officers
  officerDefs.forEach((o, idx) => {
    users.push({
      id: `65f2000000000000000000${(idx + 1).toString().padStart(2, '0')}`,
      name: o.name,
      email: o.email,
      passwordHash,
      role: ROLES.OFFICER,
      phone: o.phone,
      address: `PMC Zonal Office, ${o.ward}`,
      ward: o.ward,
      employeeId: o.employeeId,
      driverId: null,
      vehicleNumber: null,
      vehicleType: null,
      availability: null
    });
  });

  // Generate Drivers
  driverDefs.forEach((d, idx) => {
    users.push({
      id: `65f3000000000000000000${(idx + 1).toString().padStart(2, '0')}`,
      name: d.name,
      email: d.email,
      passwordHash,
      role: ROLES.DRIVER,
      phone: d.phone,
      address: `PMC Sanitation Depot ${(idx % 3) + 1}, ${idx < 4 ? 'Bibwewadi Ward 19, Pune' : (idx < 6 ? 'Dhankawadi Ward 20, Pune' : 'Swargate Central, Pune')}`,
      ward: idx < 4 ? 'Ward 19 - Bibwewadi' : (idx < 6 ? 'Ward 20 - Dhankawadi' : 'Ward 12 - Shivajinagar'),
      employeeId: d.employeeId,
      driverId: d.driverId,
      vehicleNumber: d.vehicleNumber,
      vehicleType: d.vehicleType,
      availability: d.availability
    });
  });

  // 2. Synthetic Driver Records (Driver Collection)
  const driverLocations = [
    { latitude: 18.4695, longitude: 73.8652 }, // DRV-DEMO-001: KK Market, Bibwewadi, Pune
    { latitude: 18.4630, longitude: 73.8590 }, // DRV-DEMO-002: Upper Indira Nagar, Bibwewadi, Pune
    { latitude: 18.4750, longitude: 73.8680 }, // DRV-DEMO-003: Market Yard, Gultekdi, Pune
    { latitude: 18.4610, longitude: 73.8520 }, // DRV-DEMO-004: Dhankawadi, Pune-Satara Road, Pune
    { latitude: 18.4680, longitude: 73.8640 }, // DRV-DEMO-005: Chintamani Nagar, Bibwewadi, Pune
    { latitude: 18.4850, longitude: 73.8550 }, // DRV-DEMO-006: Sahakar Nagar / Padmavati, Pune
    { latitude: 18.4550, longitude: 73.8680 }, // DRV-DEMO-007: Katraj-Kondhwa Road, Pune
    { latitude: 18.5080, longitude: 73.8320 }  // DRV-DEMO-008: Kothrud / Karve Road, Pune
  ];

  const now = Date.now();
  const drivers = driverDefs.map((d, idx) => ({
    driverId: d.driverId,
    employeeId: d.employeeId,
    name: d.name,
    phone: d.phone,
    vehicleNumber: d.vehicleNumber,
    vehicleType: d.vehicleType,
    availability: d.availability,
    currentLocation: {
      latitude: driverLocations[idx].latitude,
      longitude: driverLocations[idx].longitude,
      timestamp: now - (idx * 60000)
    }
  }));

  // 3. Synthetic Complaints (Target: 120 complaints)
  // Distribution:
  // Status: SUBMITTED (22), ASSIGNED (26), PICKUP_IN_PROGRESS (16), CLEANED (24), VERIFIED (22), REJECTED (10) = 120
  // Garbage Types: OVERFLOWING_BIN (34), ROADSIDE_GARBAGE (30), FESTIVAL_WASTE (18), EVENT_WASTE (16), FLOOD_WASTE (12), OTHER_WASTE (10) = 120
  // Emergency: 18 total (SUBMITTED: 4, ASSIGNED: 6, PICKUP_IN_PROGRESS: 4, CLEANED: 2, VERIFIED: 2, REJECTED: 0)

  const citizens = users.filter(u => u.role === ROLES.CITIZEN);
  const officers = users.filter(u => u.role === ROLES.OFFICER);
  const driverUsers = users.filter(u => u.role === ROLES.DRIVER);

  const statusPlan = [
    { status: COMPLAINT_STATUS.SUBMITTED, count: 22, emergencyCount: 4 },
    { status: COMPLAINT_STATUS.ASSIGNED, count: 26, emergencyCount: 6 },
    { status: COMPLAINT_STATUS.PICKUP_IN_PROGRESS, count: 16, emergencyCount: 4 },
    { status: COMPLAINT_STATUS.CLEANED, count: 24, emergencyCount: 2 },
    { status: COMPLAINT_STATUS.VERIFIED, count: 22, emergencyCount: 2 },
    { status: COMPLAINT_STATUS.REJECTED, count: 10, emergencyCount: 0 }
  ];

  const complaints = [];
  let globalIndex = 1;

  // Realistic Pune & Bibwewadi location descriptions & coordinates
  const locationPool = [
    { text: 'KK Market Main Road, Bibwewadi, Pune', lat: 18.4685, lng: 73.8650 },
    { text: 'Chintamani Nagar Gate No. 2, Bibwewadi, Pune', lat: 18.4690, lng: 73.8635 },
    { text: 'Upper Indira Nagar Bus Terminal, Bibwewadi, Pune', lat: 18.4625, lng: 73.8610 },
    { text: 'Mahavir Nagar Society Corner, Bibwewadi, Pune', lat: 18.4710, lng: 73.8670 },
    { text: 'Swami Vivekanand Road Junction, Bibwewadi, Pune', lat: 18.4670, lng: 73.8660 },
    { text: 'Padmavati Temple Road, Pune-Satara Road, Bibwewadi Area', lat: 18.4740, lng: 73.8580 },
    { text: 'Sahakar Nagar Sector 2 Public Garden, Pune', lat: 18.4850, lng: 73.8530 },
    { text: 'Dhankawadi Post Office Lane, Pune', lat: 18.4580, lng: 73.8540 },
    { text: 'Market Yard Gate 3, Gultekdi, Pune', lat: 18.4890, lng: 73.8680 },
    { text: 'Katraj Lake Chowk, Pune-Satara Road, Pune', lat: 18.4520, lng: 73.8580 },
    { text: 'Kondhwa Budruk Main Chowk, Pune', lat: 18.4640, lng: 73.8820 },
    { text: 'Salisbury Park Main Roundabout, Pune', lat: 18.4980, lng: 73.8710 },
    { text: 'Swargate Junction near Jedhe Chowk, Pune', lat: 18.5015, lng: 73.8585 },
    { text: 'Fergusson College Road, Shivajinagar, Pune', lat: 18.5180, lng: 73.8420 },
    { text: 'Kothrud Bus Stand, Paud Road, Pune', lat: 18.5070, lng: 73.8060 }
  ];

  // Specific planned allocation for Citizen 1 (20 complaints)
  // 3 SUBMITTED, 3 ASSIGNED, 2 PICKUP_IN_PROGRESS, 5 CLEANED, 5 VERIFIED, 2 REJECTED
  // Emergency: 3 active (1 SUBMITTED, 1 ASSIGNED, 1 PICKUP_IN_PROGRESS)
  const citizen1Quota = {
    SUBMITTED: { count: 3, emergLeft: 1 },
    ASSIGNED: { count: 3, emergLeft: 1 },
    PICKUP_IN_PROGRESS: { count: 2, emergLeft: 1 },
    CLEANED: { count: 5, emergLeft: 0 },
    VERIFIED: { count: 5, emergLeft: 0 },
    REJECTED: { count: 2, emergLeft: 0 }
  };

  // Planned allocation for Driver 1 (DRV-DEMO-001)
  // 4 ASSIGNED, 2 PICKUP_IN_PROGRESS, 3 CLEANED, 3 VERIFIED = 12 tasks total
  const driver1Quota = {
    ASSIGNED: 4,
    PICKUP_IN_PROGRESS: 2,
    CLEANED: 3,
    VERIFIED: 3
  };

  // Type balance counter
  const typeTarget = {
    OVERFLOWING_BIN: 34,
    ROADSIDE_GARBAGE: 30,
    FESTIVAL_WASTE: 18,
    EVENT_WASTE: 16,
    FLOOD_WASTE: 12,
    OTHER_WASTE: 10
  };
  const typeAssigned = {
    OVERFLOWING_BIN: 0,
    ROADSIDE_GARBAGE: 0,
    FESTIVAL_WASTE: 0,
    EVENT_WASTE: 0,
    FLOOD_WASTE: 0,
    OTHER_WASTE: 0
  };

  function selectGarbageType() {
    for (const t of GARBAGE_TYPES) {
      if (typeAssigned[t] < typeTarget[t]) {
        typeAssigned[t]++;
        return t;
      }
    }
    return 'OVERFLOWING_BIN';
  }

  // Iterate over lifecycle stages
  for (const group of statusPlan) {
    let emergRemaining = group.emergencyCount;

    for (let i = 0; i < group.count; i++) {
      const complaintId = `SGM-DEMO-${globalIndex.toString().padStart(4, '0')}`;
      const loc = locationPool[(globalIndex - 1) % locationPool.length];

      // Assign citizen:
      let assignedCitizen;
      let isEmergency = false;

      if (citizen1Quota[group.status].count > 0) {
        assignedCitizen = citizens[0]; // citizen1
        citizen1Quota[group.status].count--;
        if (citizen1Quota[group.status].emergLeft > 0) {
          isEmergency = true;
          citizen1Quota[group.status].emergLeft--;
          emergRemaining--;
        }
      } else {
        // Distribute among citizens 2 to 10
        const citizenIndex = 1 + ((globalIndex + i) % 9);
        assignedCitizen = citizens[citizenIndex];
        if (emergRemaining > 0) {
          isEmergency = true;
          emergRemaining--;
        }
      }

      // Assign driver if applicable
      let assignedDriver = null;
      if (group.status !== COMPLAINT_STATUS.SUBMITTED && group.status !== COMPLAINT_STATUS.REJECTED) {
        if (driver1Quota[group.status] && driver1Quota[group.status] > 0) {
          assignedDriver = driverDefs[0]; // DRV-DEMO-001
          driver1Quota[group.status]--;
        } else {
          // Distribute among drivers 2 to 6 (drivers 7 & 8 are OFFLINE, though can have completed history)
          const driverIdx = 1 + ((globalIndex + i) % 7);
          assignedDriver = driverDefs[driverIdx];
        }
      }

      const gType = selectGarbageType();

      // Timestamps: chronological progression
      const ageHours = (120 - globalIndex) * 2 + 1;
      const createdTime = now - (ageHours * 3600000);
      const assignedTime = createdTime + 3600000 * 2;
      const inProgressTime = assignedTime + 3600000 * 2;
      const cleanedTime = inProgressTime + 3600000 * 3;
      const verifiedTime = cleanedTime + 3600000 * 4;

      // Status history array
      const statusHistory = [
        {
          status: COMPLAINT_STATUS.SUBMITTED,
          timestamp: createdTime,
          changedBy: assignedCitizen.name,
          role: ROLES.CITIZEN,
          note: isEmergency ? 'Complaint registered with EMERGENCY priority.' : 'Complaint registered by citizen.'
        }
      ];

      const officer = officers[globalIndex % officers.length];

      if (group.status === COMPLAINT_STATUS.ASSIGNED) {
        statusHistory.push({
          status: COMPLAINT_STATUS.ASSIGNED,
          timestamp: assignedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: `Assigned to driver ${assignedDriver.name} (${assignedDriver.vehicleNumber}).`
        });
      } else if (group.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS) {
        statusHistory.push({
          status: COMPLAINT_STATUS.ASSIGNED,
          timestamp: assignedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: `Assigned to driver ${assignedDriver.name} (${assignedDriver.vehicleNumber}).`
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.PICKUP_IN_PROGRESS,
          timestamp: inProgressTime,
          changedBy: assignedDriver.name,
          role: ROLES.DRIVER,
          note: 'Driver arrived on-site; collection commenced.'
        });
      } else if (group.status === COMPLAINT_STATUS.CLEANED) {
        statusHistory.push({
          status: COMPLAINT_STATUS.ASSIGNED,
          timestamp: assignedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: `Assigned to driver ${assignedDriver.name} (${assignedDriver.vehicleNumber}).`
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.PICKUP_IN_PROGRESS,
          timestamp: inProgressTime,
          changedBy: assignedDriver.name,
          role: ROLES.DRIVER,
          note: 'Driver initiated collection.'
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.CLEANED,
          timestamp: cleanedTime,
          changedBy: assignedDriver.name,
          role: ROLES.DRIVER,
          note: 'Waste successfully removed; site sanitized with after-cleaning photo.'
        });
      } else if (group.status === COMPLAINT_STATUS.VERIFIED) {
        statusHistory.push({
          status: COMPLAINT_STATUS.ASSIGNED,
          timestamp: assignedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: `Assigned to driver ${assignedDriver.name} (${assignedDriver.vehicleNumber}).`
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.PICKUP_IN_PROGRESS,
          timestamp: inProgressTime,
          changedBy: assignedDriver.name,
          role: ROLES.DRIVER,
          note: 'Driver initiated collection.'
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.CLEANED,
          timestamp: cleanedTime,
          changedBy: assignedDriver.name,
          role: ROLES.DRIVER,
          note: 'Waste removed and area sanitized.'
        });
        statusHistory.push({
          status: COMPLAINT_STATUS.VERIFIED,
          timestamp: verifiedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: 'Site inspected and verified completely cleared by municipal authority.'
        });
      } else if (group.status === COMPLAINT_STATUS.REJECTED) {
        const rejectedTime = createdTime + 3600000 * 3;
        statusHistory.push({
          status: COMPLAINT_STATUS.REJECTED,
          timestamp: rejectedTime,
          changedBy: officer.name,
          role: ROLES.OFFICER,
          note: 'Rejected: Waste located on private compound outside municipal jurisdiction.'
        });
      }

      // Officer notes for VERIFIED and REJECTED
      const officerNotes = [];
      if (group.status === COMPLAINT_STATUS.VERIFIED) {
        officerNotes.push({
          id: `NOTE-DEMO-${complaintId}-01`,
          officerId: officer.employeeId,
          officerName: officer.name,
          text: 'Field inspection completed. Cleanliness standard verified according to municipal guidelines.',
          createdAt: verifiedTime
        });
      } else if (group.status === COMPLAINT_STATUS.REJECTED) {
        officerNotes.push({
          id: `NOTE-DEMO-${complaintId}-01`,
          officerId: officer.employeeId,
          officerName: officer.name,
          text: 'Notice served to property owner regarding private waste clearance. Municipal intervention declined.',
          createdAt: createdTime + 3600000 * 3
        });
      }

      // Photos & Evidence
      const imgSample = SAMPLE_IMAGES[globalIndex % SAMPLE_IMAGES.length];
      const hasBeforeImg = (
        group.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS ||
        group.status === COMPLAINT_STATUS.CLEANED ||
        group.status === COMPLAINT_STATUS.VERIFIED
      );
      const hasAfterImg = (
        group.status === COMPLAINT_STATUS.CLEANED ||
        group.status === COMPLAINT_STATUS.VERIFIED
      );

      // AI Analysis
      // All complaints have an image except 5 edge cases
      const hasImage = globalIndex % 24 !== 0; // 5 out of 120 without image
      let aiAnalysis = null;
      let aiAnalysisStatus = 'PENDING';

      if (hasImage) {
        if (globalIndex % 20 === 1) {
          aiAnalysisStatus = 'FAILED';
        } else if (globalIndex % 20 === 2) {
          aiAnalysisStatus = 'AI_UNAVAILABLE';
        } else if (globalIndex % 15 === 0 && group.status === COMPLAINT_STATUS.SUBMITTED) {
          aiAnalysisStatus = 'PENDING';
        } else {
          aiAnalysisStatus = 'COMPLETED';
          const aiCat = AI_CATEGORIES[globalIndex % AI_CATEGORIES.length];
          const aiSev = isEmergency ? 'CRITICAL' : AI_SEVERITIES[globalIndex % AI_SEVERITIES.length];
          // Realistic baseline confidence (0.85 - 0.96)
          const confScore = Number((0.85 + ((globalIndex % 11) * 0.01)).toFixed(2));
          aiAnalysis = {
            classification: {
              category: aiCat,
              confidence: confScore
            },
            severity: {
              level: aiSev,
              confidence: confScore
            },
            modelStatus: 'DEVELOPMENT_BASELINE',
            analyzedAt: new Date(createdTime + 30000).toISOString()
          };
        }
      }

      // Complaint description
      const descTemplates = {
        OVERFLOWING_BIN: 'Public dustbin overflowing onto the pavement. Stray dogs and rodents scattering refuse.',
        ROADSIDE_GARBAGE: 'Accumulation of mixed residential and commercial debris blocking walking path.',
        FESTIVAL_WASTE: 'Decorative materials, plastic wrapping, and celebratory leftovers left on the open ground.',
        EVENT_WASTE: 'Discarded paper plates, food containers, and cardboard crates following community event.',
        FLOOD_WASTE: 'Waterlogged storm debris and silt deposited along street drainage channels.',
        OTHER_WASTE: 'Unattended bulk refuse dumped on the vacant lot adjacent to municipal boundary.'
      };

      const complaintDoc = {
        complaintId,
        citizenId: assignedCitizen.id,
        citizenName: assignedCitizen.name,
        citizenPhone: assignedCitizen.phone,
        garbageType: gType,
        description: descTemplates[gType],
        imageUrl: hasImage ? imgSample : null,
        imagePublicId: hasImage ? `demo/complaint_${complaintId.toLowerCase()}` : null,
        videoUrl: null,
        videoPublicId: null,
        latitude: loc.lat,
        longitude: loc.lng,
        locationText: loc.text,
        emergency: isEmergency,
        status: group.status,
        assignedDriverId: assignedDriver ? assignedDriver.driverId : null,
        assignedDriverName: assignedDriver ? assignedDriver.name : null,
        assignedVehicleNumber: assignedDriver ? assignedDriver.vehicleNumber : null,
        assignedAt: assignedDriver ? assignedTime : null,
        officerNotes,
        beforeCleaningImageUrl: hasBeforeImg ? SAMPLE_IMAGES[(globalIndex + 1) % SAMPLE_IMAGES.length] : null,
        beforeCleaningImagePublicId: hasBeforeImg ? `demo/before_${complaintId.toLowerCase()}` : null,
        afterCleaningImageUrl: hasAfterImg ? SAMPLE_IMAGES[(globalIndex + 2) % SAMPLE_IMAGES.length] : null,
        afterCleaningImagePublicId: hasAfterImg ? `demo/after_${complaintId.toLowerCase()}` : null,
        statusHistory,
        aiAnalysis,
        aiAnalysisStatus,
        createdAt: new Date(createdTime).toISOString(),
        updatedAt: new Date(statusHistory[statusHistory.length - 1].timestamp).toISOString()
      };

      complaints.push(complaintDoc);
      globalIndex++;
    }
  }

  // 4. Synthetic Notifications (40 total)
  const notifications = [];
  let notifIndex = 1;

  // Key notifications for Citizen 1
  const citizen1Complaints = complaints.filter(c => c.citizenId === citizens[0].id);
  citizen1Complaints.slice(0, 10).forEach(c => {
    let type = 'COMPLAINT_SUBMITTED';
    let title = 'Complaint Registered';
    let msg = `Your complaint ${c.complaintId} has been registered and forwarded to the municipal office.`;

    if (c.status === COMPLAINT_STATUS.ASSIGNED) {
      type = 'PICKUP_ASSIGNED';
      title = 'Garbage Pickup Assigned';
      msg = `Complaint ${c.complaintId} has been assigned to driver ${c.assignedDriverName} (${c.assignedVehicleNumber}).`;
    } else if (c.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS) {
      type = 'PICKUP_STARTED';
      title = 'Collection In Progress';
      msg = `Collection has started for your complaint ${c.complaintId}. Driver ${c.assignedDriverName} is on site.`;
    } else if (c.status === COMPLAINT_STATUS.CLEANED) {
      type = 'CLEANUP_COMPLETED';
      title = 'Garbage Cleaned';
      msg = `The reported waste for complaint ${c.complaintId} has been removed. Awaiting final officer verification.`;
    } else if (c.status === COMPLAINT_STATUS.VERIFIED) {
      type = 'CLEANUP_VERIFIED';
      title = 'Complaint Verified & Closed';
      msg = `Your complaint ${c.complaintId} has been verified by the municipal authority and officially closed. Thank you!`;
    } else if (c.emergency) {
      type = 'EMERGENCY_REQUEST_RECEIVED';
      title = 'Emergency Pickup Priority Acknowledged';
      msg = `Emergency priority escalated for complaint ${c.complaintId}. Rapid response team notified.`;
    }

    notifications.push({
      notificationId: `NOTIF-DEMO-${notifIndex.toString().padStart(4, '0')}`,
      userId: citizens[0].id,
      type,
      title,
      message: msg,
      complaintId: c.complaintId,
      isRead: notifIndex % 3 === 0,
      createdAt: new Date(now - (notifIndex * 3600000 * 2)).toISOString()
    });
    notifIndex++;
  });

  // Notifications for Driver 1 (DRV-DEMO-001)
  const driver1Complaints = complaints.filter(c => c.assignedDriverId === 'DRV-DEMO-001');
  driver1Complaints.slice(0, 8).forEach(c => {
    notifications.push({
      notificationId: `NOTIF-DEMO-${notifIndex.toString().padStart(4, '0')}`,
      userId: 'DRV-DEMO-001',
      type: 'DRIVER_ASSIGNMENT',
      title: 'New Collection Assignment',
      message: `Complaint ${c.complaintId} (${c.locationText}) has been assigned to your route.`,
      complaintId: c.complaintId,
      isRead: notifIndex % 2 === 0,
      createdAt: new Date(now - (notifIndex * 3600000)).toISOString()
    });
    notifIndex++;
  });

  // Notifications for Officer 1 (MC-OFF-DEMO-001)
  const emergComplaints = complaints.filter(c => c.emergency);
  emergComplaints.slice(0, 10).forEach(c => {
    notifications.push({
      notificationId: `NOTIF-DEMO-${notifIndex.toString().padStart(4, '0')}`,
      userId: officers[0].employeeId,
      type: 'EMERGENCY_REQUEST_RECEIVED',
      title: 'Emergency Waste Escalation Alert',
      message: `Emergency priority flagged for complaint ${c.complaintId} at ${c.locationText}.`,
      complaintId: c.complaintId,
      isRead: notifIndex % 4 === 0,
      createdAt: new Date(now - (notifIndex * 1800000)).toISOString()
    });
    notifIndex++;
  });

  // General notifications for other citizens
  citizens.slice(1, 6).forEach((cit, idx) => {
    notifications.push({
      notificationId: `NOTIF-DEMO-${notifIndex.toString().padStart(4, '0')}`,
      userId: cit.id,
      type: 'SYSTEM_NOTIFICATION',
      title: 'Municipal Clean City Drive',
      message: 'Weekly community recycling and segregation awareness drive commences this Saturday.',
      complaintId: null,
      isRead: true,
      createdAt: new Date(now - 86400000 * (idx + 1)).toISOString()
    });
    notifIndex++;
  });

  // Notifications for other drivers
  driverUsers.slice(1, 5).forEach((drv, idx) => {
    notifications.push({
      notificationId: `NOTIF-DEMO-${notifIndex.toString().padStart(4, '0')}`,
      userId: drv.driverId,
      type: 'DRIVER_ASSIGNMENT',
      title: 'Route Scheduled',
      message: `Morning municipal collection route scheduled for ${drv.vehicleNumber}.`,
      complaintId: null,
      isRead: false,
      createdAt: new Date(now - 3600000 * (idx + 2)).toISOString()
    });
    notifIndex++;
  });

  // 5. Dashboard Summary Data - EXACTLY COMPUTED FROM COMPLAINTS
  // Officer Overview Metrics
  const officerDashboard = {
    total: complaints.length,
    submitted: complaints.count(c => c.status === COMPLAINT_STATUS.SUBMITTED),
    assigned: complaints.count(c => c.status === COMPLAINT_STATUS.ASSIGNED),
    inProgress: complaints.count(c => c.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS),
    cleaned: complaints.count(c => c.status === COMPLAINT_STATUS.CLEANED),
    verified: complaints.count(c => c.status === COMPLAINT_STATUS.VERIFIED),
    rejected: complaints.count(c => c.status === COMPLAINT_STATUS.REJECTED),
    emergency: complaints.count(c => c.emergency),
    activeComplaints: complaints.count(c =>
      c.status === COMPLAINT_STATUS.SUBMITTED ||
      c.status === COMPLAINT_STATUS.ASSIGNED ||
      c.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS
    ),
    driverAvailability: {
      total: drivers.length,
      available: drivers.count(d => d.availability === 'AVAILABLE'),
      busy: drivers.count(d => d.availability === 'BUSY'),
      offline: drivers.count(d => d.availability === 'OFFLINE')
    },
    aiMetrics: {
      totalAnalyzed: complaints.count(c => c.aiAnalysisStatus === 'COMPLETED'),
      pending: complaints.count(c => c.aiAnalysisStatus === 'PENDING'),
      failed: complaints.count(c => c.aiAnalysisStatus === 'FAILED'),
      unavailable: complaints.count(c => c.aiAnalysisStatus === 'AI_UNAVAILABLE')
    }
  };

  // Citizen 1 Overview Metrics (Aarav Sharma)
  const citizen1Id = citizens[0].id;
  const c1Complaints = complaints.filter(c => c.citizenId === citizen1Id);
  const citizenDashboard = {
    demoCitizenId: citizen1Id,
    demoCitizenEmail: citizens[0].email,
    demoCitizenName: citizens[0].name,
    totalComplaints: c1Complaints.length,
    activeComplaints: c1Complaints.count(c =>
      c.status === COMPLAINT_STATUS.SUBMITTED ||
      c.status === COMPLAINT_STATUS.ASSIGNED ||
      c.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS
    ),
    pendingRequests: c1Complaints.count(c => c.status === COMPLAINT_STATUS.SUBMITTED),
    emergencyRequests: c1Complaints.count(c =>
      c.emergency &&
      c.status !== COMPLAINT_STATUS.CLEANED &&
      c.status !== COMPLAINT_STATUS.VERIFIED
    ),
    completedComplaints: c1Complaints.count(c =>
      c.status === COMPLAINT_STATUS.CLEANED ||
      c.status === COMPLAINT_STATUS.VERIFIED
    ),
    recentComplaintsCount: Math.min(5, c1Complaints.length),
    unreadNotifications: notifications.count(n => n.userId === citizen1Id && !n.isRead)
  };

  // Driver 1 Overview Metrics (Ramesh Shinde, DRV-DEMO-001)
  const d1Complaints = complaints.filter(c => c.assignedDriverId === 'DRV-DEMO-001');
  const driverDashboard = {
    demoDriverId: 'DRV-DEMO-001',
    demoDriverName: driverDefs[0].name,
    vehicleNumber: driverDefs[0].vehicleNumber,
    vehicleType: driverDefs[0].vehicleType,
    availability: driverDefs[0].availability,
    totalAssignedTasks: d1Complaints.length,
    assignedTasks: d1Complaints.count(c => c.status === COMPLAINT_STATUS.ASSIGNED),
    inProgressTasks: d1Complaints.count(c => c.status === COMPLAINT_STATUS.PICKUP_IN_PROGRESS),
    completedTasks: d1Complaints.count(c =>
      c.status === COMPLAINT_STATUS.CLEANED ||
      c.status === COMPLAINT_STATUS.VERIFIED
    ),
    emergencyTasks: d1Complaints.count(c =>
      c.emergency &&
      c.status !== COMPLAINT_STATUS.CLEANED &&
      c.status !== COMPLAINT_STATUS.VERIFIED
    ),
    unreadNotifications: notifications.count(n => n.userId === 'DRV-DEMO-001' && !n.isRead)
  };

  // Package entire synthetic dataset
  const dataset = {
    metadata: {
      title: 'SYNTHETIC DEMO DATASET',
      label: 'SYNTHETIC DEMO DATA',
      version: '1.0.0',
      description: 'Comprehensive synthetic demo dataset for Smart Garbage Management Citizen, Municipal Officer, and Garbage Driver dashboards. Contains zero real personal identifiable information.',
      generatedAt: new Date(now).toISOString(),
      compliance: {
        modelStatus: 'DEVELOPMENT_BASELINE',
        noRealPII: true,
        fictionalVehicles: true,
        idempotent: true
      },
      counts: {
        users: users.length,
        citizens: citizenDefs.length,
        officers: officerDefs.length,
        drivers: driverDefs.length,
        complaints: complaints.length,
        notifications: notifications.length
      }
    },
    users,
    drivers,
    complaints,
    notifications,
    dashboards: {
      officerDashboard,
      citizenDashboard,
      driverDashboard
    }
  };

  // Ensure output directory exists
  const outputDir = path.join(__dirname, '..', 'data', 'synthetic');
  fs.mkdirSync(outputDir, { recursive: true });

  const outputFile = path.join(outputDir, 'synthetic-dashboard-data.json');
  fs.writeFileSync(outputFile, JSON.stringify(dataset, null, 2), 'utf-8');

  console.log(`[Generator] Synthetic demo dataset generated successfully at: ${outputFile}`);
  console.log(`  Users: ${users.length} (Citizens: ${citizenDefs.length}, Officers: ${officerDefs.length}, Drivers: ${driverDefs.length})`);
  console.log(`  Drivers Collection: ${drivers.length}`);
  console.log(`  Complaints: ${complaints.length}`);
  console.log(`    - SUBMITTED: ${officerDashboard.submitted}`);
  console.log(`    - ASSIGNED: ${officerDashboard.assigned}`);
  console.log(`    - PICKUP_IN_PROGRESS: ${officerDashboard.inProgress}`);
  console.log(`    - CLEANED: ${officerDashboard.cleaned}`);
  console.log(`    - VERIFIED: ${officerDashboard.verified}`);
  console.log(`    - REJECTED: ${officerDashboard.rejected}`);
  console.log(`    - Emergency: ${officerDashboard.emergency}`);
  console.log(`  Notifications: ${notifications.length}`);

  return dataset;
}

if (require.main === module) {
  generateDataset().catch(err => {
    console.error('[Generator] Error generating synthetic dataset:', err);
    process.exit(1);
  });
}

module.exports = { generateDataset };
