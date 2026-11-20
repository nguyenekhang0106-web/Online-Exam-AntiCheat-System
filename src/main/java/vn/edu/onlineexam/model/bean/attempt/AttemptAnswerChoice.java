package vn.edu.onlineexam.model.bean.attempt;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code attempt_answer_choices}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class AttemptAnswerChoice {
    private Long answerId;
    private Long optionId;
    private LocalDateTime selectedAt;

    public AttemptAnswerChoice() {
    }

    public AttemptAnswerChoice(
            Long answerId,
            Long optionId,
            LocalDateTime selectedAt) {
        this.answerId = answerId;
        this.optionId = optionId;
        this.selectedAt = selectedAt;
    }

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public LocalDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(LocalDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }
}
