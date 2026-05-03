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
public class SupportTicketMessage {
    private int messageId;         
    private int ticketId;           
    private int senderId;         
    private String senderType;     
    private String content;        
    private LocalDateTime createdAt; 
    private int isInternalNote;    
    private int isDeleted;

    public SupportTicketMessage() {
    }

    public SupportTicketMessage(int messageId, int ticketId, int senderId, String senderType, String content, LocalDateTime createdAt, int isInternalNote, int isDeleted) {
        this.messageId = messageId;
        this.ticketId = ticketId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.content = content;
        this.createdAt = createdAt;
        this.isInternalNote = isInternalNote;
        this.isDeleted = isDeleted;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getIsInternalNote() {
        return isInternalNote;
    }

    public void setIsInternalNote(int isInternalNote) {
        this.isInternalNote = isInternalNote;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }
    
    
}
