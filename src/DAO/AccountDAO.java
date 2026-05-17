package DAO;

import ConnectDB.ConnectionUtils;
import Model.Account;
import java.sql.*;
import java.util.*;

public class AccountDAO {

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
            rs.getInt("ACCOUNT_ID"),
            rs.getInt("USER_ID"),
            rs.getString("USERNAME"),
            rs.getString("PASSWORD_HASH"),
            rs.getString("STATUS"),
            rs.getDate("CREATED_AT"),
            rs.getDate("UPDATED_AT"),
            rs.getInt("IS_DELETED")
        );
    }

    public List<Account> getAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM ACCOUNT WHERE IS_DELETED = 0 ORDER BY ACCOUNT_ID";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("AccountDAO.getAll: " + e);
        }
        return list;
    }

    public Account getById(int accountId) {
        String sql = "SELECT * FROM ACCOUNT WHERE ACCOUNT_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("AccountDAO.getById: " + e);
        }
        return null;
    }

    public Account getByUsername(String username) {
        String sql = "SELECT * FROM ACCOUNT WHERE USERNAME = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("AccountDAO.getByUsername: " + e);
        }
        return null;
    }

    public Account getByUserId(int userId) {
        String sql = "SELECT * FROM ACCOUNT WHERE USER_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("AccountDAO.getByUserId: " + e);
        }
        return null;
    }

    public boolean insert(Account a) {
        String sql = "INSERT INTO ACCOUNT "
                   + "(USER_ID, USERNAME, PASSWORD_HASH, STATUS, CREATED_AT, UPDATED_AT, IS_DELETED) "
                   + "VALUES (?, ?, ?, ?, SYSDATE, SYSDATE, 0)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, a.getUserId());
            ps.setString(2, a.getUsername());
            ps.setString(3, a.getPasswordHash());
            ps.setString(4, a.getStatus() != null ? a.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("AccountDAO.insert: " + e);
            return false;
        }
    }

    public boolean update(Account a) {
        String sql = "UPDATE ACCOUNT SET USERNAME = ?, STATUS = ?, UPDATED_AT = SYSDATE "
                   + "WHERE ACCOUNT_ID = ? AND IS_DELETED = 0";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getUsername());
            ps.setString(2, a.getStatus());
            ps.setInt(3, a.getAccountId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("AccountDAO.update: " + e);
            return false;
        }
    }

    public boolean softDelete(int accountId) {
        String sql = "UPDATE ACCOUNT SET IS_DELETED = 1, UPDATED_AT = SYSDATE "
                   + "WHERE ACCOUNT_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("AccountDAO.softDelete: " + e);
            return false;
        }
    }
}
