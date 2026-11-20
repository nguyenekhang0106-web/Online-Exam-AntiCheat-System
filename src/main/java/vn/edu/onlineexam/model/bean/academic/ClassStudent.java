package vn.edu.onlineexam.model.bean.academic;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code class_students}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class ClassStudent {
    private Long classId;
    private Long studentId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private String status;

    public ClassStudent() {
    }

    public ClassStudent(
            Long classId,
            Long studentId,
            LocalDateTime joinedAt,
            LocalDateTime leftAt,
            String status) {
        this.classId = classId;
        this.studentId = studentId;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.status = status;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
