/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author 84941
 */
public class EarlyRedemptionEvent {
    private int eventId;              
    private int investmentId;         
    private Integer policyId;         
    private LocalDate redemptionDate; 
    private BigDecimal originalAmount; 
    private BigDecimal penaltyAmount;  
    private BigDecimal actualPayout;   
    private int transactionId;        
    private LocalDateTime createdAt;   
    private int isDeleted;

    public EarlyRedemptionEvent() {
    }

    public EarlyRedemptionEvent(int eventId, int investmentId, Integer policyId, LocalDate redemptionDate, BigDecimal originalAmount, BigDecimal penaltyAmount, BigDecimal actualPayout, int transactionId, LocalDateTime createdAt, int isDeleted) {
        this.eventId = eventId;
        this.investmentId = investmentId;
        this.policyId = policyId;
        this.redemptionDate = redemptionDate;
        this.originalAmount = originalAmount;
        this.penaltyAmount = penaltyAmount;
        this.actualPayout = actualPayout;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(int investmentId) {
        this.investmentId = investmentId;
    }

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
    }

    public LocalDate getRedemptionDate() {
        return redemptionDate;
    }

    public void setRedemptionDate(LocalDate redemptionDate) {
        this.redemptionDate = redemptionDate;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    public BigDecimal getActualPayout() {
        return actualPayout;
    }

    public void setActualPayout(BigDecimal actualPayout) {
        this.actualPayout = actualPayout;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
