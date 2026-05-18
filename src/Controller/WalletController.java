package Controller;

import DAO.MissionDAO;
import DAO.NotificationDAO;
import DAO.RequestDAO;
import DAO.WalletDAO;
import Model.Deposit;
import Model.Transaction;
import Model.Wallet;
import Model.Withdraw;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WalletController — gộp DepositController + WithdrawController.
 *
 * Các method public:
 *  getBalance(), credit(), debit(), getTransactionHistory()
 *  requestDeposit()  → tạo lệnh PENDING
 *  approveDeposit()  → duyệt → credit ví → trigger mission
 *  rejectDeposit()
 *  requestWithdraw() → lock balance → tạo lệnh PENDING
 *  approveWithdraw() → deductLocked → cập nhật trạng thái
 *  rejectWithdraw()  → unlock balance
 *
 * Tất cả approve đều đảm bảo DB-level transaction (xem RequestDAO).
 */
public class WalletController {

    private final WalletDAO       walletDAO   = new WalletDAO();
    private final RequestDAO      reqDAO      = new RequestDAO();
    private final MissionDAO      missionDAO  = new MissionDAO();
    private final NotificationDAO notifDAO    = new NotificationDAO();
    private final MissionController missionCtrl = new MissionController();

    // =========================================================================
    //  Enum kết quả — dùng chung cho Deposit + Withdraw
    // =========================================================================

    public enum Result {
        SUCCESS,
        NOT_FOUND,          // deposit/withdraw không tồn tại
        ALREADY_PROCESSED,  // không còn ở PENDING
        INSUFFICIENT_FUNDS, // số dư không đủ
        DB_ERROR
    }

    // =========================================================================
    //  1. Truy vấn số dư & lịch sử
    // =========================================================================

    /** Lấy object Wallet (chứa availableBalance + lockedBalance). */
    public Wallet getBalance(int userId) {
        return walletDAO.getByUserId(userId);
    }

    /**
     * Credit thủ công (dùng nội bộ hoặc admin điều chỉnh).
     * @return transaction_id mới, hoặc -1 nếu thất bại
     */
    public int credit(int walletId, String typeCode, BigDecimal amount, String note) {
        return walletDAO.credit(walletId, typeCode, amount, note);
    }

    /**
     * Debit thủ công.
     * @return transaction_id mới, hoặc -1 nếu thất bại (kể cả không đủ số dư)
     */
    public int debit(int walletId, String typeCode, BigDecimal amount, String note) {
        return walletDAO.debit(walletId, typeCode, amount, note);
    }

    /** Toàn bộ lịch sử giao dịch của user (mới nhất trước). */
    public List<Transaction> getTransactionHistory(int userId) {
        Wallet w = walletDAO.getByUserId(userId);
        if (w == null) return List.of();
        return walletDAO.getHistory(w.getWalletId());
    }

    /** Lịch sử giao dịch theo loại (DEPOSIT / WITHDRAW / INVEST / PAYOUT …). */
    public List<Transaction> getTransactionHistory(int userId, String typeCode) {
        Wallet w = walletDAO.getByUserId(userId);
        if (w == null) return List.of();
        return walletDAO.getHistory(w.getWalletId(), typeCode);
    }

    // =========================================================================
    //  2. Deposit flow
    // =========================================================================

    /**
     * Tạo lệnh nạp tiền PENDING.
     *
     * @param userId          user nạp tiền
     * @param amount          số tiền
     * @param paymentGateway  e.g. "MOMO", "BANKING", "VNPAY"
     * @param receivingAccount tài khoản thụ hưởng
     * @param expireMinutes   thời gian hết hạn (phút), truyền 0 để không hết hạn
     * @return deposit_id mới, hoặc -1 nếu thất bại
     */
    public int requestDeposit(int userId, BigDecimal amount,
                              String paymentGateway, String receivingAccount,
                              int expireMinutes) {
        Wallet w = walletDAO.getByUserId(userId);
        if (w == null) return -1;

        // Sinh request_code ngắn gọn: DEP + timestamp
        String requestCode = "DEP" + System.currentTimeMillis();
        Timestamp expiredAt = expireMinutes > 0
            ? Timestamp.valueOf(LocalDateTime.now().plusMinutes(expireMinutes))
            : null;

        return reqDAO.createDeposit(
            w.getWalletId(), amount,
            requestCode, paymentGateway,
            receivingAccount, null,   // qrString — optional
            expiredAt
        );
    }

