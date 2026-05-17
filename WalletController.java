package Controller;

import ConnectDB.ConnectionUtils;
import DAO.MissionDAO;
import DAO.NotificationDAO;
import DAO.RequestDAO;
import DAO.WalletDAO;
import Model.Deposit;
import Model.Mission;
import Model.Transaction;
import Model.Wallet;
import Model.Withdraw;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Xử lý ví tiền: nạp tiền, rút tiền (tạo lệnh, duyệt, từ chối).
 * approveDeposit / approveWithdraw dùng atomic JDBC transaction — rollback toàn bộ nếu thất bại.
 */
public class WalletController {

    private final WalletDAO       walletDAO  = new WalletDAO();
    private final RequestDAO      requestDAO = new RequestDAO();
    private final MissionDAO      missionDAO = new MissionDAO();
    private final NotificationDAO notifDAO   = new NotificationDAO();

    // ── Enum kết quả ─────────────────────────────────────────────────────────

    public enum DepositResult {
        SUCCESS,
        NOT_FOUND,           // deposit_id không tồn tại
        ALREADY_PROCESSED,   // đã COMPLETED hoặc REJECTED
        INVALID_AMOUNT,      // amount <= 0 hoặc null
        DB_ERROR             // lỗi DB / rollback
    }

    public enum WithdrawResult {
        SUCCESS,
        NOT_FOUND,
        ALREADY_PROCESSED,
        INVALID_AMOUNT,
        INSUFFICIENT_BALANCE,
        DB_ERROR
    }

    // ── 1. getBalance ─────────────────────────────────────────────────────────

    /** Số dư khả dụng của user; trả về ZERO nếu ví không tồn tại. */
    public BigDecimal getBalance(int userId) {
        return walletDAO.getBalance(userId);
    }

    // ── 2. credit ────────────────────────────────────────────────────────────

    /** Cộng tiền vào ví. Trả về transaction_id mới, hoặc -1 nếu thất bại. */
    public int credit(int walletId, String typeCode, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return -1;
        return walletDAO.credit(walletId, typeCode, amount, note);
    }

    // ── 3. debit ─────────────────────────────────────────────────────────────

    /** Trừ tiền từ ví. Trả về transaction_id mới, hoặc -1 nếu thất bại / không đủ số dư. */
    public int debit(int walletId, String typeCode, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return -1;
        return walletDAO.debit(walletId, typeCode, amount, note);
    }

    // ── 4. getTransactionHistory ──────────────────────────────────────────────

    /** Lịch sử giao dịch của ví (mới nhất trước). */
    public List<Transaction> getTransactionHistory(int walletId) {
        return walletDAO.getHistory(walletId);
    }

    /** Lịch sử giao dịch lọc theo loại (VD: "DEPOSIT", "WITHDRAW"). */
    public List<Transaction> getTransactionHistory(int walletId, String typeCode) {
        return walletDAO.getHistory(walletId, typeCode);
    }

    // ── 5. requestDeposit — tạo lệnh nạp PENDING ─────────────────────────────

    /**
     * Tạo lệnh nạp tiền mới (PENDING).
     * @return deposit_id mới, hoặc -1 nếu thất bại
     */
    public int requestDeposit(int userId, BigDecimal amount,
                               String requestCode, String paymentGateway,
                               String receivingAccount, String qrString,
                               Timestamp expiredAt) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return -1;

        Wallet wallet = walletDAO.getByUserId(userId);
        if (wallet == null) {
            System.err.println("[WalletController.requestDeposit] ví không tồn tại userId=" + userId);
            return -1;
        }

        int depositId = requestDAO.createDeposit(
                wallet.getWalletId(), amount, requestCode,
                paymentGateway, receivingAccount, qrString, expiredAt);

