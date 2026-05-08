package Model;

import java.math.BigDecimal;

/**
 * Model ánh xạ bảng WALLET.
 * Dùng BigDecimal cho available_balance và locked_balance
 * để khớp với kiểu NUMBER(18,2) trong Oracle DB.
 */
public class Wallet {
    private int walletId;
    private int userId;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;
    private String status;
    private int isDeleted;

    public Wallet() {}

    public Wallet(int walletId, int userId, BigDecimal availableBalance,
                  BigDecimal lockedBalance, String status, int isDeleted) {
        this.walletId          = walletId;
        this.userId            = userId;
        this.availableBalance  = availableBalance;
        this.lockedBalance     = lockedBalance;
        this.status            = status;
        this.isDeleted         = isDeleted;
    }

    public int getWalletId() { return walletId; }
    public void setWalletId(int walletId) { this.walletId = walletId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(BigDecimal lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getIsDeleted() { return isDeleted; }
    public void setIsDeleted(int isDeleted) { this.isDeleted = isDeleted; }
}
