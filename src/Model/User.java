package Model;

import java.sql.Timestamp;

public class User {
    private int userId;       // NUMBER GENERATED AS IDENTITY in DB
    private int roleId;
    private String email;
    private String passwordHash;
    private Timestamp createdAt;
    private String status;
    private String referralCode;
    private int isDeleted;

    public User() {}

    public User(int userId, int roleId, String email, String passwordHash,
                Timestamp createdAt, String status, String referralCode, int isDeleted) {
        this.userId       = userId;
        this.roleId       = roleId;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.createdAt    = createdAt;
        this.status       = status;
        this.referralCode = referralCode;
        this.isDeleted    = isDeleted;
    }

    public int    getUserId()               { return userId; }
    public void   setUserId(int v)          { this.userId = v; }

    public int    getRoleId()               { return roleId; }
    public void   setRoleId(int v)          { this.roleId = v; }

    public String getEmail()                { return email; }
    public void   setEmail(String v)        { this.email = v; }

    public String getPasswordHash()         { return passwordHash; }
    public void   setPasswordHash(String v) { this.passwordHash = v; }

    public Timestamp getCreatedAt()         { return createdAt; }
    public void   setCreatedAt(Timestamp v) { this.createdAt = v; }

    public String getStatus()               { return status; }
    public void   setStatus(String v)       { this.status = v; }

    public String getReferralCode()         { return referralCode; }
    public void   setReferralCode(String v) { this.referralCode = v; }

    public int    getIsDeleted()            { return isDeleted; }
    public void   setIsDeleted(int v)       { this.isDeleted = v; }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", roleId=" + roleId +
               ", email=" + email + ", status=" + status + "}";
    }
}
