/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 *
 * @author 84941
 */
public class Payout {
    private int payoutId;          
    private int investmentId;  
    private int transactionId;    
    private String payoutType;     
    private LocalDate payoutDate;  
    private BigDecimal payoutAmount; 
    private int isDeleted;

    public Payout() {
    }

    public Payout(int payoutId, int investmentId, int transactionId, String payoutType, LocalDate payoutDate, BigDecimal payoutAmount, int isDeleted) {
        this.payoutId = payoutId;
        this.investmentId = investmentId;
        this.transactionId = transactionId;
        this.payoutType = payoutType;
        this.payoutDate = payoutDate;
        this.payoutAmount = payoutAmount;
        this.isDeleted = isDeleted;
    }

    public int getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(int payoutId) {
        this.payoutId = payoutId;
    }

    public int getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(int investmentId) {
        this.investmentId = investmentId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getPayoutType() {
        return payoutType;
    }

    public void setPayoutType(String payoutType) {
        this.payoutType = payoutType;
    }

    public LocalDate getPayoutDate() {
        return payoutDate;
    }

    public void setPayoutDate(LocalDate payoutDate) {
        this.payoutDate = payoutDate;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    
    
}
