package DAO;

import ConnectDB.ConnectionUtils;
import Model.Wallet;
import java.sql.*;

/**
 * DAO cho bảng WALLET.
 * Thường được gọi trong cùng transaction với UserDAO + AccountDAO khi đăng ký.
 */
public class WalletDAO {

    private Wallet mapRow(ResultSet rs) throws SQLException {
        return new Wallet(
            rs.getInt("WALLET_ID"),
            rs.getInt("USER_ID"),
            rs.getInt("AVAILABLE_BALANCE"),
            rs.getInt("LOCKED_BALANCE"),
            rs.getString("STATUS"),
            rs.getInt("IS_DELETED")
        );
    }

    /** Lấy ví theo userId (trả về null nếu không tìm thấy). */
    public Wallet getByUserId(int userId) {
        String sql = "SELECT * FROM WALLET WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("WalletDAO.getByUserId: " + e);
        }
        return null;
    }

    /**
     * Insert ví mới trong connection/transaction đã có sẵn.
     * Dùng khi đăng ký (cùng transaction với User + Account).
     *
     * @param con    Connection đang trong transaction (autoCommit=false)
     * @param userId ID của user vừa được insert
     * @return true nếu thành công
     */
    public boolean insertWithConnection(Connection con, int userId) throws SQLException {
        String sql = "INSERT INTO WALLET (USER_ID, AVAILABLE_BALANCE, LOCKED_BALANCE, STATUS, IS_DELETED) "
                   + "VALUES (?, 0, 0, 'ACTIVE', 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Cập nhật số dư khả dụng (dùng khi nạp tiền / rút tiền). */
    public boolean updateBalance(int userId, int newAvailable, int newLocked) {
        String sql = "UPDATE WALLET SET AVAILABLE_BALANCE = ?, LOCKED_BALANCE = ? "
                   + "WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newAvailable);
            ps.setInt(2, newLocked);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("WalletDAO.updateBalance: " + e);
            return false;
        }
    }
}
