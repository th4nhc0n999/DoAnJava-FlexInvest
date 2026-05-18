package Controller;

import ConnectDB.ConnectionOracle;
import DAO.InvestmentDAO;
import DAO.MissionDAO;
import DAO.NotificationDAO;
import DAO.TokenDAO;
import DAO.WalletDAO;
import Model.Mission;
import Model.UserMission;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * MissionController — Xử lý toàn bộ logic nhiệm vụ + thưởng token (QĐ13).
 *
 * Public methods (gọi từ WalletController sau khi approve):
 *  onDailyCheckIn(userId)       — điểm danh, cộng token theo streak
 *  onDeposit(userId, amount)    — trigger sau khi duyệt nạp tiền
 *  checkMonthlyBalance(userId)  — gọi cuối tháng
 *  checkTopDepositors()         — gọi cuối tháng, thưởng top 20
 *  onShareApp(userId)           — chia sẻ app (1 lần/tuần)
 *  onReferral(referrerId, newAccountId) — giới thiệu bạn
 */
public class MissionController {

    // ── Bảng thưởng điểm danh (ngày 1→7) ────────────────────────────────────
    private static final BigDecimal[] CHECK_IN_REWARDS = {
        new BigDecimal("5"),   // Ngày 1
        new BigDecimal("10"),  // Ngày 2
        new BigDecimal("15"),  // Ngày 3
        new BigDecimal("20"),  // Ngày 4
        new BigDecimal("30"),  // Ngày 5
        new BigDecimal("50"),  // Ngày 6
        new BigDecimal("75"),  // Ngày 7
    };

    // ── Ngưỡng thưởng nạp tiền trong tuần ────────────────────────────────────
    private static final BigDecimal WEEKLY_THRESHOLD_1 = new BigDecimal("100000");  // 100K → 100 token
    private static final BigDecimal WEEKLY_THRESHOLD_2 = new BigDecimal("500000");  // 500K → 750 token thêm
    private static final BigDecimal WEEKLY_BONUS_1     = new BigDecimal("100");
    private static final BigDecimal WEEKLY_BONUS_2     = new BigDecimal("750");

    // ── Ngưỡng thưởng số ngày nạp trong tháng ───────────────────────────────
    private static final int         MONTHLY_DAYS_TARGET = 10;           // 10 ngày/tháng
    private static final BigDecimal  MONTHLY_DAYS_BONUS  = new BigDecimal("1000");

    // ── Ngưỡng duy trì số dư tích lũy ───────────────────────────────────────
    private static final BigDecimal  BALANCE_THRESHOLD   = new BigDecimal("1000000"); // 1 triệu
    private static final BigDecimal  BALANCE_BONUS       = new BigDecimal("1500");

    // ── Top depositor cuối tháng ─────────────────────────────────────────────
    private static final int         TOP_DEPOSITOR_COUNT  = 20;
    private static final BigDecimal  TOP_DEPOSITOR_BONUS  = new BigDecimal("5000");

    // ── Share app ────────────────────────────────────────────────────────────
    private static final BigDecimal  SHARE_BONUS          = new BigDecimal("50");

    // ── Referral ─────────────────────────────────────────────────────────────
    private static final BigDecimal  REFERRAL_MIN_DEPOSIT = new BigDecimal("50000");
    private static final BigDecimal  REFERRAL_BONUS       = new BigDecimal("2000");

    private final TokenDAO          tokenDAO    = new TokenDAO();
    private final MissionDAO        missionDAO  = new MissionDAO();
    private final NotificationDAO   notifDAO    = new NotificationDAO();
    private final InvestmentDAO     investDAO   = new InvestmentDAO();
    private final WalletDAO         walletDAO   = new WalletDAO();

    // =========================================================================
    //  1. onDailyCheckIn() — Điểm danh hàng ngày (QĐ13)
    // =========================================================================

