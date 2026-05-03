/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

/**
 *
 * @author 84941
 */
public class TransactionType {
    private String typeCode;
    private String typeName;
    private String description;
    private int isDeleted;

    public TransactionType() {
    }

    public TransactionType(String typeCode, String typeName, String description, int isDeleted) {
        this.typeCode = typeCode;
        this.typeName = typeName;
        this.description = description;
        this.isDeleted = isDeleted;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
}
