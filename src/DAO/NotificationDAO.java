package DAO;

import ConnectDB.ConnectionOracle;
import Model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO quản lý bảng NOTIFICATION.
 *
 * Các loại type thường dùng:
 *  MISSION    → hoàn thành nhiệm vụ / nhận thưởng
 *  TRANSACTION→ biến động tài khoản (nạp / rút / đầu tư)
 *  SYSTEM     → thông báo hệ thống
 *  EKYC       → trạng thái xét duyệt KYC
 */
public class NotificationDAO {

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 1: GỬI THÔNG BÁO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gửi thông báo đến user.
     *
     * @param userId ID người nhận
     * @param title  Tiêu đề thông báo
     * @param body   Nội dung chi tiết
     * @param type   Loại thông báo (MISSION / TRANSACTION / SYSTEM / EKYC / ...)
     * @return true nếu gửi thành công
     */
    public boolean send(int userId, String title, String body, String type) {
        String sql = "INSERT INTO NOTIFICATION (user_id, title, body, type) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            // body là CLOB — setString vẫn hoạt động với text thông thường
            ps.setString(3, body);
            ps.setString(4, type);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[NotificationDAO.send] Lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi thông báo không có body (chỉ tiêu đề ngắn).
     */
    public boolean send(int userId, String title, String type) {
        return send(userId, title, null, type);
    }

    /**
     * Gửi thông báo cho toàn bộ người dùng (Broadcast).
     */
    public boolean broadcast(String title, String body, String type) {
        String sql = "INSERT INTO NOTIFICATION (user_id, title, body, type) " +
                     "SELECT user_id, ?, ?, ? FROM USERS WHERE is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, body);
            ps.setString(3, type);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[NotificationDAO.broadcast] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 2: LẤY THÔNG BÁO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Lấy tất cả thông báo của user, mới nhất trên cùng.
     * @param userId ID user
     * @return Danh sách Notification
     */
    public List<Notification> findByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT notification_id, user_id, title, " +
                     "CAST(body AS VARCHAR2(4000)) AS body, " +  // đọc CLOB an toàn
                     "type, is_read, sent_at, read_at, is_deleted " +
                     "FROM NOTIFICATION " +
                     "WHERE user_id = ? AND is_deleted = 0 " +
                     "ORDER BY sent_at DESC";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("[NotificationDAO.findByUserId] Lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy N thông báo gần nhất của user (dùng cho popup / badge).
     * @param userId ID user
     * @param limit  Số lượng tối đa
     */
    public List<Notification> findRecent(int userId, int limit) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM (" +
                     "  SELECT notification_id, user_id, title, " +
                     "  CAST(body AS VARCHAR2(4000)) AS body, " +
                     "  type, is_read, sent_at, read_at, is_deleted " +
                     "  FROM NOTIFICATION " +
                     "  WHERE user_id = ? AND is_deleted = 0 " +
                     "  ORDER BY sent_at DESC" +
                     ") WHERE ROWNUM <= ?";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("[NotificationDAO.findRecent] Lỗi: " + e.getMessage());
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 3: ĐỌC / ĐẾM CHƯA ĐỌC
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Đánh dấu một thông báo là đã đọc.
     * @return true nếu cập nhật thành công
     */
    public boolean markRead(int notificationId) {
        String sql = "UPDATE NOTIFICATION SET is_read = 1, read_at = SYSTIMESTAMP " +
                     "WHERE notification_id = ? AND is_deleted = 0 AND is_read = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[NotificationDAO.markRead] Lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * Dùng khi user mở trang thông báo.
     * @return số bản ghi được cập nhật
     */
    public int markAllRead(int userId) {
        String sql = "UPDATE NOTIFICATION SET is_read = 1, read_at = SYSTIMESTAMP " +
                     "WHERE user_id = ? AND is_read = 0 AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[NotificationDAO.markAllRead] Lỗi: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Đếm số thông báo chưa đọc của user.
     * Dùng để hiển thị badge trên icon chuông.
     * @return số lượng thông báo chưa đọc
     */
    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM NOTIFICATION " +
                     "WHERE user_id = ? AND is_read = 0 AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[NotificationDAO.countUnread] Lỗi: " + e.getMessage());
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 4: XÓA MỀM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Xóa mềm một thông báo (is_deleted = 1).
     */
    public boolean delete(int notificationId) {
        String sql = "UPDATE NOTIFICATION SET is_deleted = 1 " +
                     "WHERE notification_id = ?";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[NotificationDAO.delete] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 5: MAPPING
    // ═══════════════════════════════════════════════════════════════════════

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setType(rs.getString("type"));
        n.setIsRead(rs.getInt("is_read"));
        n.setSentAt(rs.getTimestamp("sent_at"));
        n.setReadAt(rs.getTimestamp("read_at"));
        n.setIsDeleted(rs.getInt("is_deleted"));
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 6: TEST
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        NotificationDAO dao = new NotificationDAO();
        int testUserId = 1;

        // Test 1: Gửi thông báo
        System.out.println("=== Gửi thông báo ===");
        System.out.println(dao.send(testUserId,
                "Chào mừng bạn đến với FlexInvest!",
                "Tài khoản của bạn đã được kích hoạt thành công.",
                "SYSTEM"));

        // Test 2: Đếm chưa đọc
        System.out.println("\n=== Số thông báo chưa đọc ===");
        System.out.println(dao.countUnread(testUserId));

        // Test 3: Lấy danh sách
        System.out.println("\n=== Danh sách thông báo ===");
        dao.findByUserId(testUserId).forEach(System.out::println);

        // Test 4: Đánh dấu tất cả đã đọc
        System.out.println("\n=== Mark all read ===");
        System.out.println("Đã cập nhật: " + dao.markAllRead(testUserId) + " bản ghi");
        System.out.println("Còn chưa đọc: " + dao.countUnread(testUserId));
    }
}
