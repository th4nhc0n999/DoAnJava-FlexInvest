package DAO;

import ConnectDB.ConnectionUtils;
import Model.Role;
import java.sql.*;
import java.util.*;

/** Maps to ROLES table in FlexInvest.sql */
public class RoleDAO {

    private Role mapRow(ResultSet rs) throws SQLException {
        return new Role(
            rs.getInt("ROLE_ID"),
            rs.getString("ROLE_NAME"),
            rs.getString("DESCRIPTION"),
            rs.getTimestamp("CREATED_AT"),
            rs.getInt("IS_DELETED")
        );
    }

    public List<Role> getAll() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT * FROM ROLES WHERE IS_DELETED = 0 ORDER BY ROLE_ID";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("RoleDAO.getAll: " + e);
        }
        return list;
    }

    public Role getById(int roleId) {
        String sql = "SELECT * FROM ROLES WHERE ROLE_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("RoleDAO.getById: " + e);
        }
        return null;
    }

    public boolean insert(Role role) {
        String sql = "INSERT INTO ROLES (ROLE_NAME, DESCRIPTION, CREATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, SYSTIMESTAMP, 0)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.getRoleName());
            ps.setString(2, role.getDescription());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleDAO.insert: " + e);
            return false;
        }
    }

    public boolean update(Role role) {
        String sql = "UPDATE ROLES SET ROLE_NAME = ?, DESCRIPTION = ? WHERE ROLE_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.getRoleName());
            ps.setString(2, role.getDescription());
            ps.setInt(3, role.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleDAO.update: " + e);
            return false;
        }
    }

    public boolean softDelete(int roleId) {
        String sql = "UPDATE ROLES SET IS_DELETED = 1 WHERE ROLE_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("RoleDAO.softDelete: " + e);
            return false;
        }
    }
}
