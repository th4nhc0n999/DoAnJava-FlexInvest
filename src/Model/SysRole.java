package Model;

import java.sql.Date;

/** Maps to SYS_ROLE table.
 *  Một SysRole = tập hợp quyền (view/add/edit/delete/download) trên một SysFunction. */
public class SysRole {
    private int  roleId;
    private int  functionId;
    private int  addPerm;
    private int  editPerm;
    private int  deletePerm;
    private int  downloadPerm;
    private int  viewPerm;
    private Date createdAt;
    private Date updatedAt;
    private int  isDeleted;

    // Tên function để hiển thị UI (không phải cột DB, join thêm)
    private String functionName;

    public SysRole() {}

    public SysRole(int roleId, int functionId,
                   int addPerm, int editPerm, int deletePerm,
                   int downloadPerm, int viewPerm,
                   Date createdAt, Date updatedAt, int isDeleted) {
        this.roleId       = roleId;
        this.functionId   = functionId;
        this.addPerm      = addPerm;
        this.editPerm     = editPerm;
        this.deletePerm   = deletePerm;
        this.downloadPerm = downloadPerm;
        this.viewPerm     = viewPerm;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
        this.isDeleted    = isDeleted;
    }

    public int    getRoleId()               { return roleId; }
    public void   setRoleId(int v)          { this.roleId = v; }
    public int    getFunctionId()           { return functionId; }
    public void   setFunctionId(int v)      { this.functionId = v; }
    public int    getAddPerm()              { return addPerm; }
    public void   setAddPerm(int v)         { this.addPerm = v; }
    public int    getEditPerm()             { return editPerm; }
    public void   setEditPerm(int v)        { this.editPerm = v; }
    public int    getDeletePerm()           { return deletePerm; }
    public void   setDeletePerm(int v)      { this.deletePerm = v; }
    public int    getDownloadPerm()         { return downloadPerm; }
    public void   setDownloadPerm(int v)    { this.downloadPerm = v; }
    public int    getViewPerm()             { return viewPerm; }
    public void   setViewPerm(int v)        { this.viewPerm = v; }
    public Date   getCreatedAt()            { return createdAt; }
    public void   setCreatedAt(Date v)      { this.createdAt = v; }
    public Date   getUpdatedAt()            { return updatedAt; }
    public void   setUpdatedAt(Date v)      { this.updatedAt = v; }
    public int    getIsDeleted()            { return isDeleted; }
    public void   setIsDeleted(int v)       { this.isDeleted = v; }
    public String getFunctionName()         { return functionName; }
    public void   setFunctionName(String v) { this.functionName = v; }

    @Override
    public String toString() {
        return "SysRole#" + roleId + " fn=" + functionId
             + " [V=" + viewPerm + " A=" + addPerm
             + " E=" + editPerm + " D=" + deletePerm
             + " DL=" + downloadPerm + "]";
    }
}