    /**
     * Duyệt lệnh nạp tiền.
     * Flow (đã có trong RequestDAO với JDBC transaction):
     *   1. Cập nhật TRANSACTION → COMPLETED
     *   2. Ghi bank_trans_ref vào DEPOSIT
     *   3. WalletDAO.credit() → cộng ví + ghi ledger
     *   4. Trigger mission check (gọi MissionDAO)
     *   5. Gửi thông báo
     *
     * @param depositId    ID lệnh nạp
     * @param bankTransRef mã tham chiếu ngân hàng (nullable khi thủ công)
     * @param staffNote    ghi chú của nhân viên (dùng cho notification)
     */
    public Result approveDeposit(int depositId, String bankTransRef, String staffNote) {
        Deposit d = reqDAO.findDepositById(depositId);
        if (d == null) return Result.NOT_FOUND;

        // RequestDAO.approveDeposit xử lý toàn bộ DB transaction nội tại
        boolean ok = reqDAO.approveDeposit(depositId, bankTransRef);
        if (!ok) return Result.DB_ERROR;

        // Lấy userId để trigger mission & notification
        int userId = getUserIdByDeposit(d);
        if (userId > 0) {
            // Trigger mission check (MissionController.onDeposit)
            try {
                BigDecimal amount = getAmountFromDeposit(d);
                if (amount != null && amount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    missionCtrl.onDeposit(userId, amount);
                }
            } catch (Exception e) {
                System.err.println("[WalletController] MissionController.onDeposit error: " + e.getMessage());
            }

            // Thông báo
            notifDAO.send(userId,
                "Nạp tiền thành công",
                "Lệnh nạp #" + depositId + " đã được duyệt"
                    + (staffNote != null && !staffNote.isBlank() ? " · " + staffNote : ""),
                "WALLET");
        }
        return Result.SUCCESS;
    }

    /**
     * Từ chối lệnh nạp tiền.
     * @param reason lý do từ chối (ghi vào note, gửi thông báo)
     */
    public Result rejectDeposit(int depositId, String reason) {
        Deposit d = reqDAO.findDepositById(depositId);
        if (d == null) return Result.NOT_FOUND;

        boolean ok = reqDAO.rejectDeposit(depositId, reason);
        if (!ok) return Result.DB_ERROR;

        int userId = getUserIdByDeposit(d);
        if (userId > 0) {
            notifDAO.send(userId,
                "Lệnh nạp bị từ chối",
                "Lệnh nạp #" + depositId + " bị từ chối"
                    + (reason != null && !reason.isBlank() ? ": " + reason : ""),
                "WALLET");
        }
        return Result.SUCCESS;
    }

    // =========================================================================
    //  3. Withdraw flow
    // =========================================================================

    /**
     * Tạo lệnh rút tiền PENDING + lock balance.
     *
     * @param userId        user rút
     * @param bankAccountId tài khoản ngân hàng đích
     * @param amount        số tiền rút
     * @param fee           phí rút (0 nếu miễn phí)
     * @return Result.SUCCESS hoặc INSUFFICIENT_FUNDS / DB_ERROR
     */
    public Result requestWithdraw(int userId, int bankAccountId,
                                  BigDecimal amount, BigDecimal fee) {
        Wallet w = walletDAO.getByUserId(userId);
        if (w == null) return Result.DB_ERROR;

        BigDecimal totalNeeded = amount.add(fee != null ? fee : BigDecimal.ZERO);
        if (w.getAvailableBalance().compareTo(totalNeeded) < 0)
            return Result.INSUFFICIENT_FUNDS;

        int withdrawId = reqDAO.createWithdraw(
            w.getWalletId(), bankAccountId, amount,
            fee != null ? fee : BigDecimal.ZERO);
        if (withdrawId < 0) return Result.DB_ERROR;

        notifDAO.send(userId,
            "Lệnh rút đang xử lý",
            "Lệnh rút #" + withdrawId + " đang chờ duyệt. Số tiền: " + amount.toPlainString() + " đ",
            "WALLET");

        return Result.SUCCESS;
    }

