const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const env = require('./config/env');
const { getDBStatus } = require('./config/db');

// Route imports
const authRoutes = require('./routes/authRoutes');
const complaintRoutes = require('./routes/complaintRoutes');
const officerRoutes = require('./routes/officerRoutes');
const driverRoutes = require('./routes/driverRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const mediaRoutes = require('./routes/mediaRoutes');

// Middleware imports
const { notFoundHandler, errorHandler } = require('./middleware/errorMiddleware');

const app = express();

// Security headers
app.use(helmet());

// CORS configuration
app.use(
  cors({
    origin: env.CLIENT_ORIGIN === '*' ? true : env.CLIENT_ORIGIN,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
  })
);

// Body parser with comfortable JSON size
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Request logging (suppressed during unit tests)
if (env.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}

// Health check endpoint
app.get('/api/health', (req, res) => {
  const dbStatus = getDBStatus();
  return res.status(200).json({
    success: true,
    message: 'Smart Garbage Management API is running',
    version: '1.0.0',
    database: dbStatus.state,
    timestamp: new Date().toISOString()
  });
});

// API Routes mounting
app.use('/api/auth', authRoutes);
app.use('/api/complaints', complaintRoutes);
app.use('/api/officer', officerRoutes);
app.use('/api/driver', driverRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/media', mediaRoutes);

// Catch-all 404 handler
app.use(notFoundHandler);

// Centralized error handler
app.use(errorHandler);

module.exports = app;
