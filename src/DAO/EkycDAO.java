/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DAO;

import ConnectDB.ConnectionUtils;
import Model.Ekyc;
import java.sql.*;
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
        rs.getInt("MATCH_SCORE"),
        rs.getString("VERIFIED_STATUS"),
        rs.getString("NOTE"),
        rs.getTimestamp("VERIFIED_AT") != null ? rs.getTimestamp("VERIFIED_AT").toLocalDateTime() : null,
        rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null,
        rs.getTimestamp("UPDATED_AT") != null ? rs.getTimestamp("UPDATED_AT").toLocalDateTime() : null,
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
            
    
}
