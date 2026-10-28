const Notification = require('../models/Notification');
const crypto = require('crypto');
const syntheticDataService = require('./syntheticDataService');

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
    const now = Date.now();

    const notifObj = {
      id: notificationId,
      notificationId,
      userId: userId.toString(),
      type,
      title,
      message,
      complaintId,
      isRead: false,
      createdAt: now,
      updatedAt: now
    };

    if (syntheticDataService.isSyntheticMode()) {
      syntheticDataService.addSyntheticNotification(notifObj);
      return notifObj;
    }

    const notification = await Notification.create(notifObj);
    return notification;
  } catch (error) {
    console.error(`[NotificationService] Error creating notification: ${error.message}`);
    return null;
  }
};

module.exports = {
  createNotification
};
