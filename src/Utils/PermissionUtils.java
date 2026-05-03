package Utils;

import DAO.AccountPermissionDAO;
import Model.Permission;
import Model.SysRole;
import java.util.List;

/**
 * Common function phân quyền — đồng bộ với FlexInvest.sql.
 *
 * Input:  accountId  (ACCOUNT.ACCOUNT_ID)
 *         functionId (SYS_FUNCTION.FUNCTION_ID)
 *
 * Output: List<SysRole>  — tất cả SysRole của account cho function đó
 *         Permission     — kết quả OR tất cả 5 quyền (view/add/edit/delete/download)
 *
 * Nguồn quyền được gộp từ 2 luồng:
 *   1. Trực tiếp : ACCOUNT_ASSIGN_ROLE → SYS_ROLE
 *   2. Qua nhóm  : ACCOUNT_ASSIGN_ROLE_GROUP → ROLE_GROUP_ASSIGN_ROLE → SYS_ROLE
 */
public class PermissionUtils {

    private static final AccountPermissionDAO permDAO = new AccountPermissionDAO();

    // ── 1. Lấy tất cả SysRole của account trên một function ─────────────────
    public static List<SysRole> getAccountSysRoles(int accountId, int functionId) {
        return permDAO.getSysRolesForFunction(accountId, functionId);
    }

    // ── 2. Trả về Permission đã Merge (OR) ───────────────────────────────────
    /**
     * @param accountId  ACCOUNT.ACCOUNT_ID
     * @param functionId SYS_FUNCTION.FUNCTION_ID
     * @return Permission với 5 trường boolean đã OR, và sourceRoles chứa
     *         danh sách SysRole góp vào kết quả.
     */
    public static Permission getMergedPermission(int accountId, int functionId) {
        List<SysRole> roles  = getAccountSysRoles(accountId, functionId);
        Permission    merged = new Permission(accountId, functionId);
        merged.setSourceRoles(roles);

        for (SysRole sr : roles) {
            if (sr.getViewPerm()     == 1) merged.setViewPerm(true);
            if (sr.getAddPerm()      == 1) merged.setAddPerm(true);
            if (sr.getEditPerm()     == 1) merged.setEditPerm(true);
            if (sr.getDeletePerm()   == 1) merged.setDeletePerm(true);
            if (sr.getDownloadPerm() == 1) merged.setDownloadPerm(true);
        }
        return merged;
    }

    // ── 3. Helper riêng lẻ ──────────────────────────────────────────────────
    public static boolean canView(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).isViewPerm();
    }

    public static boolean canAdd(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).isAddPerm();
    }

    public static boolean canEdit(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).isEditPerm();
    }

    public static boolean canDelete(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).isDeletePerm();
    }

    public static boolean canDownload(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).isDownloadPerm();
    }

    public static boolean hasAnyPermission(int accountId, int functionId) {
        return getMergedPermission(accountId, functionId).hasAnyPermission();
    }
}
