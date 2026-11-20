package vn.edu.onlineexam.model.bean.academic;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code course_enrollments}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class CourseEnrollment {
    private Long offeringId;
    private Long studentId;
    private LocalDateTime enrolledAt;
    private String status;

    public CourseEnrollment() {
    }

    public CourseEnrollment(
            Long offeringId,
            Long studentId,
            LocalDateTime enrolledAt,
            String status) {
        this.offeringId = offeringId;
        this.studentId = studentId;
        this.enrolledAt = enrolledAt;
        this.status = status;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
