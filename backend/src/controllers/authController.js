const bcrypt = require('bcryptjs');
const User = require('../models/User');
const Driver = require('../models/Driver');
const generateToken = require('../utils/generateToken');
const { getDBStatus } = require('../config/db');

// In-memory demo users used when MongoDB is offline / disconnected
const DEMO_USERS = [
  {
    _id: '67b300000000000000000001',
    id: '67b300000000000000000001',
    email: 'citizen@example.com',
    name: 'Aarav Sharma (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543210',
    address: 'Sector 4, Green Park, Smart City',
    password: 'password123'
  },
  {
    _id: '67b300000000000000000002',
    id: '67b300000000000000000002',
    email: 'citizen2@example.com',
    name: 'Priya Singh (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543220',
    address: 'Tower B, Lotus Apartments, Ward 12',
    password: 'password123'
  },
  {
    _id: '67b300000000000000000003',
    id: '67b300000000000000000003',
    email: 'citizen3@example.com',
    name: 'Rahul Mehta (Citizen)',
    role: 'CITIZEN',
    phone: '+91 9876543230',
    address: '42 Heritage Colony, Central Zone',
    password: 'password123'
  },
  {
    _id: '67b300000000000000000004',
    id: '67b300000000000000000004',
    email: 'officer@example.com',
    name: 'Vikram Patel (Officer)',
    role: 'MUNICIPAL_OFFICER',
    phone: '+91 9876543211',
    employeeId: 'MC-OFF-2026',
    ward: 'Ward 12 - Central Zone',
    password: 'password123'
  },
  {
    _id: '67b300000000000000000005',
    id: '67b300000000000000000005',
    email: 'officer2@example.com',
    name: 'Ananya Deshmukh (Officer)',
    role: 'MUNICIPAL_OFFICER',
    phone: '+91 9876543221',
    employeeId: 'MC-OFF-2027',
    ward: 'Ward 14 - North Zone',
    password: 'password123'
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
    vehicleNumber: 'DL-01-GB-4040',
    vehicleType: 'Heavy Compactor Truck',
    availability: 'AVAILABLE',
    password: 'password123'
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
    vehicleNumber: 'DL-01-GB-5050',
    vehicleType: 'Medium Tipper Truck',
    availability: 'AVAILABLE',
    password: 'password123'
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
    vehicleNumber: 'DL-01-GB-6060',
    vehicleType: 'Electric Mini Van',
    availability: 'BUSY',
    password: 'password123'
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
    vehicleNumber: 'DL-01-GB-7070',
    vehicleType: 'Hydraulic Lift Loader',
    availability: 'AVAILABLE',
    password: 'password123'
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
    vehicleNumber: 'DL-01-GB-8080',
    vehicleType: 'Heavy Compactor Truck',
    availability: 'OFFLINE',
    password: 'password123'
  }
];

/**
 * Register a new user
 * POST /api/auth/register
 */
