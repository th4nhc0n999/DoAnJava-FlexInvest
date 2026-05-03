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
public class ExchangeMoney {
    private int exmId;             
    private int userId;            
    private int transactionId;     
    private BigDecimal tokenSpent;  
    private BigDecimal moneyReceived; 
    private BigDecimal exchangeRate; 
    private String status;         
    private LocalDateTime createdAt; 
    private int isDeleted;

    public ExchangeMoney() {
    }

    public ExchangeMoney(int exmId, int userId, int transactionId, BigDecimal tokenSpent, BigDecimal moneyReceived, BigDecimal exchangeRate, String status, LocalDateTime createdAt, int isDeleted) {
        this.exmId = exmId;
        this.userId = userId;
        this.transactionId = transactionId;
        this.tokenSpent = tokenSpent;
        this.moneyReceived = moneyReceived;
        this.exchangeRate = exchangeRate;
        this.status = status;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getExmId() {
        return exmId;
    }

    public void setExmId(int exmId) {
        this.exmId = exmId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getTokenSpent() {
        return tokenSpent;
    }

    public void setTokenSpent(BigDecimal tokenSpent) {
        this.tokenSpent = tokenSpent;
    }

    public BigDecimal getMoneyReceived() {
        return moneyReceived;
    }

    public void setMoneyReceived(BigDecimal moneyReceived) {
        this.moneyReceived = moneyReceived;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
