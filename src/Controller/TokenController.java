package Controller;

import DAO.InvestmentDAO;
import DAO.NotificationDAO;
import DAO.RequestDAO;
import DAO.TokenDAO;
import DAO.WalletDAO;
import Model.SavingsProduct;
import Model.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TokenController — Quản lý FlexToken theo QĐ10, QĐ11, QĐ12.
 *
 * QĐ10: Token chỉ hợp lệ trong năm hiện tại (TokenDAO.checkAndExpireIfNewYear).
 * QĐ11-PT1: convertToVND()  — 1 Token = 10 VNĐ, tạo Withdraw APPROVED tự động.
 * QĐ11-PT2: convertToVipProduct() — đổi token mua gói VIP, mỗi lần 1 gói.
 * QĐ12: Gói VIP sau khi mua là tài sản độc lập, không bị expire theo token.
 */
public class TokenController {

    private static final BigDecimal TOKEN_TO_VND_RATE = BigDecimal.TEN; // 1 Token = 10 VNĐ

    private final TokenDAO          tokenDAO     = new TokenDAO();
    private final WalletDAO         walletDAO    = new WalletDAO();
    private final RequestDAO        reqDAO       = new RequestDAO();
    private final InvestmentDAO     investDAO    = new InvestmentDAO();
    private final NotificationDAO   notifDAO     = new NotificationDAO();
    private final InvestmentController investCtrl = new InvestmentController();

    // =========================================================================
    //  Enum kết quả
    // =========================================================================

    public enum Result {
        SUCCESS,
        INSUFFICIENT_TOKEN,
        PRODUCT_NOT_FOUND,
        PRODUCT_NOT_OPEN,
        MAX_AMOUNT_EXCEEDED,
        DB_ERROR
    }

    // =========================================================================
    //  1. getBalance() — Số dư token năm hiện tại
    // =========================================================================

    /**
     * Lấy số dư FlexToken hiện tại của user.
     * Tự động expire nếu sang năm mới (QĐ10).
     *
     * @return số dư ≥ 0 (BigDecimal.ZERO nếu hết hạn hoặc chưa có ví)
     */
    public BigDecimal getBalance(int userId) {
        tokenDAO.checkAndExpireIfNewYear(); // QĐ10: kiểm tra expire đầu năm
        return tokenDAO.getBalance(userId);
    }

    // =========================================================================
    //  2. convertToVND() — QĐ11-PT1: Token → VNĐ
    // =========================================================================

    /**
     * Chuyển đổi toàn bộ (hoặc một phần) token sang VNĐ.
     * Tỉ lệ: 1 Token = 10 VNĐ (QĐ11).
     *
     * Flow:
     *  1. Kiểm tra số dư token
     *  2. Trừ token
     *  3. Tính VNĐ = tokenAmount × 10
     *  4. Credit thẳng vào ví (internal transfer — không cần Staff duyệt)
     *  5. Gửi thông báo
     *
     * @param userId      ID user
     * @param tokenAmount Số token muốn đổi (null = đổi toàn bộ)
     * @return Result.SUCCESS hoặc lý do thất bại
     */
    public Result convertToVND(int userId, BigDecimal tokenAmount) {
        tokenDAO.checkAndExpireIfNewYear();

        BigDecimal balance = tokenDAO.getBalance(userId);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) return Result.INSUFFICIENT_TOKEN;

        // Nếu null → đổi toàn bộ
        BigDecimal amount = (tokenAmount == null || tokenAmount.compareTo(balance) > 0)
            ? balance : tokenAmount;

        if (amount.compareTo(BigDecimal.ZERO) <= 0) return Result.INSUFFICIENT_TOKEN;

        // 1. Trừ token
        boolean deducted = tokenDAO.deductToken(userId, amount);
        if (!deducted) return Result.INSUFFICIENT_TOKEN;

        // 2. Tính VNĐ
        BigDecimal vndAmount = amount.multiply(TOKEN_TO_VND_RATE).setScale(0, RoundingMode.HALF_UP);

        // 3. Credit vào ví (nội bộ — không qua Staff duyệt, QĐ11-PT1)
        Wallet w = walletDAO.getByUserId(userId);
        if (w == null) {
            // Rollback token nếu không tìm được ví
            tokenDAO.addToken(userId, amount);
            return Result.DB_ERROR;
        }

