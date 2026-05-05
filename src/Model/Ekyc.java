package Model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Model ánh xạ bảng EKYC.
 * verified_status: PENDING | APPROVED | REJECTED
 */
public class Ekyc {

    private int        kycId;
    private int        userId;
    private String     idNumber;
    private String     fullName;
    private Date       dateOfBirth;
    private String     gender;
    private String     placeOfOrigin;
    private String     placeOfResidence;
    private Date       issueDate;
    private Date       expiryDate;
    private String     issuePlace;
    private String     frontImageUrl;
    private String     backImageUrl;
    private String     faceImageUrl;
    private BigDecimal matchScore;       // NUMBER(5,2)
    private String     verifiedStatus;   // PENDING | APPROVED | REJECTED
    private String     note;
    private Timestamp  verifiedAt;
    private Timestamp  createdAt;
    private Timestamp  updatedAt;
    private int        isDeleted;

    public Ekyc() {}

    public Ekyc(int kycId, int userId, String idNumber, String fullName,
                Date dateOfBirth, String gender, String placeOfOrigin,
                String placeOfResidence, Date issueDate, Date expiryDate,
                String issuePlace, String frontImageUrl, String backImageUrl,
                String faceImageUrl, BigDecimal matchScore, String verifiedStatus,
                String note, Timestamp verifiedAt, Timestamp createdAt,
                Timestamp updatedAt, int isDeleted) {
        this.kycId             = kycId;
        this.userId            = userId;
        this.idNumber          = idNumber;
        this.fullName          = fullName;
        this.dateOfBirth       = dateOfBirth;
        this.gender            = gender;
        this.placeOfOrigin     = placeOfOrigin;
        this.placeOfResidence  = placeOfResidence;
        this.issueDate         = issueDate;
        this.expiryDate        = expiryDate;
        this.issuePlace        = issuePlace;
        this.frontImageUrl     = frontImageUrl;
        this.backImageUrl      = backImageUrl;
        this.faceImageUrl      = faceImageUrl;
        this.matchScore        = matchScore;
        this.verifiedStatus    = verifiedStatus;
        this.note              = note;
        this.verifiedAt        = verifiedAt;
        this.createdAt         = createdAt;
        this.updatedAt         = updatedAt;
        this.isDeleted         = isDeleted;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getKycId()                   { return kycId; }
    public void setKycId(int kycId)         { this.kycId = kycId; }

    public int getUserId()                  { return userId; }
    public void setUserId(int userId)       { this.userId = userId; }

    public String getIdNumber()             { return idNumber; }
    public void setIdNumber(String idNumber){ this.idNumber = idNumber; }

    public String getFullName()             { return fullName; }
    public void setFullName(String fullName){ this.fullName = fullName; }

    public Date getDateOfBirth()                    { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth)    { this.dateOfBirth = dateOfBirth; }

    public String getGender()               { return gender; }
    public void setGender(String gender)    { this.gender = gender; }

    public String getPlaceOfOrigin()                    { return placeOfOrigin; }
    public void setPlaceOfOrigin(String placeOfOrigin)  { this.placeOfOrigin = placeOfOrigin; }

    public String getPlaceOfResidence()                       { return placeOfResidence; }
    public void setPlaceOfResidence(String placeOfResidence)  { this.placeOfResidence = placeOfResidence; }

    public Date getIssueDate()                  { return issueDate; }
    public void setIssueDate(Date issueDate)    { this.issueDate = issueDate; }

    public Date getExpiryDate()                 { return expiryDate; }
    public void setExpiryDate(Date expiryDate)  { this.expiryDate = expiryDate; }

    public String getIssuePlace()               { return issuePlace; }
    public void setIssuePlace(String issuePlace){ this.issuePlace = issuePlace; }

    public String getFrontImageUrl()                    { return frontImageUrl; }
    public void setFrontImageUrl(String frontImageUrl)  { this.frontImageUrl = frontImageUrl; }

    public String getBackImageUrl()                   { return backImageUrl; }
    public void setBackImageUrl(String backImageUrl)  { this.backImageUrl = backImageUrl; }

    public String getFaceImageUrl()                   { return faceImageUrl; }
    public void setFaceImageUrl(String faceImageUrl)  { this.faceImageUrl = faceImageUrl; }

    public BigDecimal getMatchScore()               { return matchScore; }
    public void setMatchScore(BigDecimal matchScore){ this.matchScore = matchScore; }

    public String getVerifiedStatus()                       { return verifiedStatus; }
    public void setVerifiedStatus(String verifiedStatus)    { this.verifiedStatus = verifiedStatus; }

    public String getNote()                 { return note; }
    public void setNote(String note)        { this.note = note; }

    public Timestamp getVerifiedAt()                { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }

    public Timestamp getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Timestamp createdAt)   { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt)   { this.updatedAt = updatedAt; }

    public int getIsDeleted()                   { return isDeleted; }
    public void setIsDeleted(int isDeleted)     { this.isDeleted = isDeleted; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isPending()   { return "PENDING".equals(verifiedStatus); }
    public boolean isApproved()  { return "APPROVED".equals(verifiedStatus); }
    public boolean isRejected()  { return "REJECTED".equals(verifiedStatus); }

    @Override
    public String toString() {
        return "Ekyc{kycId=" + kycId +
               ", userId=" + userId +
               ", idNumber='" + idNumber + "'" +
               ", status='" + verifiedStatus + "'}";
    }
}
