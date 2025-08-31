package dev.mars.apex.demo.model;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.time.Instant;

/**
 * Message Header for trade confirmations
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
