package Model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model ánh xạ bảng TOKEN.
 * QĐ10: Token chỉ có giá trị trong năm dương lịch hiện tại.
 *       Hết ngày 31/12 → expireTokens() sẽ zero hóa balance.
 */
public class Token {

    private int       tokenWalletId;
    private int       userId;
    private BigDecimal balance;       // Số dư hiện tại (NUMBER 18,4 → BigDecimal)
    private BigDecimal totalEarned;   // Tổng tích lũy từ trước đến nay
    private String    status;         // ACTIVE | EXPIRED
    private Timestamp updatedAt;
    private int       isDeleted;

    public Token() {}

    public Token(int tokenWalletId, int userId, BigDecimal balance,
                 BigDecimal totalEarned, String status,
                 Timestamp updatedAt, int isDeleted) {
        this.tokenWalletId = tokenWalletId;
        this.userId        = userId;
        this.balance       = balance;
        this.totalEarned   = totalEarned;
        this.status        = status;
        this.updatedAt     = updatedAt;
        this.isDeleted     = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getTokenWalletId()                    { return tokenWalletId; }
    public void setTokenWalletId(int tokenWalletId)  { this.tokenWalletId = tokenWalletId; }

    public int getUserId()                { return userId; }
    public void setUserId(int userId)     { this.userId = userId; }

    public BigDecimal getBalance()                        { return balance; }
    public void setBalance(BigDecimal balance)            { this.balance = balance; }

    public BigDecimal getTotalEarned()                    { return totalEarned; }
    public void setTotalEarned(BigDecimal totalEarned)    { this.totalEarned = totalEarned; }

    public String getStatus()                 { return status; }
    public void setStatus(String status)      { this.status = status; }

    public Timestamp getUpdatedAt()                   { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt)     { this.updatedAt = updatedAt; }

    public int getIsDeleted()                  { return isDeleted; }
    public void setIsDeleted(int isDeleted)    { this.isDeleted = isDeleted; }

    @Override
    public String toString() {
        return "Token{userId=" + userId +
               ", balance=" + balance +
               ", status='" + status + "'}";
    }
}