    /**
     * Duyệt lệnh rút tiền.
     * Flow (RequestDAO.approveWithdraw có JDBC transaction):
     *   1. WalletDAO.deductLocked() — trừ hẳn locked balance
     *   2. Cập nhật WITHDRAW.status = APPROVED
     *   3. Cập nhật TRANSACTION.status = COMPLETED
     */
    public Result approveWithdraw(int withdrawId, String note) {
        Withdraw w = reqDAO.findWithdrawById(withdrawId);
        if (w == null) return Result.NOT_FOUND;
        if (!"PENDING".equals(w.getStatus())) return Result.ALREADY_PROCESSED;

        boolean ok = reqDAO.approveWithdraw(withdrawId, note);
        if (!ok) return Result.DB_ERROR;

        int userId = getUserIdByWithdraw(w);
        if (userId > 0) {
            notifDAO.send(userId,
                "Rút tiền thành công",
                "Lệnh rút #" + withdrawId + " đã được duyệt. Tiền sẽ về tài khoản trong 1-3 ngày làm việc.",
                "WALLET");
        }
        return Result.SUCCESS;
    }

    /**
     * Từ chối lệnh rút tiền — giải phóng locked balance về available.
     */
    public Result rejectWithdraw(int withdrawId, String reason) {
        Withdraw w = reqDAO.findWithdrawById(withdrawId);
        if (w == null) return Result.NOT_FOUND;
        if (!"PENDING".equals(w.getStatus())) return Result.ALREADY_PROCESSED;

        boolean ok = reqDAO.rejectWithdraw(withdrawId, reason);
        if (!ok) return Result.DB_ERROR;

        int userId = getUserIdByWithdraw(w);
        if (userId > 0) {
            notifDAO.send(userId,
                "Lệnh rút bị từ chối",
                "Lệnh rút #" + withdrawId + " bị từ chối"
                    + (reason != null && !reason.isBlank() ? ": " + reason : "")
                    + ". Số tiền đã được hoàn về ví.",
                "WALLET");
        }
        return Result.SUCCESS;
    }

    // =========================================================================
    //  4. Query helpers cho Staff panels
    // =========================================================================

    /** Danh sách tất cả lệnh nạp đang PENDING (Staff duyệt). */
    public List<Deposit> getPendingDeposits() {
        return reqDAO.findPendingDeposits();
    }

    /** Danh sách tất cả lệnh rút đang PENDING (Staff duyệt). */
    public List<Withdraw> getPendingWithdraws() {
        return reqDAO.findPendingWithdraws();
    }

    /** Lệnh nạp theo user. */
    public List<Deposit> getDepositsByUser(int userId) {
        Wallet w = walletDAO.getByUserId(userId);
        return w != null ? reqDAO.findDepositsByUser(userId) : List.of();
    }

    /** Lệnh rút theo user. */
    public List<Withdraw> getWithdrawsByUser(int userId) {
        return reqDAO.findWithdrawsByUser(userId);
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    /** Lấy userId từ Deposit (qua transaction → wallet). */
    private int getUserIdByDeposit(Deposit d) {
        try {
            // TRANSACTION.wallet_id → WALLET.user_id
            Wallet w = walletDAO.getById(
                getWalletIdFromTransaction(d.getTransactionId()));
            return w != null ? w.getUserId() : -1;
        } catch (Exception e) { return -1; }
    }

    private int getUserIdByWithdraw(Withdraw w) {
        try {
            Wallet wallet = walletDAO.getById(
                getWalletIdFromTransaction(w.getTransactionId()));
            return wallet != null ? wallet.getUserId() : -1;
        } catch (Exception e) { return -1; }
    }

    /** Đọc wallet_id từ TRANSACTION (dùng WalletDAO.getTransactionById). */
    private int getWalletIdFromTransaction(int txId) {
        Transaction tx = walletDAO.getTransactionById(txId);
        return tx != null ? tx.getWalletId() : -1;
    }

    /** Lấy số tiền từ Deposit (qua transaction). */
    private java.math.BigDecimal getAmountFromDeposit(Deposit d) {
        try {
            Transaction tx = walletDAO.getTransactionById(d.getTransactionId());
            return tx != null ? tx.getAmount() : null;
        } catch (Exception e) { return null; }
    }
}
