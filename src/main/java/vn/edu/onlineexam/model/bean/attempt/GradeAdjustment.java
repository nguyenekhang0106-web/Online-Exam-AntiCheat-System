package vn.edu.onlineexam.model.bean.attempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code grade_adjustments}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class GradeAdjustment {
    private Long id;
    private Long attemptId;
    private Long attemptQuestionId;
    private Long graderId;
    private BigDecimal oldPoints;
    private BigDecimal newPoints;
    private String reason;
    private LocalDateTime createdAt;

    public GradeAdjustment() {
    }

    public GradeAdjustment(
            Long id,
            Long attemptId,
            Long attemptQuestionId,
            Long graderId,
            BigDecimal oldPoints,
            BigDecimal newPoints,
            String reason,
            LocalDateTime createdAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.attemptQuestionId = attemptQuestionId;
        this.graderId = graderId;
        this.oldPoints = oldPoints;
        this.newPoints = newPoints;
        this.reason = reason;
        this.createdAt = createdAt;
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

    public Long getAttemptQuestionId() {
        return attemptQuestionId;
    }

    public void setAttemptQuestionId(Long attemptQuestionId) {
        this.attemptQuestionId = attemptQuestionId;
    }

    public Long getGraderId() {
        return graderId;
    }

    public void setGraderId(Long graderId) {
        this.graderId = graderId;
    }

    public BigDecimal getOldPoints() {
        return oldPoints;
    }

    public void setOldPoints(BigDecimal oldPoints) {
        this.oldPoints = oldPoints;
    }

    public BigDecimal getNewPoints() {
        return newPoints;
    }

    public void setNewPoints(BigDecimal newPoints) {
        this.newPoints = newPoints;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
