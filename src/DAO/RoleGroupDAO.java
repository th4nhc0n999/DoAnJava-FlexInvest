package DAO;

import ConnectDB.ConnectionUtils;
import Model.RoleGroup;
import Model.SysRole;
import java.sql.*;
import java.util.*;

public class RoleGroupDAO {

    private RoleGroup mapRow(ResultSet rs) throws SQLException {
        return new RoleGroup(
            rs.getInt("ROLE_GROUP_ID"),
            rs.getString("NAME_ROLE_GROUP"),
            rs.getDate("CREATED_AT"),
            rs.getDate("UPDATED_AT"),
            rs.getInt("IS_DELETED")
        );
    }

    public List<RoleGroup> getAll() {
        List<RoleGroup> list = new ArrayList<>();
        String sql = "SELECT * FROM ROLE_GROUP WHERE IS_DELETED = 0 ORDER BY ROLE_GROUP_ID";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.getAll: " + e);
        }
        return list;
    }

    public RoleGroup getById(int roleGroupId) {
        String sql = "SELECT * FROM ROLE_GROUP WHERE ROLE_GROUP_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleGroupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.getById: " + e);
        }
        return null;
    }

    public boolean insert(RoleGroup rg) {
        String sql = "INSERT INTO ROLE_GROUP (NAME_ROLE_GROUP, CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rg.getNameRoleGroup());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.insert: " + e);
            return false;
        }
    }

    public boolean update(RoleGroup rg) {
        String sql = "UPDATE ROLE_GROUP SET NAME_ROLE_GROUP = ?, UPDATED_AT = SYSDATE "
                   + "WHERE ROLE_GROUP_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rg.getNameRoleGroup());
            ps.setInt(2, rg.getRoleGroupId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.update: " + e);
            return false;
        }
    }

    public boolean softDelete(int roleGroupId) {
        String sql = "UPDATE ROLE_GROUP SET IS_DELETED = 1, UPDATED_AT = SYSDATE "
                   + "WHERE ROLE_GROUP_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleGroupId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.softDelete: " + e);
            return false;
        }
    }

    // ── Quản lý ROLE_GROUP_ASSIGN_ROLE ──────────────────────────────────────

    public List<Integer> getRoleIdsByGroupId(int roleGroupId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT ROLE_ID FROM ROLE_GROUP_ASSIGN_ROLE "
                   + "WHERE ROLE_GROUP_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleGroupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("ROLE_ID"));
            }
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.getRoleIdsByGroupId: " + e);
        }
        return list;
    }

    /** Lưu lại danh sách SysRole cho một RoleGroup (xóa cũ, insert mới). */
    public boolean saveRoles(int roleGroupId, List<Integer> roleIds) {
        String del = "UPDATE ROLE_GROUP_ASSIGN_ROLE SET IS_DELETED = 1 "
                   + "WHERE ROLE_GROUP_ID = ?";
        String ins = "INSERT INTO ROLE_GROUP_ASSIGN_ROLE "
                   + "(ROLE_GROUP_ID, ROLE_ID, CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement d = con.prepareStatement(del)) {
                d.setInt(1, roleGroupId); d.executeUpdate();
            }
            try (PreparedStatement i = con.prepareStatement(ins)) {
                for (int roleId : roleIds) {
                    i.setInt(1, roleGroupId);
                    i.setInt(2, roleId);
                    i.addBatch();
                }
                i.executeBatch();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.saveRoles: " + e);
            return false;
        }
    }

    /** Lấy tất cả SysRole (kèm tên function) thuộc một RoleGroup. */
    public List<SysRole> getSysRolesByGroupId(int roleGroupId) {
        List<SysRole> list = new ArrayList<>();
        String sql = "SELECT sr.*, sf.NAME_FUNCTION "
                   + "FROM SYS_ROLE sr "
                   + "JOIN ROLE_GROUP_ASSIGN_ROLE rgar ON sr.ROLE_ID = rgar.ROLE_ID "
                   + "LEFT JOIN SYS_FUNCTION sf ON sr.FUNCTION_ID = sf.FUNCTION_ID "
                   + "WHERE rgar.ROLE_GROUP_ID = ? AND rgar.IS_DELETED = 0 "
                   + "  AND sr.IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleGroupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SysRole sr = new SysRole(
                        rs.getInt("ROLE_ID"), rs.getInt("FUNCTION_ID"),
                        rs.getInt("ADD_PERM"), rs.getInt("EDIT_PERM"),
                        rs.getInt("DELETE_PERM"), rs.getInt("DOWNLOAD_PERM"),
                        rs.getInt("VIEW_PERM"),
                        rs.getDate("CREATED_AT"), rs.getDate("UPDATED_AT"),
                        rs.getInt("IS_DELETED")
                    );
                    try { sr.setFunctionName(rs.getString("NAME_FUNCTION")); }
                    catch (SQLException ignored) {}
                    list.add(sr);
                }
            }
        } catch (Exception e) {
            System.err.println("RoleGroupDAO.getSysRolesByGroupId: " + e);
        }
        return list;
    }
}
