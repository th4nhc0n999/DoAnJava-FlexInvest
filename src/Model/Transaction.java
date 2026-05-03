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
public class Transaction {
    private int transactionId;
    private int walletId;
    private String typeCode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private int isDeleted;

    public Transaction() {
    }

    public Transaction(int transactionId, int walletId, String typeCode, BigDecimal amount, String status, LocalDateTime createdAt, int isDeleted) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.typeCode = typeCode;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
