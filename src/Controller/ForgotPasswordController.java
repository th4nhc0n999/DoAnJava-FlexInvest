package Controller;

import DAO.UserDAO;
import Model.User;
import Utils.OtpUtils;
import Utils.PasswordUtils;

/**
 * ForgotPasswordController — xử lý luồng quên mật khẩu 3 bước:
 *
 *  Bước 1: sendOtp(email)     → xác minh email tồn tại → sinh OTP → "gửi" (giả lập)
 *  Bước 2: verifyOtp(input)   → kiểm tra OTP nhập đúng không
 *  Bước 3: resetPassword(pwd) → hash + lưu mật khẩu mới
 *
 * OTP và userId được giữ trong bộ nhớ (in-memory state) của controller instance.
 * Mỗi lần mở ForgotPassword sẽ new một instance mới → state sạch.
 */
public class ForgotPasswordController {

    private final UserDAO userDAO = new UserDAO();

    /** OTP đã sinh, chờ người dùng nhập. null = chưa gửi OTP. */
    private String pendingOtp    = null;

    /** userId tìm được ở bước 1, dùng để reset ở bước 3. */
    private int    pendingUserId = -1;

    // =========================================================================
    //  Bước 1: Gửi OTP
    // =========================================================================

    /**
     * Xác minh email và sinh OTP.
     *
     * @param email email người dùng nhập
     * @return OTP vừa sinh (để View giả lập "gửi mail" bằng dialog/console),
     *         hoặc null nếu email không tồn tại / tài khoản bị khóa
     */
    public String sendOtp(String email) {
        if (email == null || email.isBlank()) return null;

        User user = userDAO.getUserByEmail(email.trim());
        if (user == null || user.getIsDeleted() == 1) return null;
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()))  return null;

        // Sinh OTP và lưu state
        pendingOtp    = OtpUtils.generate();
        pendingUserId = user.getUserId();

        // Giả lập "gửi mail" — in ra console để debug
        System.out.printf("[ForgotPassword] OTP cho %s: %s%n", email, pendingOtp);

        return pendingOtp;
    }

    // =========================================================================
    //  Bước 2: Xác minh OTP
    // =========================================================================

    /**
     * Kiểm tra OTP người dùng nhập có khớp với OTP đã sinh không.
     *
     * @param input OTP người dùng nhập (6 ký tự)
     * @return true nếu đúng
     */
    public boolean verifyOtp(String input) {
        if (pendingOtp == null) return false;
        return OtpUtils.verify(input, pendingOtp);
    }

    // =========================================================================
    //  Bước 3: Đặt lại mật khẩu
    // =========================================================================

    /**
     * Đặt mật khẩu mới cho user đã xác thực OTP.
     * Chỉ gọi sau khi verifyOtp() trả về true.
     *
     * @param newPassword mật khẩu mới (plaintext)
     * @return true nếu cập nhật DB thành công
     */
    public boolean resetPassword(String newPassword) {
        if (pendingUserId < 0 || newPassword == null || newPassword.isBlank()) return false;
        boolean ok = userDAO.updatePassword(pendingUserId, PasswordUtils.hash(newPassword));
        if (ok) {
            // Xoá state sau khi reset xong (bảo mật)
            pendingOtp    = null;
            pendingUserId = -1;
        }
        return ok;
    }

    // =========================================================================
    //  Legacy API (giữ tương thích nếu còn nơi nào gọi)
    // =========================================================================

    /** @deprecated Dùng sendOtp() thay thế. */
    @Deprecated
    public String verifyEmail(String email) {
        String otp = sendOtp(email);
        return otp != null ? String.valueOf(pendingUserId) : null;
    }

    /** @deprecated Dùng resetPassword(String) thay thế. */
    @Deprecated
    public boolean resetPassword(String userId, String newPassword) {
        try {
            int id = Integer.parseInt(userId);
            return userDAO.updatePassword(id, PasswordUtils.hash(newPassword));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
