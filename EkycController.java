package Controller;

import ConnectDB.ConnectionUtils;
import DAO.EkycDAO;
import DAO.MissionDAO;
import DAO.NotificationDAO;
import DAO.UserDAO;
import Model.Ekyc;
import Model.Mission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/** Xử lý luồng eKYC: nộp hồ sơ, duyệt, từ chối. */
public class EkycController {

    private final EkycDAO         ekycDAO    = new EkycDAO();
    private final UserDAO         userDAO    = new UserDAO();
    private final MissionDAO      missionDAO = new MissionDAO();
    private final NotificationDAO notifDAO   = new NotificationDAO();

    public enum EkycResult {
        SUCCESS,
        NOT_FOUND,         // kycId / userId không tồn tại
        ALREADY_PROCESSED, // đã APPROVED hoặc REJECTED
        DUPLICATE_PENDING, // user đã có hồ sơ đang chờ
        INVALID_DATA,      // thiếu trường bắt buộc
        DB_ERROR           // lỗi DB / rollback
    }

    // ── 1. submitEkyc — INSERT EKYC với VERIFIED_STATUS = PENDING ─────────────

    /** Nộp hồ sơ eKYC mới. Từ chối nếu user đã có hồ sơ PENDING. */
    public EkycResult submitEkyc(Ekyc ekyc) {
        if (ekyc == null || ekyc.getUserId() <= 0)                     return EkycResult.INVALID_DATA;
        if (ekyc.getIdNumber() == null || ekyc.getIdNumber().isBlank()) return EkycResult.INVALID_DATA;
        if (ekyc.getFullName() == null || ekyc.getFullName().isBlank()) return EkycResult.INVALID_DATA;

        Ekyc existing = ekycDAO.findByUserId(ekyc.getUserId());
        if (existing != null && existing.isPending()) {
            System.err.printf("[EkycController.submitEkyc] userId=%d đã có PENDING kycId=%d%n",
                    ekyc.getUserId(), existing.getKycId());
            return EkycResult.DUPLICATE_PENDING;
        }

        if (!ekycDAO.submit(ekyc)) return EkycResult.DB_ERROR;

        notifDAO.send(ekyc.getUserId(),
                "Hồ sơ eKYC đã được nộp",
                "Hồ sơ đang chờ xét duyệt, kết quả có trong 1–2 ngày làm việc.",
                "EKYC");

        return EkycResult.SUCCESS;
    }

    // ── 2. approveEkyc — atomic: EKYC + USERS trong 1 transaction ────────────

