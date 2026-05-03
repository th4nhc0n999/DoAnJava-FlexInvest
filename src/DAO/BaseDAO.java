package DAO;

import ConnectDB.ConnectionUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseDAO<T> {

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    protected Connection getConnection() throws SQLException {
        try {
            return ConnectionUtils.getMyConnection();
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found", e);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping — mỗi DAO con bắt buộc tự implement
    // -------------------------------------------------------------------------

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // -------------------------------------------------------------------------
    // Bind params
    // -------------------------------------------------------------------------

    private void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    protected List<T> queryList(String sql, Object... params) {
        List<T> list = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".queryList: " + e);
        }
        return list;
    }

    protected T queryOne(String sql, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".queryOne: " + e);
        }
        return null;
    }

    protected int queryCount(String sql, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".queryCount: " + e);
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Update helpers
    // -------------------------------------------------------------------------

    protected boolean executeUpdate(String sql, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".executeUpdate: " + e);
            return false;
        }
    }

    protected int executeInsertGetId(String sql, String[] keyColumns, Object... params) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, keyColumns)) {
            bindParams(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println(getClass().getSimpleName() + ".executeInsertGetId: " + e);
        }
        return -1;
    }
}
