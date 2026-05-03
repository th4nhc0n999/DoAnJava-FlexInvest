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
public class Ledger {
    private int ledgerId;          
    private int transactionId;     
    private int walletId;           
    private BigDecimal debit;       
    private BigDecimal credit;      
    private BigDecimal balanceAfter; 
    private LocalDateTime createdAt; 
    private int isDeleted;

    public Ledger() {
    }

    public Ledger(int ledgerId, int transactionId, int walletId, BigDecimal debit, BigDecimal credit, BigDecimal balanceAfter, LocalDateTime createdAt, int isDeleted) {
        this.ledgerId = ledgerId;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.debit = debit;
        this.credit = credit;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(int ledgerId) {
        this.ledgerId = ledgerId;
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

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
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
