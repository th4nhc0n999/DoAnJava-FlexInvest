package DAO;

import ConnectDB.ConnectionUtils;
import Model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private User mapRow(ResultSet rs) throws Exception {
        return new User(
            rs.getInt("USER_ID"),
            rs.getInt("ROLE_ID"),
            rs.getString("EMAIL"),
            rs.getString("PASSWORD_HASH"),
            rs.getTimestamp("CREATED_AT"),
            rs.getString("STATUS"),
            rs.getString("REFERRAL_CODE"),
            rs.getInt("IS_DELETED")
        );
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM USERS WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception ex) {
            System.err.println("UserDAO.getUserById: " + ex);
        }
        return null;
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM USERS WHERE EMAIL = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception ex) {
            System.err.println("UserDAO.getUserByEmail: " + ex);
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM USERS WHERE IS_DELETED = 0 ORDER BY CREATED_AT DESC";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception ex) {
            System.err.println("UserDAO.getAllUsers: " + ex);
        }
        return list;
    }

    /** Inserts a new user and returns the generated USER_ID, or -1 on failure. */
    public int insertUser(User user) {
        String sql = "INSERT INTO USERS (ROLE_ID, EMAIL, PASSWORD_HASH, STATUS, REFERRAL_CODE, IS_DELETED) "
                   + "VALUES (?, ?, ?, ?, ?, 0)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"USER_ID"})) {
            ps.setInt(1, user.getRoleId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            ps.setString(5, user.getReferralCode());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ex) {
            System.err.println("UserDAO.insertUser: " + ex);
        }
        return -1;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE USERS SET ROLE_ID = ?, EMAIL = ?, PASSWORD_HASH = ?, "
                   + "STATUS = ?, REFERRAL_CODE = ? WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, user.getRoleId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getStatus());
            ps.setString(5, user.getReferralCode());
            ps.setInt(6, user.getUserId());
            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            System.err.println("UserDAO.updateUser: " + ex);
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        String sql = "UPDATE USERS SET IS_DELETED = 1 WHERE USER_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            System.err.println("UserDAO.deleteUser: " + ex);
        }
        return false;
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE USERS SET PASSWORD_HASH = ? WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            System.err.println("UserDAO.updatePassword: " + ex);
        }
        return false;
    }

    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE USERS SET STATUS = ? WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            System.err.println("UserDAO.updateStatus: " + ex);
        }
        return false;
    }
}
