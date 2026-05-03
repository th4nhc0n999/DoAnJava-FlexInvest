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
public class Withdraw {
    private int withdrawId;         
    private int transactionId;      
    private int bankAccountId;     
    private BigDecimal fee;         
    private String status;          
    private LocalDateTime createdAt; 
    private LocalDateTime processedAt; 
    private String note;           
    private int isDeleted;

    public Withdraw() {
    }

    public Withdraw(int withdrawId, int transactionId, int bankAccountId, BigDecimal fee, String status, LocalDateTime createdAt, LocalDateTime processedAt, String note, int isDeleted) {
        this.withdrawId = withdrawId;
        this.transactionId = transactionId;
        this.bankAccountId = bankAccountId;
        this.fee = fee;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.note = note;
        this.isDeleted = isDeleted;
    }

    public int getWithdrawId() {
        return withdrawId;
    }

    public void setWithdrawId(int withdrawId) {
        this.withdrawId = withdrawId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(int bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
