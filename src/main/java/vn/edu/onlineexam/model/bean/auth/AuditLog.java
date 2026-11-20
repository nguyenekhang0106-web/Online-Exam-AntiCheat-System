package vn.edu.onlineexam.model.bean.auth;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code audit_logs}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AuditLog {
    private Long id;
    private Long actorUserId;
    private String actionCode;
    private String targetType;
    private Long targetId;
    private String ipAddress;
    private String detailJson;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(
            Long id,
            Long actorUserId,
            String actionCode,
            String targetType,
            Long targetId,
            String ipAddress,
            String detailJson,
            LocalDateTime createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actionCode = actionCode;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.detailJson = detailJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
