const Notification = require('../models/Notification');
const crypto = require('crypto');

/**
 * Service to dispatch and persist in-app notifications for users.
 */
const createNotification = async ({ userId, type, title, message, complaintId = null }) => {
  try {
    if (!userId || !title || !message) {
      console.warn('[NotificationService] Missing required parameters to create notification');
      return null;
    }

    const notificationId = `NOTIF-${Date.now()}-${crypto.randomBytes(3).toString('hex')}`;
    const notification = await Notification.create({
      notificationId,
      userId,
      type,
      title,
      message,
      complaintId,
      isRead: false
    });

    return notification;
  } catch (error) {
    console.error(`[NotificationService] Error creating notification: ${error.message}`);
    return null;
  }
};

module.exports = {
  createNotification
};
