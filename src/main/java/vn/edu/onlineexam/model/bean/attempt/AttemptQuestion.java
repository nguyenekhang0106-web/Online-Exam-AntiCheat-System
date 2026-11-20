package vn.edu.onlineexam.model.bean.attempt;

import java.math.BigDecimal;

/**
 * JDBC data bean for {@code attempt_questions}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AttemptQuestion {
    private Long id;
    private Long attemptId;
    private Long examQuestionId;
    private Integer displayOrder;
    private BigDecimal pointsSnapshot;

    public AttemptQuestion() {
    }

    public AttemptQuestion(
            Long id,
            Long attemptId,
            Long examQuestionId,
            Integer displayOrder,
            BigDecimal pointsSnapshot) {
        this.id = id;
        this.attemptId = attemptId;
        this.examQuestionId = examQuestionId;
        this.displayOrder = displayOrder;
        this.pointsSnapshot = pointsSnapshot;
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

    public Long getExamQuestionId() {
        return examQuestionId;
    }

    public void setExamQuestionId(Long examQuestionId) {
        this.examQuestionId = examQuestionId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getPointsSnapshot() {
        return pointsSnapshot;
    }

    public void setPointsSnapshot(BigDecimal pointsSnapshot) {
        this.pointsSnapshot = pointsSnapshot;
    }
}
