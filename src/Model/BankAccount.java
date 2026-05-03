/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

/**
 *
 * @author 84941
 */
public class BankAccount {
    private int bankAccountId;
    private int userId;
    private String bankName;
    private String accountNumber;
    private int isLinked;
    private int isDeleted;

    public BankAccount() {
    }

    public BankAccount(int bankAccountId, int userId, String bankName, String accountNumber, int isLinked, int isDeleted) {
        this.bankAccountId = bankAccountId;
        this.userId = userId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.isLinked = isLinked;
        this.isDeleted = isDeleted;
    }

    public int getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(int bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getIsLinked() {
        return isLinked;
    }

    public void setIsLinked(int isLinked) {
        this.isLinked = isLinked;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
}
