package DAO;

import ConnectDB.ConnectionOracle;
import Model.SavingsProduct;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SavingsProductDAO — quản lý bảng SAVINGS_PRODUCT.
 */
public class SavingsProductDAO {

    // =========================================================================
    //  Mapping
    // =========================================================================

    private SavingsProduct mapProduct(ResultSet rs) throws SQLException {
        SavingsProduct p = new SavingsProduct();
        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setInterestRate(rs.getBigDecimal("interest_rate"));
        p.setTerm(rs.getInt("term"));
        p.setMinInvestmentAmount(rs.getBigDecimal("min_investment_amount"));
        p.setMaxInvestmentAmount(rs.getBigDecimal("max_investment_amount"));
        p.setPenaltyRate(rs.getBigDecimal("penalty_rate"));
        p.setFallbackInterestRate(rs.getBigDecimal("fallback_interest_rate"));
        p.setMinHoldingDays(rs.getInt("min_holding_days"));
        p.setStatus(rs.getString("status"));
        p.setCurrency(rs.getString("currency"));
        Date sd = rs.getDate("start_date");
        Date ed = rs.getDate("end_date");
        p.setStartDate(sd != null ? sd.toLocalDate() : null);
        p.setEndDate(ed != null ? ed.toLocalDate() : null);
        p.setIsDeleted(rs.getInt("is_deleted"));
        return p;
    }

    // =========================================================================
    //  Queries
    // =========================================================================

    public List<SavingsProduct> findAll() {
        List<SavingsProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE is_deleted = 0 ORDER BY product_id";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduct(rs));
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findAll] " + e.getMessage());
        }
        return list;
    }

    public List<SavingsProduct> findActive() {
        List<SavingsProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE status = 'ACTIVE' AND is_deleted = 0 ORDER BY product_id";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduct(rs));
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findActive] " + e.getMessage());
        }
        return list;
    }

    public SavingsProduct findById(int productId) {
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findById] " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    //  Insert / Update / Delete
    // =========================================================================

    public int insert(SavingsProduct p) {
        String sql = """
            INSERT INTO SAVINGS_PRODUCT
              (product_name, interest_rate, term, min_investment_amount, max_investment_amount,
               penalty_rate, fallback_interest_rate, min_holding_days, status, currency,
               start_date, end_date, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"product_id"})) {
            ps.setString(1, p.getProductName());
            ps.setBigDecimal(2, p.getInterestRate());
            ps.setInt(3, p.getTerm());
            ps.setBigDecimal(4, p.getMinInvestmentAmount());
            ps.setBigDecimal(5, p.getMaxInvestmentAmount());
            ps.setBigDecimal(6, p.getPenaltyRate());
            ps.setBigDecimal(7, p.getFallbackInterestRate());
            ps.setInt(8, p.getMinHoldingDays());
            ps.setString(9, p.getStatus() != null ? p.getStatus() : "ACTIVE");
            ps.setString(10, p.getCurrency());
            ps.setDate(11, p.getStartDate() != null ? Date.valueOf(p.getStartDate()) : null);
            ps.setDate(12, p.getEndDate() != null ? Date.valueOf(p.getEndDate()) : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.insert] " + e.getMessage());
        }
        return -1;
    }

    public boolean update(SavingsProduct p) {
        String sql = """
            UPDATE SAVINGS_PRODUCT SET
              product_name=?, interest_rate=?, term=?, min_investment_amount=?, max_investment_amount=?,
              penalty_rate=?, fallback_interest_rate=?, min_holding_days=?, currency=?,
              start_date=?, end_date=?
            WHERE product_id=? AND is_deleted=0
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setBigDecimal(2, p.getInterestRate());
            ps.setInt(3, p.getTerm());
            ps.setBigDecimal(4, p.getMinInvestmentAmount());
            ps.setBigDecimal(5, p.getMaxInvestmentAmount());
            ps.setBigDecimal(6, p.getPenaltyRate());
            ps.setBigDecimal(7, p.getFallbackInterestRate());
            ps.setInt(8, p.getMinHoldingDays());
            ps.setString(9, p.getCurrency());
            ps.setDate(10, p.getStartDate() != null ? Date.valueOf(p.getStartDate()) : null);
            ps.setDate(11, p.getEndDate() != null ? Date.valueOf(p.getEndDate()) : null);
            ps.setInt(12, p.getProductId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.update] " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int productId, String newStatus) {
        String sql = "UPDATE SAVINGS_PRODUCT SET status = ? WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.updateStatus] " + e.getMessage());
        }
        return false;
    }

    public boolean softDelete(int productId) {
        String sql = "UPDATE SAVINGS_PRODUCT SET is_deleted = 1 WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.softDelete] " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    //  isOpenToday
    // =========================================================================

    /**
     * Kiểm tra gói có mở bán hôm nay không dựa trên productId.
     */
    public boolean isOpenToday(int productId) {
        SavingsProduct p = findById(productId);
        return p != null && isOpenToday(p);
    }

    /**
     * Kiểm tra gói có mở bán hôm nay không.
     * Logic:
     * - Phải ACTIVE và is_deleted=0
     * - Nếu có start_date/end_date thì hôm nay phải nằm trong khoảng
     */
    public boolean isOpenToday(SavingsProduct p) {
        if (p == null || !"ACTIVE".equalsIgnoreCase(p.getStatus())) return false;
        LocalDate today = LocalDate.now();
        if (p.getStartDate() != null && today.isBefore(p.getStartDate())) return false;
        if (p.getEndDate() != null && today.isAfter(p.getEndDate())) return false;
        return true;
    }

    /**
     * Lọc trong bộ nhớ các gói ACTIVE đang mở hôm nay.
     */
    public List<SavingsProduct> findOpenTodayInMemory() {
        return findActive().stream()
                .filter(this::isOpenToday)
                .collect(Collectors.toList());
    }
}
