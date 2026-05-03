package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsProduct {
    private int productId;               
    private String productName;          
    private BigDecimal interestRate;    
    private int term;                
    private BigDecimal minInvestmentAmount; 
    private BigDecimal maxInvestmentAmount;
    private BigDecimal penaltyRate;      
    private BigDecimal fallbackInterestRate; 
    private int minHoldingDays;         
    private String status;               
    private String currency;             
    private LocalDate startDate;       
    private LocalDate endDate;           
    private int isDeleted;

    public SavingsProduct() {
    }

    public SavingsProduct(int productId, String productName, BigDecimal interestRate, int term, BigDecimal minInvestmentAmount, BigDecimal maxInvestmentAmount, BigDecimal penaltyRate, BigDecimal fallbackInterestRate, int minHoldingDays, String status, String currency, LocalDate startDate, LocalDate endDate, int isDeleted) {
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

    @Override
    public String toString() {
        return "SavingsProduct{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", interestRate=" + interestRate +
                ", status='" + status + '\'' +
                '}';
    }
}