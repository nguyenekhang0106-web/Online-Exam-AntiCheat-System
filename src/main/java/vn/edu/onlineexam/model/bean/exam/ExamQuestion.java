package vn.edu.onlineexam.model.bean.exam;

import java.math.BigDecimal;

/**
 * JDBC data bean for {@code exam_questions}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ExamQuestion {
    private Long id;
    private Long examId;
    private Long questionVersionId;
    private BigDecimal points;
    private Integer fixedOrder;

    public ExamQuestion() {
    }

    public ExamQuestion(
            Long id,
            Long examId,
            Long questionVersionId,
            BigDecimal points,
            Integer fixedOrder) {
        this.id = id;
        this.examId = examId;
        this.questionVersionId = questionVersionId;
        this.points = points;
        this.fixedOrder = fixedOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getQuestionVersionId() {
        return questionVersionId;
    }

    public void setQuestionVersionId(Long questionVersionId) {
        this.questionVersionId = questionVersionId;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public Integer getFixedOrder() {
        return fixedOrder;
    }

    public void setFixedOrder(Integer fixedOrder) {
        this.fixedOrder = fixedOrder;
    }
}
