package DAO;

import ConnectDB.ConnectionUtils;
import Model.SysRole;
import java.sql.*;
import java.util.*;

/**
 * Xử lý hai bảng pivot:
 *   ACCOUNT_ASSIGN_ROLE       → gán SysRole trực tiếp vào Account
 *   ACCOUNT_ASSIGN_ROLE_GROUP → gán RoleGroup vào Account
 */
public class AccountPermissionDAO {

    // ── ACCOUNT_ASSIGN_ROLE (trực tiếp) ─────────────────────────────────────

    public List<Integer> getDirectRoleIdsByAccountId(int accountId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT ROLE_ID FROM ACCOUNT_ASSIGN_ROLE "
                   + "WHERE ACCOUNT_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("ROLE_ID"));
            }
        } catch (Exception e) {
            System.err.println("AccountPermissionDAO.getDirectRoleIds: " + e);
        }
        return list;
    }

    /** Lưu danh sách SysRole trực tiếp cho Account (soft-delete cũ, insert mới). */
    public boolean saveDirectRoles(int accountId, List<Integer> roleIds) {
        String del = "UPDATE ACCOUNT_ASSIGN_ROLE SET IS_DELETED = 1, UPDATED_AT = SYSDATE "
                   + "WHERE ACCOUNT_ID = ?";
        String ins = "INSERT INTO ACCOUNT_ASSIGN_ROLE "
                   + "(ACCOUNT_ID, ROLE_ID, CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement d = con.prepareStatement(del)) {
                d.setInt(1, accountId); d.executeUpdate();
            }
            try (PreparedStatement i = con.prepareStatement(ins)) {
                for (int roleId : roleIds) {
                    i.setInt(1, accountId); i.setInt(2, roleId); i.addBatch();
                }
                i.executeBatch();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            System.err.println("AccountPermissionDAO.saveDirectRoles: " + e);
            return false;
        }
    }

    // ── ACCOUNT_ASSIGN_ROLE_GROUP ────────────────────────────────────────────

    public List<Integer> getRoleGroupIdsByAccountId(int accountId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT ROLE_GROUP_ID FROM ACCOUNT_ASSIGN_ROLE_GROUP "
                   + "WHERE ACCOUNT_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("ROLE_GROUP_ID"));
            }
        } catch (Exception e) {
            System.err.println("AccountPermissionDAO.getRoleGroupIds: " + e);
        }
        return list;
    }

    /** Lưu danh sách RoleGroup cho Account. */
    public boolean saveRoleGroups(int accountId, List<Integer> roleGroupIds) {
        String del = "UPDATE ACCOUNT_ASSIGN_ROLE_GROUP SET IS_DELETED = 1, UPDATED_AT = SYSDATE "
                   + "WHERE ACCOUNT_ID = ?";
        String ins = "INSERT INTO ACCOUNT_ASSIGN_ROLE_GROUP "
                   + "(ACCOUNT_ID, ROLE_GROUP_ID, CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement d = con.prepareStatement(del)) {
                d.setInt(1, accountId); d.executeUpdate();
            }
            try (PreparedStatement i = con.prepareStatement(ins)) {
                for (int gid : roleGroupIds) {
                    i.setInt(1, accountId); i.setInt(2, gid); i.addBatch();
                }
                i.executeBatch();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            System.err.println("AccountPermissionDAO.saveRoleGroups: " + e);
            return false;
        }
    }

    // ── Query tổng hợp cho PermissionUtils ───────────────────────────────────

    /**
     * Lấy tất cả SysRole active của account cho một function cụ thể.
     * Bao gồm: trực tiếp (ACCOUNT_ASSIGN_ROLE) + qua nhóm (ACCOUNT_ASSIGN_ROLE_GROUP).
     */
    public List<SysRole> getSysRolesForFunction(int accountId, int functionId) {
        List<SysRole> list = new ArrayList<>();

        // Trực tiếp
        String sqlDirect =
            "SELECT sr.* FROM SYS_ROLE sr "
          + "JOIN ACCOUNT_ASSIGN_ROLE aar ON sr.ROLE_ID = aar.ROLE_ID "
          + "WHERE aar.ACCOUNT_ID = ? AND aar.IS_DELETED = 0 "
          + "  AND sr.FUNCTION_ID = ? AND sr.IS_DELETED = 0";

        // Qua nhóm
        String sqlGroup =
            "SELECT sr.* FROM SYS_ROLE sr "
          + "JOIN ROLE_GROUP_ASSIGN_ROLE rgar ON sr.ROLE_ID = rgar.ROLE_ID "
          + "JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg ON rgar.ROLE_GROUP_ID = aarg.ROLE_GROUP_ID "
          + "WHERE aarg.ACCOUNT_ID = ? AND aarg.IS_DELETED = 0 "
          + "  AND rgar.IS_DELETED = 0 "
          + "  AND sr.FUNCTION_ID = ? AND sr.IS_DELETED = 0";

        Set<Integer> seen = new HashSet<>();
        try (Connection con = ConnectionUtils.getMyConnection()) {
            for (String sql : new String[]{sqlDirect, sqlGroup}) {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, accountId);
                    ps.setInt(2, functionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int rid = rs.getInt("ROLE_ID");
                            if (seen.add(rid)) {
                                list.add(new SysRole(
                                    rid, rs.getInt("FUNCTION_ID"),
                                    rs.getInt("ADD_PERM"), rs.getInt("EDIT_PERM"),
                                    rs.getInt("DELETE_PERM"), rs.getInt("DOWNLOAD_PERM"),
                                    rs.getInt("VIEW_PERM"),
                                    rs.getDate("CREATED_AT"), rs.getDate("UPDATED_AT"),
                                    rs.getInt("IS_DELETED")
                                ));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AccountPermissionDAO.getSysRolesForFunction: " + e);
        }
        return list;
    }
}
