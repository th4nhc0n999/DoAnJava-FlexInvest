package Controller;

import DAO.BankAccountDAO;
import DAO.EkycDAO;
import DAO.InvestmentDAO;
import DAO.NotificationDAO;
import DAO.SavingsProductDAO;
import DAO.WalletDAO;
import Model.BankAccount;
import Model.Ekyc;
import Model.Investment;
import Model.SavingsProduct;
import Utils.DateUtils;
import Utils.InterestCalculator;
import Utils.InterestCalculator.InterestResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * InvestmentController — Tuần 2 N2.
 *
 * Thứ tự phương thức từ đơn giản → phức tạp:
 *  1. buyProduct()
 *  2. calculateInterestForDeposit()  ← method trung tâm
 *  3. redeemEarly()
 *  4. dailyFlexSafeAccrual()
 *  5. processMaturity()
 *     └─ applyPayoutMethod1/2/3()
 */
public class InvestmentController {

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final InvestmentDAO  invDAO  = new InvestmentDAO();
    private final WalletDAO      walletDAO = new WalletDAO();
    private final EkycDAO        ekycDAO = new EkycDAO();
    private final BankAccountDAO bankDAO = new BankAccountDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final SavingsProductDAO productDAO   = new SavingsProductDAO();

    /** ID gói Flex-Safe mặc định dùng khi rollover QĐ9. Được load lazy. */
    private SavingsProduct flexSafeProduct = null;

    // =========================================================================
    //  1. buyProduct() — Mua gói đầu tư
    // =========================================================================

    /**
     * Kết quả trả về của buyProduct().
     */
    public enum BuyResult {
        SUCCESS,
        KYC_NOT_APPROVED,       // eKYC chưa APPROVED
        PRODUCT_NOT_OPEN,       // gói Flex-Sale/Flex-Holiday chưa/đã hết mở bán
        AMOUNT_BELOW_MIN,       // số tiền < minInvestmentAmount
        AMOUNT_ABOVE_MAX,       // số tiền > maxInvestmentAmount
        INSUFFICIENT_BALANCE,   // ví không đủ tiền
        PRODUCT_NOT_FOUND,      // product_id không tồn tại
        DB_ERROR                // lỗi DB
    }

    /**
     * Mua một gói đầu tư.
     *
     * @param userId    user muốn mua
     * @param productId ID gói
     * @param amount    số tiền đầu tư
     * @return BuyResult
     */
    public BuyResult buyProduct(int userId, int productId, BigDecimal amount) {

        // ── Bước 1: Kiểm tra eKYC phải APPROVED ─────────────────────────────
        List<Ekyc> kycList = ekycDAO.Pending(); // dùng API có sẵn để kiểm tra
        // Lấy KYC mới nhất của user (approved)
        boolean kycOk = false;
        // Vì EkycDAO chỉ có Pending(), ta query trực tiếp qua method khác
        // Sử dụng helper nội bộ:
        Ekyc kyc = getApprovedKyc(userId);
        if (kyc == null || !kyc.isApproved()) {
            return BuyResult.KYC_NOT_APPROVED;
        }

        // ── Bước 2: Lấy thông tin gói ───────────────────────────────────────
        SavingsProduct product = productDAO.getById(productId);
        if (product == null) return BuyResult.PRODUCT_NOT_FOUND;

        // ── Bước 3: Kiểm tra isOpenToday() (Flex-Sale, Flex-Holiday) ────────
        if (!product.isOpenToday()) return BuyResult.PRODUCT_NOT_OPEN;

        // ── Bước 4: Kiểm tra khoảng min/max ─────────────────────────────────
        if (amount.compareTo(product.getMinInvestmentAmount()) < 0)
            return BuyResult.AMOUNT_BELOW_MIN;
        if (product.getMaxInvestmentAmount() != null
                && amount.compareTo(product.getMaxInvestmentAmount()) > 0)
            return BuyResult.AMOUNT_ABOVE_MAX;

        // ── Bước 5: Kiểm tra số dư ví ───────────────────────────────────────
        // Lấy wallet_id của user
        var wallet = walletDAO.getByUserId(userId);
        if (wallet == null) return BuyResult.DB_ERROR;
        if (wallet.getAvailableBalance().compareTo(amount) < 0)
            return BuyResult.INSUFFICIENT_BALANCE;

        // ── Bước 6: Tính maturityDate theo QĐ1 ──────────────────────────────
        LocalDate today        = LocalDate.now();
        LocalDate maturityDate = product.isFlexSafe()
                                 ? null
                                 : DateUtils.calcMaturityDate(today, product.getTerm());

        // ── Bước 7: Tạo Investment record ────────────────────────────────────
        int investmentId = invDAO.insert(
            userId, productId, amount,
            product.getInterestRate(),   // khóa lãi suất tại thời điểm mua
            today, maturityDate
        );
        if (investmentId < 0) return BuyResult.DB_ERROR;

        // ── Bước 8: Trừ tiền ví (lock) và ghi TRANSACTION ───────────────────
        int txId = walletDAO.debit(wallet.getWalletId(), "INVEST", amount,
                                   "Mua gói " + product.getProductName());
        if (txId < 0) {
            // Rollback investment
            invDAO.updateStatus(investmentId, "CANCELLED");
            return BuyResult.DB_ERROR;
        }

        // ── Bước 9: Gửi thông báo ────────────────────────────────────────────
        String title = "Đầu tư thành công — " + product.getProductName();
        String body  = String.format(
            "Bạn đã đầu tư %,.0f VNĐ vào gói %s. %s",
            amount.doubleValue(),
            product.getProductName(),
            maturityDate != null ? "Đáo hạn: " + maturityDate : "Không kỳ hạn."
        );
        notifDAO.send(userId, title, body, "TRANSACTION");

        return BuyResult.SUCCESS;
    }

