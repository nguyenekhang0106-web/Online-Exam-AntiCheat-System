package vn.edu.onlineexam.model.bean.question;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code question_attachments}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class QuestionAttachment {
    private Long id;
    private Long questionVersionId;
    private Long optionId;
    private String storageKey;
    private String originalFilename;
    private String mediaType;
    private Long sizeBytes;
    private String sha256;
    private LocalDateTime createdAt;

    public QuestionAttachment() {
    }

    public QuestionAttachment(
            Long id,
            Long questionVersionId,
            Long optionId,
            String storageKey,
            String originalFilename,
            String mediaType,
            Long sizeBytes,
            String sha256,
            LocalDateTime createdAt) {
        this.id = id;
        this.questionVersionId = questionVersionId;
        this.optionId = optionId;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionVersionId() {
        return questionVersionId;
    }

    public void setQuestionVersionId(Long questionVersionId) {
        this.questionVersionId = questionVersionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
