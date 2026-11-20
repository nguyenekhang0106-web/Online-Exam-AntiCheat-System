package vn.edu.onlineexam.model.bean.common;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code exam_appeals}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ExamAppeal {
    private Long id;
    private Long attemptId;
    private Long studentId;
    private String content;
    private String status;
    private Long reviewerId;
    private String response;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public ExamAppeal() {
    }

    public ExamAppeal(
            Long id,
            Long attemptId,
            Long studentId,
            String content,
            String status,
            Long reviewerId,
            String response,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.content = content;
        this.status = status;
        this.reviewerId = reviewerId;
        this.response = response;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
