/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.time.LocalDateTime;

/**
 *
 * @author 84941
 */
public class Notification {
    private int notificationId;     
    private int userId;           
    private String title;           
    private String body;           
    private String type;            
    private int isRead;             
    private LocalDateTime sentAt;  
    private LocalDateTime readAt;   
    private int isDeleted;

    public Notification() {
    }

    public Notification(int notificationId, int userId, String title, String body, String type, int isRead, LocalDateTime sentAt, LocalDateTime readAt, int isDeleted) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.isRead = isRead;
        this.sentAt = sentAt;
        this.readAt = readAt;
        this.isDeleted = isDeleted;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
