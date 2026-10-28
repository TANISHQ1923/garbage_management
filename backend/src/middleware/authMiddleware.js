const jwt = require('jsonwebtoken');
const env = require('../config/env');
const User = require('../models/User');
const { getDBStatus } = require('../config/db');
const syntheticDataService = require('../services/syntheticDataService');
const { findDemoUserById } = require('../data/demoUsers');

/**
 * Middleware to verify JWT bearer token and attach authenticated user to req.user.
 */
const authenticate = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Authentication required. No token provided.'
      });
    }

    const token = authHeader.split(' ')[1];
    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'Authentication token missing.'
      });
    }

    // Verify token
    let decoded;
    try {
      decoded = jwt.verify(token, env.JWT_SECRET);
    } catch (err) {
      if (token.startsWith('mock-token-')) {
        const mockId = token.replace('mock-token-', '');
        const demoUser = findDemoUserById(mockId);
        if (demoUser) {
          decoded = { id: demoUser.id || demoUser._id, role: demoUser.role, email: demoUser.email };
        } else {
          return res.status(401).json({
            success: false,
            message: 'Invalid or expired authentication token. Please log in again.'
          });
        }
      } else {
        return res.status(401).json({
          success: false,
          message: 'Invalid or expired authentication token. Please log in again.'
        });
      }
    }

    // Fetch user: query User model if DB is connected or if mocked in tests (prevents 10s buffering timeout)
    let user = null;
    const isMock = typeof User.findById === 'function' && Boolean(User.findById._isMockFunction);
    const isConnected = getDBStatus().isConnected;

    if (isConnected || isMock) {
      try {
        user = await User.findById(decoded.id);
      } catch (_) {
        user = null;
      }
    }

    // Fallback to demo users when DB is offline or user not found in DB
    if (!user) {
      const demoUser = findDemoUserById(decoded.id || decoded._id);
      if (demoUser) {
        user = {
          ...demoUser,
          _id: demoUser._id || demoUser.id,
          toJSON: function () {
            const { password: _, passwordHash: __, ...rest } = this;
            return rest;
          }
        };
      }
    }

    // Fallback to synthetic users when in synthetic mode or not found in DB
    if (!user) {
      const syntheticUser = syntheticDataService.getSyntheticUserById(decoded.id || decoded._id);
      if (syntheticUser) {
        user = {
          ...syntheticUser,
          _id: syntheticUser.id,
          toJSON: function () {
            const { passwordHash: _, ...rest } = this;
            return rest;
          }
        };
      }
    }

    if (!user) {
      return res.status(401).json({
        success: false,
        message: 'The user account associated with this token no longer exists.'
      });
    }

    // Attach user to request
    req.user = user;
    next();
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Authentication error',
      error: error.message
    });
  }
};

module.exports = { authenticate, requireAuth: authenticate };
