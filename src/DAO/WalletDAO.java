package DAO;

// import ConnectDB.ConnectionUtils;
import Model.Wallet;
import java.sql.*;
import Model.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng WALLET.
 * Thường được gọi trong cùng transaction với UserDAO + AccountDAO khi đăng ký.
 */
// public class WalletDAO {

//     private Wallet mapRow(ResultSet rs) throws SQLException {
//         return new Wallet(
//             rs.getInt("WALLET_ID"),
//             rs.getInt("USER_ID"),
//             rs.getInt("AVAILABLE_BALANCE"),
//             rs.getInt("LOCKED_BALANCE"),
//             rs.getString("STATUS"),
//             rs.getInt("IS_DELETED")
//         );
//     }

//     /** Lấy ví theo userId (trả về null nếu không tìm thấy). */
//     public Wallet getByUserId(int userId) {
//         String sql = "SELECT * FROM WALLET WHERE USER_ID = ? AND IS_DELETED = 0";
//         try (Connection con = ConnectionUtils.getMyConnection();
//              PreparedStatement ps = con.prepareStatement(sql)) {
//             ps.setInt(1, userId);
//             try (ResultSet rs = ps.executeQuery()) {
//                 if (rs.next()) return mapRow(rs);
//             }
//         } catch (Exception e) {
//             System.err.println("WalletDAO.getByUserId: " + e);
//         }
//         return null;
//     }

//     /**
//      * Insert ví mới trong connection/transaction đã có sẵn.
//      * Dùng khi đăng ký (cùng transaction với User + Account).
//      *
//      * @param con    Connection đang trong transaction (autoCommit=false)
//      * @param userId ID của user vừa được insert
//      * @return true nếu thành công
//      */
//     public boolean insertWithConnection(Connection con, int userId) throws SQLException {
//         String sql = "INSERT INTO WALLET (USER_ID, AVAILABLE_BALANCE, LOCKED_BALANCE, STATUS, IS_DELETED) "
//                    + "VALUES (?, 0, 0, 'ACTIVE', 0)";
//         try (PreparedStatement ps = con.prepareStatement(sql)) {
//             ps.setInt(1, userId);
//             return ps.executeUpdate() > 0;
//         }
//     }

//     /** Cập nhật số dư khả dụng (dùng khi nạp tiền / rút tiền). */
//     public boolean updateBalance(int userId, int newAvailable, int newLocked) {
//         String sql = "UPDATE WALLET SET AVAILABLE_BALANCE = ?, LOCKED_BALANCE = ? "
//                    + "WHERE USER_ID = ? AND IS_DELETED = 0";
//         try (Connection con = ConnectionUtils.getMyConnection();
//              PreparedStatement ps = con.prepareStatement(sql)) {
//             ps.setInt(1, newAvailable);
//             ps.setInt(2, newLocked);
//             ps.setInt(3, userId);
//             return ps.executeUpdate() > 0;
//         } catch (Exception e) {
//             System.err.println("WalletDAO.updateBalance: " + e);
//             return false;
//         }
//     }
// }


// Su dung BaseDAO 

/**
 * WalletDAO — quản lý ví + ghi nhận giao dịch (gộp TransactionDAO).
 *
 * Mỗi thao tác credit/debit đều ghi sổ cái (LEDGER) và TRANSACTION
 * trong cùng một transaction JDBC để đảm bảo tính nhất quán.
 */
public class WalletDAO extends BaseDAO<Wallet> {

    // =========================================================================
    // Mapping
    // =========================================================================

    @Override
    protected Wallet mapRow(ResultSet rs) throws SQLException {
        Wallet w = new Wallet();
        w.setWalletId(rs.getInt("wallet_id"));
        w.setUserId(rs.getInt("user_id"));
        // FIX: dùng getBigDecimal để khớp với NUMBER(18,2) trong Oracle
        w.setAvailableBalance(rs.getBigDecimal("available_balance"));
        w.setLockedBalance(rs.getBigDecimal("locked_balance"));
        w.setStatus(rs.getString("status"));
        w.setIsDeleted(rs.getInt("is_deleted"));
        return w;
    }

