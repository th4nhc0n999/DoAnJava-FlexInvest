/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import java.time.LocalDateTime;
import java.util.Date;

/**
 *
 * @author 84941
 */
public class Ekyc {
    private int kycId;
    private int userId;
    private String idNumber;
    private String fullName;
    private Date dateOfBirth;
    private String gender;
    private String placeOfOrigin;
    private String placeOfResidence;
    private Date issueDate;
    private Date expiryDate;
    private String issuePlace;
    private String frontImageUrl;
    private String backImageUrl;
    private String faceImageUrl;
    private int matchScore;
    private String verifiedStatus;
    private String note;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int isDeleted;

    public Ekyc() {
    }

    public Ekyc(int kycId, int userId, String idNumber, String fullName, Date dateOfBirth, String gender, String placeOfOrigin, String placeOfResidence, Date issueDate, Date expiryDate, String issuePlace, String frontImageUrl, String backImageUrl, String faceImageUrl, int matchScore, String verifiedStatus, String note, LocalDateTime verifiedAt, LocalDateTime createdAt, LocalDateTime updatedAt, int isDeleted) {
        this.kycId = kycId;
        this.userId = userId;
        this.idNumber = idNumber;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.placeOfOrigin = placeOfOrigin;
        this.placeOfResidence = placeOfResidence;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.issuePlace = issuePlace;
        this.frontImageUrl = frontImageUrl;
        this.backImageUrl = backImageUrl;
        this.faceImageUrl = faceImageUrl;
        this.matchScore = matchScore;
        this.verifiedStatus = verifiedStatus;
        this.note = note;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public int getKycId() {
        return kycId;
    }

    public void setKycId(int kycId) {
        this.kycId = kycId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPlaceOfOrigin() {
        return placeOfOrigin;
    }

    public void setPlaceOfOrigin(String placeOfOrigin) {
        this.placeOfOrigin = placeOfOrigin;
    }

    public String getPlaceOfResidence() {
        return placeOfResidence;
    }

    public void setPlaceOfResidence(String placeOfResidence) {
        this.placeOfResidence = placeOfResidence;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getIssuePlace() {
        return issuePlace;
    }

    public void setIssuePlace(String issuePlace) {
        this.issuePlace = issuePlace;
    }

    public String getFrontImageUrl() {
        return frontImageUrl;
    }

    public void setFrontImageUrl(String frontImageUrl) {
        this.frontImageUrl = frontImageUrl;
    }

    public String getBackImageUrl() {
        return backImageUrl;
    }

    public void setBackImageUrl(String backImageUrl) {
        this.backImageUrl = backImageUrl;
    }

    public String getFaceImageUrl() {
        return faceImageUrl;
    }

    public void setFaceImageUrl(String faceImageUrl) {
        this.faceImageUrl = faceImageUrl;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getVerifiedStatus() {
        return verifiedStatus;
    }

    public void setVerifiedStatus(String verifiedStatus) {
        this.verifiedStatus = verifiedStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
}
