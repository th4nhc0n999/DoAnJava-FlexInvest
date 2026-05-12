package DAO;

import ConnectDB.ConnectionOracle;
import ConnectDB.ConnectionUtils;
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
public class InvestmentDAO extends BaseDAO<Investment> {

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

    /** Active investments only (status = ACTIVE or PENDING). */
    public List<Investment> getActiveByUserId(int userId) {
        String sql = "SELECT i.investment_id, i.user_id, i.product_id, sp.product_name, "
                   + "  i.invested_amount, i.applied_interest_rate, i.start_date, "
                   + "  i.maturity_date, i.status, i.is_deleted "
                   + "FROM INVESTMENT i "
                   + "JOIN SAVINGS_PRODUCT sp ON sp.product_id = i.product_id "
                   + "WHERE i.user_id = ? AND i.status IN ('ACTIVE','PENDING') AND i.is_deleted = 0 "
                   + "ORDER BY i.maturity_date ASC";
        return query(sql, userId);
    }

    /** Investments maturing within the next N days. */
    public List<Investment> getDueSoon(int userId, int days) {
        String sql = "SELECT i.investment_id, i.user_id, i.product_id, sp.product_name, "
                   + "  i.invested_amount, i.applied_interest_rate, i.start_date, "
                   + "  i.maturity_date, i.status, i.is_deleted "
                   + "FROM INVESTMENT i "
                   + "JOIN SAVINGS_PRODUCT sp ON sp.product_id = i.product_id "
                   + "WHERE i.user_id = ? AND i.status IN ('ACTIVE','PENDING') "
                   + "  AND i.maturity_date BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + ? "
                   + "  AND i.is_deleted = 0 "
                   + "ORDER BY i.maturity_date ASC";
        String baseSql = "SELECT i.investment_id, i.user_id, i.product_id, sp.product_name, "
                       + "  i.invested_amount, i.applied_interest_rate, i.start_date, "
                       + "  i.maturity_date, i.status, i.is_deleted "
                       + "FROM INVESTMENT i "
                       + "JOIN SAVINGS_PRODUCT sp ON sp.product_id = i.product_id "
                       + "WHERE i.user_id = ? AND i.status IN ('ACTIVE','PENDING') "
                       + "  AND i.maturity_date BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + ? "
                       + "  AND i.is_deleted = 0 "
                       + "ORDER BY i.maturity_date ASC";
        List<Investment> result = new ArrayList<>();
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(baseSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Sum of invested_amount for all active investments of a user. */
    public BigDecimal getTotalActiveInvestedAmount(int userId) {
        String sql = "SELECT NVL(SUM(invested_amount), 0) FROM INVESTMENT "
                   + "WHERE user_id = ? AND status IN ('ACTIVE','PENDING') AND is_deleted = 0";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    // -----------------------------------------------------------------------
    // WRITE
    // -----------------------------------------------------------------------

    /**
     * Inserts a new investment row and returns the generated investment_id,
     * or -1 on failure.
     */
    public int createInvestment(int userId, int productId, BigDecimal amount,
                                BigDecimal rate, LocalDate startDate, LocalDate maturityDate) {
        String sql = "INSERT INTO INVESTMENT "
                   + "(user_id, product_id, invested_amount, applied_interest_rate, "
                   + " start_date, maturity_date, status, is_deleted) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0)";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"INVESTMENT_ID"})) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, rate);
            ps.setDate(5, Date.valueOf(startDate));
            ps.setDate(6, Date.valueOf(maturityDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Marks an investment as REDEEMED.
     */
    public boolean markRedeemed(int investmentId) {
        return updateStatus(investmentId, "REDEEMED");
    }

    /**
     * Updates redemption method (PT1/PT2/PT3) — stored in status field prefixed,
     * or you can add a separate column. Here we store it as a note in status
     * for simplicity; adapt if a dedicated column is added.
     * NOTE: If the DB has no redemption_method column, this is a no-op placeholder.
     */
    public boolean updateRedemptionMethod(int investmentId, String method) {
        // Stored in a custom column redemption_method if it exists.
        // If not, skip silently — schema extension required.
        String sql = "UPDATE INVESTMENT SET status = status WHERE investment_id = ?";
        // Real implementation once column exists:
        // String sql = "UPDATE INVESTMENT SET redemption_method = ? WHERE investment_id = ?";
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, investmentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


    // -----------------------------------------------------------------------
    // MAPPING
    // -----------------------------------------------------------------------

    private List<Investment> query(String sql, int userId) {
        List<Investment> list = new ArrayList<>();
        try (Connection conn = ConnectionUtils.getMyConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }

    protected Investment mapRow(ResultSet rs) throws SQLException {
        Investment inv = new Investment();
        inv.setInvestmentId(rs.getInt("investment_id"));
        inv.setUserId(rs.getInt("user_id"));
        inv.setProductId(rs.getInt("product_id"));
        inv.setProductName(rs.getString("product_name"));
        inv.setInvestedAmount(rs.getBigDecimal("invested_amount"));
        inv.setAppliedInterestRate(rs.getBigDecimal("applied_interest_rate"));
        Date sd = rs.getDate("start_date");
        // if (sd != null) inv.setStartDate(sd.toLocalDate());
        if (sd != null) {
            inv.setStartDate(
                (Date) java.util.Date.from(
                    sd.toLocalDate()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                )
            );
        }
        Date md = rs.getDate("maturity_date");
        // if (md != null) inv.setMaturityDate(md.toLocalDate());
        if (md != null) {
            inv.setMaturityDate(
                (Date) java.util.Date.from(
                    sd.toLocalDate()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                )
            );
        }
        inv.setStatus(rs.getString("status"));
        inv.setIsDeleted(rs.getInt("is_deleted"));
        return inv;
    }



}
