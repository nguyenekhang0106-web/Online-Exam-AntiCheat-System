package vn.edu.onlineexam.model.bean.question;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code question_banks}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class QuestionBank {
    private Long id;
    private Long subjectId;
    private Long ownerId;
    private String bankName;
    private String description;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public QuestionBank() {
    }

    public QuestionBank(
            Long id,
            Long subjectId,
            Long ownerId,
            String bankName,
            String description,
            String visibility,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.ownerId = ownerId;
        this.bankName = bankName;
        this.description = description;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
