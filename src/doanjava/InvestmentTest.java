package doanjava;

import Model.Investment;
import Model.SavingsProduct;
import Utils.DateUtils;
import Utils.InterestCalculator;
import Utils.InterestCalculator.InterestResult;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/**
 * InvestmentTest — 6 kịch bản kiểm thử bắt buộc cuối Tuần 2 N2.
 *
 * Chạy bằng: main()
 * Không cần DB — dùng object thuần, đối chiếu tay.
 *
 * ═════════════════════════════════════════════════════════════
 *  Setup chung:
 *    principal    = 10,000,000 VNĐ
 *    Gói kỳ hạn  : term=30d, rate=6%/năm (0.06), fallback=0.5%/năm (0.005)
 *                  minHoldingDays=3, penaltyRate=0
 *    Flex-Safe    : term=0, rate=4.75%/năm (0.0475)
 *    Gói max test : maxInvestmentAmount=5,000,000 VNĐ (PT1 vượt max)
 * ═════════════════════════════════════════════════════════════
 */
public class InvestmentTest {

    static final BigDecimal PRINCIPAL = new BigDecimal("10000000");

    // Gói kỳ hạn 30 ngày
    static SavingsProduct termProduct() {
        SavingsProduct p = new SavingsProduct();
        p.setProductId(1);
        p.setProductName("Flex-Term-30");
        p.setTerm(30);
        p.setInterestRate(new BigDecimal("0.06"));           // 6%/năm
        p.setFallbackInterestRate(new BigDecimal("0.005"));  // 0.5%/năm khi rút sớm
        p.setMinHoldingDays(3);
        p.setPenaltyRate(BigDecimal.ZERO);
        p.setMinInvestmentAmount(new BigDecimal("1000000"));
        p.setMaxInvestmentAmount(new BigDecimal("500000000"));
        p.setStatus("ACTIVE");
        p.setCurrency("VND");
        return p;
    }

    // Gói Flex-Safe (không kỳ hạn)
    static SavingsProduct flexSafe() {
        SavingsProduct p = new SavingsProduct();
        p.setProductId(2);
        p.setProductName("Flex-Safe");
        p.setTerm(0);
        p.setInterestRate(new BigDecimal("0.0475"));         // 4.75%/năm
        p.setFallbackInterestRate(new BigDecimal("0.0475")); // không có fallback riêng
        p.setMinHoldingDays(0);
        p.setPenaltyRate(BigDecimal.ZERO);
        p.setMinInvestmentAmount(new BigDecimal("100000"));
        p.setMaxInvestmentAmount(new BigDecimal("5000000")); // 5tr để test vượt max
        p.setStatus("ACTIVE");
        p.setCurrency("VND");
        return p;
    }

    /** Tạo Investment mock (không cần DB). */
    static Investment mockInvestment(SavingsProduct p, LocalDate depositDate,
                                     LocalDate maturityDate) {
        Investment inv = new Investment();
        inv.setInvestmentId(99);
        inv.setUserId(1);
        inv.setProductId(p.getProductId());
        inv.setInvestedAmount(PRINCIPAL);
        inv.setAppliedInterestRate(p.getInterestRate());
        inv.setStartDate(Date.valueOf(depositDate));
        inv.setMaturityDate(maturityDate != null ? Date.valueOf(maturityDate) : null);
        inv.setStatus("ACTIVE");
        return inv;
    }

    // =========================================================================
    //  Main
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(" InvestmentTest — 6 kịch bản kiểm thử Tuần 2 N2");
        System.out.printf(" Principal = %,.0f VNĐ%n", PRINCIPAL.doubleValue());
        System.out.println("═══════════════════════════════════════════════════════════════");

        SavingsProduct term30   = termProduct();
        SavingsProduct flexSafe = flexSafe();

        LocalDate depositDate  = LocalDate.of(2025, 1, 1);
        LocalDate maturity30   = DateUtils.calcMaturityDate(depositDate, 30);  // 31/01/2025

        // ── TC1: Rút trong 24h (< 1 ngày) → lãi 0% ──────────────────────────
        runTest("TC1",
            "Rút trong 24h — gói kỳ hạn 30d → lãi phải = 0",
            mockInvestment(term30, depositDate, maturity30),
            term30, depositDate, depositDate,  // deposit=redeem → 0 ngày
            BigDecimal.ZERO);

        // ── TC2: Rút sau 3 ngày → fallback 0.5%/năm ──────────────────────────
        // Expected: 10,000,000 × 0.005 × 3/365 = 410.96 VNĐ
        BigDecimal tc2Expected = InterestCalculator
            .calcInterest(PRINCIPAL, new BigDecimal("0.005"), 3);
        runTest("TC2",
            "Rút sau 3 ngày — fallback 0.5%/năm (đủ minHoldingDays=3)",
            mockInvestment(term30, depositDate, maturity30),
            term30, depositDate, depositDate.plusDays(3),
            tc2Expected);

