/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import ConnectDB.ConnectionUtils;
import Model.Ekyc;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 84941
 */
public class EkycDAO {
    private Ekyc mapRow(ResultSet rs) throws Exception {
        return new Ekyc (
        rs.getInt("KYC_ID"),
        rs.getInt("USER_ID"),
        rs.getString("ID_NUMBER"),
        rs.getString("FULL_NAME"),
        rs.getDate("DATE_OF_BIRTH"),
        rs.getString("GENDER"),
        rs.getString("PLACE_OF_ORIGIN"),
        rs.getString("PLACE_OF_RESIDENCE"),
        rs.getDate("ISSUE_DATE"),
        rs.getDate("EXPIRY_DATE"),
        rs.getString("ISSUE_PLACE"),
        rs.getString("FRONT_IMAGE_URL"),
        rs.getString("BACK_IMAGE_URL"),
        rs.getString("FACE_IMAGE_URL"),
        rs.getBigDecimal("MATCH_SCORE"),
        rs.getString("VERIFIED_STATUS"),
        rs.getString("NOTE"),
        rs.getTimestamp("VERIFIED_AT"),
        rs.getTimestamp("CREATED_AT"),
        rs.getTimestamp("UPDATED_AT"),
        rs.getInt("IS_DELETED")
        );
    }
    
    public List<Ekyc> Pending() {
        List<Ekyc> list = new ArrayList<>();
        String sql = "SELECT * FROM EKYC WHERE VERIFIED_STATUS = 'PENDING'";
        try(Connection conn = ConnectionUtils.getMyConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while(rs.next())
            {
                list.add(mapRow(rs));
            }
        } catch(Exception e) {
            e.printStackTrace();
            
        }
        return list;
    }
    
    public boolean submit(Ekyc ekyc) {
        String sql = "INSERT INTO EKYC (USER_ID, ID_NUMBER, FULL_NAME, DATE_OF_BIRTH, GENDER, "
                   + "PLACE_OF_ORIGIN, PLACE_OF_RESIDENCE, ISSUE_DATE, EXPIRY_DATE, ISSUE_PLACE, "
                   + "FRONT_IMAGE_URL, BACK_IMAGE_URL, FACE_IMAGE_URL, VERIFIED_STATUS, CREATED_AT) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ekyc.getUserId());
            ps.setString(2, ekyc.getIdNumber());
            ps.setString(3, ekyc.getFullName());
            // Xử lý Date (kiểm tra null để tránh lỗi)
            ps.setDate(4, ekyc.getDateOfBirth() != null ? new java.sql.Date(ekyc.getDateOfBirth().getTime()) : null);
            ps.setString(5, ekyc.getGender());
            ps.setString(6, ekyc.getPlaceOfOrigin());
            ps.setString(7, ekyc.getPlaceOfResidence());
            ps.setDate(8, ekyc.getIssueDate() != null ? new java.sql.Date(ekyc.getIssueDate().getTime()) : null);
            ps.setDate(9, ekyc.getExpiryDate() != null ? new java.sql.Date(ekyc.getExpiryDate().getTime()) : null);
            ps.setString(10, ekyc.getIssuePlace());
            ps.setString(11, ekyc.getFrontImageUrl());
            ps.setString(12, ekyc.getBackImageUrl());
            ps.setString(13, ekyc.getFaceImageUrl());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean approve(int kycId) {
        String sql = "UPDATE EKYC SET VERIFIED_STATUS = 'APPROVED', VERIFIED_AT = CURRENT_TIMESTAMP, "
                   + "UPDATED_AT = CURRENT_TIMESTAMP WHERE KYC_ID = ?";
        try(Connection conn = ConnectionUtils.getMyConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, kycId);
            return ps.executeUpdate() > 0;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean reject(int kycId, String reason) {
        String sql = "UPDATE EKYC SET VERIFIED_STATUS = 'REJECTED', NOTE = ?, VERIFIED_AT = CURRENT_TIMESTAMP, "
                + "UPDATED_AT = CURRENT_TIMESTAMP WHERE KYC_ID = ?";
        try(Connection conn = ConnectionUtils.getMyConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,reason);
            ps.setInt(2, kycId);
            return ps.executeUpdate() > 0;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
            
    /**
     * Returns the latest eKYC record for the user, or null if none exists.
     * @throws Exception 
     */
    public Ekyc getLatestByUserId(int userId) throws Exception {
        String sql = "SELECT kyc_id, user_id, id_number, full_name, date_of_birth, gender, "
                   + "  place_of_origin, place_of_residence, issue_date, expiry_date, "
                   + "  issue_place, front_image_url, back_image_url, face_image_url, "
                   + "  match_score, verified_status, note, verified_at, created_at, "
                   + "  updated_at, is_deleted "
                   + "FROM EKYC "
                   + "WHERE user_id = ? AND is_deleted = 0 "
                   + "ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserts a new PENDING eKYC record (stub — image upload handled separately).
     * Returns generated kyc_id or -1 on failure.
     */
    public int submitKyc(int userId, String idNumber, String fullName, LocalDate dob, String gender) {
        String sql = "INSERT INTO EKYC (user_id, id_number, full_name, date_of_birth, gender, "
                   + "  verified_status, is_deleted) "
                   + "VALUES (?, ?, ?, ?, ?, 'PENDING', 0)";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"KYC_ID"})) {
            ps.setInt(1, userId);
            ps.setString(2, idNumber);
            ps.setString(3, fullName);
            ps.setDate(4, Date.valueOf(dob));
            ps.setString(5, gender);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }


}