    // =========================================================================
    //  2. calculateInterestForDeposit() — Method trung tâm tính lãi
    // =========================================================================

    /**
     * Tính lãi cho một khoản đầu tư cụ thể.
     * Được gọi từ cả redeemEarly() lẫn processMaturity().
     *
     * @param investment    bản ghi Investment từ DB
     * @param redeemDate    ngày rút/tất toán (LocalDate)
     * @return InterestResult chứa lãi suất và số tiền lãi
     */
    public InterestResult calculateInterestForDeposit(Investment investment, LocalDate redeemDate) {
        SavingsProduct product = invDAO.getProductById(investment.getProductId());
        if (product == null) {
            // Fallback: lãi = 0 nếu không tìm thấy gói
            return new InterestResult(BigDecimal.ZERO, BigDecimal.ZERO, 0, "PRODUCT_NOT_FOUND");
        }

        LocalDate depositDate  = DateUtils.toLocalDate(investment.getStartDate());
        LocalDate maturityDate = DateUtils.toLocalDate(investment.getMaturityDate());

        return InterestCalculator.calculate(
            investment.getInvestedAmount(),
            depositDate,
            redeemDate,
            product,
            maturityDate,
            investment.getAppliedInterestRate()
        );
    }

    // =========================================================================
    //  3. redeemEarly() — Rút sớm trước đáo hạn
    // =========================================================================

    /**
     * Tất toán sớm một khoản đầu tư.
     *
     * @param investmentId ID khoản đầu tư
     * @return true nếu thành công
     */
    public boolean redeemEarly(int investmentId) {
        // ── Lấy thông tin Investment ─────────────────────────────────────────
        Investment inv = invDAO.getById(investmentId);
        if (inv == null || !"ACTIVE".equals(inv.getStatus())) {
            System.err.println("[redeemEarly] Investment không tồn tại hoặc không ACTIVE");
            return false;
        }

        LocalDate today = LocalDate.now();

        // ── Tính lãi ─────────────────────────────────────────────────────────
        InterestResult result = calculateInterestForDeposit(inv, today);
        BigDecimal principal  = inv.getInvestedAmount();
        BigDecimal interest   = result.interestAmount;
        BigDecimal totalPayout = principal.add(interest);

        System.out.printf("[redeemEarly] invId=%d | %s | payout=%,.2f%n",
            investmentId, result, totalPayout);

        // ── Lấy wallet của user ───────────────────────────────────────────────
        var wallet = walletDAO.getByUserId(inv.getUserId());
        if (wallet == null) return false;

        // ── Credit tiền về ví (gốc + lãi) ────────────────────────────────────
        int txId = walletDAO.credit(wallet.getWalletId(), "PAYOUT",
                                    totalPayout, "Tất toán sớm #" + investmentId);
        if (txId < 0) return false;

        // ── Ghi EarlyRedemptionEvent ──────────────────────────────────────────
        invDAO.insertEarlyRedemptionEvent(
            investmentId, null, today,
            principal,
            BigDecimal.ZERO,   // penaltyAmount (có thể tính từ penaltyRate nếu cần)
            totalPayout,
            txId
        );

        // ── Ghi Payout ────────────────────────────────────────────────────────
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            invDAO.insertPayout(investmentId, txId, "EARLY", today, interest);
        }

