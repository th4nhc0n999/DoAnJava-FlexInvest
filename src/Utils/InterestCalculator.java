package Utils;

import Model.Investment;
import Model.SavingsProduct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.Date;

/**
 * InterestCalculator — Engine tính lãi suất cho FlexInvest (Tuần 1 N2).
 *
 * ═══════════════════════════════════════════════════════════
 *  QĐ áp dụng:
 *
 *  QĐ2 (Lãi cơ bản):
 *    interest = principal × rate × (days / 365)
 *
 *  QĐ3 (Rút sớm - gói có kỳ hạn):
 *    Case 1: holdDays < 1 ngày (< 24h)     → lãi = 0
 *    Case 2: holdDays < minHoldingDays      → lãi dùng fallbackRate
 *    Case 3: holdDays >= minHoldingDays
 *             nhưng < term                  → lãi dùng fallbackRate
 *
 *  QĐ4 (Rút đúng hạn / quá hạn):
 *    → lãi dùng appliedInterestRate (rate tại thời điểm mua)
 *
 *  QĐ5 (Flex-Safe, không kỳ hạn):
 *    - Không phân biệt rút sớm/đúng hạn.
 *    - Case 1: holdDays < 1 → lãi = 0
 *    - Khác: dùng product.interestRate (accrued daily).
 * ═══════════════════════════════════════════════════════════
 */
public class InterestCalculator {

    private InterestCalculator() {}

    // =========================================================================
    //  Result container
    // =========================================================================

    /** Kết quả tính lãi từ InterestCalculator. */
    public static class InterestResult {
        /** Lãi suất thực tế được áp dụng (năm, dạng thập phân 0.xx). */
        public final BigDecimal appliedRate;
        /** Số tiền lãi = principal × appliedRate × (days/365). */
        public final BigDecimal interestAmount;
        /** Số ngày thực tế nắm giữ. */
        public final long holdDays;
        /** Mô tả case áp dụng để debug/log. */
        public final String caseLabel;

        public InterestResult(BigDecimal appliedRate, BigDecimal interestAmount,
                              long holdDays, String caseLabel) {
            this.appliedRate    = appliedRate;
            this.interestAmount = interestAmount;
            this.holdDays       = holdDays;
            this.caseLabel      = caseLabel;
        }

        @Override
        public String toString() {
            return String.format(
                "[%s] holdDays=%d | rate=%.4f%% | interest=%,.2f VNĐ",
                caseLabel, holdDays,
                appliedRate.multiply(BigDecimal.valueOf(100)),
                interestAmount);
        }
    }

    // =========================================================================
    //  Entry point chính — được gọi từ InvestmentController
    // =========================================================================

