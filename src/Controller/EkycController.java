package Controller;

import ConnectDB.ConnectionUtils;
import DAO.EkycDAO;
import DAO.NotificationDAO;
import DAO.UserDAO;
import Model.Ekyc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * EkycController — quản lý vòng đời hồ sơ eKYC.
 *
 * Flow:
 *   submitEkyc()   → tạo record PENDING trong EKYC
 *   approveEkyc()  → EKYC.status = APPROVED + cập nhật USERS.kyc_status = APPROVED
 *   rejectEkyc()   → EKYC.status = REJECTED + ghi lý do
 */
public class EkycController {

    private final EkycDAO         ekycDAO  = new EkycDAO();
    private final UserDAO         userDAO  = new UserDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    // =========================================================================
    //  Result enum
    // =========================================================================

    public enum Result {
        SUCCESS,
        NOT_FOUND,
        ALREADY_PROCESSED,  // đã APPROVED / REJECTED rồi
        DB_ERROR
    }

    // =========================================================================
    //  1. Submit eKYC (Customer)
    // =========================================================================

    /**
     * Nộp hồ sơ KYC — tạo record PENDING.
     * Nếu user đã có hồ sơ PENDING thì không cho nộp lại.
     *
     * @param ekyc đối tượng Ekyc đã điền đầy đủ thông tin (userId, ảnh, …)
     * @return Result.SUCCESS nếu nộp thành công
     */
    public Result submitEkyc(Ekyc ekyc) {
        // Kiểm tra đã có hồ sơ PENDING / APPROVED chưa
        Ekyc existing = ekycDAO.getLatestByUserId(ekyc.getUserId());
        if (existing != null) {
            if (existing.isPending())  return Result.ALREADY_PROCESSED;
            if (existing.isApproved()) return Result.ALREADY_PROCESSED;
            // Nếu REJECTED → cho phép nộp lại
        }

        boolean ok = ekycDAO.submit(ekyc);
        if (!ok) return Result.DB_ERROR;

        notifDAO.send(ekyc.getUserId(),
            "Hồ sơ KYC đã được gửi",
            "Chúng tôi sẽ xem xét và phê duyệt trong vòng 24h.",
            "EKYC");
        return Result.SUCCESS;
    }

    // =========================================================================
    //  2. Approve eKYC (Staff / Admin)
    // =========================================================================

    /**
     * Duyệt hồ sơ KYC:
     *  1. Cập nhật EKYC.verified_status = APPROVED
     *  2. Cập nhật USERS.kyc_status = 'APPROVED' (nếu cột tồn tại)
     *  3. Gửi thông báo cho user
     */
    public Result approveEkyc(int kycId) {
        Ekyc kyc = ekycDAO.getLatestByKycId(kycId);
        if (kyc == null) return Result.NOT_FOUND;
        if (kyc.isApproved()) return Result.ALREADY_PROCESSED;

        // Bước 1: cập nhật bảng EKYC
        boolean ok = ekycDAO.approve(kycId);
        if (!ok) return Result.DB_ERROR;

        // Bước 2: cập nhật USERS.kyc_status (best-effort — không rollback nếu thất bại)
        updateUserKycStatus(kyc.getUserId(), "APPROVED");

        // Bước 3: thông báo
        notifDAO.send(kyc.getUserId(),
            "eKYC được xác minh ✔",
            "Hồ sơ eKYC của bạn đã được phê duyệt. Bạn có thể bắt đầu đầu tư ngay!",
            "EKYC");
        return Result.SUCCESS;
    }

    // =========================================================================
    //  3. Reject eKYC (Staff / Admin)
    // =========================================================================

    /**
     * Từ chối hồ sơ KYC — ghi lý do để user sửa và nộp lại.
     */
    public Result rejectEkyc(int kycId, String reason) {
        Ekyc kyc = ekycDAO.getLatestByKycId(kycId);
        if (kyc == null) return Result.NOT_FOUND;
        if (!kyc.isPending()) return Result.ALREADY_PROCESSED;

        boolean ok = ekycDAO.reject(kycId, reason);
        if (!ok) return Result.DB_ERROR;

        // Cập nhật USERS.kyc_status nếu cần
        updateUserKycStatus(kyc.getUserId(), "REJECTED");

        notifDAO.send(kyc.getUserId(),
            "eKYC bị từ chối",
            "Hồ sơ KYC bị từ chối"
                + (reason != null && !reason.isBlank() ? ": " + reason : "")
                + ". Vui lòng kiểm tra và nộp lại.",
            "EKYC");
        return Result.SUCCESS;
    }

    // =========================================================================
    //  4. Query helpers cho StaffDashboard / EkycApprovalPanel
    // =========================================================================

    /** Tất cả hồ sơ KYC đang PENDING. */
    public List<Ekyc> getPendingKyc() {
        return ekycDAO.Pending();
    }

    /** Hồ sơ KYC mới nhất của user. */
    public Ekyc getKycByUser(int userId) {
        return ekycDAO.getLatestByUserId(userId);
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    /**
     * Cập nhật cột kyc_status trên bảng USERS.
     * Best-effort: không ném exception nếu cột không tồn tại (schema cũ).
     */
    private void updateUserKycStatus(int userId, String status) {
        String sql = "UPDATE USERS SET kyc_status = ? WHERE user_id = ?";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            // Cột kyc_status chưa có trong schema → bỏ qua, không crash
            System.err.println("[EkycController.updateUserKycStatus] " + e.getMessage());
        }
    }
}
