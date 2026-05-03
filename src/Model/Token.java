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
public class Token {
    private int tokenWalletId;      
    private int userId;             
    private BigDecimal balance;    
    private BigDecimal totalEarned; 
    private String status;          
    private LocalDateTime updatedAt; 
    private int isDeleted;

    public Token() {
    }

    public Token(int tokenWalletId, int userId, BigDecimal balance, BigDecimal totalEarned, String status, LocalDateTime updatedAt, int isDeleted) {
        this.tokenWalletId = tokenWalletId;
        this.userId = userId;
        this.balance = balance;
        this.totalEarned = totalEarned;
        this.status = status;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public int getTokenWalletId() {
        return tokenWalletId;
    }

    public void setTokenWalletId(int tokenWalletId) {
        this.tokenWalletId = tokenWalletId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(BigDecimal totalEarned) {
        this.totalEarned = totalEarned;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
