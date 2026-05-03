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
public class Deposit {
    private int depositId;
    private int transactionId;
    private String requestCode;
    private String paymentGateway;
    private String receivingAccount;
    private String qrString;
    private String bankTransRef;
    private LocalDateTime expiredAt;
    private int isDeleted;

    public Deposit() {
    }

    public Deposit(int depositId, int transactionId, String requestCode, String paymentGateway, String receivingAccount, String qrString, String bankTransRef, LocalDateTime expiredAt, int isDeleted) {
        this.depositId = depositId;
        this.transactionId = transactionId;
        this.requestCode = requestCode;
        this.paymentGateway = paymentGateway;
        this.receivingAccount = receivingAccount;
        this.qrString = qrString;
        this.bankTransRef = bankTransRef;
        this.expiredAt = expiredAt;
        this.isDeleted = isDeleted;
    }

    public int getDepositId() {
        return depositId;
    }

    public void setDepositId(int depositId) {
        this.depositId = depositId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(String paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public String getReceivingAccount() {
        return receivingAccount;
    }

    public void setReceivingAccount(String receivingAccount) {
        this.receivingAccount = receivingAccount;
    }

    public String getQrString() {
        return qrString;
    }

    public void setQrString(String qrString) {
        this.qrString = qrString;
    }

    public String getBankTransRef() {
        return bankTransRef;
    }

    public void setBankTransRef(String bankTransRef) {
        this.bankTransRef = bankTransRef;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
