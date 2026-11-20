package vn.edu.onlineexam.model.bean.question;


/**
 * JDBC data bean for {@code question_version_options}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class QuestionVersionOption {
    private Long id;
    private Long questionVersionId;
    private String optionContent;
    private Boolean isCorrect;
    private Short displayOrder;

    public QuestionVersionOption() {
    }

    public QuestionVersionOption(
            Long id,
            Long questionVersionId,
            String optionContent,
            Boolean isCorrect,
            Short displayOrder) {
        this.id = id;
        this.questionVersionId = questionVersionId;
        this.optionContent = optionContent;
        this.isCorrect = isCorrect;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuestionVersionId() {
        return questionVersionId;
    }

    public void setQuestionVersionId(Long questionVersionId) {
        this.questionVersionId = questionVersionId;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Short getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Short displayOrder) {
        this.displayOrder = displayOrder;
    }
}
