package DAO;

import ConnectDB.ConnectionUtils;
import Model.SysFunction;
import java.sql.*;
import java.util.*;

public class SysFunctionDAO {

    private SysFunction mapRow(ResultSet rs) throws SQLException {
        return new SysFunction(
            rs.getInt("FUNCTION_ID"),
            rs.getString("NAME_FUNCTION"),
            rs.getDate("CREATED_AT"),
            rs.getDate("UPDATED_AT")
        );
    }

    public List<SysFunction> getAll() {
        List<SysFunction> list = new ArrayList<>();
        String sql = "SELECT * FROM SYS_FUNCTION ORDER BY FUNCTION_ID";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("SysFunctionDAO.getAll: " + e);
        }
        return list;
    }

    public SysFunction getById(int functionId) {
        String sql = "SELECT * FROM SYS_FUNCTION WHERE FUNCTION_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, functionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("SysFunctionDAO.getById: " + e);
        }
        return null;
    }

    public boolean insert(SysFunction f) {
        String sql = "INSERT INTO SYS_FUNCTION (NAME_FUNCTION, CREATED_AT, UPDATED_AT) "
                   + "VALUES (?, SYSDATE, SYSDATE)";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, f.getNameFunction());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysFunctionDAO.insert: " + e);
            return false;
        }
    }

    public boolean update(SysFunction f) {
        String sql = "UPDATE SYS_FUNCTION SET NAME_FUNCTION = ?, UPDATED_AT = SYSDATE "
                   + "WHERE FUNCTION_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, f.getNameFunction());
            ps.setInt(2, f.getFunctionId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysFunctionDAO.update: " + e);
            return false;
        }
    }

    public boolean softDelete(int functionId) {
        // SYS_FUNCTION không có cột is_deleted — xóa thật nếu không có SYS_ROLE tham chiếu
        String sql = "DELETE FROM SYS_FUNCTION WHERE FUNCTION_ID = ?";
        try (Connection con = ConnectionUtils.getMyConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, functionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("SysFunctionDAO.softDelete: " + e);
            return false;
        }
    }
}
