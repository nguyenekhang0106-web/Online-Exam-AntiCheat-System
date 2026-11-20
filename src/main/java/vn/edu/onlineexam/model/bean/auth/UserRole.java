package vn.edu.onlineexam.model.bean.auth;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code user_roles}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class UserRole {
    private Long userId;
    private Short roleId;
    private LocalDateTime assignedAt;
    private Long assignedBy;

    public UserRole() {
    }

    public UserRole(
            Long userId,
            Short roleId,
            LocalDateTime assignedAt,
            Long assignedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Short getRoleId() {
        return roleId;
    }

    public void setRoleId(Short roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }
}
