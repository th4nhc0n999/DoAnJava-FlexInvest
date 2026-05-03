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
public class InterestRate {
    private int historyId;           
    private int productId;           
    private BigDecimal oldRate;      
    private BigDecimal newRate;      
    private LocalDate effectiveDate; 
    private LocalDateTime createdAt; 
    private int changedBy;           
    private int isDeleted;

    public InterestRate() {
    }

    public InterestRate(int historyId, int productId, BigDecimal oldRate, BigDecimal newRate, LocalDate effectiveDate, LocalDateTime createdAt, int changedBy, int isDeleted) {
        this.historyId = historyId;
        this.productId = productId;
        this.oldRate = oldRate;
        this.newRate = newRate;
        this.effectiveDate = effectiveDate;
        this.createdAt = createdAt;
        this.changedBy = changedBy;
        this.isDeleted = isDeleted;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public BigDecimal getOldRate() {
        return oldRate;
    }

    public void setOldRate(BigDecimal oldRate) {
        this.oldRate = oldRate;
    }

    public BigDecimal getNewRate() {
        return newRate;
    }

    public void setNewRate(BigDecimal newRate) {
        this.newRate = newRate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(int changedBy) {
        this.changedBy = changedBy;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