const register = async (req, res, next) => {
  try {
    const {
      name,
      email,
      password,
      role = 'CITIZEN',
      phone,
      address,
      employeeId,
      ward,
      driverId,
      vehicleNumber,
      vehicleType
    } = req.body;

    // Validate email & password presence
    if (!name || !email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Name, email, and password are required.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();

    // Check duplicate
    const existingUser = await User.findOne({ email: cleanEmail });
    if (existingUser) {
      return res.status(409).json({
        success: false,
        message: 'An account with this email already exists.'
      });
    }

    // Hash password
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);

    // Create user
    const newUser = await User.create({
      name: name.trim(),
      email: cleanEmail,
      passwordHash,
      role,
      phone: phone || null,
      address: address || null,
      employeeId: employeeId || null,
      ward: ward || null,
      driverId: driverId || null,
      vehicleNumber: vehicleNumber || null,
      vehicleType: vehicleType || (role === 'GARBAGE_DRIVER' ? 'Heavy Compactor Truck' : null)
    });

    // Create matching driver record if role is GARBAGE_DRIVER
    if (role === 'GARBAGE_DRIVER') {
      const assignedDriverId = driverId || `DRV-${Math.floor(100 + Math.random() * 900)}`;
      await Driver.findOneAndUpdate(
        { driverId: assignedDriverId },
        {
          driverId: assignedDriverId,
          employeeId: employeeId || null,
          name: name.trim(),
          phone: phone || 'N/A',
          vehicleNumber: vehicleNumber || 'N/A',
          vehicleType: vehicleType || 'Heavy Compactor Truck',
          availability: 'AVAILABLE'
        },
        { upsert: true, new: true }
      );
    }

    // Generate JWT
    const token = generateToken(newUser);

    const userJson = newUser.toJSON();
    userJson.token = token;

    return res.status(201).json({
      success: true,
      message: 'User registered successfully',
      data: {
        user: userJson,
        token
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Login user
 * POST /api/auth/login
 */
const login = async (req, res, next) => {
  try {
    const { email, password, role } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: 'Email and password are required.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();

    // If MongoDB is not connected, use in-memory demo users directly (prevents 10s buffering timeout)
    if (!getDBStatus().isConnected) {
      const demoMatch = DEMO_USERS.find(
        (u) => u.email.toLowerCase() === cleanEmail && u.password === password
      );
      if (demoMatch) {
        if (role && demoMatch.role !== role) {
          return res.status(401).json({
            success: false,
            message: `Role mismatch: This account is registered as ${demoMatch.role}.`
          });
        }
        const token = generateToken(demoMatch);
        const { password: _, ...cleanUser } = demoMatch;
        return res.status(200).json({
          success: true,
          message: 'Login successful',
          data: {
            user: { ...cleanUser, token },
            token
          }
        });
      }
    }

    // Query user in MongoDB and explicitly select passwordHash
    const user = await User.findOne({ email: cleanEmail }).select('+passwordHash');
    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.'
      });
    }

    // Compare password
    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.'
      });
    }

    // If a specific role was requested for login, verify it
    if (role && user.role !== role) {
      return res.status(401).json({
        success: false,
        message: `Role mismatch: This account is registered as ${user.role}.`
      });
    }

    // Generate token
    const token = generateToken(user);

    const userJson = user.toJSON();
    userJson.token = token;

    return res.status(200).json({
      success: true,
      message: 'Login successful',
      data: {
        user: userJson,
        token
      }
    });
  } catch (error) {
    // Fail-safe: If DB buffer timed out or threw, check demo accounts
    const { email, password, role } = req.body;
    if (email && password) {
      const cleanEmail = email.trim().toLowerCase();
      const demoMatch = DEMO_USERS.find(
        (u) => u.email.toLowerCase() === cleanEmail && u.password === password
      );
      if (demoMatch) {
        if (role && demoMatch.role !== role) {
          return res.status(401).json({
            success: false,
            message: `Role mismatch: This account is registered as ${demoMatch.role}.`
          });
        }
        const token = generateToken(demoMatch);
        const { password: _, ...cleanUser } = demoMatch;
        return res.status(200).json({
          success: true,
          message: 'Login successful',
          data: {
            user: { ...cleanUser, token },
            token
          }
        });
      }
    }
    next(error);
  }
};

/**
 * Get current authenticated user profile
 * GET /api/auth/me
 */
const getMe = async (req, res, next) => {
  try {
    const user = req.user.toJSON ? req.user.toJSON() : req.user;
    return res.status(200).json({
      success: true,
      data: user
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Informational Forgot Password endpoint
 * POST /api/auth/forgot-password
 */
const forgotPassword = async (req, res, next) => {
  try {
    const { email } = req.body;
    return res.status(200).json({
      success: true,
      message: `If an account exists with ${email || 'this email'}, password reset instructions have been dispatched.`
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  register,
  login,
  getMe,
  forgotPassword
};
