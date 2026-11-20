package vn.edu.onlineexam.model.bean.exam;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code exams}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class Exam {
    private Long id;
    private Long subjectId;
    private Long createdBy;
    private String title;
    private String instructions;
    private Short durationMinutes;
    private Short maxAttempts;
    private BigDecimal passPercentage;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private String gradingPolicy;
    private String attemptScorePolicy;
    private String resultReleaseMode;
    private String answerReleaseMode;
    private Long policyId;
    private String policySnapshotJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    public Exam() {
    }

    public Exam(
            Long id,
            Long subjectId,
            Long createdBy,
            String title,
            String instructions,
            Short durationMinutes,
            Short maxAttempts,
            BigDecimal passPercentage,
            Boolean shuffleQuestions,
            Boolean shuffleOptions,
            String gradingPolicy,
            String attemptScorePolicy,
            String resultReleaseMode,
            String answerReleaseMode,
            Long policyId,
            String policySnapshotJson,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime publishedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.createdBy = createdBy;
        this.title = title;
        this.instructions = instructions;
        this.durationMinutes = durationMinutes;
        this.maxAttempts = maxAttempts;
        this.passPercentage = passPercentage;
        this.shuffleQuestions = shuffleQuestions;
        this.shuffleOptions = shuffleOptions;
        this.gradingPolicy = gradingPolicy;
        this.attemptScorePolicy = attemptScorePolicy;
        this.resultReleaseMode = resultReleaseMode;
        this.answerReleaseMode = answerReleaseMode;
        this.policyId = policyId;
        this.policySnapshotJson = policySnapshotJson;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Short getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Short durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Short getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Short maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BigDecimal getPassPercentage() {
        return passPercentage;
    }

    public void setPassPercentage(BigDecimal passPercentage) {
        this.passPercentage = passPercentage;
    }

    public Boolean getShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(Boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public Boolean getShuffleOptions() {
        return shuffleOptions;
    }

    public void setShuffleOptions(Boolean shuffleOptions) {
        this.shuffleOptions = shuffleOptions;
    }

    public String getGradingPolicy() {
        return gradingPolicy;
    }

    public void setGradingPolicy(String gradingPolicy) {
        this.gradingPolicy = gradingPolicy;
    }

    public String getAttemptScorePolicy() {
        return attemptScorePolicy;
    }

    public void setAttemptScorePolicy(String attemptScorePolicy) {
        this.attemptScorePolicy = attemptScorePolicy;
    }

    public String getResultReleaseMode() {
        return resultReleaseMode;
    }

    public void setResultReleaseMode(String resultReleaseMode) {
        this.resultReleaseMode = resultReleaseMode;
    }

    public String getAnswerReleaseMode() {
        return answerReleaseMode;
    }

    public void setAnswerReleaseMode(String answerReleaseMode) {
        this.answerReleaseMode = answerReleaseMode;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicySnapshotJson() {
        return policySnapshotJson;
    }

    public void setPolicySnapshotJson(String policySnapshotJson) {
        this.policySnapshotJson = policySnapshotJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
