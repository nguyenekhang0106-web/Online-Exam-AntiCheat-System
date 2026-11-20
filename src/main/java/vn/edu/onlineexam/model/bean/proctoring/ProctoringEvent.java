package vn.edu.onlineexam.model.bean.proctoring;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code proctoring_events}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ProctoringEvent {
    private Long id;
    private Long attemptId;
    private Long attemptSessionId;
    private String eventCode;
    private String severity;
    private LocalDateTime clientOccurredAt;
    private LocalDateTime serverReceivedAt;
    private String clientEventId;
    private String detailJson;

    public ProctoringEvent() {
    }

    public ProctoringEvent(
            Long id,
            Long attemptId,
            Long attemptSessionId,
            String eventCode,
            String severity,
            LocalDateTime clientOccurredAt,
            LocalDateTime serverReceivedAt,
            String clientEventId,
            String detailJson) {
        this.id = id;
        this.attemptId = attemptId;
        this.attemptSessionId = attemptSessionId;
        this.eventCode = eventCode;
        this.severity = severity;
        this.clientOccurredAt = clientOccurredAt;
        this.serverReceivedAt = serverReceivedAt;
        this.clientEventId = clientEventId;
        this.detailJson = detailJson;
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

    public Long getAttemptSessionId() {
        return attemptSessionId;
    }

    public void setAttemptSessionId(Long attemptSessionId) {
        this.attemptSessionId = attemptSessionId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDateTime getClientOccurredAt() {
        return clientOccurredAt;
    }

    public void setClientOccurredAt(LocalDateTime clientOccurredAt) {
        this.clientOccurredAt = clientOccurredAt;
    }

    public LocalDateTime getServerReceivedAt() {
        return serverReceivedAt;
    }

    public void setServerReceivedAt(LocalDateTime serverReceivedAt) {
        this.serverReceivedAt = serverReceivedAt;
    }

    public String getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(String clientEventId) {
        this.clientEventId = clientEventId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }
}
