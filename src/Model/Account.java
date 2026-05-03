package Model;

import java.sql.Date;

/** Maps to ACCOUNT table — dùng cho đăng nhập (username/password).
 *  Khác với USERS (thông tin profile). */
public class Account {
    private int    accountId;
    private int    userId;
    private String username;
    private String passwordHash;
    private String status;
    private Date   createdAt;
    private Date   updatedAt;
    private int    isDeleted;

    public Account() {}

    public Account(int accountId, int userId, String username, String passwordHash,
                   String status, Date createdAt, Date updatedAt, int isDeleted) {
        this.accountId    = accountId;
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.status       = status;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
        this.isDeleted    = isDeleted;
    }

    public int    getAccountId()               { return accountId; }
    public void   setAccountId(int v)          { this.accountId = v; }
    public int    getUserId()                  { return userId; }
    public void   setUserId(int v)             { this.userId = v; }
    public String getUsername()                { return username; }
    public void   setUsername(String v)        { this.username = v; }
    public String getPasswordHash()            { return passwordHash; }
    public void   setPasswordHash(String v)    { this.passwordHash = v; }
    public String getStatus()                  { return status; }
    public void   setStatus(String v)          { this.status = v; }
    public Date   getCreatedAt()               { return createdAt; }
    public void   setCreatedAt(Date v)         { this.createdAt = v; }
    public Date   getUpdatedAt()               { return updatedAt; }
    public void   setUpdatedAt(Date v)         { this.updatedAt = v; }
    public int    getIsDeleted()               { return isDeleted; }
    public void   setIsDeleted(int v)          { this.isDeleted = v; }

    @Override public String toString() { return username; }
}
