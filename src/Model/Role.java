package Model;

import java.sql.Timestamp;

/** Maps to ROLES table */
public class Role {
    private int       roleId;
    private String    roleName;
    private String    description;
    private Timestamp createdAt;
    private int       isDeleted;

    public Role() {}

    public Role(int roleId, String roleName, String description,
                Timestamp createdAt, int isDeleted) {
        this.roleId      = roleId;
        this.roleName    = roleName;
        this.description = description;
        this.createdAt   = createdAt;
        this.isDeleted   = isDeleted;
    }

    public Role(String roleName, String description) {
        this.roleName    = roleName;
        this.description = description;
        this.isDeleted   = 0;
    }

    public int       getRoleId()                   { return roleId; }
    public void      setRoleId(int v)              { this.roleId = v; }
    public String    getRoleName()                 { return roleName; }
    public void      setRoleName(String v)         { this.roleName = v; }
    public String    getDescription()              { return description; }
    public void      setDescription(String v)      { this.description = v; }
    public Timestamp getCreatedAt()                { return createdAt; }
    public void      setCreatedAt(Timestamp v)     { this.createdAt = v; }
    public int       getIsDeleted()                { return isDeleted; }
    public void      setIsDeleted(int v)           { this.isDeleted = v; }

    @Override public String toString() { return roleName; }
}
