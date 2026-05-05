package Model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model ánh xạ bảng MISSIONS.
 *
 * mission_type : WEEKLY | MONTHLY | DAILY
 * action_type  : DEPOSIT | INVEST | CHECKIN | ... (do N1 quy định data mẫu)
 *
 * QĐ13:
 *  - WEEKLY  → reset mỗi đầu tuần (Thứ 2)
 *  - MONTHLY → reset mỗi ngày 1 hàng tháng
 *  - DAILY   → mission điểm danh, reset về 0 nếu bỏ 1 ngày
 */
public class Mission {

    private int        missionId;
    private String     title;
    private String     description;
    private String     missionType;   // WEEKLY | MONTHLY | DAILY
    private String     actionType;    // DEPOSIT | INVEST | CHECKIN | ...
    private int        targetValue;
    private BigDecimal rewardToken;
    private int        isActive;      // 1 = active, 0 = inactive
    private Date       startDate;
    private Date       endDate;
    private int        sortOrder;
    private Timestamp  createdAt;
    private int        isDeleted;

    public Mission() {}

    public Mission(int missionId, String title, String description,
                   String missionType, String actionType, int targetValue,
                   BigDecimal rewardToken, int isActive, Date startDate,
                   Date endDate, int sortOrder, Timestamp createdAt, int isDeleted) {
        this.missionId   = missionId;
        this.title       = title;
        this.description = description;
        this.missionType = missionType;
        this.actionType  = actionType;
        this.targetValue = targetValue;
        this.rewardToken = rewardToken;
        this.isActive    = isActive;
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.sortOrder   = sortOrder;
        this.createdAt   = createdAt;
        this.isDeleted   = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getMissionId()                   { return missionId; }
    public void setMissionId(int missionId)     { this.missionId = missionId; }

    public String getTitle()                    { return title; }
    public void setTitle(String title)          { this.title = title; }

    public String getDescription()              { return description; }
    public void setDescription(String desc)     { this.description = desc; }

    public String getMissionType()              { return missionType; }
    public void setMissionType(String t)        { this.missionType = t; }

    public String getActionType()               { return actionType; }
    public void setActionType(String t)         { this.actionType = t; }

    public int getTargetValue()                 { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

    public BigDecimal getRewardToken()              { return rewardToken; }
    public void setRewardToken(BigDecimal r)        { this.rewardToken = r; }

    public int getIsActive()                    { return isActive; }
    public void setIsActive(int isActive)       { this.isActive = isActive; }

    public Date getStartDate()                  { return startDate; }
    public void setStartDate(Date startDate)    { this.startDate = startDate; }

    public Date getEndDate()                    { return endDate; }
    public void setEndDate(Date endDate)        { this.endDate = endDate; }

    public int getSortOrder()                   { return sortOrder; }
    public void setSortOrder(int sortOrder)     { this.sortOrder = sortOrder; }

    public Timestamp getCreatedAt()             { return createdAt; }
    public void setCreatedAt(Timestamp t)       { this.createdAt = t; }

    public int getIsDeleted()                   { return isDeleted; }
    public void setIsDeleted(int isDeleted)     { this.isDeleted = isDeleted; }

    @Override
    public String toString() {
        return "Mission{missionId=" + missionId +
               ", title='" + title + "'" +
               ", type='" + missionType + "'" +
               ", action='" + actionType + "'}";
    }
}