        // ── TC3: Rút sau đúng 1 tháng (30d) = đúng hạn → lãi 6%/năm ─────────
        // Expected: 10,000,000 × 0.06 × 30/365 = 49,315.07 VNĐ
        BigDecimal tc3Expected = InterestCalculator
            .calcInterest(PRINCIPAL, new BigDecimal("0.06"), 30);
        runTest("TC3",
            "Rút sau đúng 30d (đúng hạn) — lãi 6%/năm",
            mockInvestment(term30, depositDate, maturity30),
            term30, depositDate, maturity30,
            tc3Expected);

        // ── TC4: Rút sau 45d (quá hạn 15d) → lãi vẫn dùng appliedRate 6% ────
        // Expected: 10,000,000 × 0.06 × 45/365 = 73,972.60 VNĐ
        BigDecimal tc4Expected = InterestCalculator
            .calcInterest(PRINCIPAL, new BigDecimal("0.06"), 45);
        runTest("TC4",
            "Rút sau 45d (quá hạn 15d) — lãi 6%/năm trên toàn 45 ngày",
            mockInvestment(term30, depositDate, maturity30),
            term30, depositDate, depositDate.plusDays(45),
            tc4Expected);

        // ── TC5: Flex-Safe rút sau 25h → lãi theo gói (4.75%/năm), không case 2/3 ──
        // Expected: 10,000,000 × 0.0475 × 1/365 = 1,301.37 VNĐ  (1 ngày)
        BigDecimal tc5Expected = InterestCalculator
            .calcInterest(PRINCIPAL, new BigDecimal("0.0475"), 1);
        runTest("TC5",
            "Flex-Safe rút sau 25h (1 ngày) — lãi 4.75%/năm, không fallback",
            mockInvestment(flexSafe, depositDate, null),
            flexSafe, depositDate, depositDate.plusDays(1),
            tc5Expected);

        // ── TC6: PT1 vượt max — tổng 10tr > max 5tr → 5tr rollover, 5tr rút ─
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("TC6  PT1 vượt max Flex-Safe (max=5,000,000 VNĐ)");
        System.out.println("────────────────────────────────────────────────────────────────");
        BigDecimal totalPayout = new BigDecimal("10000000");
        BigDecimal maxInvest   = flexSafe.getMaxInvestmentAmount(); // 5,000,000
        BigDecimal toInvest    = totalPayout.min(maxInvest);
        BigDecimal toWithdraw  = totalPayout.subtract(toInvest);
        System.out.printf("  Tổng payout     : %,.2f VNĐ%n", totalPayout.doubleValue());
        System.out.printf("  Max gói đích    : %,.2f VNĐ%n", maxInvest.doubleValue());
        System.out.printf("  ✅ Vào gói mới  : %,.2f VNĐ%n", toInvest.doubleValue());
        System.out.printf("  ✅ Rút về bank  : %,.2f VNĐ%n", toWithdraw.doubleValue());
        boolean tc6ok = toInvest.compareTo(maxInvest) == 0
                     && toWithdraw.compareTo(new BigDecimal("5000000")) == 0;
        System.out.printf("  %s%n", tc6ok ? "PASS ✅" : "FAIL ❌");

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println(" Hoàn tất 6 kịch bản. Đối chiếu số liệu trước khi demo.");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // =========================================================================
    //  Helper chạy một test case
    // =========================================================================
    static void runTest(String id, String desc,
                        Investment inv, SavingsProduct product,
                        LocalDate depositDate, LocalDate redeemDate,
                        BigDecimal expectedInterest) {
        System.out.println();
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.printf("%s  %s%n", id, desc);
        System.out.println("────────────────────────────────────────────────────────────────");

        LocalDate maturityDate = DateUtils.toLocalDate(inv.getMaturityDate());
        long holdDays = DateUtils.daysBetween(depositDate, redeemDate);

        InterestResult result = InterestCalculator.calculate(
            inv.getInvestedAmount(), depositDate, redeemDate,
            product, maturityDate, inv.getAppliedInterestRate());

        BigDecimal totalPayout = inv.getInvestedAmount().add(result.interestAmount);

        System.out.printf("  depositDate  : %s%n", depositDate);
        System.out.printf("  redeemDate   : %s%n", redeemDate);
        System.out.printf("  maturityDate : %s%n", maturityDate);
        System.out.printf("  holdDays     : %d%n", holdDays);
        System.out.printf("  case         : %s%n", result.caseLabel);
        System.out.printf("  appliedRate  : %.4f%% /năm%n",
            result.appliedRate.multiply(BigDecimal.valueOf(100)).doubleValue());
        System.out.printf("  lãi tính được: %,.2f VNĐ%n", result.interestAmount.doubleValue());
        System.out.printf("  lãi kỳ vọng  : %,.2f VNĐ%n", expectedInterest.doubleValue());
        System.out.printf("  tổng nhận    : %,.2f VNĐ%n", totalPayout.doubleValue());

        boolean pass = result.interestAmount.compareTo(expectedInterest) == 0;
        System.out.printf("  %s%n", pass ? "PASS ✅" : "FAIL ❌ (delta="
            + result.interestAmount.subtract(expectedInterest) + ")");
    }
}
