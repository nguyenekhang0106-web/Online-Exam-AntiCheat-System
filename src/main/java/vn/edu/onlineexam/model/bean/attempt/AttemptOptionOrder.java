package vn.edu.onlineexam.model.bean.attempt;


/**
 * JDBC data bean for {@code attempt_option_orders}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AttemptOptionOrder {
    private Long attemptQuestionId;
    private Long optionId;
    private Short displayOrder;

    public AttemptOptionOrder() {
    }

    public AttemptOptionOrder(
            Long attemptQuestionId,
            Long optionId,
            Short displayOrder) {
        this.attemptQuestionId = attemptQuestionId;
        this.optionId = optionId;
        this.displayOrder = displayOrder;
    }

    public Long getAttemptQuestionId() {
        return attemptQuestionId;
    }

    public void setAttemptQuestionId(Long attemptQuestionId) {
        this.attemptQuestionId = attemptQuestionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public Short getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Short displayOrder) {
        this.displayOrder = displayOrder;
    }
}
