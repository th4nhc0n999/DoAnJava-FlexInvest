package Model;

import java.sql.Timestamp;

/**
 * Model ánh xạ bảng USER_MISSION.
 *
 * status: IN_PROGRESS | COMPLETED | CLAIMED
 *
 * Lưu ý quan trọng về check-in (QĐ13):
 *   - Trường claimedAt được tái sử dụng để lưu thời điểm check-in gần nhất.
 *   - MissionDAO.checkIn() so sánh claimedAt với ngày hôm nay / hôm qua
 *     để xác định streak có tiếp tục hay bị reset về 1.
 */
public class UserMission {

    private int       userMissionId;
    private int       userId;
    private int       missionId;
    private String    status;         // IN_PROGRESS | COMPLETED | CLAIMED
    private int       progress;       // Tiến độ hiện tại / streak điểm danh
    private Timestamp completedAt;
    private Timestamp claimedAt;      // Với DAILY: dùng làm mốc check-in gần nhất
    private int       isDeleted;

    // ── Thuộc tính mở rộng (không ánh xạ DB — dùng khi JOIN với MISSIONS) ──
    private Mission mission;          // Gắn vào khi query JOIN, không lưu DB

    public UserMission() {}

    public UserMission(int userMissionId, int userId, int missionId,
                       String status, int progress,
                       Timestamp completedAt, Timestamp claimedAt, int isDeleted) {
        this.userMissionId = userMissionId;
        this.userId        = userId;
        this.missionId     = missionId;
        this.status        = status;
        this.progress      = progress;
        this.completedAt   = completedAt;
        this.claimedAt     = claimedAt;
        this.isDeleted     = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getUserMissionId()                        { return userMissionId; }
    public void setUserMissionId(int userMissionId)      { this.userMissionId = userMissionId; }

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public int getMissionId()               { return missionId; }
    public void setMissionId(int missionId) { this.missionId = missionId; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public int getProgress()                { return progress; }
    public void setProgress(int progress)   { this.progress = progress; }

    public Timestamp getCompletedAt()               { return completedAt; }
    public void setCompletedAt(Timestamp completedAt){ this.completedAt = completedAt; }

    public Timestamp getClaimedAt()                 { return claimedAt; }
    public void setClaimedAt(Timestamp claimedAt)   { this.claimedAt = claimedAt; }

    public int getIsDeleted()                  { return isDeleted; }
    public void setIsDeleted(int isDeleted)    { this.isDeleted = isDeleted; }

    public Mission getMission()                { return mission; }
    public void setMission(Mission mission)    { this.mission = mission; }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Trả về true nếu user đã nhận thưởng cho nhiệm vụ này */
    public boolean isClaimed()    { return "CLAIMED".equals(status); }

    /** Trả về true nếu nhiệm vụ đã hoàn thành nhưng chưa nhận thưởng */
    public boolean isCompleted()  { return "COMPLETED".equals(status); }

    /** Trả về true nếu đang còn tiến hành */
    public boolean isInProgress() { return "IN_PROGRESS".equals(status); }

    @Override
    public String toString() {
        return "UserMission{userId=" + userId +
               ", missionId=" + missionId +
               ", progress=" + progress +
               ", status='" + status + "'}";
    }
}
