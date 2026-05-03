/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author 84941
 */
public class TokenTransaction {
    private int tokenTxId;          
    private int userId;             
    private String type;            
    private BigDecimal amount;     
    private String sourceType;      
    private Integer sourceId;       
    private BigDecimal balanceAfter; 
    private LocalDateTime createdAt; 
    private int isDeleted;

    public TokenTransaction() {
    }

    public TokenTransaction(int tokenTxId, int userId, String type, BigDecimal amount, String sourceType, Integer sourceId, BigDecimal balanceAfter, LocalDateTime createdAt, int isDeleted) {
        this.tokenTxId = tokenTxId;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getTokenTxId() {
        return tokenTxId;
    }

    public void setTokenTxId(int tokenTxId) {
        this.tokenTxId = tokenTxId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
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
