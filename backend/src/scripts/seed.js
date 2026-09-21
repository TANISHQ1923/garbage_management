const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const env = require('../config/env');
const User = require('../models/User');
const Driver = require('../models/Driver');
const Complaint = require('../models/Complaint');
const Notification = require('../models/Notification');

const seedData = async () => {
  console.log('[Seed] Starting database seeding process...');
  console.log(`[Seed] Target Database URI: ${env.MONGODB_URI.replace(/\/\/.*@/, '//***:***@')}`);

  try {
    await mongoose.connect(env.MONGODB_URI);
    console.log('[Seed] Connected to MongoDB.');

    // 1. Password hashing
    const demoPassword = 'password123';
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(demoPassword, salt);

    // 2. Demo Users definition (3 Citizens, 2 Officers, 5 Drivers)
    const demoUsers = [
      // Citizens
      {
        email: 'citizen@example.com',
        name: 'Aarav Sharma (Citizen)',
        passwordHash,
        role: 'CITIZEN',
        phone: '+91 9876543210',
        address: 'Sector 4, Green Park, Smart City'
      },
      {
        email: 'citizen2@example.com',
        name: 'Priya Singh (Citizen)',
        passwordHash,
        role: 'CITIZEN',
        phone: '+91 9876543220',
        address: 'Tower B, Lotus Apartments, Ward 12'
      },
      {
        email: 'citizen3@example.com',
        name: 'Rahul Mehta (Citizen)',
        passwordHash,
        role: 'CITIZEN',
        phone: '+91 9876543230',
        address: '42 Heritage Colony, Central Zone'
      },
      // Municipal Officers
      {
        email: 'officer@example.com',
        name: 'Vikram Patel (Officer)',
        passwordHash,
        role: 'MUNICIPAL_OFFICER',
        phone: '+91 9876543211',
        employeeId: 'MC-OFF-2026',
        ward: 'Ward 12 - Central Zone'
      },
      {
        email: 'officer2@example.com',
        name: 'Ananya Deshmukh (Officer)',
        passwordHash,
        role: 'MUNICIPAL_OFFICER',
        phone: '+91 9876543221',
        employeeId: 'MC-OFF-2027',
        ward: 'Ward 14 - North Zone'
      },
      // Garbage Drivers
      {
        email: 'driver@example.com',
        name: 'Ramesh Kumar (Driver)',
        passwordHash,
        role: 'GARBAGE_DRIVER',
        phone: '+91 9876543212',
        driverId: 'DRV-882',
        employeeId: 'EMP-DRV-001',
        vehicleNumber: 'DL-01-GB-4040',
        vehicleType: 'Heavy Compactor Truck',
        availability: 'AVAILABLE'
      },
      {
        email: 'driver2@example.com',
        name: 'Sunil Verma (Driver)',
        passwordHash,
        role: 'GARBAGE_DRIVER',
        phone: '+91 9876543215',
        driverId: 'DRV-104',
        employeeId: 'EMP-DRV-002',
        vehicleNumber: 'DL-01-GB-5050',
        vehicleType: 'Medium Tipper Truck',
        availability: 'AVAILABLE'
      },
      {
        email: 'driver3@example.com',
        name: 'Amitabh Ghosh (Driver)',
        passwordHash,
        role: 'GARBAGE_DRIVER',
        phone: '+91 9876543216',
        driverId: 'DRV-305',
        employeeId: 'EMP-DRV-003',
        vehicleNumber: 'DL-01-GB-6060',
        vehicleType: 'Electric Mini Van',
        availability: 'BUSY'
      },
      {
        email: 'driver4@example.com',
        name: 'Rajesh Chauhan (Driver)',
        passwordHash,
        role: 'GARBAGE_DRIVER',
        phone: '+91 9876543217',
        driverId: 'DRV-412',
        employeeId: 'EMP-DRV-004',
        vehicleNumber: 'DL-01-GB-7070',
        vehicleType: 'Hydraulic Lift Loader',
        availability: 'AVAILABLE'
      },
      {
        email: 'driver5@example.com',
        name: 'Kavita Rao (Driver)',
        passwordHash,
        role: 'GARBAGE_DRIVER',
        phone: '+91 9876543218',
        driverId: 'DRV-520',
        employeeId: 'EMP-DRV-005',
        vehicleNumber: 'DL-01-GB-8080',
        vehicleType: 'Heavy Compactor Truck',
        availability: 'OFFLINE'
      }
    ];

    console.log('[Seed] Upserting demo user accounts...');
    const savedUsers = {};
    for (const userData of demoUsers) {
      const user = await User.findOneAndUpdate(
        { email: userData.email },
        userData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
      );
      savedUsers[userData.email] = user;
      console.log(`  + User [${user.role}]: ${user.email}`);
    }

    // 3. Driver records in Driver collection
    console.log('[Seed] Upserting Driver collection records...');
    const driversData = [
      {
        driverId: 'DRV-882',
        employeeId: 'EMP-DRV-001',
        name: 'Ramesh Kumar (Driver)',
        phone: '+91 9876543212',
        vehicleNumber: 'DL-01-GB-4040',
        vehicleType: 'Heavy Compactor Truck',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6139, longitude: 77.2090, timestamp: Date.now() }
      },
      {
        driverId: 'DRV-104',
        employeeId: 'EMP-DRV-002',
        name: 'Sunil Verma (Driver)',
        phone: '+91 9876543215',
        vehicleNumber: 'DL-01-GB-5050',
        vehicleType: 'Medium Tipper Truck',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6250, longitude: 77.2180, timestamp: Date.now() }
      },
      {
        driverId: 'DRV-305',
        employeeId: 'EMP-DRV-003',
        name: 'Amitabh Ghosh (Driver)',
        phone: '+91 9876543216',
        vehicleNumber: 'DL-01-GB-6060',
        vehicleType: 'Electric Mini Van',
        availability: 'BUSY',
        currentLocation: { latitude: 28.6300, longitude: 77.2250, timestamp: Date.now() }
      },
      {
        driverId: 'DRV-412',
        employeeId: 'EMP-DRV-004',
        name: 'Rajesh Chauhan (Driver)',
        phone: '+91 9876543217',
        vehicleNumber: 'DL-01-GB-7070',
        vehicleType: 'Hydraulic Lift Loader',
        availability: 'AVAILABLE',
        currentLocation: { latitude: 28.6050, longitude: 77.2000, timestamp: Date.now() }
      },
      {
        driverId: 'DRV-520',
        employeeId: 'EMP-DRV-005',
        name: 'Kavita Rao (Driver)',
        phone: '+91 9876543218',
        vehicleNumber: 'DL-01-GB-8080',
        vehicleType: 'Heavy Compactor Truck',
        availability: 'OFFLINE',
        currentLocation: { latitude: 28.5980, longitude: 77.1950, timestamp: Date.now() }
      }
    ];

    for (const drv of driversData) {
      await Driver.findOneAndUpdate({ driverId: drv.driverId }, drv, { upsert: true, new: true });
      console.log(`  + Driver: ${drv.driverId} (${drv.name}) - ${drv.availability}`);
    }

    // 4. Demo Complaints covering all 6 lifecycle stages
    console.log('[Seed] Upserting demo garbage complaints across all 6 lifecycle stages...');
    const citizen = savedUsers['citizen@example.com'];
    const citizen2 = savedUsers['citizen2@example.com'];
    const now = Date.now();

    const demoComplaints = [
      // 1. SUBMITTED (Organic, Medium severity)
      {
        complaintId: 'SGM-2026-004128',
        citizenId: citizen.id,
        citizenName: citizen.name,
        citizenPhone: citizen.phone,
        garbageType: 'OVERFLOWING_BIN',
        description: 'Public bin near community park entrance has been overflowing for 2 days. Animals are scattering waste.',
        latitude: 28.6139,
        longitude: 77.2090,
        locationText: 'Sector 4 Community Park Gate 2, Green Park',
        emergency: false,
        status: 'SUBMITTED',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'ORGANIC', confidence: 0.92 },
          severity: { level: 'MEDIUM', confidence: 0.88 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 3)
        },
        aiAnalysisStatus: 'COMPLETED',
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 3,
            changedBy: citizen.name,
            role: 'CITIZEN',
            note: 'Complaint registered by citizen.'
          }
        ]
      },
      // 2. ASSIGNED (Emergency, Hazardous/Mixed, High severity)
      {
        complaintId: 'SGM-2026-003890',
        citizenId: citizen.id,
        citizenName: citizen.name,
        citizenPhone: citizen.phone,
        garbageType: 'ROADSIDE_GARBAGE',
        description: 'Large garbage pile blocking pedestrian footpath and vehicular lane.',
        latitude: 28.6210,
        longitude: 77.2155,
        locationText: 'Main Market Road Corner, Ward 12',
        emergency: true,
        status: 'ASSIGNED',
        assignedDriverId: 'DRV-882',
        assignedDriverName: 'Ramesh Kumar (Driver)',
        assignedVehicleNumber: 'DL-01-GB-4040',
        assignedAt: now - 3600000 * 2,
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'MIXED_SOLID_WASTE', confidence: 0.89 },
          severity: { level: 'HIGH', confidence: 0.85 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 4)
        },
        aiAnalysisStatus: 'COMPLETED',
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 4,
            changedBy: citizen.name,
            role: 'CITIZEN',
            note: 'Emergency complaint registered by citizen.'
          },
          {
            status: 'ASSIGNED',
            timestamp: now - 3600000 * 2,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Assigned to driver Ramesh Kumar (DL-01-GB-4040).'
          }
        ]
      },
      // 3. PICKUP_IN_PROGRESS (Paper, High severity, with before-cleaning photo)
      {
        complaintId: 'SGM-2026-003204',
        citizenId: citizen2.id,
        citizenName: citizen2.name,
        citizenPhone: citizen2.phone,
        garbageType: 'FESTIVAL_WASTE',
        description: 'Accumulation of celebratory decorative waste and food boxes near community hall.',
        latitude: 28.6080,
        longitude: 77.2040,
        locationText: 'Community Hall Ground, Block C, Ward 12',
        emergency: false,
        status: 'PICKUP_IN_PROGRESS',
        assignedDriverId: 'DRV-882',
        assignedDriverName: 'Ramesh Kumar (Driver)',
        assignedVehicleNumber: 'DL-01-GB-4040',
        assignedAt: now - 3600000 * 5,
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        beforeCleaningImageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'PAPER', confidence: 0.94 },
          severity: { level: 'HIGH', confidence: 0.87 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 6)
        },
        aiAnalysisStatus: 'COMPLETED',
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 6,
            changedBy: citizen2.name,
            role: 'CITIZEN',
            note: 'Complaint filed.'
          },
          {
            status: 'ASSIGNED',
            timestamp: now - 3600000 * 5,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Assigned to DRV-882.'
          },
          {
            status: 'PICKUP_IN_PROGRESS',
            timestamp: now - 3600000,
            changedBy: 'Ramesh Kumar (Driver)',
            role: 'GARBAGE_DRIVER',
            note: 'Driver on-site; collection in progress.'
          }
        ]
      },
      // 4. CLEANED (Plastic, High severity, with before + after cleaning photo)
      {
        complaintId: 'SGM-2026-002195',
        citizenId: citizen.id,
        citizenName: citizen.name,
        citizenPhone: citizen.phone,
        garbageType: 'OVERFLOWING_BIN',
        description: 'Bus stop dustbin cleared and area sanitized.',
        latitude: 28.6150,
        longitude: 77.2110,
        locationText: 'Bus Terminal Stop #3, Green Park',
        emergency: false,
        status: 'CLEANED',
        assignedDriverId: 'DRV-882',
        assignedDriverName: 'Ramesh Kumar (Driver)',
        assignedVehicleNumber: 'DL-01-GB-4040',
        assignedAt: now - 3600000 * 8,
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        beforeCleaningImageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        afterCleaningImageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'PLASTIC', confidence: 0.91 },
          severity: { level: 'HIGH', confidence: 0.86 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 9)
        },
        aiAnalysisStatus: 'COMPLETED',
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 9,
            changedBy: citizen.name,
            role: 'CITIZEN',
            note: 'Complaint submitted.'
          },
          {
            status: 'ASSIGNED',
            timestamp: now - 3600000 * 8,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Assigned to DRV-882.'
          },
          {
            status: 'PICKUP_IN_PROGRESS',
            timestamp: now - 3600000 * 4,
            changedBy: 'Ramesh Kumar (Driver)',
            role: 'GARBAGE_DRIVER',
            note: 'Pickup started.'
          },
          {
            status: 'CLEANED',
            timestamp: now - 3600000 * 2,
            changedBy: 'Ramesh Kumar (Driver)',
            role: 'GARBAGE_DRIVER',
            note: 'Cleaned with photo evidence attached.'
          }
        ]
      },
      // 5. VERIFIED (Organic, Critical severity, inspected and verified by Officer)
      {
        complaintId: 'SGM-2026-001050',
        citizenId: citizen.id,
        citizenName: citizen.name,
        citizenPhone: citizen.phone,
        garbageType: 'ROADSIDE_GARBAGE',
        description: 'Commercial market waste pile cleared and inspected.',
        latitude: 28.6180,
        longitude: 77.2130,
        locationText: 'Central Wholesale Market Lane 4, Ward 12',
        emergency: false,
        status: 'VERIFIED',
        assignedDriverId: 'DRV-104',
        assignedDriverName: 'Sunil Verma (Driver)',
        assignedVehicleNumber: 'DL-01-GB-5050',
        assignedAt: now - 3600000 * 24,
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        beforeCleaningImageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        afterCleaningImageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'ORGANIC', confidence: 0.95 },
          severity: { level: 'CRITICAL', confidence: 0.90 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 26)
        },
        aiAnalysisStatus: 'COMPLETED',
        officerNotes: [
          {
            id: 'NOTE-SEED-001',
            officerId: 'MC-OFF-2026',
            officerName: 'Vikram Patel (Officer)',
            text: 'Site inspected and verified completely cleared and disinfected.',
            createdAt: now - 3600000 * 5
          }
        ],
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 26,
            changedBy: citizen.name,
            role: 'CITIZEN',
            note: 'Report filed.'
          },
          {
            status: 'ASSIGNED',
            timestamp: now - 3600000 * 24,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Assigned to Sunil Verma (DRV-104).'
          },
          {
            status: 'PICKUP_IN_PROGRESS',
            timestamp: now - 3600000 * 18,
            changedBy: 'Sunil Verma (Driver)',
            role: 'GARBAGE_DRIVER',
            note: 'Driver arrived on scene.'
          },
          {
            status: 'CLEANED',
            timestamp: now - 3600000 * 12,
            changedBy: 'Sunil Verma (Driver)',
            role: 'GARBAGE_DRIVER',
            note: 'Waste removed and area washed.'
          },
          {
            status: 'VERIFIED',
            timestamp: now - 3600000 * 5,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Inspected and verified.'
          }
        ]
      },
      // 6. REJECTED (Construction debris on private property)
      {
        complaintId: 'SGM-2026-000980',
        citizenId: citizen.id,
        citizenName: citizen.name,
        citizenPhone: citizen.phone,
        garbageType: 'OTHER_WASTE',
        description: 'Construction debris dumped inside private residential compound.',
        latitude: 28.6110,
        longitude: 77.2020,
        locationText: 'Private Plot 18, Sector 4',
        emergency: false,
        status: 'REJECTED',
        imageUrl: 'https://res.cloudinary.com/demo/image/upload/v1/sample.jpg',
        aiAnalysis: {
          classification: { category: 'CONSTRUCTION', confidence: 0.85 },
          severity: { level: 'LOW', confidence: 0.80 },
          modelStatus: 'DEVELOPMENT_BASELINE',
          analyzedAt: new Date(now - 3600000 * 48)
        },
        aiAnalysisStatus: 'COMPLETED',
        officerNotes: [
          {
            id: 'NOTE-SEED-002',
            officerId: 'MC-OFF-2026',
            officerName: 'Vikram Patel (Officer)',
            text: 'Rejected: Reported waste is located on private residential property not within municipal jurisdiction; citizen advised to contact private waste management.',
            createdAt: now - 3600000 * 40
          }
        ],
        statusHistory: [
          {
            status: 'SUBMITTED',
            timestamp: now - 3600000 * 48,
            changedBy: citizen.name,
            role: 'CITIZEN',
            note: 'Report filed.'
          },
          {
            status: 'REJECTED',
            timestamp: now - 3600000 * 40,
            changedBy: 'Vikram Patel (Officer)',
            role: 'MUNICIPAL_OFFICER',
            note: 'Rejected: Private property outside municipal jurisdiction.'
          }
        ]
      }
    ];

    for (const cData of demoComplaints) {
      await Complaint.findOneAndUpdate({ complaintId: cData.complaintId }, cData, {
        upsert: true,
        new: true
      });
      console.log(`  + Complaint [${cData.status}]: ${cData.complaintId} - ${cData.locationText}`);
    }

    // 5. Initial Notifications
    console.log('[Seed] Upserting demo notifications...');
    const demoNotifications = [
      {
        notificationId: 'NOTIF-SEED-001',
        userId: citizen.id,
        type: 'COMPLAINT_SUBMITTED',
        title: 'Complaint Registered',
        message: 'Your complaint SGM-2026-004128 has been registered.',
        complaintId: 'SGM-2026-004128',
        isRead: false
      },
      {
        notificationId: 'NOTIF-SEED-002',
        userId: citizen.id,
        type: 'PICKUP_ASSIGNED',
        title: 'Garbage Pickup Assigned',
        message: 'Complaint SGM-2026-003890 has been assigned to vehicle DL-01-GB-4040.',
        complaintId: 'SGM-2026-003890',
        isRead: false
      },
      {
        notificationId: 'NOTIF-SEED-003',
        userId: 'DRV-882',
        type: 'DRIVER_ASSIGNMENT',
        title: 'New Collection Assignment',
        message: 'Complaint SGM-2026-003890 has been assigned to your route.',
        complaintId: 'SGM-2026-003890',
        isRead: false
      }
    ];

    for (const nData of demoNotifications) {
      await Notification.findOneAndUpdate({ notificationId: nData.notificationId }, nData, {
        upsert: true,
        new: true
      });
      console.log(`  + Notification: ${nData.title} -> ${nData.userId}`);
    }

    console.log('====================================================');
    console.log('[Seed] Database successfully seeded with demo accounts:');
    console.log('  Citizens (3):');
    console.log('    1. citizen@example.com   / password123');
    console.log('    2. citizen2@example.com  / password123');
    console.log('    3. citizen3@example.com  / password123');
    console.log('  Officers (2):');
    console.log('    1. officer@example.com   / password123 (MC-OFF-2026)');
    console.log('    2. officer2@example.com  / password123 (MC-OFF-2027)');
    console.log('  Drivers (5):');
    console.log('    1. driver@example.com    / password123 (DRV-882)');
    console.log('    2. driver2@example.com   / password123 (DRV-104)');
    console.log('    3. driver3@example.com   / password123 (DRV-305)');
    console.log('    4. driver4@example.com   / password123 (DRV-412)');
    console.log('    5. driver5@example.com   / password123 (DRV-520)');
    console.log('  Complaints (6 stages):');
    console.log('    - SUBMITTED (SGM-2026-004128) - Organic / Medium');
    console.log('    - ASSIGNED (SGM-2026-003890) - Emergency / Mixed / High');
    console.log('    - PICKUP_IN_PROGRESS (SGM-2026-003204) - Paper / High');
    console.log('    - CLEANED (SGM-2026-002195) - Plastic / High');
    console.log('    - VERIFIED (SGM-2026-001050) - Organic / Critical');
    console.log('    - REJECTED (SGM-2026-000980) - Construction / Low');
    console.log('====================================================');

    process.exit(0);
  } catch (error) {
    console.error(`[Seed] Error during seeding: ${error.message}`);
    process.exit(1);
  }
};

seedData();
