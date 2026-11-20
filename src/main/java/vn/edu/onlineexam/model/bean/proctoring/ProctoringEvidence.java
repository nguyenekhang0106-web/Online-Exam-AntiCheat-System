package vn.edu.onlineexam.model.bean.proctoring;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code proctoring_evidence}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ProctoringEvidence {
    private Long id;
    private Long eventId;
    private String storageKey;
    private String mediaType;
    private Long sizeBytes;
    private String sha256;
    private LocalDateTime capturedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime deletedAt;

    public ProctoringEvidence() {
    }

    public ProctoringEvidence(
            Long id,
            Long eventId,
            String storageKey,
            String mediaType,
            Long sizeBytes,
            String sha256,
            LocalDateTime capturedAt,
            LocalDateTime expiresAt,
            LocalDateTime deletedAt) {
        this.id = id;
        this.eventId = eventId;
        this.storageKey = storageKey;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.capturedAt = capturedAt;
        this.expiresAt = expiresAt;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
