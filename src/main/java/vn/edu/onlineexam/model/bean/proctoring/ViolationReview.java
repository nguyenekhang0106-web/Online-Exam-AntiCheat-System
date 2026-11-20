package vn.edu.onlineexam.model.bean.proctoring;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code violation_reviews}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ViolationReview {
    private Long id;
    private Long attemptId;
    private Long relatedEventId;
    private Long reviewerId;
    private String decision;
    private String reason;
    private LocalDateTime reviewedAt;

    public ViolationReview() {
    }

    public ViolationReview(
            Long id,
            Long attemptId,
            Long relatedEventId,
            Long reviewerId,
            String decision,
            String reason,
            LocalDateTime reviewedAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.relatedEventId = relatedEventId;
        this.reviewerId = reviewerId;
        this.decision = decision;
        this.reason = reason;
        this.reviewedAt = reviewedAt;
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

    public Long getRelatedEventId() {
        return relatedEventId;
    }

    public void setRelatedEventId(Long relatedEventId) {
        this.relatedEventId = relatedEventId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
