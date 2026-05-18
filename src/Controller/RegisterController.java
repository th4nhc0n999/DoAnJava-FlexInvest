package Controller;

import ConnectDB.ConnectionUtils;
import DAO.AccountDAO;
import DAO.UserDAO;
import DAO.WalletDAO;
import Model.Account;
import Model.User;
import Utils.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Xử lý đăng ký tài khoản mới.
 *
 * Toàn bộ quá trình (insert USERS → insert ACCOUNT → insert WALLET)
 * diễn ra trong **một transaction duy nhất**:
 * - Nếu bất kỳ bước nào thất bại → rollback toàn bộ.
 * - Chỉ commit khi cả 3 bước thành công.
 */
public class RegisterController {

    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    /**
     * Đăng ký người dùng mới.
     *
     * @return "USERNAME_EXISTS" | "EMAIL_EXISTS" | "SUCCESS" | "ERROR"
     */
    public String registerController(String username, String email,
            String password, String referralCode) {

        // ── Kiểm tra trùng trước khi mở transaction ──────────────────────────
        if (accountDAO.getByUsername(username) != null)
            return "USERNAME_EXISTS";
        if (userDAO.getUserByEmail(email) != null)
            return "EMAIL_EXISTS";

        String hash = PasswordUtils.hash(password);

        // ── Một transaction duy nhất: User + Account + Wallet ─────────────────
        Connection con = null;
        try {
            con = ConnectionUtils.getMyConnection();
            con.setAutoCommit(false); // bắt đầu transaction

            // Bước 1: Insert USERS → lấy userId được generate
            int userId = insertUser(con, email, hash,
                    referralCode.isEmpty() ? null : referralCode);
            if (userId < 0) {
                con.rollback();
                return "ERROR";
            }

            // Bước 2: Insert ACCOUNT
            boolean accountOk = insertAccount(con, userId, username, hash);
            if (!accountOk) {
                con.rollback();
                return "ERROR";
            }

            // Bước 3: Insert WALLET (số dư ban đầu = 0)
            boolean walletOk = walletDAO.insertWithConnection(con, userId);
            if (!walletOk) {
                con.rollback();
                return "ERROR";
            }

            // Tất cả thành công → commit
            con.commit();
            return "SUCCESS";

        } catch (Exception e) {
            System.err.println("RegisterController: " + e);
            try {
                if (con != null)
                    con.rollback();
            } catch (Exception ignored) {
            }
            return "ERROR";
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ── Helpers nội bộ (dùng connection sẵn có để cùng transaction) ──────────

    /** Insert USERS, trả về userId được generate hoặc -1 nếu lỗi. */
    private int insertUser(Connection con, String email, String hash,
            String referralCode) throws Exception {
        String sql = "INSERT INTO USERS (ROLE_ID, EMAIL, PASSWORD_HASH, STATUS, REFERRAL_CODE, IS_DELETED) "
                + "VALUES (3, ?, ?, 'ACTIVE', ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[] { "USER_ID" })) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ps.setString(3, referralCode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return -1;
    }

    /** Insert ACCOUNT liên kết với userId, trả về true nếu thành công. */
    private boolean insertAccount(Connection con, int userId,
            String username, String hash) throws Exception {
        String sql = "INSERT INTO ACCOUNT (USER_ID, USERNAME, PASSWORD_HASH, STATUS, CREATED_AT, UPDATED_AT, IS_DELETED) "
                + "VALUES (?, ?, ?, 'ACTIVE', SYSDATE, SYSDATE, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, hash);
            return ps.executeUpdate() > 0;
        }
    }
}
