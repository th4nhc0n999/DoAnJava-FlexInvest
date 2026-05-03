/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author 84941
 */
public class Mission {
    private int missionId;          
    private String title;           
    private String description;      
    private String missionType;     
    private String actionType;       
    private BigDecimal targetValue;  
    private BigDecimal rewardToken;  
    private int isActive;           
    private LocalDate startDate;     
    private LocalDate endDate;      
    private Integer sortOrder;      
    private LocalDateTime createdAt; 
    private int isDeleted;

    public Mission() {
    }

    public Mission(int missionId, String title, String description, String missionType, String actionType, BigDecimal targetValue, BigDecimal rewardToken, int isActive, LocalDate startDate, LocalDate endDate, Integer sortOrder, LocalDateTime createdAt, int isDeleted) {
        this.missionId = missionId;
        this.title = title;
        this.description = description;
        this.missionType = missionType;
        this.actionType = actionType;
        this.targetValue = targetValue;
        this.rewardToken = rewardToken;
        this.isActive = isActive;
        this.startDate = startDate;
        this.endDate = endDate;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getMissionId() {
        return missionId;
    }

    public void setMissionId(int missionId) {
        this.missionId = missionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMissionType() {
        return missionType;
    }

    public void setMissionType(String missionType) {
        this.missionType = missionType;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public BigDecimal getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getRewardToken() {
        return rewardToken;
    }

    public void setRewardToken(BigDecimal rewardToken) {
        this.rewardToken = rewardToken;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
