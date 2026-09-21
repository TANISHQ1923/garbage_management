const jwt = require('jsonwebtoken');
const env = require('../config/env');

/**
 * Generates a signed JSON Web Token (JWT) for the authenticated user.
 * @param {Object} userPayload - Information to embed in token (id, email, role)
 * @returns {string} Signed JWT token
 */
const generateToken = (userPayload) => {
  return jwt.sign(
    {
      id: userPayload.id || userPayload._id,
      email: userPayload.email,
      role: userPayload.role
    },
    env.JWT_SECRET,
    {
      expiresIn: env.JWT_EXPIRES_IN
    }
  );
};

module.exports = generateToken;