    private Transaction mapTransaction(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setWalletId(rs.getInt("wallet_id"));
        t.setTypeCode(rs.getString("type_code"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setStatus(rs.getString("status"));
        t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        t.setIsDeleted(rs.getInt("is_deleted"));
        return t;
    }

    // =========================================================================
    // Wallet queries
    // =========================================================================

    /** Lấy ví theo user_id (active). */
    public Wallet getByUserId(int userId) {
        return queryOne(
            "SELECT * FROM WALLET WHERE user_id = ? AND is_deleted = 0",
            userId
        );
    }

    /** Lấy ví theo wallet_id. */
    public Wallet getById(int walletId) {
        return queryOne(
            "SELECT * FROM WALLET WHERE wallet_id = ? AND is_deleted = 0",
            walletId
        );
    }

    /** Số dư khả dụng dưới dạng BigDecimal (khớp NUMBER(18,2)). */
    public BigDecimal getBalance(int userId) {
        Wallet w = getByUserId(userId);
        return (w != null && w.getAvailableBalance() != null)
               ? w.getAvailableBalance() : BigDecimal.ZERO;
    }

    public boolean insertWithConnection(Connection con, int userId) throws SQLException {
        String sql = "INSERT INTO WALLET (USER_ID, AVAILABLE_BALANCE, LOCKED_BALANCE, STATUS, IS_DELETED) "
                   + "VALUES (?, 0, 0, 'ACTIVE', 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================================
    // Credit / Debit  (tự quản lý Connection để dùng transaction JDBC)
    // =========================================================================

    /**
     * Cộng tiền vào ví.
     *
     * @param walletId  ví nhận tiền
     * @param typeCode  mã loại giao dịch (e.g. "DEPOSIT")
     * @param amount    số tiền dương
     * @param note      ghi chú (nullable)
     * @return transaction_id mới, hoặc -1 nếu thất bại
     */
    public int credit(int walletId, String typeCode, BigDecimal amount, String note) {
        return applyBalance(walletId, typeCode, amount, note, true);
    }

    /**
     * Trừ tiền từ ví.
     *
     * @return transaction_id mới, hoặc -1 nếu thất bại (bao gồm không đủ số dư)
     */
    public int debit(int walletId, String typeCode, BigDecimal amount, String note) {
        return applyBalance(walletId, typeCode, amount, note, false);
    }

    /**
     * Khóa một khoản tiền (chuyển available → locked).
     * Dùng khi tạo lệnh đầu tư / rút tiền đang chờ duyệt.
     */
    public boolean lock(int walletId, BigDecimal amount) {
        String sql = """
            UPDATE WALLET
               SET available_balance = available_balance - ?,
                   locked_balance    = locked_balance    + ?
             WHERE wallet_id = ?
               AND available_balance >= ?
               AND is_deleted = 0
            """;
        return executeUpdate(sql, amount, amount, walletId, amount);
    }

    /**
     * Giải phóng tiền bị khóa (locked → available).
     */
    public boolean unlock(int walletId, BigDecimal amount) {
        String sql = """
            UPDATE WALLET
               SET locked_balance    = locked_balance    - ?,
                   available_balance = available_balance + ?
             WHERE wallet_id = ?
               AND locked_balance >= ?
               AND is_deleted = 0
            """;
        return executeUpdate(sql, amount, amount, walletId, amount);
    }

    /**
     * Trừ hẳn tiền đang khóa (khi đầu tư được duyệt).
     */
    public boolean deductLocked(int walletId, BigDecimal amount) {
        String sql = """
            UPDATE WALLET
               SET locked_balance = locked_balance - ?
             WHERE wallet_id = ?
               AND locked_balance >= ?
               AND is_deleted = 0
            """;
        return executeUpdate(sql, amount, walletId, amount);
    }

    // =========================================================================
    // Transaction history
    // =========================================================================

    /** Toàn bộ lịch sử giao dịch của ví, sắp xếp mới nhất trước. */
    public List<Transaction> getHistory(int walletId) {
        String sql = """
            SELECT * FROM TRANSACTION
             WHERE wallet_id = ? AND is_deleted = 0
             ORDER BY created_at DESC
            """;
        return queryTransactions(sql, walletId);
    }

    /** Lọc lịch sử theo loại giao dịch. */
    public List<Transaction> getHistory(int walletId, String typeCode) {
        String sql = """
            SELECT * FROM TRANSACTION
             WHERE wallet_id = ? AND type_code = ? AND is_deleted = 0
             ORDER BY created_at DESC
            """;
        return queryTransactions(sql, walletId, typeCode);
    }

    /** Lấy một giao dịch theo ID. */
    public Transaction getTransactionById(int transactionId) {
        String sql = "SELECT * FROM TRANSACTION WHERE transaction_id = ? AND is_deleted = 0";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapTransaction(rs);
            }
        } catch (Exception e) {
            System.err.println("WalletDAO.getTransactionById: " + e);
        }
        return null;
    }

    /**
     * Ghi nhận một giao dịch có sẵn (chỉ INSERT vào TRANSACTION, không động ví).
     * Dùng cho các luồng phức tạp mà caller tự quản lý Connection/transaction.
     */
    public int recordTransaction(int walletId, String typeCode,
                                 BigDecimal amount, String status) {
        String sql = """
            INSERT INTO TRANSACTION (wallet_id, type_code, amount, status, created_at, is_deleted)
            VALUES (?, ?, ?, ?, SYSDATE, 0)
            """;
        return executeInsertGetId(sql, new String[]{"transaction_id"},
                                  walletId, typeCode, amount, status);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Core credit/debit — thực hiện trong một JDBC transaction:
     *  1. Cập nhật WALLET balance
     *  2. INSERT TRANSACTION
     *  3. INSERT LEDGER
     */
    private int applyBalance(int walletId, String typeCode,
                              BigDecimal amount, String note, boolean isCredit) {
        String updateWallet = isCredit
            ? "UPDATE WALLET SET available_balance = available_balance + ? WHERE wallet_id = ? AND is_deleted = 0"
            : "UPDATE WALLET SET available_balance = available_balance - ? WHERE wallet_id = ? AND available_balance >= ? AND is_deleted = 0";

        String insertTx = """
            INSERT INTO TRANSACTION (wallet_id, type_code, amount, status, created_at, is_deleted)
            VALUES (?, ?, ?, 'COMPLETED', SYSDATE, 0)
            """;

        String insertLedger = """
            INSERT INTO LEDGER (transaction_id, wallet_id, debit, credit, balance_after, created_at, is_deleted)
            VALUES (?, ?, ?, ?, (SELECT available_balance FROM WALLET WHERE wallet_id = ?), SYSDATE, 0)
            """;

        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // 1. Update wallet
            try (PreparedStatement ps = con.prepareStatement(updateWallet)) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, walletId);
                if (!isCredit) ps.setBigDecimal(3, amount);   // kiểm tra đủ số dư
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    con.rollback();
                    return -1; // không đủ số dư hoặc ví không tồn tại
                }
            }

