/**
 * Safe Demo Users Dataset
 * Used for offline development fallback, demo evaluations, and synthetic mode.
 */
const DEMO_USERS = [
  {
    _id: '67b300000000000000000001',
    id: '67b300000000000000000001',
    email: 'citizen@example.com',
    name: 'Aarav Sharma (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543210',
    address: 'Flat 402, Green Meadows, Bibwewadi Main Road, Pune',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000002',
    id: '67b300000000000000000002',
    email: 'citizen2@example.com',
    name: 'Priya Singh (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543220',
    address: 'Flat 101, Chintamani Nagar, Bibwewadi, Pune',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000003',
    id: '67b300000000000000000003',
    email: 'citizen3@example.com',
    name: 'Rahul Mehta (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543230',
    address: 'Upper Indira Nagar, Near KK Market, Bibwewadi, Pune',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000004',
    id: '67b300000000000000000004',
    email: 'officer@example.com',
    name: 'Vikram Patel (Officer)',
    role: 'MUNICIPAL_OFFICER',
    phone: '+91 9876543211',
    employeeId: 'MC-OFF-2026',
    ward: 'Ward 19 - Bibwewadi & Sahakar Nagar, Pune',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000005',
    id: '67b300000000000000000005',
    email: 'officer2@example.com',
    name: 'Ananya Deshmukh (Officer)',
    role: 'MUNICIPAL_OFFICER',
    phone: '+91 9876543221',
    employeeId: 'MC-OFF-2027',
    ward: 'Ward 20 - Dhankawadi & Katraj, Pune',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000006',
    id: '67b300000000000000000006',
    email: 'driver@example.com',
    name: 'Ramesh Kumar (Driver)',
    role: 'GARBAGE_DRIVER',
    phone: '+91 9876543212',
    driverId: 'DRV-882',
    employeeId: 'EMP-DRV-001',
    vehicleNumber: 'MH-12-GB-4040',
    vehicleType: 'Heavy Compactor Truck',
    availability: 'AVAILABLE',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000007',
    id: '67b300000000000000000007',
    email: 'driver2@example.com',
    name: 'Sunil Verma (Driver)',
    role: 'GARBAGE_DRIVER',
    phone: '+91 9876543215',
    driverId: 'DRV-104',
    employeeId: 'EMP-DRV-002',
    vehicleNumber: 'MH-12-GB-5050',
    vehicleType: 'Medium Tipper Truck',
    availability: 'AVAILABLE',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000008',
    id: '67b300000000000000000008',
    email: 'driver3@example.com',
    name: 'Amitabh Ghosh (Driver)',
    role: 'GARBAGE_DRIVER',
    phone: '+91 9876543216',
    driverId: 'DRV-305',
    employeeId: 'EMP-DRV-003',
    vehicleNumber: 'MH-12-GB-6060',
    vehicleType: 'Electric Mini Van',
    availability: 'BUSY',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000009',
    id: '67b300000000000000000009',
    email: 'driver4@example.com',
    name: 'Rajesh Chauhan (Driver)',
    role: 'GARBAGE_DRIVER',
    phone: '+91 9876543217',
    driverId: 'DRV-412',
    employeeId: 'EMP-DRV-004',
    vehicleNumber: 'MH-12-GB-7070',
    vehicleType: 'Hydraulic Lift Loader',
    availability: 'AVAILABLE',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  },
  {
    _id: '67b300000000000000000010',
    id: '67b300000000000000000010',
    email: 'driver5@example.com',
    name: 'Kavita Rao (Driver)',
    role: 'GARBAGE_DRIVER',
    phone: '+91 9876543218',
    driverId: 'DRV-520',
    employeeId: 'EMP-DRV-005',
    vehicleNumber: 'MH-12-GB-8080',
    vehicleType: 'Heavy Compactor Truck',
    availability: 'OFFLINE',
    password: 'password123',
    passwordHash: '$2a$10$MfiXa0QhwFbJmitSVa9ghui2DmrhoW4nh.KNBmzd7CSGfl2BqJ3I.'
  }
];

const findDemoUserById = (id) => {
  if (!id) return null;
  const targetId = id.toString();
  return (
    DEMO_USERS.find(
      (u) =>
        u.id === targetId ||
        u._id === targetId ||
        u.driverId === targetId ||
        u.employeeId === targetId ||
        (targetId === 'mock-citizen-001' && u.email === 'citizen@example.com') ||
        (targetId === 'mock-citizen-002' && u.email === 'citizen2@example.com') ||
        (targetId === 'mock-citizen-003' && u.email === 'citizen3@example.com') ||
        (targetId === 'mock-officer-001' && u.email === 'officer@example.com') ||
        (targetId === 'mock-officer-002' && u.email === 'officer2@example.com') ||
        (targetId === 'mock-driver-001' && u.email === 'driver@example.com') ||
        (targetId === 'mock-driver-002' && u.email === 'driver2@example.com')
    ) || null
  );
};

const findDemoUserByEmail = (email) => {
  if (!email) return null;
  const cleanEmail = email.trim().toLowerCase();
  const direct = DEMO_USERS.find((u) => u.email.toLowerCase() === cleanEmail);
  if (direct) return direct;

  for (const u of DEMO_USERS) {
    if (u.name) {
      const cleanName = u.name.toLowerCase().replace(/\s*\(driver\)|\s*\(citizen\)|\s*\(officer\)/gi, '').trim();
      const dotEmail = cleanName.replace(/\s+/g, '.') + '@demo.local';
      const exampleEmail = cleanName.replace(/\s+/g, '.') + '@example.com';
      if (cleanEmail === dotEmail || cleanEmail === exampleEmail) {
        return u;
      }
    }
  }
  return null;
};

module.exports = {
  DEMO_USERS,
  findDemoUserById,
  findDemoUserByEmail
};
