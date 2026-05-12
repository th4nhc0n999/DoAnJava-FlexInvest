package Controller;

import DAO.NotificationDAO;
import Model.Notification;

import java.util.List;

/**
 * Controller quản lý thông báo.
 * Wrapper trên NotificationDAO — thêm logic nghiệp vụ nếu cần.
 */
public class NotificationController {

    private final NotificationDAO notifDAO = new NotificationDAO();

    // ── Gửi thông báo ────────────────────────────────────────────────────────

    /**
     * Gửi thông báo đến một user cụ thể.
     *
     * @param userId ID người nhận
     * @param title  Tiêu đề
     * @param body   Nội dung chi tiết (có thể null)
     * @param type   MISSION | TRANSACTION | SYSTEM | EKYC | ...
     * @return true nếu gửi thành công
     */
    public boolean sendToUser(int userId, String title, String body, String type) {
        if (userId <= 0 || title == null || title.isBlank()) return false;
        return notifDAO.send(userId, title, body, type);
    }

    /** Overload không có body */
    public boolean sendToUser(int userId, String title, String type) {
        return sendToUser(userId, title, null, type);
    }

    /**
     * Gửi thông báo đến tất cả user trong danh sách userIds.
     * Dùng sau batch xử lý đáo hạn — mỗi user nhận 1 notif riêng.
     *
     * @param userIds Danh sách userId cần gửi
     * @param title   Tiêu đề chung
     * @param body    Nội dung chung
     * @param type    Loại thông báo
     * @return số user gửi thành công
     */
    public int sendBroadcast(List<Integer> userIds, String title, String body, String type) {
        if (userIds == null || userIds.isEmpty()) return 0;
        int count = 0;
        for (int userId : userIds) {
            if (notifDAO.send(userId, title, body, type)) count++;
        }
        System.out.printf("[NotificationController.sendBroadcast] Đã gửi %d/%d thông báo.%n",
                count, userIds.size());
        return count;
    }

    /**
     * Broadcast đến TẤT CẢ user (truyền danh sách đầy đủ từ ngoài vào).
     * Admin dùng khi thông báo toàn hệ thống.
     */
    public int sendBroadcast(List<Integer> allUserIds, String title, String type) {
        return sendBroadcast(allUserIds, title, null, type);
    }

    // ── Đọc thông báo ────────────────────────────────────────────────────────

    /**
     * Lấy tất cả thông báo của user, mới nhất trên cùng.
     */
    public List<Notification> getAll(int userId) {
        return notifDAO.findByUserId(userId);
    }

    /**
     * Lấy N thông báo gần nhất (dùng cho popup chuông).
     */
    public List<Notification> getRecent(int userId, int limit) {
        return notifDAO.findRecent(userId, limit);
    }

    /**
     * Lấy danh sách thông báo chưa đọc.
     */
    public List<Notification> getUnread(int userId) {
        return notifDAO.findByUserId(userId)
                .stream()
                .filter(Notification::isUnread)
                .toList();
    }

    /**
     * Đếm số thông báo chưa đọc — dùng cho badge icon chuông.
     */
    public int countUnread(int userId) {
        return notifDAO.countUnread(userId);
    }

    // ── Đánh dấu đã đọc ──────────────────────────────────────────────────────

    /**
     * Đánh dấu một thông báo là đã đọc.
     */
    public boolean markRead(int notificationId) {
        return notifDAO.markRead(notificationId);
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * Gọi khi user mở trang thông báo.
     *
     * @return số bản ghi được cập nhật
     */
    public int markAllRead(int userId) {
        return notifDAO.markAllRead(userId);
    }

    // ── Xóa ──────────────────────────────────────────────────────────────────

    public boolean delete(int notificationId) {
        return notifDAO.delete(notificationId);
    }
}
