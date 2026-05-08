package DAO;

import Model.Deposit;
import Model.Withdraw;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RequestDAO — gộp DepositDAO + WithdrawDAO.
 *
 * Cả hai đều theo pattern:
 *   create (PENDING) → approve/ reject
 *
 * Luồng Deposit:
 *   createDeposit() → ghi TRANSACTION(PENDING) + DEPOSIT
 *   approveDeposit() → cập nhật status + gọi WalletDAO.credit()
 *   rejectDeposit()  → cập nhật status (tiền không về ví)
 *
 * Luồng Withdraw:
 *   createWithdraw() → WalletDAO.lock() + ghi TRANSACTION(PENDING) + WITHDRAW
 *   approveWithdraw() → WalletDAO.deductLocked() + cập nhật status
 *   rejectWithdraw()  → WalletDAO.unlock() + cập nhật status
 */
public class RequestDAO extends BaseDAO<Deposit> {

    private final WalletDAO walletDAO = new WalletDAO();

    // =========================================================================
    // Mapping
    // =========================================================================

    @Override
    protected Deposit mapRow(ResultSet rs) throws SQLException {
        return mapDeposit(rs);
    }

    private Deposit mapDeposit(ResultSet rs) throws SQLException {
        Deposit d = new Deposit();
        d.setDepositId(rs.getInt("deposit_id"));
        d.setTransactionId(rs.getInt("transaction_id"));
        d.setRequestCode(rs.getString("request_code"));
        d.setPaymentGateway(rs.getString("payment_gateway"));
        d.setReceivingAccount(rs.getString("receiving_account"));
        d.setQrString(rs.getString("qr_string"));
        d.setBankTransRef(rs.getString("bank_trans_ref"));
        // FIX: expired_at có thể NULL — kiểm tra trước khi gọi toLocalDateTime()
        Timestamp expTs = rs.getTimestamp("expired_at");
        d.setExpiredAt(expTs != null ? expTs.toLocalDateTime() : null);
        d.setIsDeleted(rs.getInt("is_deleted"));
        return d;
    }

    private Withdraw mapWithdraw(ResultSet rs) throws SQLException {
        Withdraw w = new Withdraw();
        w.setWithdrawId(rs.getInt("withdraw_id"));
        w.setTransactionId(rs.getInt("transaction_id"));
        w.setBankAccountId(rs.getInt("bank_account_id"));
        w.setFee(rs.getBigDecimal("fee"));
        w.setStatus(rs.getString("status"));
        // FIX: created_at có thể NULL — null-safe
        Timestamp createdTs = rs.getTimestamp("created_at");
        w.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
        // FIX: processed_at được định nghĩa NULL trong schema — phải kiểm tra null
        Timestamp processedTs = rs.getTimestamp("processed_at");
        w.setProcessedAt(processedTs != null ? processedTs.toLocalDateTime() : null);
        w.setNote(rs.getString("note"));
        w.setIsDeleted(rs.getInt("is_deleted"));
        return w;
    }

    // =========================================================================
    // DEPOSIT — create
    // =========================================================================

    /**
     * Tạo lệnh nạp tiền mới (trạng thái PENDING).
     *
     * @return deposit_id mới, hoặc -1 nếu thất bại
     */
    public int createDeposit(int walletId, BigDecimal amount,
                              String requestCode, String paymentGateway,
                              String receivingAccount, String qrString,
                              Timestamp expiredAt) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // 1. Ghi TRANSACTION trạng thái PENDING
            int txId = insertTransaction(con, walletId, "DEPOSIT", amount, "PENDING");
            if (txId < 0) { con.rollback(); return -1; }

