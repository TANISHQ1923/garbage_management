const bcrypt = require('bcryptjs');
const User = require('../models/User');
const Driver = require('../models/Driver');
const generateToken = require('../utils/generateToken');
const { getDBStatus } = require('../config/db');
const syntheticDataService = require('../services/syntheticDataService');

const { DEMO_USERS, findDemoUserByEmail } = require('../data/demoUsers');

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

    if (role !== 'CITIZEN') {
      return res.status(403).json({ success: false, message: 'Public signup is for citizens only. Staff accounts require controlled provisioning.' });
    }
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
      employeeId: null,
      ward: null,
      driverId: null,
      vehicleNumber: null,
      vehicleType: vehicleType || (role === 'GARBAGE_DRIVER' ? 'Heavy Compactor Truck' : null)
    });

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

    // Check synthetic users if in synthetic mode or DB offline
    if (syntheticDataService.isSyntheticMode()) {
      const synUser = syntheticDataService.getSyntheticUserByEmail(cleanEmail);
      if (synUser) {
        const isMatch = await bcrypt.compare(password, synUser.passwordHash);
        if (isMatch) {
          if (role && synUser.role !== role) {
            return res.status(401).json({
              success: false,
              message: `Role mismatch: This account is registered as ${synUser.role}.`
            });
          }
          const token = generateToken(synUser);
          const { passwordHash: _, ...cleanSynUser } = synUser;
          if (cleanSynUser.loginEmail) {
            cleanSynUser.email = cleanSynUser.loginEmail;
          }
          return res.status(200).json({
            success: true,
            message: 'Login successful',
            data: {
              user: { ...cleanSynUser, token },
              token
            }
          });
        }
      }

      const demoMatch = findDemoUserByEmail(cleanEmail) || DEMO_USERS.find(
        (u) => u.email.toLowerCase() === cleanEmail
      );
      if (demoMatch) {
        const isPassMatch = demoMatch.password === password ||
          (demoMatch.passwordHash && (await bcrypt.compare(password, demoMatch.passwordHash)));
        if (isPassMatch) {
          if (role && demoMatch.role !== role) {
            return res.status(401).json({
              success: false,
              message: `Role mismatch: This account is registered as ${demoMatch.role}.`
            });
          }
          const token = generateToken(demoMatch);
          const { password: _, passwordHash: __, ...cleanUser } = demoMatch;
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
    }

    if (!getDBStatus().isConnected) {
      return res.status(401).json({
        success: false,
        message: 'Invalid email or password.'
      });
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
const forgotPassword = async (req, res) => {
  return res.status(501).json({ success: false, message: 'Password reset is not configured. Contact municipal support.' });
};

module.exports = {
  register,
  login,
  getMe,
  forgotPassword
};
