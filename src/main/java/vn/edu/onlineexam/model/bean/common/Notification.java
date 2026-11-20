package vn.edu.onlineexam.model.bean.common;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code notifications}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class Notification {
    private Long id;
    private Long recipientId;
    private String type;
    private String title;
    private String body;
    private String linkPath;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(
            Long id,
            Long recipientId,
            String type,
            String title,
            String body,
            String linkPath,
            LocalDateTime readAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkPath = linkPath;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public void setLinkPath(String linkPath) {
        this.linkPath = linkPath;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
