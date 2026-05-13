package Controller;

import DAO.NotificationDAO;
import Model.Notification;
import java.util.List;

public class NotificationController {

    private final NotificationDAO notificationDAO;

    public NotificationController() {
        this.notificationDAO = new NotificationDAO();
    }

    public boolean sendToUser(int userId, String title, String message, String type) {
        return notificationDAO.send(userId, title, message, type);
    }

    public boolean sendBroadcast(String title, String message, String type) {
        return notificationDAO.broadcast(title, message, type);
    }

    public List<Notification> getUnread(int userId) {
        // Return all notifications, can be filtered by isRead later
        return notificationDAO.findByUserId(userId);
    }

    public boolean markAllRead(int userId) {
        return notificationDAO.markAllRead(userId) > 0;
    }
}