        if (depositId > 0) {
            notifDAO.send(userId,
                    "Lệnh nạp tiền đã được tạo",
                    String.format("Yêu cầu nạp %,.0f VNĐ (ref: %s) đang chờ xử lý.",
                            amount.doubleValue(), requestCode),
                    "TRANSACTION");
        }
        return depositId;
    }

    // ── 6. approveDeposit — duyệt nạp tiền (atomic) ──────────────────────────

    /**
     * Duyệt lệnh nạp tiền (rollback-safe):
     *  Bước 1: TRANSACTION → COMPLETED
     *  Bước 2: DEPOSIT.bank_trans_ref
     *  Bước 3: WALLET.available_balance += amount
     *  Bước 4: INSERT LEDGER (credit)
     */
    public DepositResult approveDeposit(int depositId, String bankTransRef) {
        Deposit deposit = requestDAO.findDepositById(depositId);
        if (deposit == null) return DepositResult.NOT_FOUND;

        Transaction tx = walletDAO.getTransactionById(deposit.getTransactionId());
        if (tx == null)                        return DepositResult.NOT_FOUND;
        if (!"PENDING".equals(tx.getStatus())) return DepositResult.ALREADY_PROCESSED;

        int walletId    = tx.getWalletId();
        BigDecimal amount = tx.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return DepositResult.INVALID_AMOUNT;

        Wallet wallet = walletDAO.getById(walletId);
        if (wallet == null) return DepositResult.NOT_FOUND;
        int userId = wallet.getUserId();

        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Bước 1: TRANSACTION → COMPLETED
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE \"TRANSACTION\" SET status='COMPLETED' WHERE transaction_id=?")) {
                ps.setInt(1, tx.getTransactionId());
                if (ps.executeUpdate() == 0) { con.rollback(); return DepositResult.DB_ERROR; }
            }

            // Bước 2: DEPOSIT.bank_trans_ref
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE DEPOSIT SET bank_trans_ref=? WHERE deposit_id=?")) {
                ps.setString(1, bankTransRef);
                ps.setInt(2, depositId);
                ps.executeUpdate();
            }

            // Bước 3: WALLET credit
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE WALLET SET available_balance=available_balance+? " +
                    "WHERE wallet_id=? AND is_deleted=0")) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, walletId);
                if (ps.executeUpdate() == 0) { con.rollback(); return DepositResult.DB_ERROR; }
            }

            // Bước 4: LEDGER credit entry
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO LEDGER(transaction_id,wallet_id,debit,credit,balance_after,created_at,is_deleted)" +
                    " VALUES(?,?,0?,(SELECT available_balance FROM WALLET WHERE wallet_id=?),SYSDATE,0)")) {
                ps.setInt(1, tx.getTransactionId());
                ps.setInt(2, walletId);
                ps.setBigDecimal(3, amount);
                ps.setInt(4, walletId);
                if (ps.executeUpdate() == 0) { con.rollback(); return DepositResult.DB_ERROR; }
            }

            con.commit();
        } catch (Exception e) {
            System.err.println("[WalletController.approveDeposit] " + e);
            rollback(con);
            return DepositResult.DB_ERROR;
        } finally {
            closeConn(con);
        }

        triggerMissionCheck(userId, "DEPOSIT"); // fire-and-forget
        notifDAO.send(userId,
                "Nạp tiền thành công",
                String.format("Số tiền %,.0f VNĐ đã vào tài khoản (ref: %s).",
                        amount.doubleValue(), bankTransRef != null ? bankTransRef : "N/A"),
                "TRANSACTION");

        return DepositResult.SUCCESS;
    }

    // ── 7. rejectDeposit ─────────────────────────────────────────────────────

    /** Từ chối lệnh nạp — chỉ đổi trạng thái, không động ví. */
    public DepositResult rejectDeposit(int depositId, String reason) {
        Deposit deposit = requestDAO.findDepositById(depositId);
        if (deposit == null) return DepositResult.NOT_FOUND;

        Transaction tx = walletDAO.getTransactionById(deposit.getTransactionId());
        if (tx == null)                        return DepositResult.NOT_FOUND;
        if (!"PENDING".equals(tx.getStatus())) return DepositResult.ALREADY_PROCESSED;

        Wallet wallet = walletDAO.getById(tx.getWalletId());
        int userId = wallet != null ? wallet.getUserId() : -1;

        if (!requestDAO.rejectDeposit(depositId, reason)) return DepositResult.DB_ERROR;

        if (userId > 0)
            notifDAO.send(userId,
                    "Lệnh nạp tiền bị từ chối",
                    "Lệnh nạp #" + depositId + (reason != null ? ": " + reason : "."),
                    "TRANSACTION");

        return DepositResult.SUCCESS;
    }

    // ── 8. requestWithdraw — tạo lệnh rút PENDING + lock số dư ───────────────

    /**
     * Tạo lệnh rút tiền (PENDING) và khóa số dư (amount + fee).
     */
    public WithdrawResult requestWithdraw(int userId, int bankAccountId,
                                           BigDecimal amount, BigDecimal fee) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return WithdrawResult.INVALID_AMOUNT;

        BigDecimal effectiveFee = (fee != null) ? fee : BigDecimal.ZERO;
        BigDecimal totalNeeded  = amount.add(effectiveFee);

        Wallet wallet = walletDAO.getByUserId(userId);
        if (wallet == null) return WithdrawResult.DB_ERROR;

        if (wallet.getAvailableBalance() == null ||
                wallet.getAvailableBalance().compareTo(totalNeeded) < 0)
            return WithdrawResult.INSUFFICIENT_BALANCE;

        int withdrawId = requestDAO.createWithdraw(wallet.getWalletId(), bankAccountId, amount, effectiveFee);
        if (withdrawId < 0) return WithdrawResult.DB_ERROR;

        notifDAO.send(userId,
                "Lệnh rút tiền đang chờ xử lý",
                String.format("Yêu cầu rút %,.0f VNĐ (phí: %,.0f VNĐ) đang được xét duyệt.",
                        amount.doubleValue(), effectiveFee.doubleValue()),
                "TRANSACTION");

        return WithdrawResult.SUCCESS;
    }

    // ── 9. approveWithdraw — duyệt rút tiền (atomic) ─────────────────────────

    /**
     * Duyệt lệnh rút tiền (rollback-safe):
     *  Bước 1: WALLET.locked_balance -= (amount + fee)
     *  Bước 2: INSERT LEDGER (debit)
     *  Bước 3: WITHDRAW → APPROVED
     *  Bước 4: TRANSACTION → COMPLETED
     */
    public WithdrawResult approveWithdraw(int withdrawId, String note) {
        Withdraw withdraw = requestDAO.findWithdrawById(withdrawId);
        if (withdraw == null)                     return WithdrawResult.NOT_FOUND;
        if (!"PENDING".equals(withdraw.getStatus())) return WithdrawResult.ALREADY_PROCESSED;

        Transaction tx = walletDAO.getTransactionById(withdraw.getTransactionId());
        if (tx == null) return WithdrawResult.NOT_FOUND;

        int walletId      = tx.getWalletId();
        BigDecimal amount = tx.getAmount();
        BigDecimal fee    = withdraw.getFee() != null ? withdraw.getFee() : BigDecimal.ZERO;
        BigDecimal total  = amount.add(fee);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return WithdrawResult.INVALID_AMOUNT;

        Wallet wallet = walletDAO.getById(walletId);
        if (wallet == null) return WithdrawResult.NOT_FOUND;
        int userId = wallet.getUserId();

        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Bước 1: bỏ lock (kiểm tra đủ locked trước khi trừ)
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE WALLET SET locked_balance=locked_balance-? " +
                    "WHERE wallet_id=? AND locked_balance>=? AND is_deleted=0")) {
                ps.setBigDecimal(1, total);
                ps.setInt(2, walletId);
                ps.setBigDecimal(3, total);
                if (ps.executeUpdate() == 0) { con.rollback(); return WithdrawResult.INSUFFICIENT_BALANCE; }
            }

            // Bước 2: LEDGER debit entry
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO LEDGER(transaction_id,wallet_id,debit,credit,balance_after,created_at,is_deleted)" +
                    " VALUES(?,?,?,0,(SELECT available_balance FROM WALLET WHERE wallet_id=?),SYSDATE,0)")) {
                ps.setInt(1, tx.getTransactionId());
                ps.setInt(2, walletId);
                ps.setBigDecimal(3, amount);
                ps.setInt(4, walletId);
                if (ps.executeUpdate() == 0) { con.rollback(); return WithdrawResult.DB_ERROR; }
            }

            // Bước 3: WITHDRAW → APPROVED
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE WITHDRAW SET status='APPROVED', processed_at=SYSDATE, note=? WHERE withdraw_id=?")) {
                ps.setString(1, note);
                ps.setInt(2, withdrawId);
                if (ps.executeUpdate() == 0) { con.rollback(); return WithdrawResult.DB_ERROR; }
            }

            // Bước 4: TRANSACTION → COMPLETED
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE \"TRANSACTION\" SET status='COMPLETED' WHERE transaction_id=?")) {
                ps.setInt(1, tx.getTransactionId());
                if (ps.executeUpdate() == 0) { con.rollback(); return WithdrawResult.DB_ERROR; }
            }

            con.commit();
        } catch (Exception e) {
            System.err.println("[WalletController.approveWithdraw] " + e);
            rollback(con);
            return WithdrawResult.DB_ERROR;
        } finally {
            closeConn(con);
        }

        notifDAO.send(userId,
                "Lệnh rút tiền được duyệt",
                String.format("Số tiền %,.0f VNĐ đã được chuyển về tài khoản ngân hàng.", amount.doubleValue()),
                "TRANSACTION");

        return WithdrawResult.SUCCESS;
    }

    // ── 10. rejectWithdraw — từ chối + unlock số dư ──────────────────────────

    /** Từ chối lệnh rút — unlock locked_balance và cập nhật trạng thái. */
    public WithdrawResult rejectWithdraw(int withdrawId, String note) {
        Withdraw withdraw = requestDAO.findWithdrawById(withdrawId);
        if (withdraw == null)                     return WithdrawResult.NOT_FOUND;
        if (!"PENDING".equals(withdraw.getStatus())) return WithdrawResult.ALREADY_PROCESSED;

        Transaction tx = walletDAO.getTransactionById(withdraw.getTransactionId());
        if (tx == null) return WithdrawResult.NOT_FOUND;

        Wallet wallet = walletDAO.getById(tx.getWalletId());
        int userId = wallet != null ? wallet.getUserId() : -1;

        if (!requestDAO.rejectWithdraw(withdrawId, note)) return WithdrawResult.DB_ERROR;

        if (userId > 0)
            notifDAO.send(userId,
                    "Lệnh rút tiền bị từ chối",
                    "Lệnh rút #" + withdrawId + (note != null ? ": " + note : "."),
                    "TRANSACTION");

        return WithdrawResult.SUCCESS;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Cập nhật tiến độ mission theo actionType sau commit (fire-and-forget). */
    private void triggerMissionCheck(int userId, String actionType) {
        try {
            for (Mission m : missionDAO.getAllActive()) {
                if (actionType.equalsIgnoreCase(m.getActionType())) {
                    if (missionDAO.updateProgress(userId, m.getMissionId(), 1))
                        System.out.printf("[WalletController] userId=%d missionId=%d (+1)%n",
                                userId, m.getMissionId());
                }
            }
        } catch (Exception e) {
            System.err.println("[WalletController.triggerMissionCheck] " + e.getMessage());
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
