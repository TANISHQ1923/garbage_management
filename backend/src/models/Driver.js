const mongoose = require('mongoose');

const driverLocationSchema = new mongoose.Schema(
  {
    latitude: { type: Number, default: 28.6139 },
    longitude: { type: Number, default: 77.2090 },
    timestamp: { type: Number, default: () => Date.now() }
  },
  { _id: false }
);

const driverSchema = new mongoose.Schema(
  {
    driverId: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      index: true
    },
    employeeId: {
      type: String,
      trim: true,
      default: null
    },
    name: {
      type: String,
      required: [true, 'Driver name is required'],
      trim: true
    },
    phone: {
      type: String,
      required: [true, 'Driver phone is required'],
      trim: true
    },
    vehicleNumber: {
      type: String,
      required: [true, 'Vehicle number is required'],
      trim: true
    },
    vehicleType: {
      type: String,
      default: 'Heavy Compactor Truck',
      trim: true
    },
    availability: {
      type: String,
      enum: ['AVAILABLE', 'BUSY', 'OFFLINE'],
      default: 'AVAILABLE',
      index: true
    },
    currentLocation: {
      type: driverLocationSchema,
      default: () => ({
        latitude: 28.6139,
        longitude: 77.2090,
        timestamp: Date.now()
      })
    }
  },
  {
    timestamps: true
  }
);

driverSchema.set('toJSON', {
  transform: function (doc, ret) {
    ret.id = ret.driverId || ret._id.toString();
    delete ret._id;
    delete ret.__v;
    return ret;
  }
});

module.exports = mongoose.model('Driver', driverSchema);
