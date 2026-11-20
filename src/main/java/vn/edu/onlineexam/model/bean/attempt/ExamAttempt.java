package vn.edu.onlineexam.model.bean.attempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code exam_attempts}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ExamAttempt {
    private Long id;
    private Long examId;
    private Long studentId;
    private Short attemptNo;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime submittedAt;
    private LocalDateTime lastSavedAt;
    private String status;
    private String submitReason;
    private BigDecimal totalScore;
    private BigDecimal maxScore;
    private BigDecimal percentage;
    private Boolean passed;
    private LocalDateTime gradedAt;

    public ExamAttempt() {
    }

    public ExamAttempt(
            Long id,
            Long examId,
            Long studentId,
            Short attemptNo,
            LocalDateTime startedAt,
            LocalDateTime deadlineAt,
            LocalDateTime submittedAt,
            LocalDateTime lastSavedAt,
            String status,
            String submitReason,
            BigDecimal totalScore,
            BigDecimal maxScore,
            BigDecimal percentage,
            Boolean passed,
            LocalDateTime gradedAt) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.attemptNo = attemptNo;
        this.startedAt = startedAt;
        this.deadlineAt = deadlineAt;
        this.submittedAt = submittedAt;
        this.lastSavedAt = lastSavedAt;
        this.status = status;
        this.submitReason = submitReason;
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.percentage = percentage;
        this.passed = passed;
        this.gradedAt = gradedAt;
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Short getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Short attemptNo) {
        this.attemptNo = attemptNo;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubmitReason() {
        return submitReason;
    }

    public void setSubmitReason(String submitReason) {
        this.submitReason = submitReason;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }
}
