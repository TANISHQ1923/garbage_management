const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema(
  {
    notificationId: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true
    },
    userId: {
      type: String,
      required: [true, 'User ID is required'],
      trim: true,
      index: true
    },
    type: {
      type: String,
      required: true,
      enum: [
        'COMPLAINT_SUBMITTED',
        'COMPLAINT_STATUS_UPDATED',
        'EMERGENCY_REQUEST_RECEIVED',
        'PICKUP_ASSIGNED',
        'PICKUP_STARTED',
        'CLEANUP_COMPLETED',
        'CLEANUP_VERIFIED',
        'DRIVER_ASSIGNMENT',
        'SYSTEM_NOTIFICATION'
      ],
      default: 'SYSTEM_NOTIFICATION'
    },
    title: {
      type: String,
      required: [true, 'Title is required'],
      trim: true
    },
    message: {
      type: String,
      required: [true, 'Message is required'],
      trim: true
    },
    complaintId: {
      type: String,
      default: null,
      trim: true
    },
    isRead: {
      type: Boolean,
      default: false,
      index: true
    }
  },
  {
    timestamps: true
  }
);

notificationSchema.set('toJSON', {
  transform: function (doc, ret) {
    ret.id = ret.notificationId || ret._id.toString();
    delete ret._id;
    delete ret.__v;
    return ret;
  }
});

module.exports = mongoose.model('Notification', notificationSchema);