    /**
     * Duyệt eKYC (rollback-safe):
     *  Bước 1: EKYC.VERIFIED_STATUS  → APPROVED
     *  Bước 2: USERS.KYC_STATUS      → APPROVED
     */
    public EkycResult approveEkyc(int kycId) {
        Ekyc ekyc = ekycDAO.findByKycId(kycId);
        if (ekyc == null)      return EkycResult.NOT_FOUND;
        if (!ekyc.isPending()) return EkycResult.ALREADY_PROCESSED;

        int userId = ekyc.getUserId();
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Bước 1: cập nhật bảng EKYC
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE EKYC SET VERIFIED_STATUS='APPROVED', " +
                    "VERIFIED_AT=CURRENT_TIMESTAMP, UPDATED_AT=CURRENT_TIMESTAMP " +
                    "WHERE KYC_ID=? AND IS_DELETED=0")) {
                ps.setInt(1, kycId);
                if (ps.executeUpdate() == 0) { con.rollback(); return EkycResult.DB_ERROR; }
            }

            // Bước 2: cập nhật KYC_STATUS trên USERS
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE USERS SET KYC_STATUS='APPROVED' WHERE USER_ID=? AND IS_DELETED=0")) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() == 0) { con.rollback(); return EkycResult.DB_ERROR; }
            }

            con.commit();
        } catch (Exception e) {
            System.err.println("[EkycController.approveEkyc] " + e);
            rollback(con);
            return EkycResult.DB_ERROR;
        } finally {
            closeConn(con);
        }

        triggerMissionCheck(userId, "EKYC"); // fire-and-forget
        notifDAO.send(userId,
                "eKYC được duyệt thành công",
                "Tài khoản của bạn đã được xác thực danh tính đầy đủ.",
                "EKYC");

        return EkycResult.SUCCESS;
    }

    // ── 3. rejectEkyc — ghi lý do từ chối, atomic: EKYC + USERS ─────────────

    /**
     * Từ chối eKYC (rollback-safe):
     *  Bước 1: EKYC.VERIFIED_STATUS → REJECTED, NOTE = reason
     *  Bước 2: USERS.KYC_STATUS     → REJECTED
     */
    public EkycResult rejectEkyc(int kycId, String reason) {
        Ekyc ekyc = ekycDAO.findByKycId(kycId);
        if (ekyc == null)      return EkycResult.NOT_FOUND;
        if (!ekyc.isPending()) return EkycResult.ALREADY_PROCESSED;

        int userId = ekyc.getUserId();
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Bước 1: EKYC → REJECTED + ghi lý do vào NOTE
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE EKYC SET VERIFIED_STATUS='REJECTED', NOTE=?, " +
                    "VERIFIED_AT=CURRENT_TIMESTAMP, UPDATED_AT=CURRENT_TIMESTAMP " +
                    "WHERE KYC_ID=? AND IS_DELETED=0")) {
                ps.setString(1, reason);
                ps.setInt(2, kycId);
                if (ps.executeUpdate() == 0) { con.rollback(); return EkycResult.DB_ERROR; }
            }

            // Bước 2: USERS.KYC_STATUS → REJECTED
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE USERS SET KYC_STATUS='REJECTED' WHERE USER_ID=? AND IS_DELETED=0")) {
                ps.setInt(1, userId);
                if (ps.executeUpdate() == 0) { con.rollback(); return EkycResult.DB_ERROR; }
            }

            con.commit();
        } catch (Exception e) {
            System.err.println("[EkycController.rejectEkyc] " + e);
            rollback(con);
            return EkycResult.DB_ERROR;
        } finally {
            closeConn(con);
        }

        String reasonText = (reason != null && !reason.isBlank())
                ? "Lý do: " + reason : "Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.";
        notifDAO.send(userId,
                "Hồ sơ eKYC bị từ chối",
                "Hồ sơ #" + kycId + " không được chấp thuận. " + reasonText,
                "EKYC");

        return EkycResult.SUCCESS;
    }

    // ── 4 & 5. Query helpers ──────────────────────────────────────────────────

    /** Danh sách hồ sơ PENDING (dành cho admin). */
    public List<Ekyc> getPendingList() { return ekycDAO.Pending(); }

    /** Bản ghi eKYC mới nhất của user, null nếu chưa nộp. */
    public Ekyc getEkycByUser(int userId) { return ekycDAO.findByUserId(userId); }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Cập nhật tiến độ mission theo actionType sau commit (fire-and-forget). */
    private void triggerMissionCheck(int userId, String actionType) {
        try {
            for (Mission m : missionDAO.getAllActive()) {
                if (actionType.equalsIgnoreCase(m.getActionType())) {
                    if (missionDAO.updateProgress(userId, m.getMissionId(), 1))
                        System.out.printf("[EkycController] userId=%d missionId=%d (+1)%n",
                                userId, m.getMissionId());
                }
            }
        } catch (Exception e) {
            System.err.println("[EkycController.triggerMissionCheck] " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        try { return ConnectionUtils.getMyConnection(); }
        catch (ClassNotFoundException e) { throw new SQLException("JDBC driver not found", e); }
    }

    private void rollback(Connection con) {
        if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
    }

    private void closeConn(Connection con) {
        if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
    }
}
