const mongoose = require('mongoose');

const officerNoteSchema = new mongoose.Schema(
  {
    id: { type: String, required: true },
    officerId: { type: String, required: true },
    officerName: { type: String, required: true },
    text: { type: String, required: true },
    createdAt: { type: Number, default: () => Date.now() }
  },
  { _id: false }
);

const statusHistorySchema = new mongoose.Schema(
  {
    status: {
      type: String,
      required: true,
      enum: ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS', 'CLEANED', 'VERIFIED', 'REJECTED']
    },
    timestamp: { type: Number, default: () => Date.now() },
    changedBy: { type: String, default: 'System' },
    role: { type: String, default: 'SYSTEM' },
    note: { type: String, default: '' }
  },
  { _id: false }
);

const aiAnalysisSchema = new mongoose.Schema(
  {
    classification: {
      category: { type: String, default: null },
      confidence: { type: Number, default: null }
    },
    severity: {
      level: { type: String, default: null },
      confidence: { type: Number, default: null }
    },
    modelStatus: { type: String, default: 'DEVELOPMENT_BASELINE' },
    analyzedAt: { type: Date, default: null }
  },
  { _id: false }
);

const feedbackSchema = new mongoose.Schema(
  {
    rating: {
      type: Number,
      required: [true, 'Rating is required'],
      min: [1, 'Rating must be at least 1 star'],
      max: [5, 'Rating cannot exceed 5 stars'],
      validate: {
        validator: Number.isInteger,
        message: 'Rating must be an integer between 1 and 5'
      }
    },
    comment: {
      type: String,
      maxlength: [500, 'Comment cannot exceed 500 characters'],
      trim: true,
      default: ''
    },
    submittedAt: {
      type: Number,
      default: () => Date.now()
    },
    citizenId: {
      type: String,
      required: true
    },
    citizenName: {
      type: String,
      default: ''
    }
  },
  { _id: false }
);

const complaintSchema = new mongoose.Schema(
  {
    complaintId: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true
    },
    source: { type: String, enum: ['CITIZEN', 'SENSOR'], default: 'CITIZEN' },
    binId: { type: String, default: null },
    activeSensorTask: { type: Boolean, default: false },
    sensorEmptyConfirmedAt: { type: Number, default: null },
    dispatchRetry: {
      state: { type: String, enum: ['PENDING_DRIVER', 'ASSIGNED', 'CLOSED'], default: undefined },
      attempts: { type: Number, default: 0 },
      nextAttemptAt: { type: Number, default: null },
      reason: { type: String, default: null },
      leaseUntil: { type: Number, default: 0 }
    },
    citizenId: {
      type: String,
      required: function () { return this.source !== 'SENSOR'; },
      index: true
    },
    citizenName: {
      type: String,
      required: function () { return this.source !== 'SENSOR'; }
    },
    citizenPhone: {
      type: String,
      default: null
    },
    garbageType: {
      type: String,
      required: [true, 'Garbage type is required'],
      enum: [
        'OVERFLOWING_BIN',
        'FESTIVAL_WASTE',
        'EVENT_WASTE',
        'ROADSIDE_GARBAGE',
        'FLOOD_WASTE',
        'OTHER_WASTE'
      ],
      default: 'OVERFLOWING_BIN'
    },
    description: {
      type: String,
      trim: true,
      default: ''
    },
    imageUrl: {
      type: String,
      default: null
    },
    imagePublicId: {
      type: String,
      default: null
    },
    videoUrl: {
      type: String,
      default: null
    },
    videoPublicId: {
      type: String,
      default: null
    },
    latitude: {
      type: Number,
      default: null
    },
    longitude: {
      type: Number,
      default: null
    },
    locationText: {
      type: String,
      required: [true, 'Location description is required'],
      trim: true
    },
    emergency: {
      type: Boolean,
      default: false
    },
    status: {
      type: String,
      required: true,
      enum: ['SUBMITTED', 'ASSIGNED', 'PICKUP_IN_PROGRESS', 'CLEANED', 'VERIFIED', 'REJECTED'],
      default: 'SUBMITTED',
      index: true
    },
    // Driver Assignment Details
    assignedDriverId: {
      type: String,
      default: null,
      index: true
    },
    assignedDriverName: {
      type: String,
      default: null
    },
    assignedVehicleNumber: {
      type: String,
      default: null
    },
    assignedAt: {
      type: Number,
      default: null
    },
    // Officer notes
    officerNotes: {
      type: [officerNoteSchema],
      default: []
    },
    // Collection Evidence
    beforeCleaningImageUrl: {
      type: String,
      default: null
    },
    beforeCleaningImagePublicId: {
      type: String,
      default: null
    },
    afterCleaningImageUrl: {
      type: String,
      default: null
    },
    afterCleaningImagePublicId: {
      type: String,
      default: null
    },
    // Lifecycle Status History
    statusHistory: {
      type: [statusHistorySchema],
      default: []
    },
    // AI Waste Analysis (Step 9)
    aiAnalysis: {
      type: aiAnalysisSchema,
      default: null
    },
    aiAnalysisStatus: {
      type: String,
      enum: ['PENDING', 'COMPLETED', 'FAILED', 'AI_UNAVAILABLE'],
      default: 'PENDING'
    },
    // Citizen Cleanup Feedback
    feedback: {
      type: feedbackSchema,
      default: null
    }
  },
  {
    timestamps: true
  }
);

complaintSchema.index({ binId: 1 }, { name: 'one_active_sensor_task_per_bin', unique: true,
  partialFilterExpression: { source: 'SENSOR', activeSensorTask: true } });
complaintSchema.index({ source: 1, status: 1, 'dispatchRetry.nextAttemptAt': 1 });

complaintSchema.set('toJSON', {
  transform: function (doc, ret) {
    ret.id = ret.complaintId || ret._id.toString();
    delete ret._id;
    delete ret.__v;
    return ret;
  }
});

module.exports = mongoose.model('Complaint', complaintSchema);