        int txId = walletDAO.credit(w.getWalletId(), "BONUS",
            vndAmount, "Đổi " + amount.toPlainString() + " Token → VNĐ (QĐ11-PT1)");
        if (txId < 0) {
            // Rollback token
            tokenDAO.addToken(userId, amount);
            return Result.DB_ERROR;
        }

        // 4. Thông báo
        notifDAO.send(userId,
            "Đổi Token thành công",
            String.format("Bạn đã đổi %s Token → %s VNĐ vào ví.",
                amount.toPlainString(), vndAmount.toPlainString()),
            "TRANSACTION");

        System.out.printf("[TokenController.convertToVND] userId=%d: %s Token → %s VNĐ (txId=%d)%n",
            userId, amount, vndAmount, txId);
        return Result.SUCCESS;
    }

    // =========================================================================
    //  3. convertToVipProduct() — QĐ11-PT2: Token → Gói VIP
    // =========================================================================

    /**
     * Dùng token để mua một gói VIP (QĐ11-PT2).
     *
     * Điều kiện:
     *  - Gói phải tồn tại và đang ACTIVE
     *  - Gói phải đang mở bán hôm nay (isOpenToday())
     *  - Số token phải đủ theo giá gói (tokenCost)
     *  - Mỗi lần chỉ mua được 1 gói (QĐ11)
     *  - Gói sau khi mua là tài sản độc lập (QĐ12) — không bị expire
     *
     * @param userId    ID user
     * @param productId Gói VIP muốn mua
     * @param tokenCost Số token cần trả cho gói này (bộ phận Admin cấu hình)
     * @param vndAmount Số VNĐ đầu tư tương đương (minInvestmentAmount của gói)
     * @return Result
     */
    public Result convertToVipProduct(int userId, int productId,
                                      BigDecimal tokenCost, BigDecimal vndAmount) {
        tokenDAO.checkAndExpireIfNewYear();

        // 1. Kiểm tra gói VIP
        SavingsProduct product = investDAO.getProductById(productId);
        if (product == null) return Result.PRODUCT_NOT_FOUND;
        if (!product.isOpenToday()) return Result.PRODUCT_NOT_OPEN;

        // 2. Kiểm tra số dư token
        BigDecimal balance = tokenDAO.getBalance(userId);
        if (balance.compareTo(tokenCost) < 0) return Result.INSUFFICIENT_TOKEN;

        // 3. Trừ token trước
        boolean deducted = tokenDAO.deductToken(userId, tokenCost);
        if (!deducted) return Result.INSUFFICIENT_TOKEN;

        // 4. Tạo Investment với amount = vndAmount
        //    Dùng InvestmentController.buyProduct() — gói VIP là tài sản độc lập (QĐ12)
        //    Không cần nạp tiền thật — ghi trực tiếp vào Investment với applied_rate của gói
        int investmentId = investDAO.insert(
            userId, productId, vndAmount,
            product.getInterestRate(),
            java.time.LocalDate.now(),
            product.getTerm() > 0
                ? java.time.LocalDate.now().plusDays(product.getTerm())
                : null
        );

        if (investmentId < 0) {
            // Rollback token
            tokenDAO.addToken(userId, tokenCost);
            return Result.DB_ERROR;
        }

        // 5. Thông báo
        notifDAO.send(userId,
            "Mua gói VIP bằng Token thành công",
            String.format("Bạn đã dùng %s Token mua gói '%s'. " +
                "Đây là tài sản độc lập, không bị ảnh hưởng bởi thời hạn token (QĐ12).",
                tokenCost.toPlainString(), product.getProductName()),
            "TRANSACTION");

        System.out.printf("[TokenController.convertToVipProduct] userId=%d: %s Token → gói '%s' (invId=%d)%n",
            userId, tokenCost, product.getProductName(), investmentId);
        return Result.SUCCESS;
    }

    // =========================================================================
    //  4. Các helper công khai
    // =========================================================================

    /**
     * Cộng token cho user (dùng từ MissionController).
     * @return true nếu thành công
     */
    public boolean addToken(int userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        return tokenDAO.addToken(userId, amount);
    }

    /**
     * Tính VNĐ sẽ nhận nếu đổi tokenAmount.
     * VNĐ = tokenAmount × 10 (QĐ11-PT1).
     */
    public BigDecimal previewVndConversion(BigDecimal tokenAmount) {
        if (tokenAmount == null) return BigDecimal.ZERO;
        return tokenAmount.multiply(TOKEN_TO_VND_RATE).setScale(0, RoundingMode.HALF_UP);
    }
}