    /**
     * Xử lý điểm danh hàng ngày.
     *
     * Logic QĐ13:
     *  - Đã điểm danh hôm nay → false
     *  - Bỏ hôm qua → reset streak về 1 (ngày 1 = hôm nay)
     *  - Streak tiếp tục → cộng token theo bảng thưởng
     *  - Streak >= 7 → reset về 0 để bắt đầu vòng mới
     *
     * @return số token đã thưởng, hoặc -1 nếu đã điểm danh hôm nay
     */
    public BigDecimal onDailyCheckIn(int userId) {
        boolean ok = missionDAO.checkIn(userId);
        if (!ok) {
            System.out.printf("[MissionController.onDailyCheckIn] userId=%d đã điểm danh hôm nay.%n", userId);
            return BigDecimal.valueOf(-1);
        }

        int streak = missionDAO.getCheckInStreak(userId);
        // streak = 0 khi lần đầu hoặc vừa reset, gọi xong checkIn → streak đã là 1
        // Đảm bảo idx hợp lệ
        int idx = Math.max(0, Math.min(streak - 1, CHECK_IN_REWARDS.length - 1));
        BigDecimal reward = CHECK_IN_REWARDS[idx];

        boolean added = tokenDAO.addToken(userId, reward);
        if (added) {
            notifDAO.send(userId,
                "Điểm danh ngày " + streak + " thành công!",
                String.format("Bạn nhận được %s FlexToken. Chuỗi điểm danh: %d ngày.", reward, streak),
                "MISSION");
            System.out.printf("[MissionController.onDailyCheckIn] userId=%d streak=%d reward=%s%n",
                userId, streak, reward);
        }

        return added ? reward : BigDecimal.ZERO;
    }

    // =========================================================================
    //  2. onDeposit() — Trigger sau khi duyệt nạp tiền
    // =========================================================================

    /**
     * Được gọi từ WalletController.approveDeposit() sau khi nạp tiền thành công.
     *
     * Kiểm tra 2 loại mốc:
     *  A) Tổng nạp tuần này (WEEKLY missions):
     *     - Vượt 100K → thưởng 100 token (chỉ 1 lần)
     *     - Vượt 500K → thưởng thêm 750 token (chỉ 1 lần)
     *  B) Số ngày có nạp tiền trong tháng:
     *     - Đủ 10 ngày/tháng → thưởng 1.000 token (chỉ 1 lần)
     *
     * @param userId ID user vừa được duyệt nạp
     * @param amount Số tiền vừa nạp
     */
    public void onDeposit(int userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;

        // ── A. Tổng nạp tuần này ────────────────────────────────────────────
        BigDecimal weeklyTotal = getWeeklyDepositTotal(userId);
        BigDecimal prevTotal   = weeklyTotal.subtract(amount); // trước khi nạp

        // Mốc 100K
        if (prevTotal.compareTo(WEEKLY_THRESHOLD_1) < 0
                && weeklyTotal.compareTo(WEEKLY_THRESHOLD_1) >= 0) {
            if (!hasClaimedWeeklyMission(userId, "WEEKLY_100K")) {
                tokenDAO.addToken(userId, WEEKLY_BONUS_1);
                markWeeklyMissionClaimed(userId, "WEEKLY_100K");
                notifDAO.send(userId,
                    "Nhiệm vụ tuần: Nạp 100K ✅",
                    "Bạn đã nạp tổng cộng 100.000đ trong tuần và nhận " + WEEKLY_BONUS_1 + " Token!",
                    "MISSION");
                System.out.printf("[MissionController.onDeposit] userId=%d đạt mốc 100K → +%s Token%n",
                    userId, WEEKLY_BONUS_1);
            }
        }

        // Mốc 500K (cộng thêm, không thay thế mốc 100K)
        if (prevTotal.compareTo(WEEKLY_THRESHOLD_2) < 0
                && weeklyTotal.compareTo(WEEKLY_THRESHOLD_2) >= 0) {
            if (!hasClaimedWeeklyMission(userId, "WEEKLY_500K")) {
                tokenDAO.addToken(userId, WEEKLY_BONUS_2);
                markWeeklyMissionClaimed(userId, "WEEKLY_500K");
                notifDAO.send(userId,
                    "Nhiệm vụ tuần: Nạp 500K ✅",
                    "Bạn đã nạp tổng cộng 500.000đ trong tuần và nhận thêm " + WEEKLY_BONUS_2 + " Token!",
                    "MISSION");
                System.out.printf("[MissionController.onDeposit] userId=%d đạt mốc 500K → +%s Token%n",
                    userId, WEEKLY_BONUS_2);
            }
        }

        // ── B. Số ngày nạp trong tháng ──────────────────────────────────────
        int depositDaysThisMonth = getDepositDaysThisMonth(userId);
        if (depositDaysThisMonth == MONTHLY_DAYS_TARGET) {
            if (!hasClaimedMonthlyMission(userId, "MONTHLY_10DAYS")) {
                tokenDAO.addToken(userId, MONTHLY_DAYS_BONUS);
                markMonthlyMissionClaimed(userId, "MONTHLY_10DAYS");
                notifDAO.send(userId,
                    "Nhiệm vụ tháng: Nạp 10 ngày ✅",
                    "Bạn đã nạp tiền đủ 10 ngày trong tháng và nhận " + MONTHLY_DAYS_BONUS + " Token!",
                    "MISSION");
                System.out.printf("[MissionController.onDeposit] userId=%d đủ 10 ngày nạp → +%s Token%n",
                    userId, MONTHLY_DAYS_BONUS);
            }
        }
    }

