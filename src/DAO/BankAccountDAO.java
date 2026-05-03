/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import ConnectDB.ConnectionUtils;
import Model.BankAccount;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 84941
 */
public class BankAccountDAO {
    private BankAccount mapRow(ResultSet rs) throws Exception {
        return new BankAccount(
                rs.getInt("BANK_ACCOUNT_ID"),
                rs.getInt("USER_ID"),
                rs.getString("BANK_NAME"),
                rs.getString("ACCOUNT_NUMBER"),
                rs.getInt("IS_LINKED"),
                rs.getInt("IS_DELETED")
        );
    }
    
    public boolean add(BankAccount bankAccount) {
        // Mặc định khi thêm mới, IS_DELETED = 0 (chưa xóa)
        String sql = "INSERT INTO BANK_ACCOUNT (USER_ID, BANK_NAME, ACCOUNT_NUMBER, IS_LINKED, IS_DELETED) "
                   + "VALUES (?, ?, ?, ?, 0)";
        
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bankAccount.getUserId());
            ps.setString(2, bankAccount.getBankName());
            ps.setString(3, bankAccount.getAccountNumber());
            ps.setInt(4, bankAccount.getIsLinked()); // Có thể set 0 (chưa link) hoặc 1 (link luôn)
            
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
   
    public List<BankAccount> findByAccount(int userId) {
        List<BankAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM BANK_ACCOUNT WHERE USER_ID = ? AND IS_DELETED = 0 ORDER BY IS_LINKED DESC";
        try(Connection conn = ConnectionUtils.getMyConnection();
                PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setInt(1,userId);
            try(ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public boolean setLinked(int bankAccountId, int userId) {
        String sqlRemoveOld = "UPDATE BANK_ACCOUNT SET IS_LINKED = 0 WHERE USER_ID = ?";
        String sqlSetNew = "UPDATE BANK_ACCOUNT SET IS_LINKED = 1 WHERE BANK_ACCOUNT_ID = ? AND USER_ID = ?";
        
        Connection conn = null;
        PreparedStatement psRemove = null;
        PreparedStatement psSet = null;
        
        try {
            conn = ConnectionUtils.getMyConnection();
            conn.setAutoCommit(false); // Bật Transaction
            
            // Bước 1: Hủy liên kết tất cả các thẻ của User này
            psRemove = conn.prepareStatement(sqlRemoveOld);
            psRemove.setInt(1, userId);
            psRemove.executeUpdate();
            
            // Bước 2: Liên kết thẻ được chọn
            psSet = conn.prepareStatement(sqlSetNew);
            psSet.setInt(1, bankAccountId);
            psSet.setInt(2, userId);
            int rowsAffected = psSet.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { if (psRemove != null) psRemove.close(); } catch (Exception e) {}
            try { if (psSet != null) psSet.close(); } catch (Exception e) {}
            try { 
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close(); 
                }
            } catch (Exception e) {}
        }
    }
}

//private int bankAccountId;
//    private int userId;
//    private String bankName;
//    private String accountNumber;
//    private int isLinked;
//    private int isDeleted;