const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Name is required'],
      trim: true,
      maxlength: [100, 'Name cannot exceed 100 characters']
    },
    email: {
      type: String,
      required: [true, 'Email is required'],
      unique: true,
      lowercase: true,
      trim: true,
      match: [/^\S+@\S+\.\S+$/, 'Please enter a valid email address']
    },
    passwordHash: {
      type: String,
      required: [true, 'Password hash is required'],
      select: false // Excluded by default in queries
    },
    role: {
      type: String,
      required: [true, 'Role is required'],
      enum: {
        values: ['CITIZEN', 'MUNICIPAL_OFFICER', 'GARBAGE_DRIVER'],
        message: 'Role must be CITIZEN, MUNICIPAL_OFFICER, or GARBAGE_DRIVER'
      },
      default: 'CITIZEN'
    },
    phone: {
      type: String,
      trim: true,
      default: null
    },
    address: {
      type: String,
      trim: true,
      default: null
    },
    // Officer & Driver specific fields
    employeeId: {
      type: String,
      trim: true,
      default: null
    },
    ward: {
      type: String,
      trim: true,
      default: null
    },
    // Driver specific fields
    driverId: {
      type: String,
      trim: true,
      default: null
    },
    vehicleNumber: {
      type: String,
      trim: true,
      default: null
    },
    vehicleType: {
      type: String,
      trim: true,
      default: 'Heavy Compactor Truck'
    },
    availability: {
      type: String,
      enum: ['AVAILABLE', 'BUSY', 'OFFLINE'],
      default: 'AVAILABLE'
    }
  },
  {
    timestamps: true
  }
);

// Method to verify candidate password against stored bcrypt hash
userSchema.methods.comparePassword = async function (enteredPassword) {
  return await bcrypt.compare(enteredPassword, this.passwordHash);
};

// Transform toJSON to eliminate sensitive fields and format id
userSchema.set('toJSON', {
  transform: function (doc, ret) {
    ret.id = ret._id.toString();
    delete ret._id;
    delete ret.__v;
    delete ret.passwordHash;
    return ret;
  }
});

module.exports = mongoose.model('User', userSchema);
