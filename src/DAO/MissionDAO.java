package DAO;

import ConnectDB.ConnectionOracle;
import Model.Mission;
import Model.UserMission;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO quản lý bảng MISSIONS và USER_MISSION.
 *
 * QĐ13 — Quy tắc reset:
 *  - WEEKLY  : reset đầu tuần (gọi resetWeeklyMissions() mỗi thứ 2)
 *  - MONTHLY : reset đầu tháng (gọi resetMonthlyMissions() mỗi ngày 1)
 *  - DAILY   : check-in. Bỏ 1 ngày → resetCheckIn(userId). Quay lại = Ngày 1.
 *
 * Lưu ý về check-in:
 *  USER_MISSION.claimedAt  → lưu timestamp check-in gần nhất (tái sử dụng cột)
 *  USER_MISSION.progress   → streak hiện tại (1, 2, 3, ...)
 */
public class MissionDAO {

    private final TokenDAO tokenDAO = new TokenDAO();

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 1: QUERY MISSIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Lấy tất cả mission đang active (is_active=1 và còn trong thời hạn).
     */
    public List<Mission> getAllActive() {
        List<Mission> list = new ArrayList<>();
        String sql = "SELECT * FROM MISSIONS " +
                     "WHERE is_active = 1 AND is_deleted = 0 " +
                     "AND (start_date IS NULL OR start_date <= SYSDATE) " +
                     "AND (end_date IS NULL OR end_date >= SYSDATE) " +
                     "ORDER BY sort_order, mission_id";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapMission(rs));
        } catch (Exception e) {
            System.err.println("[MissionDAO.getAllActive] Lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy danh sách UserMission của user kèm thông tin Mission (JOIN).
     * Dùng để hiển thị trang nhiệm vụ của người dùng.
     */
    public List<UserMission> getUserMissions(int userId) {
        List<UserMission> list = new ArrayList<>();
        String sql = "SELECT um.*, " +
                     "m.title, m.description, m.mission_type, m.action_type, " +
                     "m.target_value, m.reward_token, m.is_active, " +
                     "m.start_date, m.end_date, m.sort_order, m.created_at AS m_created_at " +
                     "FROM USER_MISSION um " +
                     "JOIN MISSIONS m ON um.mission_id = m.mission_id " +
                     "WHERE um.user_id = ? AND um.is_deleted = 0 AND m.is_deleted = 0 " +
                     "ORDER BY m.sort_order, m.mission_id";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserMission um = mapUserMission(rs);
                    Mission m = mapMissionFromJoin(rs);
                    um.setMission(m);
                    list.add(um);
                }
            }
        } catch (Exception e) {
            System.err.println("[MissionDAO.getUserMissions] Lỗi: " + e.getMessage());
        }
        return list;
    }

    /**
     * Lấy UserMission cụ thể của một user cho một mission.
     * @return null nếu chưa có record
     */
    public UserMission getUserMission(int userId, int missionId) {
        String sql = "SELECT * FROM USER_MISSION " +
                     "WHERE user_id = ? AND mission_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, missionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUserMission(rs);
            }
        } catch (Exception e) {
            System.err.println("[MissionDAO.getUserMission] Lỗi: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 2: CẬP NHẬT TIẾN ĐỘ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cập nhật tiến độ nhiệm vụ cho user (dùng MERGE INTO Oracle).
     * Nếu chưa có record → INSERT mới. Nếu đã có → UPDATE progress.
     * Tự động chuyển sang COMPLETED nếu progress >= target_value.
     *
     * @param userId    ID user
     * @param missionId ID mission
     * @param delta     Tăng thêm bao nhiêu (thường là 1)
     * @return true nếu cập nhật thành công
     */
    public boolean updateProgress(int userId, int missionId, int delta) {
        // Lấy target_value của mission để check hoàn thành
        Mission mission = getMissionById(missionId);
        if (mission == null) return false;

        UserMission existing = getUserMission(userId, missionId);

        if (existing == null) {
            // INSERT mới
            return insertUserMission(userId, missionId, delta, mission.getTargetValue());
        } else {
            // Không cập nhật nếu đã COMPLETED hoặc CLAIMED
            if (existing.isClaimed() || existing.isCompleted()) return false;
            return incrementProgress(userId, missionId, delta, mission.getTargetValue());
        }
    }

    private boolean insertUserMission(int userId, int missionId, int progress, int target) {
        String status = (progress >= target) ? "COMPLETED" : "IN_PROGRESS";
        Timestamp completedAt = (progress >= target)
                ? new Timestamp(System.currentTimeMillis()) : null;
        String sql = "INSERT INTO USER_MISSION " +
                     "(user_id, mission_id, status, progress, completed_at) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, missionId);
            ps.setString(3, status);
            ps.setInt(4, Math.min(progress, target));
            ps.setTimestamp(5, completedAt);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MissionDAO.insertUserMission] Lỗi: " + e.getMessage());
            return false;
        }
    }

    private boolean incrementProgress(int userId, int missionId, int delta, int target) {
        String sql = "UPDATE USER_MISSION SET " +
                     "progress = LEAST(progress + ?, ?), " +
                     "status = CASE WHEN progress + ? >= ? THEN 'COMPLETED' ELSE status END, " +
                     "completed_at = CASE WHEN progress + ? >= ? AND completed_at IS NULL " +
                     "               THEN SYSTIMESTAMP ELSE completed_at END " +
                     "WHERE user_id = ? AND mission_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, target);    // LEAST cap
            ps.setInt(3, delta);
            ps.setInt(4, target);    // check status
            ps.setInt(5, delta);
            ps.setInt(6, target);    // check completed_at
            ps.setInt(7, userId);
            ps.setInt(8, missionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MissionDAO.incrementProgress] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 3: NHẬN THƯỞNG
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * User nhận thưởng mission đã COMPLETED.
     * Cộng token vào ví và chuyển status → CLAIMED.
     *
     * @return true nếu nhận thưởng thành công
     */
    public boolean claimReward(int userId, int missionId) {
        UserMission um = getUserMission(userId, missionId);
        if (um == null || !um.isCompleted()) return false;

        Mission mission = getMissionById(missionId);
        if (mission == null) return false;

        // Cộng token (QĐ10: TokenDAO tự xử lý logic năm)
        boolean tokenAdded = tokenDAO.addToken(userId, mission.getRewardToken());
        if (!tokenAdded) {
            System.err.println("[MissionDAO.claimReward] Không thể cộng token cho user " + userId);
            return false;
        }

        // Cập nhật status → CLAIMED
        String sql = "UPDATE USER_MISSION SET status = 'CLAIMED', " +
                     "claimed_at = SYSTIMESTAMP " +
                     "WHERE user_id = ? AND mission_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, missionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[MissionDAO.claimReward] Lỗi: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 4: RESET MISSIONS (QĐ13)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reset toàn bộ nhiệm vụ WEEKLY về IN_PROGRESS, progress = 0.
     * Gọi vào đầu mỗi tuần (Thứ 2).
     * QĐ13: "Bảng nhiệm vụ hàng tuần sẽ reset mỗi tuần"
     */
    public void resetWeeklyMissions() {
        String sql = "UPDATE USER_MISSION SET " +
                     "status = 'IN_PROGRESS', progress = 0, " +
                     "completed_at = NULL, claimed_at = NULL " +
                     "WHERE mission_id IN (" +
                     "  SELECT mission_id FROM MISSIONS " +
                     "  WHERE mission_type = 'WEEKLY' AND is_deleted = 0" +
                     ") AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            System.out.println("[MissionDAO.resetWeeklyMissions] Reset " + rows + " bản ghi.");
        } catch (Exception e) {
            System.err.println("[MissionDAO.resetWeeklyMissions] Lỗi: " + e.getMessage());
        }
    }

    /**
     * Reset toàn bộ nhiệm vụ MONTHLY về IN_PROGRESS, progress = 0.
     * Gọi vào ngày 1 đầu mỗi tháng.
     * QĐ13: "Bảng nhiệm vụ hàng tháng sẽ reset mỗi tháng"
     */
    public void resetMonthlyMissions() {
        String sql = "UPDATE USER_MISSION SET " +
                     "status = 'IN_PROGRESS', progress = 0, " +
                     "completed_at = NULL, claimed_at = NULL " +
                     "WHERE mission_id IN (" +
                     "  SELECT mission_id FROM MISSIONS " +
                     "  WHERE mission_type = 'MONTHLY' AND is_deleted = 0" +
                     ") AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            System.out.println("[MissionDAO.resetMonthlyMissions] Reset " + rows + " bản ghi.");
        } catch (Exception e) {
            System.err.println("[MissionDAO.resetMonthlyMissions] Lỗi: " + e.getMessage());
        }
    }

    /**
     * Reset streak điểm danh của một user cụ thể về 0.
     * Gọi khi phát hiện user bỏ 1 ngày không điểm danh (QĐ13).
     * Lần check-in kế tiếp sẽ được tính là Ngày 1 (progress = 1).
     */
    public void resetCheckIn(int userId) {
        String sql = "UPDATE USER_MISSION SET progress = 0, status = 'IN_PROGRESS', " +
                     "completed_at = NULL, claimed_at = NULL " +
                     "WHERE user_id = ? AND is_deleted = 0 " +
                     "AND mission_id IN (" +
                     "  SELECT mission_id FROM MISSIONS " +
                     "  WHERE action_type = 'CHECKIN' AND is_deleted = 0" +
                     ")";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[MissionDAO.resetCheckIn] Lỗi: " + e.getMessage());
        }
    }

    /**
     * Reset streak của TẤT CẢ user đã bỏ điểm danh hôm qua.
     * Gọi vào đầu ngày (ví dụ: khi app khởi động hoặc scheduler 00:05).
     * QĐ13: "reset về ngày 1 nếu có một ngày không điểm danh"
     */
    public void resetMissedCheckIns() {
        // Reset những user có claimed_at trước hôm qua (bỏ ≥ 1 ngày)
        String sql = "UPDATE USER_MISSION SET progress = 0, status = 'IN_PROGRESS', " +
                     "completed_at = NULL " +
                     "WHERE is_deleted = 0 " +
                     "AND mission_id IN (" +
                     "  SELECT mission_id FROM MISSIONS " +
                     "  WHERE action_type = 'CHECKIN' AND is_deleted = 0" +
                     ") " +
                     "AND (claimed_at IS NULL OR " +
                     "     TRUNC(claimed_at) < TRUNC(SYSDATE - 1))";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            System.out.println("[MissionDAO.resetMissedCheckIns] Reset " + rows + " streak.");
        } catch (Exception e) {
            System.err.println("[MissionDAO.resetMissedCheckIns] Lỗi: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 5: CHECK-IN (QĐ13)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Thực hiện điểm danh hàng ngày cho user.
     *
     * Logic QĐ13:
     *  - Đã điểm danh hôm nay → từ chối (return false)
     *  - Điểm danh hôm qua  → streak tiếp tục (progress++)
     *  - Bỏ ≥ 1 ngày        → reset về 1 (Ngày 1)
     *
     * @return true nếu điểm danh thành công
     */
    public boolean checkIn(int userId) {
        // Lấy mission CHECKIN đang active
        Mission checkInMission = getCheckInMission();
        if (checkInMission == null) {
            System.err.println("[MissionDAO.checkIn] Không tìm thấy mission CHECKIN active.");
            return false;
        }

        UserMission um = getUserMission(userId, checkInMission.getMissionId());
        LocalDate today = LocalDate.now();

        if (um != null && um.getClaimedAt() != null) {
            LocalDate lastCheckIn = um.getClaimedAt().toLocalDateTime().toLocalDate();
            long daysDiff = ChronoUnit.DAYS.between(lastCheckIn, today);

            if (daysDiff == 0) {
                // Đã điểm danh hôm nay rồi
                System.out.println("[MissionDAO.checkIn] User " + userId + " đã điểm danh hôm nay.");
                return false;
            } else if (daysDiff > 1) {
                // Bỏ ≥ 1 ngày → reset về 1 (QĐ13)
                resetCheckIn(userId);
            }
            // daysDiff == 1 → tiếp tục streak, không reset
        }

        // Thực hiện cập nhật check-in
        return doCheckIn(userId, checkInMission.getMissionId(), checkInMission.getTargetValue());
    }

    private boolean doCheckIn(int userId, int missionId, int targetValue) {
        // Lấy streak hiện tại
        UserMission um = getUserMission(userId, missionId);
        int currentStreak = (um != null) ? um.getProgress() : 0;
        int newStreak = currentStreak + 1;
        String newStatus = (newStreak >= targetValue) ? "COMPLETED" : "IN_PROGRESS";

        if (um == null) {
            // INSERT mới
            String sql = "INSERT INTO USER_MISSION " +
                         "(user_id, mission_id, status, progress, claimed_at) " +
                         "VALUES (?, ?, ?, 1, SYSTIMESTAMP)";
            try (Connection conn = ConnectionOracle.getOracleConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, missionId);
                ps.setString(3, newStatus);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                System.err.println("[MissionDAO.doCheckIn INSERT] Lỗi: " + e.getMessage());
                return false;
            }
        } else {
            // UPDATE streak
            String sql = "UPDATE USER_MISSION SET " +
                         "progress = ?, status = ?, " +
                         "claimed_at = SYSTIMESTAMP, " +
                         "completed_at = CASE WHEN ? >= ? AND completed_at IS NULL " +
                         "               THEN SYSTIMESTAMP ELSE completed_at END " +
                         "WHERE user_id = ? AND mission_id = ? AND is_deleted = 0";
            try (Connection conn = ConnectionOracle.getOracleConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newStreak);
                ps.setString(2, newStatus);
                ps.setInt(3, newStreak);
                ps.setInt(4, targetValue);
                ps.setInt(5, userId);
                ps.setInt(6, missionId);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                System.err.println("[MissionDAO.doCheckIn UPDATE] Lỗi: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * Lấy số ngày streak điểm danh hiện tại của user.
     * QĐ13: Trả về 0 nếu chưa bắt đầu hoặc đã bị reset.
     */
    public int getCheckInStreak(int userId) {
        Mission checkInMission = getCheckInMission();
        if (checkInMission == null) return 0;

        UserMission um = getUserMission(userId, checkInMission.getMissionId());
        if (um == null) return 0;

        // Kiểm tra có bị reset chưa (bỏ ≥ 1 ngày)
        if (um.getClaimedAt() != null) {
            LocalDate lastCheckIn = um.getClaimedAt().toLocalDateTime().toLocalDate();
            long daysDiff = ChronoUnit.DAYS.between(lastCheckIn, LocalDate.now());
            if (daysDiff > 1) return 0; // Đã miss → streak = 0
        }

        return um.getProgress();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 6: HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private Mission getMissionById(int missionId) {
        String sql = "SELECT * FROM MISSIONS WHERE mission_id = ? AND is_deleted = 0";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, missionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMission(rs);
            }
        } catch (Exception e) {
            System.err.println("[MissionDAO.getMissionById] Lỗi: " + e.getMessage());
        }
        return null;
    }

    private Mission getCheckInMission() {
        String sql = "SELECT * FROM MISSIONS " +
                     "WHERE action_type = 'CHECKIN' AND is_active = 1 AND is_deleted = 0 " +
                     "AND ROWNUM = 1";
        try (Connection conn = ConnectionOracle.getOracleConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapMission(rs);
        } catch (Exception e) {
            System.err.println("[MissionDAO.getCheckInMission] Lỗi: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 7: MAPPING
    // ═══════════════════════════════════════════════════════════════════════

    private Mission mapMission(ResultSet rs) throws SQLException {
        Mission m = new Mission();
        m.setMissionId(rs.getInt("mission_id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setMissionType(rs.getString("mission_type"));
        m.setActionType(rs.getString("action_type"));
        m.setTargetValue(rs.getInt("target_value"));
        m.setRewardToken(rs.getBigDecimal("reward_token"));
        m.setIsActive(rs.getInt("is_active"));
        m.setStartDate(rs.getDate("start_date"));
        m.setEndDate(rs.getDate("end_date"));
        m.setSortOrder(rs.getInt("sort_order"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setIsDeleted(rs.getInt("is_deleted"));
        return m;
    }

    private Mission mapMissionFromJoin(ResultSet rs) throws SQLException {
        Mission m = new Mission();
        m.setMissionId(rs.getInt("mission_id"));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        m.setMissionType(rs.getString("mission_type"));
        m.setActionType(rs.getString("action_type"));
        m.setTargetValue(rs.getInt("target_value"));
        m.setRewardToken(rs.getBigDecimal("reward_token"));
        m.setIsActive(rs.getInt("is_active"));
        m.setStartDate(rs.getDate("start_date"));
        m.setEndDate(rs.getDate("end_date"));
        m.setSortOrder(rs.getInt("sort_order"));
        m.setCreatedAt(rs.getTimestamp("m_created_at"));
        return m;
    }

    private UserMission mapUserMission(ResultSet rs) throws SQLException {
        UserMission um = new UserMission();
        um.setUserMissionId(rs.getInt("user_mission_id"));
        um.setUserId(rs.getInt("user_id"));
        um.setMissionId(rs.getInt("mission_id"));
        um.setStatus(rs.getString("status"));
        um.setProgress(rs.getInt("progress"));
        um.setCompletedAt(rs.getTimestamp("completed_at"));
        um.setClaimedAt(rs.getTimestamp("claimed_at"));
        um.setIsDeleted(rs.getInt("is_deleted"));
        return um;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PHẦN 8: TEST
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        MissionDAO dao = new MissionDAO();
        int testUserId = 1;

        // Test 1: Lấy tất cả mission active
        System.out.println("=== getAllActive ===");
        dao.getAllActive().forEach(System.out::println);

        // Test 2: Check-in ngày 1
        System.out.println("\n=== checkIn lần 1 ===");
        System.out.println("Kết quả: " + dao.checkIn(testUserId));
        System.out.println("Streak hiện tại: " + dao.getCheckInStreak(testUserId));

        // Test 3: Check-in lại cùng ngày (phải trả về false)
        System.out.println("\n=== checkIn lần 2 cùng ngày (expect false) ===");
        System.out.println("Kết quả: " + dao.checkIn(testUserId));

        // Test 4: Lấy missions của user
        System.out.println("\n=== getUserMissions ===");
        dao.getUserMissions(testUserId).forEach(um ->
            System.out.println(um + " | Mission: " + (um.getMission() != null ? um.getMission().getTitle() : "null")));
    }
}
