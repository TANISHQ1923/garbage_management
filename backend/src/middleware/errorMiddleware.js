const env = require('../config/env');

/**
 * Handle 404 Route Not Found
 */
const notFoundHandler = (req, res, next) => {
  res.status(404).json({
    success: false,
    message: `API endpoint not found: ${req.method} ${req.originalUrl}`
  });
};

/**
 * Centralized Error Handler Middleware
 */
const errorHandler = (err, req, res, next) => {
  console.error(`[Error] ${err.name || 'Application Error'}: ${err.message}`);

  // Mongoose duplicate key error (E11000)
  if (err.code === 11000) {
    const field = Object.keys(err.keyValue || {})[0] || 'field';
    return res.status(409).json({
      success: false,
      message: `An account or record with this ${field} already exists.`
    });
  }

  // Mongoose validation error
  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map((val) => val.message);
    return res.status(400).json({
      success: false,
      message: 'Database validation failed',
      errors: messages
    });
  }

  // Mongoose bad ObjectId format
  if (err.name === 'CastError') {
    return res.status(400).json({
      success: false,
      message: `Resource not found with format of ${err.path}`
    });
  }

  // JWT errors
  if (err.name === 'JsonWebTokenError') {
    return res.status(401).json({
      success: false,
      message: 'Invalid authorization token.'
    });
  }

  if (err.name === 'TokenExpiredError') {
    return res.status(401).json({
      success: false,
      message: 'Authorization token has expired. Please log in again.'
    });
  }

  // Multer file upload errors
  if (err.name === 'MulterError') {
    return res.status(400).json({
      success: false,
      message: `File upload error: ${err.message}`
    });
  }

  const statusCode = res.statusCode >= 400 ? res.statusCode : (err.statusCode || 500);

  res.status(statusCode).json({
    success: false,
    message: err.message || 'Internal Server Error',
    ...(env.NODE_ENV === 'development' ? { stack: err.stack } : {})
  });
};

module.exports = {
  notFoundHandler,
  errorHandler
};