    // =========================================================================
    //  3. checkMonthlyBalance() — Kiểm tra duy trì số dư tích lũy cuối tháng
    // =========================================================================

    /**
     * Gọi cuối tháng — kiểm tra user có duy trì tổng số dư tích lũy ≥ 1.000.000đ.
     *
     * Cách thực hiện cho đồ án:
     *  Dùng InvestmentDAO.getTotalActiveAmount() tại thời điểm cuối tháng.
     *  (Không hoàn toàn chính xác với MIN snapshot nhưng đủ cho demo.)
     *
     * @param userId ID user
     * @return token đã thưởng, hoặc ZERO nếu không đủ điều kiện
     */
    public BigDecimal checkMonthlyBalance(int userId) {
        BigDecimal totalActive = investDAO.getTotalActiveAmount(userId);
        if (totalActive.compareTo(BALANCE_THRESHOLD) >= 0) {
            if (!hasClaimedMonthlyMission(userId, "MONTHLY_BALANCE")) {
                tokenDAO.addToken(userId, BALANCE_BONUS);
                markMonthlyMissionClaimed(userId, "MONTHLY_BALANCE");
                notifDAO.send(userId,
                    "Nhiệm vụ tháng: Duy trì số dư tích lũy ✅",
                    "Bạn duy trì tổng đầu tư ≥ 1.000.000đ trong tháng và nhận " + BALANCE_BONUS + " Token!",
                    "MISSION");
                System.out.printf("[MissionController.checkMonthlyBalance] userId=%d đủ điều kiện → +%s Token%n",
                    userId, BALANCE_BONUS);
                return BALANCE_BONUS;
            }
        }
        return BigDecimal.ZERO;
    }

    // =========================================================================
    //  4. checkTopDepositors() — Top 20 người nạp nhiều nhất trong tháng
    // =========================================================================

    /**
     * Gọi cuối tháng trước khi reset.
     *
     * Query tổng tiền nạp được duyệt (DEPOSIT COMPLETED) trong tháng,
     * sort giảm dần, tie-break bằng thời điểm đạt mốc sớm nhất.
     * Top 20 → mỗi người thưởng 5.000 Token.
     *
     * @return số user được thưởng
     */
    public int checkTopDepositors() {
        List<Integer> topUsers = queryTopDepositors(TOP_DEPOSITOR_COUNT);
        int rewarded = 0;
        for (int uid : topUsers) {
            tokenDAO.addToken(uid, TOP_DEPOSITOR_BONUS);
            notifDAO.send(uid,
                "🏆 Top " + TOP_DEPOSITOR_COUNT + " người nạp tháng này!",
                "Bạn nằm trong top " + TOP_DEPOSITOR_COUNT
                    + " người nạp tiền nhiều nhất tháng. Nhận " + TOP_DEPOSITOR_BONUS + " Token!",
                "MISSION");
            rewarded++;
        }
        System.out.printf("[MissionController.checkTopDepositors] Đã thưởng %d/%d user.%n",
            rewarded, TOP_DEPOSITOR_COUNT);
        return rewarded;
    }