            // 2. Ghi DEPOSIT
            String sql = """
                INSERT INTO DEPOSIT
                  (transaction_id, request_code, payment_gateway,
                   receiving_account, qr_string, expired_at, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, 0)
                """;
            int depositId;
            try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"deposit_id"})) {
                ps.setInt(1, txId);
                ps.setString(2, requestCode);
                ps.setString(3, paymentGateway);
                ps.setString(4, receivingAccount);
                ps.setString(5, qrString);
                ps.setTimestamp(6, expiredAt);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) { con.rollback(); return -1; }
                    depositId = rs.getInt(1);
                }
            }

            con.commit();
            return depositId;

        } catch (Exception e) {
            System.err.println("RequestDAO.createDeposit: " + e);
            rollback(con);
            return -1;
        } finally {
            closeConn(con);
        }
    }

    // =========================================================================
    // DEPOSIT — approve / reject
    // =========================================================================

    /**
     * Duyệt nạp tiền: cập nhật TRANSACTION → COMPLETED, cộng ví, lưu bank_trans_ref.
     */
    public boolean approveDeposit(int depositId, String bankTransRef) {
        Deposit d = findDepositById(depositId);
        if (d == null) return false;

        int txId    = d.getTransactionId();
        int walletId = getWalletIdByTx(txId);
        if (walletId < 0) return false;

        BigDecimal amount = getAmountByTx(txId);
        if (amount == null) return false;

        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Cập nhật TRANSACTION
            updateTxStatus(con, txId, "COMPLETED");

            // Cập nhật DEPOSIT bank_trans_ref
            String upDep = "UPDATE DEPOSIT SET bank_trans_ref = ? WHERE deposit_id = ?";
            try (PreparedStatement ps = con.prepareStatement(upDep)) {
                ps.setString(1, bankTransRef);
                ps.setInt(2, depositId);
                ps.executeUpdate();
            }

            con.commit();

            // Cộng ví (dùng WalletDAO — tự quản lý transaction riêng)
            walletDAO.credit(walletId, "DEPOSIT", amount, "Duyệt nạp #" + depositId);

            return true;

        } catch (Exception e) {
            System.err.println("RequestDAO.approveDeposit: " + e);
            rollback(con);
            return false;
        } finally {
            closeConn(con);
        }
    }

    /** Từ chối lệnh nạp — chỉ cập nhật trạng thái, không động ví. */
    public boolean rejectDeposit(int depositId, String reason) {
        Deposit d = findDepositById(depositId);
        if (d == null) return false;
        return executeUpdate(
            "UPDATE TRANSACTION SET status = 'REJECTED' WHERE transaction_id = ?",
            d.getTransactionId()
        );
    }

    // =========================================================================
    // WITHDRAW — create
    // =========================================================================

    /**
     * Tạo lệnh rút tiền (trạng thái PENDING) + khóa số dư ví.
     *
     * @return withdraw_id mới, hoặc -1 nếu thất bại
     */
    public int createWithdraw(int walletId, int bankAccountId,
                               BigDecimal amount, BigDecimal fee) {
        // Khóa tiền trước
        BigDecimal totalLock = amount.add(fee);
        if (!walletDAO.lock(walletId, totalLock)) return -1;

        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            int txId = insertTransaction(con, walletId, "WITHDRAW", amount, "PENDING");
            if (txId < 0) {
                walletDAO.unlock(walletId, totalLock); // hoàn tiền khóa
                con.rollback();
                return -1;
            }

            String sql = """
                INSERT INTO WITHDRAW
                  (transaction_id, bank_account_id, fee, status, created_at, is_deleted)
                VALUES (?, ?, ?, 'PENDING', SYSDATE, 0)
                """;
            int withdrawId;
            try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"withdraw_id"})) {
                ps.setInt(1, txId);
                ps.setInt(2, bankAccountId);
                ps.setBigDecimal(3, fee);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        walletDAO.unlock(walletId, totalLock);
                        con.rollback();
                        return -1;
                    }
                    withdrawId = rs.getInt(1);
                }
            }

            con.commit();
            return withdrawId;

        } catch (Exception e) {
            System.err.println("RequestDAO.createWithdraw: " + e);
            walletDAO.unlock(walletId, totalLock);
            rollback(con);
            return -1;
        } finally {
            closeConn(con);
        }
    }

    // =========================================================================
    // WITHDRAW — approve / reject
    // =========================================================================

    /**
     * Duyệt rút tiền: trừ hẳn locked_balance, cập nhật WITHDRAW + TRANSACTION.
     */
    public boolean approveWithdraw(int withdrawId, String note) {
        Withdraw w = findWithdrawById(withdrawId);
        if (w == null || !"PENDING".equals(w.getStatus())) return false;

        int txId    = w.getTransactionId();
        int walletId = getWalletIdByTx(txId);
        BigDecimal amount = getAmountByTx(txId);
        BigDecimal fee    = w.getFee() != null ? w.getFee() : BigDecimal.ZERO;

        if (walletId < 0 || amount == null) return false;

        // Trừ hẳn locked
        if (!walletDAO.deductLocked(walletId, amount.add(fee))) return false;

        String sql = """
            UPDATE WITHDRAW
               SET status = 'APPROVED', processed_at = SYSDATE, note = ?
             WHERE withdraw_id = ?
            """;
        boolean ok = executeUpdate(sql, note, withdrawId);
        if (ok) executeUpdate("UPDATE TRANSACTION SET status = 'COMPLETED' WHERE transaction_id = ?", txId);
        return ok;
    }

    /**
     * Từ chối rút tiền: giải phóng locked_balance, cập nhật trạng thái.
     */
    public boolean rejectWithdraw(int withdrawId, String note) {
        Withdraw w = findWithdrawById(withdrawId);
        if (w == null || !"PENDING".equals(w.getStatus())) return false;

        int txId    = w.getTransactionId();
        int walletId = getWalletIdByTx(txId);
        BigDecimal amount = getAmountByTx(txId);
        BigDecimal fee    = w.getFee() != null ? w.getFee() : BigDecimal.ZERO;

        if (walletId < 0 || amount == null) return false;

        walletDAO.unlock(walletId, amount.add(fee));

        String sql = """
            UPDATE WITHDRAW
               SET status = 'REJECTED', processed_at = SYSDATE, note = ?
             WHERE withdraw_id = ?
            """;
        boolean ok = executeUpdate(sql, note, withdrawId);
        if (ok) executeUpdate("UPDATE TRANSACTION SET status = 'REJECTED' WHERE transaction_id = ?", txId);
        return ok;
    }

    // =========================================================================
    // Find helpers — Deposit
    // =========================================================================

    public Deposit findDepositById(int depositId) {
        return queryOne("SELECT * FROM DEPOSIT WHERE deposit_id = ? AND is_deleted = 0", depositId);
    }

    /** Tất cả lệnh nạp đang PENDING (admin xử lý). */
    public List<Deposit> findPendingDeposits() {
        String sql = """
            SELECT d.* FROM DEPOSIT d
              JOIN TRANSACTION t ON d.transaction_id = t.transaction_id
             WHERE t.status = 'PENDING' AND d.is_deleted = 0
             ORDER BY d.deposit_id DESC
            """;
        return queryList(sql);
    }

    /** Lệnh nạp của một user cụ thể. */
    public List<Deposit> findDepositsByUser(int userId) {
        String sql = """
            SELECT d.* FROM DEPOSIT d
              JOIN TRANSACTION t ON d.transaction_id = t.transaction_id
              JOIN WALLET       w ON t.wallet_id = w.wallet_id
             WHERE w.user_id = ? AND d.is_deleted = 0
             ORDER BY d.deposit_id DESC
            """;
        return queryList(sql, userId);
    }

    // =========================================================================
    // Find helpers — Withdraw
    // =========================================================================

    public Withdraw findWithdrawById(int withdrawId) {
        String sql = "SELECT * FROM WITHDRAW WHERE withdraw_id = ? AND is_deleted = 0";
        return queryWithdraw(sql, withdrawId);
    }

    /** Tất cả lệnh rút đang PENDING. */
    public List<Withdraw> findPendingWithdraws() {
        String sql = "SELECT * FROM WITHDRAW WHERE status = 'PENDING' AND is_deleted = 0 ORDER BY withdraw_id DESC";
        return queryWithdraws(sql);
    }

    /** Lệnh rút của một user cụ thể. */
    public List<Withdraw> findWithdrawsByUser(int userId) {
        String sql = """
            SELECT wr.* FROM WITHDRAW wr
              JOIN TRANSACTION t ON wr.transaction_id = t.transaction_id
              JOIN WALLET       w ON t.wallet_id = w.wallet_id
             WHERE w.user_id = ? AND wr.is_deleted = 0
             ORDER BY wr.withdraw_id DESC
            """;
        return queryWithdraws(sql, userId);
    }

    /**
     * Tìm lệnh rút / nạp liên quan đến một khoản đầu tư.
     */
    public List<Withdraw> findByInvestment(int investmentId) {
        String sql = """
            SELECT wr.* FROM WITHDRAW wr
              JOIN TRANSACTION  t  ON wr.transaction_id = t.transaction_id
              JOIN PAYOUT       p  ON p.transaction_id  = t.transaction_id
             WHERE p.investment_id = ? AND wr.is_deleted = 0
            """;
        return queryWithdraws(sql, investmentId);
    }

    // =========================================================================
    // Low-level helpers
    // =========================================================================

    /** INSERT vào TRANSACTION, trả về generated key. */
    private int insertTransaction(Connection con, int walletId,
                                   String typeCode, BigDecimal amount,
                                   String status) throws SQLException {
        String sql = """
            INSERT INTO TRANSACTION (wallet_id, type_code, amount, status, created_at, is_deleted)
            VALUES (?, ?, ?, ?, SYSDATE, 0)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"transaction_id"})) {
            ps.setInt(1, walletId);
            ps.setString(2, typeCode);
            ps.setBigDecimal(3, amount);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private void updateTxStatus(Connection con, int txId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE TRANSACTION SET status = ? WHERE transaction_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, txId);
            ps.executeUpdate();
        }
    }

    private int getWalletIdByTx(int txId) {
        Integer r = (Integer) queryScalar(
            "SELECT wallet_id FROM TRANSACTION WHERE transaction_id = ?", txId);
        return r != null ? r : -1;
    }

    private BigDecimal getAmountByTx(int txId) {
        return (BigDecimal) queryScalar(
            "SELECT amount FROM TRANSACTION WHERE transaction_id = ?", txId);
    }

    private Object queryScalar(String sql, Object param) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject(1);
            }
        } catch (Exception e) {
            System.err.println("RequestDAO.queryScalar: " + e);
        }
        return null;
    }

    private Withdraw queryWithdraw(String sql, Object... params) {
        List<Withdraw> list = queryWithdraws(sql, params);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Withdraw> queryWithdraws(String sql, Object... params) {
        List<Withdraw> list = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithdraw(rs));
            }
        } catch (Exception e) {
            System.err.println("RequestDAO.queryWithdraws: " + e);
        }
        return list;
    }

    private void rollback(Connection con) {
        if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
    }

    private void closeConn(Connection con) {
        if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
    }
}