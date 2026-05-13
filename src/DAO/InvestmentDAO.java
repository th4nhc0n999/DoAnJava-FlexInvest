package DAO;

import ConnectDB.ConnectionOracle;
import Model.Investment;
import Model.Payout;
import Model.SavingsProduct;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * InvestmentDAO — truy vấn DB cho bảng INVESTMENT, PAYOUT, SAVINGS_PRODUCT.
 */
public class InvestmentDAO {

    // =========================================================================
    //  Mapping
    // =========================================================================

    private Investment mapInvestment(ResultSet rs) throws SQLException {
        Investment inv = new Investment();
        inv.setInvestmentId(rs.getInt("investment_id"));
        inv.setUserId(rs.getInt("user_id"));
        inv.setProductId(rs.getInt("product_id"));
        inv.setInvestedAmount(rs.getBigDecimal("invested_amount"));
        inv.setAppliedInterestRate(rs.getBigDecimal("applied_interest_rate"));
        inv.setStartDate(rs.getDate("start_date"));
        inv.setMaturityDate(rs.getDate("maturity_date")); // null cho Flex-Safe
        inv.setStatus(rs.getString("status"));
        inv.setIsDeleted(rs.getInt("is_deleted"));
        return inv;
    }

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
        // start_date / end_date có thể null
        Date sd = rs.getDate("start_date");
        Date ed = rs.getDate("end_date");
        p.setStartDate(sd != null ? sd.toLocalDate() : null);
        p.setEndDate(ed != null ? ed.toLocalDate() : null);
        p.setIsDeleted(rs.getInt("is_deleted"));
        return p;
    }

    private Payout mapPayout(ResultSet rs) throws SQLException {
        Payout pay = new Payout();
        pay.setPayoutId(rs.getInt("payout_id"));
        pay.setInvestmentId(rs.getInt("investment_id"));
        pay.setTransactionId(rs.getInt("transaction_id"));
        pay.setPayoutType(rs.getString("payout_type"));
        Date pd = rs.getDate("payout_date");
        pay.setPayoutDate(pd != null ? pd.toLocalDate() : null);
        pay.setPayoutAmount(rs.getBigDecimal("payout_amount"));
        pay.setIsDeleted(rs.getInt("is_deleted"));
        return pay;
    }

    // =========================================================================
    //  SAVINGS_PRODUCT
    // =========================================================================

    public SavingsProduct getProductById(int productId) {
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProduct(rs);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getProductById] " + e.getMessage());
        }
        return null;
    }

    /** Lấy gói Flex-Safe (term=0, ACTIVE) — dùng cho rollover mặc định QĐ9. */
    public SavingsProduct getFlexSafeProduct() {
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE term = 0 AND status = 'ACTIVE' AND is_deleted = 0 AND ROWNUM = 1";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapProduct(rs);
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getFlexSafeProduct] " + e.getMessage());
        }
        return null;
    }

    public List<SavingsProduct> getAllActiveProducts() {
        List<SavingsProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM SAVINGS_PRODUCT WHERE status = 'ACTIVE' AND is_deleted = 0 ORDER BY product_id";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduct(rs));
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getAllActiveProducts] " + e.getMessage());
        }
        return list;
    }

    public boolean hasActiveInvestments(int productId) {
        String sql = "SELECT COUNT(*) FROM INVESTMENT WHERE product_id = ? AND status = 'ACTIVE' AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.hasActiveInvestments] " + e.getMessage());
        }
        return false;
    }

    public int insertProduct(SavingsProduct p) {
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
            System.err.println("[InvestmentDAO.insertProduct] " + e.getMessage());
        }
        return -1;
    }

    public boolean updateProduct(SavingsProduct p) {
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
            System.err.println("[InvestmentDAO.updateProduct] " + e.getMessage());
        }
        return false;
    }

    public boolean toggleProductStatus(int productId, String newStatus) {
        String sql = "UPDATE SAVINGS_PRODUCT SET status = ? WHERE product_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.toggleProductStatus] " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    //  INVESTMENT
    // =========================================================================

    public Investment getById(int investmentId) {
        String sql = "SELECT * FROM INVESTMENT WHERE investment_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, investmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapInvestment(rs);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getById] " + e.getMessage());
        }
        return null;
    }

    public List<Investment> getByUserId(int userId) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM INVESTMENT WHERE user_id = ? AND is_deleted = 0 ORDER BY investment_id DESC";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvestment(rs));
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getByUserId] " + e.getMessage());
        }
        return list;
    }

    /**
     * Chỉ lấy Investment đang ACTIVE của user.
     * Dùng trong MyInvestmentsPanel để hiển thị danh sách đang chạy.
     */
    public List<Investment> getActiveByUserId(int userId) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM INVESTMENT WHERE user_id = ? AND status = 'ACTIVE' AND is_deleted = 0 ORDER BY investment_id DESC";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvestment(rs));
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getActiveByUserId] " + e.getMessage());
        }
        return list;
    }

    /**
     * Tổng số tiền đang ACTIVE để hiển thị card "Tổng đang tích lũy" trên Dashboard.
     */
    public BigDecimal getTotalActiveAmount(int userId) {
        String sql = "SELECT NVL(SUM(invested_amount), 0) FROM INVESTMENT WHERE user_id = ? AND status = 'ACTIVE' AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getTotalActiveAmount] " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Lấy Investment sắp đáo hạn trong vòng {@code days} ngày tới của user.
     * Dùng cho "Gói sắp đáo hạn" trên CustomerDashboard và highlight MyInvestmentsPanel.
     *
     * @param userId ID user
     * @param days   Số ngày (ví dụ: 7 cho Dashboard, 3 cho highlight vàng)
     */
    public List<Investment> getMaturitySoon(int userId, int days) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM INVESTMENT "
                   + "WHERE user_id = ? AND status = 'ACTIVE' AND is_deleted = 0"
                   + "  AND maturity_date IS NOT NULL"
                   + "  AND maturity_date BETWEEN SYSDATE AND SYSDATE + ?"
                   + " ORDER BY maturity_date ASC";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvestment(rs));
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getMaturitySoon] " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy tất cả các khoản đầu tư đáo hạn hôm nay (hoặc đã quá hạn nhưng chưa xử lý).
     * Dùng cho việc chạy batch tự động.
     */
    public List<Investment> getMaturedInvestmentsToday() {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM INVESTMENT "
                   + "WHERE status = 'ACTIVE' AND is_deleted = 0"
                   + "  AND maturity_date IS NOT NULL"
                   + "  AND TRUNC(maturity_date) <= TRUNC(SYSDATE)";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapInvestment(rs));
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getMaturedInvestmentsToday] " + e.getMessage());
        }
        return list;
    }

    /** Tất cả Investment ACTIVE của một gói (dùng cho dailyFlexSafeAccrual). */
    public List<Investment> getActiveByProductId(int productId) {
        List<Investment> list = new ArrayList<>();
        String sql = "SELECT * FROM INVESTMENT WHERE product_id = ? AND status = 'ACTIVE' AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvestment(rs));
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getActiveByProductId] " + e.getMessage());
        }
        return list;
    }

    /**
     * Insert Investment mới.
     * @return investment_id được sinh ra, hoặc -1 nếu thất bại.
     */
    public int insert(int userId, int productId, BigDecimal amount,
                      BigDecimal appliedRate, LocalDate startDate, LocalDate maturityDate) {
        String sql = """
            INSERT INTO INVESTMENT
              (user_id, product_id, invested_amount, applied_interest_rate,
               start_date, maturity_date, status, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 0)
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"investment_id"})) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, appliedRate);
            ps.setDate(5, Date.valueOf(startDate));
            ps.setDate(6, maturityDate != null ? Date.valueOf(maturityDate) : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.insert] " + e.getMessage());
        }
        return -1;
    }

    /** Cập nhật invested_amount (dùng khi Flex-Safe accrual daily). */
    public boolean updateAmount(int investmentId, BigDecimal newAmount) {
        String sql = "UPDATE INVESTMENT SET invested_amount = ? WHERE investment_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newAmount);
            ps.setInt(2, investmentId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.updateAmount] " + e.getMessage());
        }
        return false;
    }

    /** Chuyển trạng thái Investment (ACTIVE → REDEEMED / MATURED / CANCELLED). */
    public boolean updateStatus(int investmentId, String status) {
        String sql = "UPDATE INVESTMENT SET status = ? WHERE investment_id = ? AND is_deleted = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, investmentId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.updateStatus] " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    //  PAYOUT
    // =========================================================================

    /**
     * Ghi bản ghi PAYOUT.
     * @return payout_id mới, hoặc -1 nếu thất bại.
     */
    public int insertPayout(int investmentId, int transactionId,
                             String payoutType, LocalDate payoutDate, BigDecimal amount) {
        String sql = """
            INSERT INTO PAYOUT
              (investment_id, transaction_id, payout_type, payout_date, payout_amount, is_deleted)
            VALUES (?, ?, ?, ?, ?, 0)
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"payout_id"})) {
            ps.setInt(1, investmentId);
            ps.setInt(2, transactionId);
            ps.setString(3, payoutType);
            ps.setDate(4, Date.valueOf(payoutDate));
            ps.setBigDecimal(5, amount);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.insertPayout] " + e.getMessage());
        }
        return -1;
    }

    public List<Payout> getPayoutsByInvestment(int investmentId) {
        List<Payout> list = new ArrayList<>();
        String sql = "SELECT * FROM PAYOUT WHERE investment_id = ? AND is_deleted = 0 ORDER BY payout_date";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, investmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPayout(rs));
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.getPayoutsByInvestment] " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    //  EARLY_REDEMPTION_EVENT
    // =========================================================================

    /**
     * Ghi sự kiện tất toán sớm.
     * @return event_id mới, hoặc -1 nếu thất bại.
     */
    public int insertEarlyRedemptionEvent(int investmentId, Integer policyId,
                                           LocalDate redemptionDate,
                                           BigDecimal originalAmount,
                                           BigDecimal penaltyAmount,
                                           BigDecimal actualPayout,
                                           int transactionId) {
        String sql = """
            INSERT INTO EARLY_REDEMPTION_EVENT
              (investment_id, policy_id, redemption_date,
               original_amount, penalty_amount, actual_payout,
               transaction_id, created_at, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, 0)
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"event_id"})) {
            ps.setInt(1, investmentId);
            if (policyId != null) ps.setInt(2, policyId); else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(redemptionDate));
            ps.setBigDecimal(4, originalAmount);
            ps.setBigDecimal(5, penaltyAmount);
            ps.setBigDecimal(6, actualPayout);
            ps.setInt(7, transactionId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[InvestmentDAO.insertEarlyRedemptionEvent] " + e.getMessage());
        }
        return -1;
    }
}
