package DAO;

import ConnectDB.ConnectionOracle;
import Model.SavingsProduct;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SavingsProductDAO {

    // =========================================================================
    // Column mapping helper
    // =========================================================================

    /**
     * Ánh xạ một hàng ResultSet → SavingsProduct.
     * Dùng chung cho tất cả các query trong DAO này.
     */
    private SavingsProduct mapRow(ResultSet rs) throws SQLException {
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

        // start_date / end_date có thể NULL (gói Flex-Safe không có cửa sổ ngày)
        Date sd = rs.getDate("start_date");
        Date ed = rs.getDate("end_date");
        p.setStartDate(sd != null ? sd.toLocalDate() : null);
        p.setEndDate(ed != null ? ed.toLocalDate() : null);

        p.setIsDeleted(rs.getInt("is_deleted"));
        return p;
    }

    // =========================================================================
    // READ — cơ bản
    // =========================================================================

    /**
     * Lấy toàn bộ sản phẩm chưa xoá (is_deleted = 0), kể cả INACTIVE.
     * Dùng cho màn hình quản trị.
     */
    public List<SavingsProduct> findAll() {
        List<SavingsProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE is_deleted = 0 ORDER BY product_id";
        try (Connection con = ConnectionOracle.getOracleConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(mapRow(rs));
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
            while (rs.next())
                list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findActive] " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy một sản phẩm theo ID (chưa xoá).
     * 
     * @return SavingsProduct hoặc {@code null} nếu không tìm thấy.
     */
    public SavingsProduct findById(int productId) {
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findById] " + e.getMessage());
        }
        return null;
    }

    public List<SavingsProduct> findOpenToday() {
        LocalDate today = LocalDate.now();
        java.sql.Date sqlToday = java.sql.Date.valueOf(today);

        String sql =
                // [1] Flex-Safe — luôn mở
                "SELECT * FROM SAVINGS_PRODUCT "
                        + "WHERE status = 'ACTIVE' AND is_deleted = 0 AND start_date IS NULL AND end_date IS NULL "
                        + "UNION ALL "
                        // [2] Flex-Sale — mở vào ngày chẵn (ngày đôi) trong cửa sổ
                        + "SELECT * FROM SAVINGS_PRODUCT "
                        + "WHERE status = 'ACTIVE' AND is_deleted = 0 "
                        + "  AND (start_date IS NOT NULL OR end_date IS NOT NULL) "
                        + "  AND UPPER(product_name) NOT LIKE '%HOLIDAY%' "
                        + "  AND (start_date IS NULL OR start_date <= ?) "
                        + "  AND (end_date   IS NULL OR end_date   >= ?) "
                        + "  AND MOD(TO_NUMBER(TO_CHAR(SYSDATE,'DD')), 2) = 0 "
                        + "UNION ALL "
                        // [3] Flex-Holiday — mở vào ngày lễ trong cửa sổ
                        + "SELECT * FROM SAVINGS_PRODUCT "
                        + "WHERE status = 'ACTIVE' AND is_deleted = 0 "
                        + "  AND (start_date IS NOT NULL OR end_date IS NOT NULL) "
                        + "  AND UPPER(product_name) LIKE '%HOLIDAY%' "
                        + "  AND (start_date IS NULL OR start_date <= ?) "
                        + "  AND (end_date   IS NULL OR end_date   >= ?) "
                        + "  AND TO_CHAR(SYSDATE,'MM-DD') IN (" + buildHolidayLiteral() + ") "
                        + "ORDER BY product_id";

        List<SavingsProduct> list = new ArrayList<>();
        try (Connection con = ConnectionOracle.getOracleConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, sqlToday);
            ps.setDate(2, sqlToday);
            ps.setDate(3, sqlToday);
            ps.setDate(4, sqlToday);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.findOpenToday] " + e.getMessage());
        }
        return list;
    }

    public boolean isOpenToday(int productId) {
        SavingsProduct p = findById(productId);
        return p != null && isOpenToday(p);
    }

    public boolean isOpenToday(SavingsProduct p) {
        if (p == null)
            return false;

        // [1] Phải ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(p.getStatus()))
            return false;

        LocalDate today = LocalDate.now();

        // [2] Không có cửa sổ ngày → Flex-Safe, luôn mở
        if (!p.hasDateWindow())
            return true;

        // [3] Kiểm tra cửa sổ ngày [startDate, endDate]
        boolean afterStart = (p.getStartDate() == null) || !today.isBefore(p.getStartDate());
        boolean beforeEnd = (p.getEndDate() == null) || !today.isAfter(p.getEndDate());
        if (!afterStart || !beforeEnd)
            return false;

        // [4] Trong cửa sổ ngày → phân loại theo tên sản phẩm
        String upperName = p.getProductName() == null ? "" : p.getProductName().toUpperCase();
        if (upperName.contains("HOLIDAY")) {
            return isPublicHoliday(today);
        } else {
            return isEvenDay(today);
        }
    }

    public static boolean isPublicHoliday(LocalDate date) {
        if (date == null)
            return false;
        // Format MM-DD để so sánh không phụ thuộc năm
        String mmdd = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
        for (String h : VIETNAM_PUBLIC_HOLIDAYS) {
            if (h.equals(mmdd))
                return true;
        }
        return false;
    }

    /**
     * Danh sách ngày lễ quốc gia Việt Nam dạng "MM-DD".
     * Cập nhật khi có thay đổi nghị định.
     */
    private static final String[] VIETNAM_PUBLIC_HOLIDAYS = {
            "01-01", "04-18", "04-30", "05-01", "09-02", "09-03"
    };

    public static boolean isEvenDay(LocalDate date) {
        return date != null && date.getDayOfMonth() % 2 == 0;
    }

    public List<SavingsProduct> findOpenTodayInMemory() {
        return findActive().stream().filter(this::isOpenToday).collect(Collectors.toList());
    }

    public int insert(SavingsProduct p) {
        String sql = "INSERT INTO SAVINGS_PRODUCT "
                + "(product_name, interest_rate, term, min_investment_amount, max_investment_amount, "
                + " penalty_rate, fallback_interest_rate, min_holding_days, status, currency, "
                + " start_date, end_date, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection con = ConnectionOracle.getOracleConnection();
                PreparedStatement ps = con.prepareStatement(sql, new String[] { "product_id" })) {
            ps.setString(1, p.getProductName());
            ps.setBigDecimal(2, p.getInterestRate());
            ps.setInt(3, p.getTerm());
            ps.setBigDecimal(4, p.getMinInvestmentAmount());
            ps.setBigDecimal(5, p.getMaxInvestmentAmount());
            ps.setBigDecimal(6, p.getPenaltyRate());
            ps.setBigDecimal(7, p.getFallbackInterestRate());
            ps.setInt(8, p.getMinHoldingDays());
            ps.setString(9, p.getStatus());
            ps.setString(10, p.getCurrency());
            ps.setDate(11, p.getStartDate() != null ? java.sql.Date.valueOf(p.getStartDate()) : null);
            ps.setDate(12, p.getEndDate() != null ? java.sql.Date.valueOf(p.getEndDate()) : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.insert] " + e.getMessage());
        }
        return -1;
    }

    public boolean update(SavingsProduct p) {
        String sql = "UPDATE SAVINGS_PRODUCT SET "
                + "product_name = ?, interest_rate = ?, term = ?, "
                + "min_investment_amount = ?, max_investment_amount = ?, "
                + "penalty_rate = ?, fallback_interest_rate = ?, min_holding_days = ?, "
                + "status = ?, currency = ?, start_date = ?, end_date = ? "
                + "WHERE product_id = ? AND is_deleted = 0";
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
            ps.setString(9, p.getStatus());
            ps.setString(10, p.getCurrency());
            ps.setDate(11, p.getStartDate() != null ? java.sql.Date.valueOf(p.getStartDate()) : null);
            ps.setDate(12, p.getEndDate() != null ? java.sql.Date.valueOf(p.getEndDate()) : null);
            ps.setInt(13, p.getProductId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[SavingsProductDAO.update] " + e.getMessage());
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

    private static String buildHolidayLiteral() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VIETNAM_PUBLIC_HOLIDAYS.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append('\'').append(VIETNAM_PUBLIC_HOLIDAYS[i]).append('\'');
        }
        return sb.toString();
    }
}