    /**
     * Query top N depositor trong tháng hiện tại.
     * Tie-break: MIN(approved_date) của lần nạp làm tổng vượt threshold.
     */
    private List<Integer> queryTopDepositors(int limit) {
        List<Integer> result = new ArrayList<>();
        String sql = """
            SELECT w.user_id
            FROM (
                SELECT t.wallet_id, SUM(t.amount) AS total_dep,
                       MIN(t.created_at) AS first_dep_ts
                FROM TRANSACTION t
                WHERE t.type_code = 'DEPOSIT'
                  AND t.status = 'COMPLETED'
                  AND t.is_deleted = 0
                  AND TRUNC(t.created_at, 'MM') = TRUNC(SYSDATE, 'MM')
                GROUP BY t.wallet_id
            ) dep_summary
            JOIN WALLET w ON dep_summary.wallet_id = w.wallet_id
            ORDER BY dep_summary.total_dep DESC, dep_summary.first_dep_ts ASC
            FETCH FIRST ? ROWS ONLY
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getInt("user_id"));
            }
        } catch (Exception e) {
            System.err.println("[MissionController.queryTopDepositors] " + e.getMessage());
        }
        return result;
    }

    // =========================================================================
    //  5. onShareApp() — Chia sẻ app (1 lần/tuần)
    // =========================================================================

    /**
     * Thưởng token khi user chia sẻ app.
     * Giới hạn: 1 lần / tuần (kiểm tra bằng flag trong DB).
     *
     * @return SHARE_BONUS nếu thành công, ZERO nếu đã chia sẻ tuần này
     */
    public BigDecimal onShareApp(int userId) {
        if (hasClaimedWeeklyMission(userId, "WEEKLY_SHARE")) {
            System.out.printf("[MissionController.onShareApp] userId=%d đã chia sẻ tuần này.%n", userId);
            return BigDecimal.ZERO;
        }

        tokenDAO.addToken(userId, SHARE_BONUS);
        markWeeklyMissionClaimed(userId, "WEEKLY_SHARE");
        notifDAO.send(userId,
            "Cảm ơn bạn đã chia sẻ FlexInvest! 🎁",
            "Bạn nhận " + SHARE_BONUS + " FlexToken cho lần chia sẻ tuần này.",
            "MISSION");
        System.out.printf("[MissionController.onShareApp] userId=%d → +%s Token%n", userId, SHARE_BONUS);
        return SHARE_BONUS;
    }

    // =========================================================================
    //  6. onReferral() — Giới thiệu bạn
    // =========================================================================

    /**
     * Thưởng token khi người được giới thiệu đủ điều kiện.
     *
     * Điều kiện (QĐ13):
     *  1. newAccountId đã KYC APPROVED
     *  2. newAccountId đã nạp tổng cộng ≥ 50.000đ
     *
     * @param referrerId   ID người giới thiệu (nhận thưởng)
     * @param newAccountId ID tài khoản mới được giới thiệu
     * @return true nếu thưởng thành công
     */
    public boolean onReferral(int referrerId, int newAccountId) {
        // 1. Kiểm tra KYC APPROVED
        if (!isKycApproved(newAccountId)) {
            System.out.printf("[MissionController.onReferral] userId=%d chưa KYC APPROVED.%n", newAccountId);
            return false;
        }

        // 2. Kiểm tra tổng nạp ≥ 50K
        BigDecimal totalDeposit = getTotalDepositEver(newAccountId);
        if (totalDeposit.compareTo(REFERRAL_MIN_DEPOSIT) < 0) {
            System.out.printf("[MissionController.onReferral] userId=%d chưa nạp đủ 50K (đã nạp: %s).%n",
                newAccountId, totalDeposit);
            return false;
        }

        // 3. Thưởng cho referrer
        tokenDAO.addToken(referrerId, REFERRAL_BONUS);
        notifDAO.send(referrerId,
            "Thưởng giới thiệu thành công! 🎁",
            "Người bạn giới thiệu (ID #" + newAccountId + ") đã đủ điều kiện. " +
                "Bạn nhận " + REFERRAL_BONUS + " FlexToken!",
            "MISSION");
        System.out.printf("[MissionController.onReferral] referrerId=%d → +%s Token (newUser=%d)%n",
            referrerId, REFERRAL_BONUS, newAccountId);
        return true;
    }

    // =========================================================================
    //  Private helpers — DB queries
    // =========================================================================

    /**
     * Tổng tiền đã nạp thành công (DEPOSIT COMPLETED) trong tuần hiện tại.
     * Tuần bắt đầu từ Thứ 2 (QĐ13).
     */
    private BigDecimal getWeeklyDepositTotal(int userId) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String sql = """
            SELECT NVL(SUM(t.amount), 0)
            FROM TRANSACTION t
            JOIN WALLET w ON t.wallet_id = w.wallet_id
            WHERE w.user_id = ?
              AND t.type_code = 'DEPOSIT'
              AND t.status = 'COMPLETED'
              AND t.is_deleted = 0
              AND TRUNC(t.created_at) >= ?
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(monday));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            System.err.println("[MissionController.getWeeklyDepositTotal] " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Số ngày khác nhau đã nạp tiền thành công trong tháng hiện tại.
     */
    private int getDepositDaysThisMonth(int userId) {
        String sql = """
            SELECT COUNT(DISTINCT TRUNC(t.created_at))
            FROM TRANSACTION t
            JOIN WALLET w ON t.wallet_id = w.wallet_id
            WHERE w.user_id = ?
              AND t.type_code = 'DEPOSIT'
              AND t.status = 'COMPLETED'
              AND t.is_deleted = 0
              AND TRUNC(t.created_at, 'MM') = TRUNC(SYSDATE, 'MM')
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("[MissionController.getDepositDaysThisMonth] " + e.getMessage());
        }
        return 0;
    }

    /**
     * Tổng tiền nạp thành công mọi thời điểm (dùng cho điều kiện referral).
     */
    private BigDecimal getTotalDepositEver(int userId) {
        String sql = """
            SELECT NVL(SUM(t.amount), 0)
            FROM TRANSACTION t
            JOIN WALLET w ON t.wallet_id = w.wallet_id
            WHERE w.user_id = ?
              AND t.type_code = 'DEPOSIT'
              AND t.status = 'COMPLETED'
              AND t.is_deleted = 0
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            System.err.println("[MissionController.getTotalDepositEver] " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /** Kiểm tra KYC APPROVED */
    private boolean isKycApproved(int userId) {
        String sql = "SELECT COUNT(*) FROM EKYC WHERE USER_ID = ? AND VERIFIED_STATUS = 'APPROVED' AND IS_DELETED = 0";
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("[MissionController.isKycApproved] " + e.getMessage());
        }
        return false;
    }

    // ── Nhiệm vụ tuần/tháng — tracking đã claim ──────────────────────────────
    // Dùng một bảng đơn giản (hoặc cột trong USER_MISSION) để đánh dấu.
    // Vì không thể thay đổi schema, ta dùng NOTIFICATION làm proxy flag.
    // Trong thực tế nên thêm bảng MISSION_CLAIM_FLAG riêng.

    /**
     * Kiểm tra đã claim nhiệm vụ tuần này chưa.
     * Dùng NOTIFICATION làm proxy (nếu có thông báo với title chứa missionKey tuần này → đã claim).
     */
    private boolean hasClaimedWeeklyMission(int userId, String missionKey) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String sql = """
            SELECT COUNT(*) FROM NOTIFICATION
            WHERE user_id = ? AND type = 'MISSION'
              AND title LIKE ?
              AND sent_at >= ?
              AND is_deleted = 0
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + missionKey + "%");
            ps.setDate(3, java.sql.Date.valueOf(monday));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("[MissionController.hasClaimedWeeklyMission] " + e.getMessage());
        }
        return false;
    }

    private void markWeeklyMissionClaimed(int userId, String missionKey) {
        // Thông báo đã được gửi ở caller → chỉ ghi log thêm
        System.out.printf("[MissionController] userId=%d claimed mission: %s%n", userId, missionKey);
    }

    private boolean hasClaimedMonthlyMission(int userId, String missionKey) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        String sql = """
            SELECT COUNT(*) FROM NOTIFICATION
            WHERE user_id = ? AND type = 'MISSION'
              AND title LIKE ?
              AND sent_at >= ?
              AND is_deleted = 0
            """;
        try (Connection con = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, "%" + missionKey + "%");
            ps.setDate(3, java.sql.Date.valueOf(firstOfMonth));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.err.println("[MissionController.hasClaimedMonthlyMission] " + e.getMessage());
        }
        return false;
    }

    private void markMonthlyMissionClaimed(int userId, String missionKey) {
        System.out.printf("[MissionController] userId=%d claimed monthly mission: %s%n", userId, missionKey);
    }
}
