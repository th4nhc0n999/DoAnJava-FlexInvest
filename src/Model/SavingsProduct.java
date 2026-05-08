package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model ánh xạ bảng SAVINGS_PRODUCT.
 * term = 0  → Flex-Safe (không kỳ hạn).
 * term > 0  → có kỳ hạn (đơn vị ngày).
 * hasDateWindow() → Flex-Sale / Flex-Holiday (mở/đóng theo ngày).
 */
public class SavingsProduct {
    private int productId;
    private String productName;
    private BigDecimal interestRate;
    private int term;                        // 0 = không kỳ hạn
    private BigDecimal minInvestmentAmount;
    private BigDecimal maxInvestmentAmount;
    private BigDecimal penaltyRate;
    private BigDecimal fallbackInterestRate; // lãi rút sớm < minHoldingDays
    private int minHoldingDays;
    private String status;
    private String currency;
    private LocalDate startDate;             // null = luôn mở
    private LocalDate endDate;
    private int isDeleted;

    public SavingsProduct() {}

    public SavingsProduct(int productId, String productName, BigDecimal interestRate,
                          int term, BigDecimal minInvestmentAmount, BigDecimal maxInvestmentAmount,
                          BigDecimal penaltyRate, BigDecimal fallbackInterestRate,
                          int minHoldingDays, String status, String currency,
                          LocalDate startDate, LocalDate endDate, int isDeleted) {
        this.productId = productId;
        this.productName = productName;
        this.interestRate = interestRate;
        this.term = term;
        this.minInvestmentAmount = minInvestmentAmount;
        this.maxInvestmentAmount = maxInvestmentAmount;
        this.penaltyRate = penaltyRate;
        this.fallbackInterestRate = fallbackInterestRate;
        this.minHoldingDays = minHoldingDays;
        this.status = status;
        this.currency = currency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isDeleted = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getProductId()                       { return productId; }
    public void setProductId(int productId)         { this.productId = productId; }

    public String getProductName()                          { return productName; }
    public void setProductName(String productName)          { this.productName = productName; }

    public BigDecimal getInterestRate()                     { return interestRate; }
    public void setInterestRate(BigDecimal interestRate)    { this.interestRate = interestRate; }

    /** Kỳ hạn tính bằng ngày. 0 = không kỳ hạn (Flex-Safe). */
    public int getTerm()              { return term; }
    public void setTerm(int term)     { this.term = term; }

    public BigDecimal getMinInvestmentAmount()                          { return minInvestmentAmount; }
    public void setMinInvestmentAmount(BigDecimal minInvestmentAmount)  { this.minInvestmentAmount = minInvestmentAmount; }

    public BigDecimal getMaxInvestmentAmount()                          { return maxInvestmentAmount; }
    public void setMaxInvestmentAmount(BigDecimal maxInvestmentAmount)  { this.maxInvestmentAmount = maxInvestmentAmount; }

    public BigDecimal getPenaltyRate()                      { return penaltyRate; }
    public void setPenaltyRate(BigDecimal penaltyRate)      { this.penaltyRate = penaltyRate; }

    public BigDecimal getFallbackInterestRate()                             { return fallbackInterestRate; }
    public void setFallbackInterestRate(BigDecimal fallbackInterestRate)    { this.fallbackInterestRate = fallbackInterestRate; }

    public int getMinHoldingDays()                      { return minHoldingDays; }
    public void setMinHoldingDays(int minHoldingDays)   { this.minHoldingDays = minHoldingDays; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getCurrency()                 { return currency; }
    public void setCurrency(String currency)    { this.currency = currency; }

    public LocalDate getStartDate()                 { return startDate; }
    public void setStartDate(LocalDate startDate)   { this.startDate = startDate; }

    public LocalDate getEndDate()                   { return endDate; }
    public void setEndDate(LocalDate endDate)       { this.endDate = endDate; }

    public int getIsDeleted()                   { return isDeleted; }
    public void setIsDeleted(int isDeleted)     { this.isDeleted = isDeleted; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** true nếu là gói không kỳ hạn (Flex-Safe). */
    public boolean isFlexSafe() { return term == 0; }

    /** true nếu gói có cửa sổ ngày mở/đóng bán (Flex-Sale, Flex-Holiday). */
    public boolean hasDateWindow() { return startDate != null || endDate != null; }

    /**
     * Kiểm tra gói có đang mở bán hôm nay không.
     *  - Gói thường / Flex-Safe: chỉ cần status=ACTIVE.
     *  - Flex-Sale / Flex-Holiday: nằm trong [startDate, endDate].
     */
    public boolean isOpenToday() {
        if (!"ACTIVE".equals(status)) return false;
        if (!hasDateWindow()) return true;
        LocalDate today = LocalDate.now();
        boolean afterStart = (startDate == null) || !today.isBefore(startDate);
        boolean beforeEnd  = (endDate   == null) || !today.isAfter(endDate);
        return afterStart && beforeEnd;
    }

    @Override
    public String toString() {
        return "SavingsProduct{id=" + productId +
               ", name='" + productName + '\'' +
               ", rate=" + interestRate +
               ", term=" + term + "d" +
               ", status='" + status + "'}";
    }
}