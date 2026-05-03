package DAO;

import ConnectDB.ConnectionUtils;
import Model.SysRole;
import java.sql.*;
import java.util.*;

public class SysRoleDAO {

    private SysRole mapRow(ResultSet rs) throws SQLException {
        SysRole sr = new SysRole(
            rs.getInt("ROLE_ID"),
            rs.getInt("FUNCTION_ID"),
            rs.getInt("ADD_PERM"),
            rs.getInt("EDIT_PERM"),
            rs.getInt("DELETE_PERM"),
            rs.getInt("DOWNLOAD_PERM"),
            rs.getInt("VIEW_PERM"),
            rs.getDate("CREATED_AT"),
            rs.getDate("UPDATED_AT"),
            rs.getInt("IS_DELETED")
        );
        // Thử đọc NAME_FUNCTION nếu có trong ResultSet (join)
        try { sr.setFunctionName(rs.getString("NAME_FUNCTION")); } catch (SQLException ignored) {}
        return sr;
    }

    public List<SysRole> getAll() {
        List<SysRole> list = new ArrayList<>();
        String sql = "SELECT sr.*, sf.NAME_FUNCTION "
                   + "FROM SYS_ROLE sr "
                   + "LEFT JOIN SYS_FUNCTION sf ON sr.FUNCTION_ID = sf.FUNCTION_ID "
                   + "WHERE sr.IS_DELETED = 0 "
                   + "ORDER BY sr.FUNCTION_ID, sr.ROLE_ID";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("SysRoleDAO.getAll: " + e);
        }
        return list;
    }

    public SysRole getById(int roleId) {
        String sql = "SELECT sr.*, sf.NAME_FUNCTION "
                   + "FROM SYS_ROLE sr "
                   + "LEFT JOIN SYS_FUNCTION sf ON sr.FUNCTION_ID = sf.FUNCTION_ID "
                   + "WHERE sr.ROLE_ID = ? AND sr.IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("SysRoleDAO.getById: " + e);
        }
        return null;
    }

    public List<SysRole> getByFunctionId(int functionId) {
        List<SysRole> list = new ArrayList<>();
        String sql = "SELECT sr.*, sf.NAME_FUNCTION "
                   + "FROM SYS_ROLE sr "
                   + "LEFT JOIN SYS_FUNCTION sf ON sr.FUNCTION_ID = sf.FUNCTION_ID "
                   + "WHERE sr.FUNCTION_ID = ? AND sr.IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, functionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("SysRoleDAO.getByFunctionId: " + e);
        }
        return list;
    }

    public boolean insert(SysRole sr) {
        String sql = "INSERT INTO SYS_ROLE "
                   + "(FUNCTION_ID, ADD_PERM, EDIT_PERM, DELETE_PERM, DOWNLOAD_PERM, VIEW_PERM, "
                   + " CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sr.getFunctionId());
            ps.setInt(2, sr.getAddPerm());
            ps.setInt(3, sr.getEditPerm());
            ps.setInt(4, sr.getDeletePerm());
            ps.setInt(5, sr.getDownloadPerm());
            ps.setInt(6, sr.getViewPerm());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysRoleDAO.insert: " + e);
            return false;
        }
    }

    public boolean update(SysRole sr) {
        String sql = "UPDATE SYS_ROLE "
                   + "SET FUNCTION_ID = ?, ADD_PERM = ?, EDIT_PERM = ?, "
                   + "    DELETE_PERM = ?, DOWNLOAD_PERM = ?, VIEW_PERM = ?, "
                   + "    UPDATED_AT = SYSDATE "
                   + "WHERE ROLE_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sr.getFunctionId());
            ps.setInt(2, sr.getAddPerm());
            ps.setInt(3, sr.getEditPerm());
            ps.setInt(4, sr.getDeletePerm());
            ps.setInt(5, sr.getDownloadPerm());
            ps.setInt(6, sr.getViewPerm());
            ps.setInt(7, sr.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysRoleDAO.update: " + e);
            return false;
        }
    }

    public boolean softDelete(int roleId) {
        String sql = "UPDATE SYS_ROLE SET IS_DELETED = 1, UPDATED_AT = SYSDATE "
                   + "WHERE ROLE_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysRoleDAO.softDelete: " + e);
            return false;
        }
    }
}
