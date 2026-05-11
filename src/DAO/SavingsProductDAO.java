package DAO;

import Model.SavingsProduct;
import java.sql.*;

public class SavingsProductDAO extends BaseDAO<SavingsProduct> {

    @Override
    protected SavingsProduct mapRow(ResultSet rs) throws SQLException {
        SavingsProduct sp = new SavingsProduct();
        sp.setProductId(rs.getInt("product_id"));
        sp.setProductName(rs.getString("product_name"));
        sp.setInterestRate(rs.getBigDecimal("interest_rate"));
        sp.setTerm(rs.getInt("term"));
        sp.setMinInvestmentAmount(rs.getBigDecimal("min_investment_amount"));
        sp.setMaxInvestmentAmount(rs.getBigDecimal("max_investment_amount"));
        sp.setPenaltyRate(rs.getBigDecimal("penalty_rate"));
        sp.setFallbackInterestRate(rs.getBigDecimal("fallback_interest_rate"));
        sp.setMinHoldingDays(rs.getInt("min_holding_days"));
        sp.setStatus(rs.getString("status"));
        sp.setCurrency(rs.getString("currency"));
        Date sd = rs.getDate("start_date");
        if (sd != null) sp.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) sp.setEndDate(ed.toLocalDate());
        sp.setIsDeleted(rs.getInt("is_deleted"));
        return sp;
    }

    private static final String SELECT_COLS =
        "SELECT product_id, product_name, interest_rate, term, "
      + "  min_investment_amount, max_investment_amount, penalty_rate, "
      + "  fallback_interest_rate, min_holding_days, status, currency, "
      + "  start_date, end_date, is_deleted FROM SAVINGS_PRODUCT ";

    public java.util.List<SavingsProduct> getAllActive() {
        return queryList(
            SELECT_COLS + "WHERE status = 'ACTIVE' AND is_deleted = 0 ORDER BY product_id ASC"
        );
    }

    public SavingsProduct getById(int productId) {
        return queryOne(
            SELECT_COLS + "WHERE product_id = ? AND is_deleted = 0",
            productId
        );
    }
}
