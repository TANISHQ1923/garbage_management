const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const env = require('../config/env');
const User = require('../models/User');
const Driver = require('../models/Driver');
const Complaint = require('../models/Complaint');
const Notification = require('../models/Notification');
const { seedSyntheticData } = require('./seedSyntheticData');

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

    const demoComplaints = [];


    for (const cData of demoComplaints) {
      await Complaint.findOneAndUpdate({ complaintId: cData.complaintId }, cData, {
        upsert: true,
        new: true
      });
      console.log(`  + Complaint [${cData.status}]: ${cData.complaintId} - ${cData.locationText}`);
    }

    // 5. Initial Notifications
    console.log('[Seed] Upserting demo notifications...');
    const demoNotifications = [];


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

    // 6. SYNTHETIC DEMO DATA SEEDING (120 Complaints, 21 Users, 8 Drivers)
    console.log('\n[Seed] Proceeding to seed synthetic demo dataset...');
    await seedSyntheticData(false);

    process.exit(0);
  } catch (error) {
    console.error(`[Seed] Error during seeding: ${error.message}`);
    process.exit(1);
  }
};

seedData();
