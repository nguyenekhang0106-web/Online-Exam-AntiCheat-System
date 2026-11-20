package vn.edu.onlineexam.model.bean.question;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code question_versions}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class QuestionVersion {
    private Long id;
    private Long questionId;
    private Integer versionNo;
    private String content;
    private String explanation;
    private BigDecimal defaultPoints;
    private Long createdBy;
    private LocalDateTime createdAt;

    public QuestionVersion() {
    }

    public QuestionVersion(
            Long id,
            Long questionId,
            Integer versionNo,
            String content,
            String explanation,
            BigDecimal defaultPoints,
            Long createdBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.versionNo = versionNo;
        this.content = content;
        this.explanation = explanation;
        this.defaultPoints = defaultPoints;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public BigDecimal getDefaultPoints() {
        return defaultPoints;
    }

    public void setDefaultPoints(BigDecimal defaultPoints) {
        this.defaultPoints = defaultPoints;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
