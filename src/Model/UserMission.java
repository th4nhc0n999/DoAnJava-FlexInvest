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
public class UserMission {
    private int userMissionId;      
    private int userId;            
    private int missionId;         
    private String status;         
    private int progress;           
    private LocalDateTime completedAt; 
    private LocalDateTime claimedAt;  
    private int isDeleted;

    public UserMission() {
    }

    public UserMission(int userMissionId, int userId, int missionId, String status, int progress, LocalDateTime completedAt, LocalDateTime claimedAt, int isDeleted) {
        this.userMissionId = userMissionId;
        this.userId = userId;
        this.missionId = missionId;
        this.status = status;
        this.progress = progress;
        this.completedAt = completedAt;
        this.claimedAt = claimedAt;
        this.isDeleted = isDeleted;
    }

    public int getUserMissionId() {
        return userMissionId;
    }

    public void setUserMissionId(int userMissionId) {
        this.userMissionId = userMissionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMissionId() {
        return missionId;
    }

    public void setMissionId(int missionId) {
        this.missionId = missionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
