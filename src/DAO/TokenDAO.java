package DAO;

import ConnectDB.ConnectionOracle;
import Model.Token;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/**
 * DAO quản lý bảng TOKEN.
 *
 * QĐ10: Token chỉ có giá trị trong năm dương lịch hiện tại.
 *       → expireTokens() nên được gọi khi hệ thống phát hiện sang năm mới.
 *       → getBalance() trả về 0 nếu ví đang ở trạng thái EXPIRED.
 */
public class TokenDAO {

    // ── Lấy ví token theo userId ────────────────────────────────────────────

    /**
     * Lấy thông tin ví token của user.
     * @return Token object, hoặc null nếu chưa tạo ví
     */
    public Token getByUserId(int userId) {
        String sql = "SELECT token_wallet_id, user_id, balance, total_earned, " +
                     "status, updated_at, is_deleted " +
                     "FROM TOKEN WHERE user_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("[TokenDAO.getByUserId] Lỗi: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy số dư token hiện tại của user (QĐ10: trả về 0 nếu EXPIRED).
     * @return BigDecimal số dư, hoặc BigDecimal.ZERO nếu chưa có ví / đã hết hạn
     */
    public BigDecimal getBalance(int userId) {
        Token token = getByUserId(userId);
        if (token == null) return BigDecimal.ZERO;
        if ("EXPIRED".equals(token.getStatus())) return BigDecimal.ZERO;
        return token.getBalance();
    }

    // ── Tạo ví nếu chưa tồn tại ────────────────────────────────────────────

    /**
     * Tạo ví token cho user nếu chưa có.
     * Thường gọi sau khi Register thành công.
     * @return true nếu tạo thành công hoặc ví đã tồn tại
     */
    public boolean createWalletIfNotExists(int userId) {
        if (getByUserId(userId) != null) return true; // Đã tồn tại
        String sql = "INSERT INTO TOKEN (user_id, balance, total_earned, status) " +
                     "VALUES (?, 0, 0, 'ACTIVE')";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[TokenDAO.createWalletIfNotExists] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ── Cộng token ──────────────────────────────────────────────────────────

    /**
     * Cộng thêm token vào ví của user (reward từ mission, check-in...).
     * Tự tạo ví nếu chưa tồn tại.
     * @param userId  ID user
     * @param amount  Số token cộng thêm (> 0)
     * @return true nếu thành công
     */
    public boolean addToken(int userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        // Đảm bảo ví tồn tại
        createWalletIfNotExists(userId);

        String sql = "UPDATE TOKEN SET " +
                     "balance = balance + ?, " +
                     "total_earned = total_earned + ?, " +
                     "updated_at = SYSTIMESTAMP, " +
                     "status = 'ACTIVE' " +       // kích hoạt lại nếu đang EXPIRED
                     "WHERE user_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[TokenDAO.addToken] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ── Trừ token (dùng khi đổi quà / sử dụng token) ───────────────────────

    /**
     * Trừ token từ ví. Kiểm tra đủ số dư trước khi trừ.
     * @return true nếu trừ thành công, false nếu không đủ số dư
     */
    public boolean deductToken(int userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        BigDecimal current = getBalance(userId);
        if (current.compareTo(amount) < 0) return false; // Không đủ số dư

        String sql = "UPDATE TOKEN SET balance = balance - ?, updated_at = SYSTIMESTAMP " +
                     "WHERE user_id = ? AND is_deleted = 0 AND status = 'ACTIVE'";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[TokenDAO.deductToken] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ── Hết hạn token cuối năm (QĐ10) ──────────────────────────────────────

    /**
     * Zero hóa toàn bộ số dư token và chuyển status → EXPIRED.
     * Gọi khi hệ thống bước sang năm mới (31/12 → 01/01).
     *
     * QĐ10: Token chỉ có giá trị trong năm dương lịch → hết ngày 31/12 là hết hạn.
     *
     * @return số ví bị expire
     */
    public int expireTokens() {
        String sql = "UPDATE TOKEN SET balance = 0, status = 'EXPIRED', " +
                     "updated_at = SYSTIMESTAMP " +
                     "WHERE status = 'ACTIVE' AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int affected = ps.executeUpdate();
            System.out.println("[TokenDAO.expireTokens] Đã expire " + affected + " ví token.");
            return affected;
        } catch (Exception e) {
            System.err.println("[TokenDAO.expireTokens] Lỗi: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Kiểm tra xem đã đến lúc cần expire token chưa (năm mới so với updated_at).
     * Có thể gọi khi khởi động ứng dụng.
     */
    public void checkAndExpireIfNewYear() {
        int currentYear = LocalDate.now().getYear();
        String sql = "SELECT COUNT(*) FROM TOKEN " +
                     "WHERE status = 'ACTIVE' AND is_deleted = 0 " +
                     "AND EXTRACT(YEAR FROM updated_at) < ?";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentYear);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("[TokenDAO] Phát hiện token từ năm cũ → tự động expire.");
                    expireTokens();
                }
            }
        } catch (Exception e) {
            System.err.println("[TokenDAO.checkAndExpireIfNewYear] Lỗi: " + e.getMessage());
        }
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private Token mapRow(ResultSet rs) throws SQLException {
        Token t = new Token();
        t.setTokenWalletId(rs.getInt("token_wallet_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setBalance(rs.getBigDecimal("balance"));
        t.setTotalEarned(rs.getBigDecimal("total_earned"));
        t.setStatus(rs.getString("status"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        t.setIsDeleted(rs.getInt("is_deleted"));
        return t;
    }

    // ── Test nhanh ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        TokenDAO dao = new TokenDAO();
        int testUserId = 1;

        // Test tạo ví
        System.out.println("Tạo ví: " + dao.createWalletIfNotExists(testUserId));

        // Test cộng token
        System.out.println("Cộng 50 token: " + dao.addToken(testUserId, new BigDecimal("50")));

        // Test lấy số dư
        System.out.println("Số dư: " + dao.getBalance(testUserId));

        // Test trừ
        System.out.println("Trừ 20 token: " + dao.deductToken(testUserId, new BigDecimal("20")));
        System.out.println("Số dư sau trừ: " + dao.getBalance(testUserId));
    }
}
