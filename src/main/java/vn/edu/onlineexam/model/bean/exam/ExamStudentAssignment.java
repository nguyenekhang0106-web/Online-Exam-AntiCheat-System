package vn.edu.onlineexam.model.bean.exam;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code exam_student_assignments}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ExamStudentAssignment {
    private Long id;
    private Long examId;
    private Long studentId;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private Short maxAttemptsOverride;
    private Long assignedBy;
    private LocalDateTime createdAt;

    public ExamStudentAssignment() {
    }

    public ExamStudentAssignment(
            Long id,
            Long examId,
            Long studentId,
            LocalDateTime opensAt,
            LocalDateTime closesAt,
            Short maxAttemptsOverride,
            Long assignedBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.maxAttemptsOverride = maxAttemptsOverride;
        this.assignedBy = assignedBy;
        this.createdAt = createdAt;
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

    public LocalDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public Short getMaxAttemptsOverride() {
        return maxAttemptsOverride;
    }

    public void setMaxAttemptsOverride(Short maxAttemptsOverride) {
        this.maxAttemptsOverride = maxAttemptsOverride;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
