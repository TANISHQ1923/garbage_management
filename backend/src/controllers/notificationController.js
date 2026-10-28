const Notification = require('../models/Notification');
const syntheticDataService = require('../services/syntheticDataService');

/**
 * Get notifications for authenticated user
 * GET /api/notifications
 */
const getNotifications = async (req, res, next) => {
  try {
    const userId = req.user.id || req.user._id.toString();
    const { isRead, limit = 50 } = req.query;

    if (syntheticDataService.isSyntheticMode()) {
      let notifications = syntheticDataService.getSyntheticNotifications(userId);
      if (req.user.driverId && notifications.length === 0) {
        notifications = syntheticDataService.getSyntheticNotifications(req.user.driverId);
      }
      if (isRead !== undefined) {
        const isR = isRead === 'true' || isRead === true;
        notifications = notifications.filter((n) => n.isRead === isR);
      }
      const unreadCount = notifications.filter((n) => !n.isRead).length;
      return res.status(200).json({
        success: true,
        count: notifications.length,
        unreadCount,
        data: notifications
      });
    }

    const filter = {
      $or: [{ userId }, { userId: req.user.driverId || '__none__' }]
    };

    if (isRead !== undefined) {
      filter.isRead = isRead === 'true' || isRead === true;
    }

    const notifications = await Notification.find(filter)
      .sort({ createdAt: -1 })
      .limit(parseInt(limit, 10));

    const unreadCount = await Notification.countDocuments({
      $or: [{ userId }, { userId: req.user.driverId || '__none__' }],
      isRead: false
    });

    return res.status(200).json({
      success: true,
      count: notifications.length,
      unreadCount,
      data: notifications
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Mark a single notification as read
 * PUT /api/notifications/:id/read
 */
const markAsRead = async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.user.id || req.user._id.toString();

    if (syntheticDataService.isSyntheticMode()) {
      return res.status(200).json({
        success: true,
        message: 'Notification marked as read (Synthetic Demo Mode)',
        data: { id, isRead: true }
      });
    }

    const notification = await Notification.findOneAndUpdate(
      {
        $or: [{ notificationId: id }, { _id: id.match(/^[0-9a-fA-F]{24}$/) ? id : null }],
        $or: [{ userId }, { userId: req.user.driverId || '__none__' }]
      },
      { isRead: true },
      { new: true }
    );

    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Notification not found or unauthorized.'
      });
    }

    return res.status(200).json({
      success: true,
      message: 'Notification marked as read',
      data: notification
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Mark all notifications as read for current user
 * PUT /api/notifications/read-all
 */
const markAllAsRead = async (req, res, next) => {
  try {
    const userId = req.user.id || req.user._id.toString();

    if (syntheticDataService.isSyntheticMode()) {
      return res.status(200).json({
        success: true,
        message: 'All notifications marked as read (Synthetic Demo Mode)'
      });
    }

    await Notification.updateMany(
      {
        $or: [{ userId }, { userId: req.user.driverId || '__none__' }],
        isRead: false
      },
      { isRead: true }
    );

    return res.status(200).json({
      success: true,
      message: 'All notifications marked as read'
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  getNotifications,
  markAsRead,
  markAllAsRead
};
