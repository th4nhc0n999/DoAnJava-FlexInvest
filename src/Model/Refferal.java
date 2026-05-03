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
public class Refferal {
    private int referralId;
    private int referrerId;
    private String status;
    private int rewardAmount;
    private LocalDateTime createdAt;
    private int isDeleted;

    public Refferal() {
    }

    public Refferal(int referralId, int referrerId, String status, int rewardAmount, LocalDateTime createdAt, int isDeleted) {
        this.referralId = referralId;
        this.referrerId = referrerId;
        this.status = status;
        this.rewardAmount = rewardAmount;
        this.createdAt = createdAt;
        this.isDeleted = isDeleted;
    }

    public int getReferralId() {
        return referralId;
    }

    public void setReferralId(int referralId) {
        this.referralId = referralId;
    }

    public int getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(int referrerId) {
        this.referrerId = referrerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(int rewardAmount) {
        this.rewardAmount = rewardAmount;
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