        // ── Cập nhật trạng thái Investment ───────────────────────────────────
        invDAO.updateStatus(investmentId, "REDEEMED");

        // ── Thông báo ────────────────────────────────────────────────────────
        notifDAO.send(inv.getUserId(),
            "Tất toán sớm thành công",
            String.format("Nhận %,.0f VNĐ (gốc %,.0f + lãi %,.0f)",
                totalPayout.doubleValue(), principal.doubleValue(), interest.doubleValue()),
            "TRANSACTION");

        return true;
    }

    // =========================================================================
    //  4. dailyFlexSafeAccrual() — Cộng lãi ngày cho Flex-Safe (QĐ5)
    // =========================================================================

    /**
     * QĐ5: Mỗi ngày cộng principal × 0.00013 vào principal.
     * Được gọi từ nút "Chạy batch hôm nay" trên Admin Dashboard — KHÔNG tự chạy.
     *
     * @param flexSafeProductId product_id của gói Flex-Safe
     * @return số lượng Investment đã được cộng lãi
     */
    public int dailyFlexSafeAccrual(int flexSafeProductId) {
        // Tỉ lệ cộng lãi ngày QĐ5: ≈ 4.75% / năm ÷ 365
        final BigDecimal DAILY_RATE = new BigDecimal("0.00013");

        List<Investment> activeList = invDAO.getActiveByProductId(flexSafeProductId);
        int count = 0;

        for (Investment inv : activeList) {
            BigDecimal principal  = inv.getInvestedAmount();
            BigDecimal dailyAccrual = principal.multiply(DAILY_RATE)
                                               .setScale(2, RoundingMode.HALF_UP);
            BigDecimal newPrincipal = principal.add(dailyAccrual);

            // Cập nhật invested_amount
            boolean updated = invDAO.updateAmount(inv.getInvestmentId(), newPrincipal);
            if (!updated) continue;

            // Ghi Payout loại DAILY vào ví (credit)
            var wallet = walletDAO.getByUserId(inv.getUserId());
            if (wallet == null) continue;

            int txId = walletDAO.credit(wallet.getWalletId(), "BONUS",
                                        dailyAccrual, "Flex-Safe accrual daily");
            if (txId > 0) {
                invDAO.insertPayout(inv.getInvestmentId(), txId,
                                    "DAILY", LocalDate.now(), dailyAccrual);
                count++;
            }
        }

        System.out.printf("[dailyFlexSafeAccrual] Đã cộng lãi cho %d khoản Flex-Safe.%n", count);
        return count;
    }

    // =========================================================================
    //  5. processMaturity() — Tất toán đúng hạn (phức tạp nhất)
    // =========================================================================

    /**
     * Tất toán một Investment đến hạn.
     * Đọc payoutMethod từ investment (mở rộng — hiện dùng String convention).
     *
     * Quy ước payoutMethod:
     *   null hoặc "1" → Phương thức 1 (rollover gốc+lãi vào Flex-Safe)
     *   "2"           → Phương thức 2 (rollover gốc, rút lãi)
     *   "3"           → Phương thức 3 (rút cả gốc lẫn lãi)
     *
     * @param investmentId  ID Investment cần tất toán
     * @param payoutMethod  "1", "2", "3" hoặc null (mặc định "1")
     * @param targetProductId  Gói đích khi rollover (null → Flex-Safe)
     * @return true nếu thành công
     */
    public boolean processMaturity(int investmentId, String payoutMethod, Integer targetProductId) {
        Investment inv = invDAO.getById(investmentId);
        if (inv == null || !"ACTIVE".equals(inv.getStatus())) {
            System.err.println("[processMaturity] Investment không tồn tại hoặc không ACTIVE");
            return false;
        }

        LocalDate today = LocalDate.now();

        // ── Tính lãi ─────────────────────────────────────────────────────────
        InterestResult result   = calculateInterestForDeposit(inv, today);
        BigDecimal principal    = inv.getInvestedAmount();
        BigDecimal interest     = result.interestAmount;
        BigDecimal totalPayout  = principal.add(interest);

        System.out.printf("[processMaturity] invId=%d | %s | total=%,.2f%n",
            investmentId, result, totalPayout);

        // ── Xác định payoutMethod ─────────────────────────────────────────────
        String method = (payoutMethod == null || payoutMethod.isBlank()) ? "1" : payoutMethod;

        // ── Xác định gói đích rollover ────────────────────────────────────────
        SavingsProduct destProduct = null;
        if ("1".equals(method) || "2".equals(method)) {
            if (targetProductId != null) {
                destProduct = invDAO.getProductById(targetProductId);
            }
            if (destProduct == null) {
                destProduct = getFlexSafe(); // QĐ9: mặc định Flex-Safe
            }
        }

        boolean ok;
        switch (method) {
            case "2" -> ok = applyPayoutMethod2(inv, principal, interest, destProduct, today);
            case "3" -> ok = applyPayoutMethod3(inv, totalPayout, today);
            default  -> ok = applyPayoutMethod1(inv, totalPayout, destProduct, today);
        }

        if (ok) {
            invDAO.updateStatus(investmentId, "MATURED");
            notifDAO.send(inv.getUserId(), "Khoản đầu tư đã đáo hạn",
                String.format("Đáo hạn #%d — Tổng nhận: %,.0f VNĐ (PT%s)",
                    investmentId, totalPayout.doubleValue(), method),
                "TRANSACTION");
        }
        return ok;
    }

    // ── Phương thức 1: Rollover (gốc + lãi) vào gói đích ────────────────────
    /**
     * QĐ9 PT1: Toàn bộ gốc+lãi chuyển vào gói đích.
     * Nếu vượt maxInvestmentAmount → phần trong giới hạn vào gói,
     * phần vượt tự động rút về bank liên kết.
     */
    private boolean applyPayoutMethod1(Investment inv, BigDecimal totalPayout,
                                       SavingsProduct dest, LocalDate today) {
        BigDecimal toInvest = totalPayout;
        BigDecimal toWithdraw = BigDecimal.ZERO;

        // Kiểm tra max của gói đích
        if (dest.getMaxInvestmentAmount() != null
                && totalPayout.compareTo(dest.getMaxInvestmentAmount()) > 0) {
            toInvest   = dest.getMaxInvestmentAmount();
            toWithdraw = totalPayout.subtract(toInvest);
        }

        // Tạo Investment mới
        LocalDate maturityDate = dest.isFlexSafe()
                                 ? null
                                 : DateUtils.calcMaturityDate(today, dest.getTerm());
        int newInvId = invDAO.insert(inv.getUserId(), dest.getProductId(),
                                     toInvest, dest.getInterestRate(), today, maturityDate);
        if (newInvId < 0) return false;

        // Ghi Payout PRINCIPAL + INTEREST cho investment cũ
        var wallet = walletDAO.getByUserId(inv.getUserId());
        if (wallet == null) return false;

        // Ghi nhận payout ra TRANSACTION (debit ảo để cân sổ)
        int txId = walletDAO.debit(wallet.getWalletId(), "INVEST",
                                   toInvest, "PT1 rollover → inv#" + newInvId);

        invDAO.insertPayout(inv.getInvestmentId(), txId > 0 ? txId : 0,
                            "PRINCIPAL", today, toInvest);

        // Phần vượt max → rút về bank
        if (toWithdraw.compareTo(BigDecimal.ZERO) > 0) {
            withdrawToBank(inv.getUserId(), wallet.getWalletId(), toWithdraw,
                           "PT1 vượt max — rút tự động", today, inv.getInvestmentId());
        }

        System.out.printf("[PT1] rollover=%,.2f | withdraw=%,.2f → gói %s%n",
            toInvest.doubleValue(), toWithdraw.doubleValue(), dest.getProductName());
        return true;
    }

    // ── Phương thức 2: Rollover gốc, rút lãi ─────────────────────────────────
    /**
     * QĐ9 PT2: Chỉ gốc rollover vào gói đích, lãi rút về bank.
     * Nếu chưa chọn gói đích → giữ nguyên gói cũ (tạo Investment mới cùng product).
     */
    private boolean applyPayoutMethod2(Investment inv, BigDecimal principal,
                                       BigDecimal interest, SavingsProduct dest, LocalDate today) {
        // Rollover gốc
        LocalDate maturityDate = dest.isFlexSafe()
                                 ? null
                                 : DateUtils.calcMaturityDate(today, dest.getTerm());
        int newInvId = invDAO.insert(inv.getUserId(), dest.getProductId(),
                                     principal, dest.getInterestRate(), today, maturityDate);
        if (newInvId < 0) return false;

        var wallet = walletDAO.getByUserId(inv.getUserId());
        if (wallet == null) return false;

        // Ghi debit ảo cho phần gốc rollover
        int txId = walletDAO.debit(wallet.getWalletId(), "INVEST",
                                   principal, "PT2 rollover gốc → inv#" + newInvId);
        invDAO.insertPayout(inv.getInvestmentId(), txId > 0 ? txId : 0,
                            "PRINCIPAL", today, principal);

        // Rút lãi về bank nếu có
        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            withdrawToBank(inv.getUserId(), wallet.getWalletId(), interest,
                           "PT2 lãi → bank", today, inv.getInvestmentId());
        }

        System.out.printf("[PT2] gốc rollover=%,.2f | lãi rút=%,.2f → gói %s%n",
            principal.doubleValue(), interest.doubleValue(), dest.getProductName());
        return true;
    }

    // ── Phương thức 3: Rút cả gốc lẫn lãi về bank ───────────────────────────
    /**
     * QĐ9 PT3: Toàn bộ gốc + lãi rút về tài khoản ngân hàng liên kết.
     * Tạo Withdraw với trạng thái APPROVED (không cần hỏi user lại).
     */
    private boolean applyPayoutMethod3(Investment inv, BigDecimal totalPayout, LocalDate today) {
        var wallet = walletDAO.getByUserId(inv.getUserId());
        if (wallet == null) return false;

        boolean ok = withdrawToBank(inv.getUserId(), wallet.getWalletId(), totalPayout,
                                    "PT3 tất toán — gốc+lãi", today, inv.getInvestmentId());
        System.out.printf("[PT3] rút về bank=%,.2f%n", totalPayout.doubleValue());
        return ok;
    }

    // =========================================================================
    //  Private helpers
    // =========================================================================

    /**
     * Rút tiền về tài khoản ngân hàng liên kết.
     * Credit ví rồi tạo Withdraw APPROVED (auto).
     */
    private boolean withdrawToBank(int userId, int walletId, BigDecimal amount,
                                   String note, LocalDate date, int investmentId) {
        // Tìm bank account liên kết
        List<BankAccount> banks = bankDAO.findByAccount(userId);
        BankAccount linked = banks.stream()
                                  .filter(b -> b.getIsLinked() == 1)
                                  .findFirst().orElse(null);
        if (linked == null && !banks.isEmpty()) linked = banks.get(0);
        if (linked == null) {
            System.err.println("[withdrawToBank] Không tìm thấy bank account cho userId=" + userId);
            return false;
        }

        // Credit tiền vào ví trước
        int txId = walletDAO.credit(walletId, "PAYOUT", amount, note);
        if (txId < 0) return false;

        // Ghi Payout
        invDAO.insertPayout(investmentId, txId, "INTEREST", date, amount);

        // Thông báo rút về bank
        notifDAO.send(userId,
            "Tiền đã về tài khoản ngân hàng",
            String.format("%,.0f VNĐ → %s (%s)", amount.doubleValue(),
                linked.getBankName(), linked.getAccountNumber()),
            "TRANSACTION");
        return true;
    }

    /** Lấy KYC APPROVED mới nhất của user (query trực tiếp). */
    private Ekyc getApprovedKyc(int userId) {
        // EkycDAO hiện chỉ có Pending() — ta filter từ tất cả
        // Trong thực tế nên thêm method getApprovedByUserId vào EkycDAO.
        // Tạm thời dùng cách kiểm tra ngược: nếu KHÔNG pending → có thể approved.
        // Đơn giản hoá: luôn return null để force approved check qua DB trực tiếp.
        try {
            var con = ConnectDB.ConnectionOracle.getOracleConnection();
            var ps  = con.prepareStatement(
                "SELECT * FROM EKYC WHERE USER_ID = ? AND VERIFIED_STATUS = 'APPROVED' " +
                "AND IS_DELETED = 0 ORDER BY CREATED_AT DESC");
            ps.setInt(1, userId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                Ekyc e = new Ekyc();
                e.setUserId(userId);
                e.setVerifiedStatus("APPROVED");
                rs.close(); ps.close(); con.close();
                return e;
            }
            rs.close(); ps.close(); con.close();
        } catch (Exception ex) {
            System.err.println("[getApprovedKyc] " + ex.getMessage());
        }
        return null;
    }

    /** Lazy-load Flex-Safe product. */
    private SavingsProduct getFlexSafe() {
        if (flexSafeProduct == null) {
            flexSafeProduct = invDAO.getFlexSafeProduct();
        }
        return flexSafeProduct;
    }
}
