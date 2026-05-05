package Model;

import java.sql.Timestamp;

/**
 * Model ánh xạ bảng NOTIFICATION.
 * body kiểu CLOB trong DB → dùng String trong Java (đủ với text thông thường).
 */
public class Notification {

    private int       notificationId;
    private int       userId;
    private String    title;
    private String    body;        // CLOB trong DB
    private String    type;        // Ví dụ: MISSION | TRANSACTION | SYSTEM | ...
    private int       isRead;      // 0 = chưa đọc, 1 = đã đọc
    private Timestamp sentAt;
    private Timestamp readAt;      // null nếu chưa đọc
    private int       isDeleted;

    public Notification() {}

    public Notification(int notificationId, int userId, String title,
                        String body, String type, int isRead,
                        Timestamp sentAt, Timestamp readAt, int isDeleted) {
        this.notificationId = notificationId;
        this.userId         = userId;
        this.title          = title;
        this.body           = body;
        this.type           = type;
        this.isRead         = isRead;
        this.sentAt         = sentAt;
        this.readAt         = readAt;
        this.isDeleted      = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getNotificationId()                           { return notificationId; }
    public void setNotificationId(int notificationId)        { this.notificationId = notificationId; }

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }

    public String getBody()                 { return body; }
    public void setBody(String body)        { this.body = body; }

    public String getType()                 { return type; }
    public void setType(String type)        { this.type = type; }

    public int getIsRead()                  { return isRead; }
    public void setIsRead(int isRead)       { this.isRead = isRead; }

    public Timestamp getSentAt()            { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    public Timestamp getReadAt()             { return readAt; }
    public void setReadAt(Timestamp readAt)  { this.readAt = readAt; }

    public int getIsDeleted()               { return isDeleted; }
    public void setIsDeleted(int isDeleted) { this.isDeleted = isDeleted; }

    // ── Helper ───────────────────────────────────────────────────────────────

    public boolean isUnread() { return isRead == 0; }

    @Override
    public String toString() {
        return "Notification{id=" + notificationId +
               ", userId=" + userId +
               ", title='" + title + "'" +
               ", isRead=" + isRead + "}";
    }
}
