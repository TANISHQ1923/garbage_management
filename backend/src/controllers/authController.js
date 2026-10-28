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

    // Check synthetic users if in synthetic mode or DB offline
    if (syntheticDataService.isSyntheticMode() || !getDBStatus().isConnected) {
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
            message: 'Login successful (Synthetic Demo Mode)',
            data: {
              user: { ...cleanSynUser, token },
              token
            }
          });
        }
      }

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
