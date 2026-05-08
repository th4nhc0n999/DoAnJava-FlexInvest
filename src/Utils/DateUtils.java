package Utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * DateUtils — tiện ích ngày tháng cho nghiệp vụ đầu tư FlexInvest.
 *
 * QĐ1: Tính maturityDate theo term (ngày dương lịch, không trừ thứ 7/CN).
 */
public class DateUtils {

    private DateUtils() {}

    /**
     * QĐ1: Tính ngày đáo hạn = startDate + term ngày dương lịch.
     *
     * @param startDate ngày bắt đầu đầu tư
     * @param termDays  kỳ hạn (ngày). 0 → Flex-Safe, trả về null.
     * @return ngày đáo hạn, hoặc null nếu Flex-Safe
     */
    public static LocalDate calcMaturityDate(LocalDate startDate, int termDays) {
        if (termDays <= 0) return null;           // Flex-Safe: không có đáo hạn
        return startDate.plusDays(termDays);
    }

    /**
     * Số ngày thực tế giữa hai ngày (inclusive depositDate, exclusive redeemDate).
     * Ví dụ: gửi ngày 1, rút ngày 1 → 0 ngày.
     *         gửi ngày 1, rút ngày 2 → 1 ngày.
     */
    public static long daysBetween(LocalDate depositDate, LocalDate redeemDate) {
        return ChronoUnit.DAYS.between(depositDate, redeemDate);
    }

    /**
     * Kiểm tra redeemDate có trước hay bằng maturityDate không.
     * Dùng để phân biệt rút sớm vs. tất toán đúng hạn.
     */
    public static boolean isEarlyRedemption(LocalDate redeemDate, LocalDate maturityDate) {
        if (maturityDate == null) return false;   // Flex-Safe không có early redemption
        return redeemDate.isBefore(maturityDate);
    }

    /**
     * Chuyển java.sql.Date sang LocalDate an toàn.
     */
    public static LocalDate toLocalDate(java.sql.Date sqlDate) {
        return (sqlDate == null) ? null : sqlDate.toLocalDate();
    }
}
