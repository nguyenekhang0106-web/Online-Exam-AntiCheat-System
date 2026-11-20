package vn.edu.onlineexam.model.bean.proctoring;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code attempt_sessions}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AttemptSession {
    private Long id;
    private Long attemptId;
    private String sessionTokenHash;
    private String deviceHash;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime openedAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime closedAt;
    private String closeReason;

    public AttemptSession() {
    }

    public AttemptSession(
            Long id,
            Long attemptId,
            String sessionTokenHash,
            String deviceHash,
            String ipAddress,
            String userAgent,
            LocalDateTime openedAt,
            LocalDateTime lastSeenAt,
            LocalDateTime closedAt,
            String closeReason) {
        this.id = id;
        this.attemptId = attemptId;
        this.sessionTokenHash = sessionTokenHash;
        this.deviceHash = deviceHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.openedAt = openedAt;
        this.lastSeenAt = lastSeenAt;
        this.closedAt = closedAt;
        this.closeReason = closeReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getSessionTokenHash() {
        return sessionTokenHash;
    }

    public void setSessionTokenHash(String sessionTokenHash) {
        this.sessionTokenHash = sessionTokenHash;
    }

    public String getDeviceHash() {
        return deviceHash;
    }

    public void setDeviceHash(String deviceHash) {
        this.deviceHash = deviceHash;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }
}
