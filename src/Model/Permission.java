package Model;

import java.util.List;

/**
 * Kết quả merged permission của một ACCOUNT trên một SYS_FUNCTION.
 * Tính bằng OR của tất cả SysRole liên quan (trực tiếp + qua RoleGroup).
 * 5 quyền: view, add, edit, delete, download.
 */
public class Permission {
    private int          accountId;
    private int          functionId;
    private boolean      viewPerm;
    private boolean      addPerm;
    private boolean      editPerm;
    private boolean      deletePerm;
    private boolean      downloadPerm;
    private List<SysRole> sourceRoles;

    public Permission(int accountId, int functionId) {
        this.accountId  = accountId;
        this.functionId = functionId;
    }

    public int           getAccountId()                       { return accountId; }
    public int           getFunctionId()                      { return functionId; }
    public boolean       isViewPerm()                         { return viewPerm; }
    public void          setViewPerm(boolean v)               { this.viewPerm = v; }
    public boolean       isAddPerm()                          { return addPerm; }
    public void          setAddPerm(boolean v)                { this.addPerm = v; }
    public boolean       isEditPerm()                         { return editPerm; }
    public void          setEditPerm(boolean v)               { this.editPerm = v; }
    public boolean       isDeletePerm()                       { return deletePerm; }
    public void          setDeletePerm(boolean v)             { this.deletePerm = v; }
    public boolean       isDownloadPerm()                     { return downloadPerm; }
    public void          setDownloadPerm(boolean v)           { this.downloadPerm = v; }
    public List<SysRole> getSourceRoles()                     { return sourceRoles; }
    public void          setSourceRoles(List<SysRole> roles)  { this.sourceRoles = roles; }

    public boolean hasAnyPermission() {
        return viewPerm || addPerm || editPerm || deletePerm || downloadPerm;
    }

    @Override
    public String toString() {
        return String.format(
            "Permission{acct=%d, fn=%d, view=%b, add=%b, edit=%b, del=%b, dl=%b}",
            accountId, functionId, viewPerm, addPerm, editPerm, deletePerm, downloadPerm);
    }
}
