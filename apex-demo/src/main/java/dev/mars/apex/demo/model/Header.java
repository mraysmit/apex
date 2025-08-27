package dev.mars.apex.demo.model;

import java.time.Instant;

/**
 * Message Header for trade confirmations
 * Contains routing and identification information
 */
public class Header {
    
    private String messageId;
    private String sentBy;
    private String sendTo;
    private Instant creationTimestamp;

    public Header() {
    }

    public Header(String messageId, String sentBy, String sendTo, Instant creationTimestamp) {
        this.messageId = messageId;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.creationTimestamp = creationTimestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public Instant getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    @Override
    public String toString() {
        return "Header{" +
                "messageId='" + messageId + '\'' +
                ", sentBy='" + sentBy + '\'' +
                ", sendTo='" + sendTo + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                '}';
    }
}