            // 2. Insert transaction
            int txId;
            try (PreparedStatement ps = con.prepareStatement(insertTx,
                                                              new String[]{"transaction_id"})) {
                ps.setInt(1, walletId);
                ps.setString(2, typeCode);
                ps.setBigDecimal(3, amount);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) { con.rollback(); return -1; }
                    txId = rs.getInt(1);
                }
            }

            // 3. Insert ledger
            try (PreparedStatement ps = con.prepareStatement(insertLedger)) {
                ps.setInt(1, txId);
                ps.setInt(2, walletId);
                ps.setBigDecimal(3, isCredit ? BigDecimal.ZERO : amount); // debit
                ps.setBigDecimal(4, isCredit ? amount : BigDecimal.ZERO); // credit
                ps.setInt(5, walletId);
                ps.executeUpdate();
            }

            con.commit();
            return txId;

        } catch (Exception e) {
            System.err.println("WalletDAO.applyBalance: " + e);
            if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
            return -1;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
        }
    }

    /** Helper để query danh sách Transaction (mapRow của BaseDAO ánh xạ Wallet, nên cần riêng). */
    private List<Transaction> queryTransactions(String sql, Object... params) {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapTransaction(rs));
            }
        } catch (Exception e) {
            System.err.println("WalletDAO.queryTransactions: " + e);
        }
        return list;
    }


    public Wallet getWalletByUserId(int userId) {
        String sql = "SELECT wallet_id, user_id, available_balance, locked_balance, status, is_deleted "
                   + "FROM WALLET WHERE user_id = ? AND is_deleted = 0";
        
        return queryOne(sql, userId);
    }

    /**
     * Deducts amount from available_balance and adds to locked_balance.
     * Used when creating an investment (lock funds).
     */
    public boolean lockFunds(int userId, BigDecimal amount) {
        String sql = "UPDATE WALLET SET available_balance = available_balance - ?, "
                   + "locked_balance = locked_balance + ? "
                   + "WHERE user_id = ? AND available_balance >= ? AND is_deleted = 0";
        
        return executeUpdate(sql, amount, amount, userId, amount);
    }

    /**
     * Returns locked funds to available (used on early redemption payout).
     */
    public boolean unlockAndCreditFunds(int userId, BigDecimal lockedToRelease, BigDecimal creditAmount) {
        String sql = "UPDATE WALLET SET locked_balance = locked_balance - ?, "
                   + "available_balance = available_balance + ? "
                   + "WHERE user_id = ? AND is_deleted = 0";
        
        return executeUpdate(sql, lockedToRelease, creditAmount, userId);
    }
    
}