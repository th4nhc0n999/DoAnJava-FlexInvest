package Model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model ánh xạ bảng TOKEN_TRANSACTION.
 * Lưu lịch sử từng lần biến động token của user.
 *
 * type        — Chiều biến động:
 *                  EARN     : nhận token (hoàn thành mission, check-in...)
 *                  SPEND    : tiêu token (đổi quà...)
 *                  EXCHANGE : quy đổi token sang quyền lợi khác
 *
 * source_type — Nguồn gốc giao dịch:
 *                  MISSION  : hoàn thành nhiệm vụ
 *                  CHECKIN  : điểm danh hàng ngày
 *                  SYSTEM   : admin thao tác thủ công
 *                  YEAR_END : expire cuối năm (QĐ10)
 *
 * source_id   — ID tham chiếu tùy source_type:
 *                  source_type = MISSION / CHECKIN → user_mission_id
 *                  source_type = SYSTEM / YEAR_END → NULL
 *
 * balance_after — Số dư token SAU khi giao dịch này xảy ra.
 *                 Dùng để hiển thị lịch sử mà không cần tính lại.
 */
public class TokenTransaction {

    private int        tokenTxId;
    private int        userId;
    private String     type;          // EARN | SPEND | EXCHANGE
    private BigDecimal amount;        // Số token biến động (luôn dương)
    private String     sourceType;    // MISSION | CHECKIN | SYSTEM | YEAR_END | ...
    private Integer    sourceId;      // Nullable
    private BigDecimal balanceAfter;  // Số dư token sau giao dịch
    private Timestamp  createdAt;
    private int        isDeleted;

    public TokenTransaction() {}

    public TokenTransaction(int tokenTxId, int userId, String type, BigDecimal amount,
                            String sourceType, Integer sourceId,
                            BigDecimal balanceAfter, Timestamp createdAt, int isDeleted) {
        this.tokenTxId    = tokenTxId;
        this.userId       = userId;
        this.type         = type;
        this.amount       = amount;
        this.sourceType   = sourceType;
        this.sourceId     = sourceId;
        this.balanceAfter = balanceAfter;
        this.createdAt    = createdAt;
        this.isDeleted    = isDeleted;
    }

    // ── Static factories — tạo nhanh các loại giao dịch phổ biến ────────────

    /**
     * Tạo giao dịch EARN (nhận token).
     * Dùng trong MissionDAO.claimReward() và TokenDAO.addToken()
     *
     * @param balanceAfter số dư SAU khi cộng token
     */
    public static TokenTransaction earn(int userId, BigDecimal amount,
                                        String sourceType, Integer sourceId,
                                        BigDecimal balanceAfter) {
        TokenTransaction tx = new TokenTransaction();
        tx.setUserId(userId);
        tx.setType("EARN");
        tx.setAmount(amount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setBalanceAfter(balanceAfter);
        return tx;
    }

    /**
     * Tạo giao dịch SPEND (tiêu token).
     * Dùng trong TokenDAO.deductToken()
     *
     * @param balanceAfter số dư SAU khi trừ token
     */
    public static TokenTransaction spend(int userId, BigDecimal amount,
                                         String sourceType, Integer sourceId,
                                         BigDecimal balanceAfter) {
        TokenTransaction tx = new TokenTransaction();
        tx.setUserId(userId);
        tx.setType("SPEND");
        tx.setAmount(amount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setBalanceAfter(balanceAfter);
        return tx;
    }

    /**
     * Tạo giao dịch EXCHANGE (quy đổi token).
     *
     * @param balanceAfter số dư SAU khi quy đổi
     */
    public static TokenTransaction exchange(int userId, BigDecimal amount,
                                            String sourceType, Integer sourceId,
                                            BigDecimal balanceAfter) {
        TokenTransaction tx = new TokenTransaction();
        tx.setUserId(userId);
        tx.setType("EXCHANGE");
        tx.setAmount(amount);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setBalanceAfter(balanceAfter);
        return tx;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getTokenTxId()                   { return tokenTxId; }
    public void setTokenTxId(int tokenTxId)     { this.tokenTxId = tokenTxId; }

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getType()                 { return type; }
    public void setType(String type)        { this.type = type; }

    public BigDecimal getAmount()               { return amount; }
    public void setAmount(BigDecimal amount)    { this.amount = amount; }

    public String getSourceType()                   { return sourceType; }
    public void setSourceType(String sourceType)    { this.sourceType = sourceType; }

    public Integer getSourceId()                { return sourceId; }
    public void setSourceId(Integer sourceId)   { this.sourceId = sourceId; }

    public BigDecimal getBalanceAfter()                   { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter)  { this.balanceAfter = balanceAfter; }

    public Timestamp getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Timestamp createdAt)   { this.createdAt = createdAt; }

    public int getIsDeleted()                   { return isDeleted; }
    public void setIsDeleted(int isDeleted)     { this.isDeleted = isDeleted; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isEarn()     { return "EARN".equals(type); }
    public boolean isSpend()    { return "SPEND".equals(type); }
    public boolean isExchange() { return "EXCHANGE".equals(type); }

    @Override
    public String toString() {
        return "TokenTransaction{" +
               "userId=" + userId +
               ", type='" + type + "'" +
               ", amount=" + amount +
               ", sourceType='" + sourceType + "'" +
               ", balanceAfter=" + balanceAfter + "}";
    }
}
