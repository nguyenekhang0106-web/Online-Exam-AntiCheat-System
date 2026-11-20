package vn.edu.onlineexam.model.bean.exam;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code proctoring_policies}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ProctoringPolicy {
    private Long id;
    private String policyName;
    private Boolean requireFullscreen;
    private Boolean warnOnFocusLoss;
    private Boolean detectPaste;
    private Integer maxTabSwitches;
    private Integer maxFullscreenExits;
    private Short maxParallelSessions;
    private Boolean requireCamera;
    private Long createdBy;
    private LocalDateTime createdAt;

    public ProctoringPolicy() {
    }

    public ProctoringPolicy(
            Long id,
            String policyName,
            Boolean requireFullscreen,
            Boolean warnOnFocusLoss,
            Boolean detectPaste,
            Integer maxTabSwitches,
            Integer maxFullscreenExits,
            Short maxParallelSessions,
            Boolean requireCamera,
            Long createdBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.policyName = policyName;
        this.requireFullscreen = requireFullscreen;
        this.warnOnFocusLoss = warnOnFocusLoss;
        this.detectPaste = detectPaste;
        this.maxTabSwitches = maxTabSwitches;
        this.maxFullscreenExits = maxFullscreenExits;
        this.maxParallelSessions = maxParallelSessions;
        this.requireCamera = requireCamera;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Boolean getRequireFullscreen() {
        return requireFullscreen;
    }

    public void setRequireFullscreen(Boolean requireFullscreen) {
        this.requireFullscreen = requireFullscreen;
    }

    public Boolean getWarnOnFocusLoss() {
        return warnOnFocusLoss;
    }

    public void setWarnOnFocusLoss(Boolean warnOnFocusLoss) {
        this.warnOnFocusLoss = warnOnFocusLoss;
    }

    public Boolean getDetectPaste() {
        return detectPaste;
    }

    public void setDetectPaste(Boolean detectPaste) {
        this.detectPaste = detectPaste;
    }

    public Integer getMaxTabSwitches() {
        return maxTabSwitches;
    }

    public void setMaxTabSwitches(Integer maxTabSwitches) {
        this.maxTabSwitches = maxTabSwitches;
    }

    public Integer getMaxFullscreenExits() {
        return maxFullscreenExits;
    }

    public void setMaxFullscreenExits(Integer maxFullscreenExits) {
        this.maxFullscreenExits = maxFullscreenExits;
    }

    public Short getMaxParallelSessions() {
        return maxParallelSessions;
    }

    public void setMaxParallelSessions(Short maxParallelSessions) {
        this.maxParallelSessions = maxParallelSessions;
    }

    public Boolean getRequireCamera() {
        return requireCamera;
    }

    public void setRequireCamera(Boolean requireCamera) {
        this.requireCamera = requireCamera;
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
