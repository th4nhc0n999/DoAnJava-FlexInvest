package Model;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Model class for Investment
 * Resolved conflict: Using BigDecimal for financial accuracy
 */
public class Investment {
    private int investmentId;
    private int userId;
    private int productId;
    private String productName; // joined from SAVINGS_PRODUCT
    private BigDecimal investedAmount;  
    private BigDecimal appliedInterestRate;
    private Date startDate;
    private Date maturityDate;
    private String status;
    private String redemptionMethod;     // PT1 | PT2 | PT3 (stored in DB as a custom column if added)
    private int isDeleted;

    public Investment() {
    }

    public Investment(int investmentId, int userId, int productId, BigDecimal investedAmount, 
                      BigDecimal appliedInterestRate, Date startDate, Date maturityDate, 
                      String status, int isDeleted) {
        this.investmentId = investmentId;
        this.userId = userId;
        this.productId = productId;
        this.investedAmount = investedAmount;
        this.appliedInterestRate = appliedInterestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    // Getter and Setter
    public int getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(int investmentId) {
        this.investmentId = investmentId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }


    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount;
    }

    public BigDecimal getAppliedInterestRate() {
        return appliedInterestRate;
    }

    public void setAppliedInterestRate(BigDecimal appliedInterestRate) {
        this.appliedInterestRate = appliedInterestRate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(Date maturityDate) {
        this.maturityDate = maturityDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "Investment{" +
                "investmentId=" + investmentId +
                ", userId=" + userId +
                ", investedAmount=" + investedAmount +
                ", status='" + status + '\'' +
                '}';
    }

    public boolean isDueWithinDays(int days) {
        if (maturityDate == null) return false;
        java.util.Date threshold = java.util.Date.from(LocalDate.now().plusDays(days).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        return !maturityDate.after(threshold) && !maturityDate.before(new java.util.Date());

    }

    public String getRedemptionMethod() { return redemptionMethod; }
    public void setRedemptionMethod(String redemptionMethod) { this.redemptionMethod = redemptionMethod; }

}