    /**
     * Tính lãi cho một khoản đầu tư khi rút/tất toán.
     *
     * @param principal       Số tiền gốc
     * @param depositDate     Ngày bắt đầu gửi
     * @param redeemDate      Ngày rút / tất toán
     * @param product         Gói sản phẩm (chứa term, interestRate, fallbackRate, minHoldingDays)
     * @param maturityDate    Ngày đáo hạn (null nếu Flex-Safe)
     * @param appliedRateAtBuy Lãi suất gói tại thời điểm mua (khóa giá)
     * @return InterestResult chứa lãi suất áp dụng + số tiền lãi
     */
    public static InterestResult calculate(BigDecimal principal,
                                           LocalDate depositDate,
                                           LocalDate redeemDate,
                                           SavingsProduct product,
                                           LocalDate maturityDate,
                                           BigDecimal appliedRateAtBuy) {
        long holdDays = DateUtils.daysBetween(depositDate, redeemDate);

        // ── Case 1: < 24h (< 1 ngày) → lãi = 0 (áp dụng mọi gói) ──────────
        if (holdDays < 1) {
            return new InterestResult(
                BigDecimal.ZERO, BigDecimal.ZERO, holdDays, "CASE1_UNDER_24H");
        }

        // ── Flex-Safe (không kỳ hạn) → QĐ5 ─────────────────────────────────
        if (product.isFlexSafe()) {
            BigDecimal rate     = safeRate(appliedRateAtBuy, product.getInterestRate());
            BigDecimal interest = calcInterest(principal, rate, holdDays);
            return new InterestResult(rate, interest, holdDays, "FLEX_SAFE");
        }

        // ── Gói có kỳ hạn ────────────────────────────────────────────────────
        boolean isEarly = DateUtils.isEarlyRedemption(redeemDate, maturityDate);

        if (!isEarly) {
            // QĐ4: rút đúng hạn / quá hạn → dùng lãi suất đã khóa khi mua
            BigDecimal rate     = safeRate(appliedRateAtBuy, product.getInterestRate());
            BigDecimal interest = calcInterest(principal, rate, holdDays);
            return new InterestResult(rate, interest, holdDays, "ON_MATURITY");
        }

        // Rút sớm (holdDays >= 1 nhưng trước đáo hạn)
        int minDays  = product.getMinHoldingDays();
        BigDecimal fallback = safeRate(product.getFallbackInterestRate(), BigDecimal.ZERO);

        if (holdDays < minDays) {
            // Case 2: chưa đủ minHoldingDays → fallback rate
            BigDecimal interest = calcInterest(principal, fallback, holdDays);
            return new InterestResult(fallback, interest, holdDays, "CASE2_EARLY_BELOW_MIN");
        } else {
            // Case 3: đủ minHoldingDays nhưng chưa đến đáo hạn → fallback rate
            BigDecimal interest = calcInterest(principal, fallback, holdDays);
            return new InterestResult(fallback, interest, holdDays, "CASE3_EARLY_ABOVE_MIN");
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /**
     * Tính lãi đơn theo năm: principal × rate × (days / 365).
     * Scale 2 chữ số thập phân, làm tròn HALF_UP.
     */
    public static BigDecimal calcInterest(BigDecimal principal, BigDecimal annualRate, long days) {
        if (principal == null || annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0
                || days <= 0) {
            return BigDecimal.ZERO;
        }
        return principal
               .multiply(annualRate)
               .multiply(BigDecimal.valueOf(days))
               .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
    }

    /** Trả về primary nếu != null và > 0, ngược lại trả fallback. */
    private static BigDecimal safeRate(BigDecimal primary, BigDecimal fallback) {
        if (primary != null && primary.compareTo(BigDecimal.ZERO) > 0) return primary;
        return (fallback != null) ? fallback : BigDecimal.ZERO;
    }

    /**
     * Convenience overload: calculates interest with redeemDate = today.
     * Used in MyInvestmentsPanel to show estimated accrued interest.
     */
    public static BigDecimal calculateInterest(Investment investment,
                                               SavingsProduct product,
                                               LocalDate redeemDate) {
        Date start = investment.getStartDate();
        if (start == null || redeemDate == null) return BigDecimal.ZERO;

        LocalDate startLocal = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long heldDays = ChronoUnit.DAYS.between(startLocal, redeemDate);
        if (heldDays <= 0) return BigDecimal.ZERO;

        int minHolding = (product != null) ? product.getMinHoldingDays() : 0;
        BigDecimal principal = investment.getInvestedAmount();
        BigDecimal rate = investment.getAppliedInterestRate();

        Date maturity = investment.getMaturityDate();
        LocalDate maturityLocal = (maturity != null) ? maturity.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

        // Below minimum holding — use fallback rate
        if (heldDays < minHolding) {
            BigDecimal fallback = (product != null && product.getFallbackInterestRate() != null)
                    ? product.getFallbackInterestRate() : BigDecimal.ZERO;
            return calcInterest(principal, fallback, heldDays);
        }

        // Early redemption — apply penalty
        boolean isEarly = maturityLocal != null
                && redeemDate.isBefore(maturityLocal);
        if (isEarly && product != null && product.getPenaltyRate() != null) {
            BigDecimal effectiveRate = rate.subtract(product.getPenaltyRate());
            if (effectiveRate.compareTo(BigDecimal.ZERO) < 0) effectiveRate = BigDecimal.ZERO;
            return calcInterest(principal, effectiveRate, heldDays);
        }

        return calcInterest(principal, rate, heldDays);
    }
    public static BigDecimal calculateInterestForDeposit(Investment investment,
                                                         SavingsProduct product) {
        return calculateInterest(investment, product, LocalDate.now());
    }


}
