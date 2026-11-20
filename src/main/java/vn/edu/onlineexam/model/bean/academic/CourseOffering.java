package vn.edu.onlineexam.model.bean.academic;

import java.time.LocalDateTime;

/**
 * JDBC data bean for {@code course_offerings}. Foreign keys are represented by IDs.
 * Date/time values represent UTC; JSON columns contain JSON text.
 * No authorization, validation or persistence logic belongs in this bean.
 */
public class CourseOffering {
    private Long id;
    private Long subjectId;
    private Long classId;
    private Long teacherId;
    private String academicYear;
    private String semester;
    private String status;
    private LocalDateTime createdAt;

    public CourseOffering() {
    }

    public CourseOffering(
            Long id,
            Long subjectId,
            Long classId,
            Long teacherId,
            String academicYear,
            String semester,
            String status,
            LocalDateTime createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.classId = classId;
        this.teacherId = teacherId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.status = status;
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

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
