package vn.edu.onlineexam.model.bean.question;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code topics}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class Topic {
    private Long id;
    private Long subjectId;
    private Long parentTopicId;
    private String topicCode;
    private String topicName;
    private Integer displayOrder;
    private LocalDateTime createdAt;

    public Topic() {
    }

    public Topic(
            Long id,
            Long subjectId,
            Long parentTopicId,
            String topicCode,
            String topicName,
            Integer displayOrder,
            LocalDateTime createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.parentTopicId = parentTopicId;
        this.topicCode = topicCode;
        this.topicName = topicName;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
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

    public Long getParentTopicId() {
        return parentTopicId;
    }

    public void setParentTopicId(Long parentTopicId) {
        this.parentTopicId = parentTopicId;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
