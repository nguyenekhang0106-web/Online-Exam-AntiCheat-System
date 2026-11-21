package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class ExamAssignmentDao extends QuestionCatalogDao {
    public enum Scope {
        COURSE("exam_course_assignments","offering_id"), STUDENT("exam_student_assignments","student_id");
        final String table,column;
        Scope(String table,String column){this.table=table;this.column=column;}
    }
    private static final String ENROLLED = """
        FROM course_enrollments n JOIN course_offerings o ON o.id=n.offering_id JOIN users u ON u.id=n.student_id
        WHERE n.status='ENROLLED' AND o.status='ACTIVE' AND u.status='ACTIVE'
        AND EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=u.id AND r.role_code='STUDENT')
        """;
    public List<Map<String,Object>> targets(Connection c,long subject,long teacher,Scope scope) throws SQLException {
        if(scope==Scope.COURSE)return rows(c,"SELECT co.id,CONCAT(cl.class_name,' · ',co.academic_year,' · ',co.semester,' (#',co.id,')') label FROM course_offerings co JOIN classes cl ON cl.id=co.class_id WHERE co.subject_id=? AND co.teacher_id=? AND co.status='ACTIVE' AND EXISTS(SELECT 1 "+ENROLLED+" AND n.offering_id=co.id) ORDER BY label",subject,teacher);
        return rows(c,"SELECT DISTINCT u.id,u.full_name label "+ENROLLED+" AND o.subject_id=? AND o.teacher_id=? ORDER BY label",subject,teacher);
    }
    public void validateTarget(Connection c,long subject,long teacher,Scope scope,long target) throws SQLException {
        one(c,"SELECT n.student_id "+ENROLLED+" AND o.subject_id=? AND o.teacher_id=? AND "+(scope==Scope.COURSE?"o.id":"u.id")+"=? LIMIT 1 FOR UPDATE",subject,teacher,target);
    }
    public List<Map<String,Object>> schedule(Connection c,Long exam,Long student) throws SQLException {
        String fields="SELECT e.id exam_id,e.title,a.id,a.opens_at,a.closes_at,a.max_attempts_override,e.max_attempts,CASE WHEN UTC_TIMESTAMP(3)<a.opens_at THEN 'UPCOMING' WHEN UTC_TIMESTAMP(3)>=a.closes_at THEN 'ENDED' ELSE 'OPEN' END window_state,";
        String courses=fields+"'COURSE' scope,a.offering_id target FROM exam_course_assignments a JOIN exams e ON e.id=a.exam_id JOIN course_offerings o ON o.id=a.offering_id WHERE (? IS NULL OR e.id=?) AND (? IS NULL OR (e.status='PUBLISHED' AND o.status='ACTIVE' AND EXISTS(SELECT 1 FROM course_enrollments n WHERE n.offering_id=o.id AND n.student_id=? AND n.status='ENROLLED')))";
        String students=fields+"'STUDENT' scope,a.student_id target FROM exam_student_assignments a JOIN exams e ON e.id=a.exam_id WHERE (? IS NULL OR e.id=?) AND (? IS NULL OR (e.status='PUBLISHED' AND a.student_id=?))";
        return rows(c,courses+" UNION ALL "+students+" ORDER BY opens_at,scope,id",exam,exam,student,student,exam,exam,student,student);
    }
    public void change(Connection c,long exam,long actor,Scope scope,long target,LocalDateTime start,LocalDateTime end,Integer attempts,String action) throws SQLException {
        if(exists(c,"SELECT 1 FROM exam_attempts WHERE exam_id=? LIMIT 1",exam))throw new IllegalStateException();
        String match=" WHERE exam_id=? AND "+scope.column+"=?";
        if(!action.equals("assign")){
            one(c,"SELECT id FROM "+scope.table+match+" FOR UPDATE",exam,target);
            if(exists(c,"SELECT 1 FROM "+scope.table+match+" AND opens_at<=UTC_TIMESTAMP(3)",exam,target))throw new IllegalStateException();
        }
        if(action.equals("cancel"))update(c,"DELETE FROM "+scope.table+match,exam,target);
        else if(action.equals("reschedule"))update(c,"UPDATE "+scope.table+" SET opens_at=?,closes_at=?,max_attempts_override=?"+match,start,end,attempts,exam,target);
        else insert(c,"INSERT INTO "+scope.table+"(exam_id,"+scope.column+",opens_at,closes_at,max_attempts_override,assigned_by) VALUES(?,?,?,?,?,?)",exam,target,start,end,attempts,actor);
    }
}
