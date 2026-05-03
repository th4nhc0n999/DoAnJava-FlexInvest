/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

/**
 *
 * @author 84941
 */
public class Wallet {
    private int walletId;
    private int userId;
    private int availableBalance;
    private int lockedBalance;
    private String status;
    private int isDeleted;
    
    public Wallet() {}

    public Wallet(int walletId, int userId, int availableBalance, int lockedBalance, String status, int isDeleted) {
        this.walletId = walletId;
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.lockedBalance = lockedBalance;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(int availableBalance) {
        this.availableBalance = availableBalance;
    }

    public int getLockedBalance() {
        return lockedBalance;
    }

    public void setLockedBalance(int lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
}
