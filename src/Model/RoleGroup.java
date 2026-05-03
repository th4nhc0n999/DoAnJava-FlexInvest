package Model;

import java.sql.Date;

/** Maps to ROLE_GROUP table */
public class RoleGroup {
    private int    roleGroupId;
    private String nameRoleGroup;
    private Date   createdAt;
    private Date   updatedAt;
    private int    isDeleted;

    public RoleGroup() {}

    public RoleGroup(int roleGroupId, String nameRoleGroup,
                     Date createdAt, Date updatedAt, int isDeleted) {
        this.roleGroupId   = roleGroupId;
        this.nameRoleGroup = nameRoleGroup;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
        this.isDeleted     = isDeleted;
    }

    public RoleGroup(String nameRoleGroup) {
        this.nameRoleGroup = nameRoleGroup;
        this.isDeleted     = 0;
    }

    public int    getRoleGroupId()             { return roleGroupId; }
    public void   setRoleGroupId(int v)        { this.roleGroupId = v; }
    public String getNameRoleGroup()           { return nameRoleGroup; }
    public void   setNameRoleGroup(String v)   { this.nameRoleGroup = v; }
    public Date   getCreatedAt()               { return createdAt; }
    public void   setCreatedAt(Date v)         { this.createdAt = v; }
    public Date   getUpdatedAt()               { return updatedAt; }
    public void   setUpdatedAt(Date v)         { this.updatedAt = v; }
    public int    getIsDeleted()               { return isDeleted; }
    public void   setIsDeleted(int v)          { this.isDeleted = v; }

    @Override public String toString() { return nameRoleGroup; }
}
