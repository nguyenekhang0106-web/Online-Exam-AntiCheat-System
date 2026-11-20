package vn.edu.onlineexam.model.bean.auth;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code login_sessions}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class LoginSession {
    private Long id;
    private Long userId;
    private String sessionHash;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    public LoginSession() {
    }

    public LoginSession(
            Long id,
            Long userId,
            String sessionHash,
            String ipAddress,
            String userAgent,
            LocalDateTime createdAt,
            LocalDateTime lastSeenAt,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt) {
        this.id = id;
        this.userId = userId;
        this.sessionHash = sessionHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionHash() {
        return sessionHash;
    }

    public void setSessionHash(String sessionHash) {
        this.sessionHash = sessionHash;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
