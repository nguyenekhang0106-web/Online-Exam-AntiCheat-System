package vn.edu.onlineexam.model.bean.attempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code attempt_answers}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AttemptAnswer {
    private Long id;
    private Long attemptQuestionId;
    private LocalDateTime lastSavedAt;
    private LocalDateTime gradedAt;
    private BigDecimal awardedPoints;
    private Boolean isCorrect;
    private LocalDateTime updatedAt;

    public AttemptAnswer() {
    }

    public AttemptAnswer(
            Long id,
            Long attemptQuestionId,
            LocalDateTime lastSavedAt,
            LocalDateTime gradedAt,
            BigDecimal awardedPoints,
            Boolean isCorrect,
            LocalDateTime updatedAt) {
        this.id = id;
        this.attemptQuestionId = attemptQuestionId;
        this.lastSavedAt = lastSavedAt;
        this.gradedAt = gradedAt;
        this.awardedPoints = awardedPoints;
        this.isCorrect = isCorrect;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttemptQuestionId() {
        return attemptQuestionId;
    }

    public void setAttemptQuestionId(Long attemptQuestionId) {
        this.attemptQuestionId = attemptQuestionId;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public BigDecimal getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(BigDecimal awardedPoints) {
        this.awardedPoints = awardedPoints;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
