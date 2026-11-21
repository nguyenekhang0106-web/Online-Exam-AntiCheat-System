package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public final class ExamDao extends QuestionCatalogDao {
    private static final String STAMP = "(SELECT COALESCE(MAX(a.id),0) FROM audit_logs a WHERE a.target_type='exams' AND a.target_id=e.id)";
    public List<Map<String,Object>> list(Connection c, Long owner, String term, String status, String sort, int page) throws SQLException {
        String order = switch(sort) {case "title" -> "e.title,e.id"; case "latest" -> "e.id DESC"; default -> throw new IllegalArgumentException();};
        return rows(c,"SELECT e.id,e.title,e.status,e.max_attempts,s.subject_name FROM exams e JOIN subjects s ON s.id=e.subject_id WHERE (? IS NULL OR e.created_by=?) AND LOCATE(?,e.title)>0 AND (?='' OR e.status=?) ORDER BY "+order+" LIMIT 21 OFFSET ?",owner,owner,term,status,status,(page-1L)*20);
    }
    public Map<String,Object> exam(Connection c,long id,boolean lock) throws SQLException {
        // Acquire the shared editing lock before reading the latest committed audit stamp.
        if(lock)one(c,"SELECT id FROM exams WHERE id=? FOR UPDATE",id);
        return one(c,"SELECT e.*,s.subject_name,"+STAMP+" AS stamp FROM exams e JOIN subjects s ON s.id=e.subject_id WHERE e.id=?",id);
    }
    public void audit(Connection c,long actor,long exam,String action) throws SQLException {
        update(c,"INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code) VALUES(?,'exams',?,?)",actor,exam,action);
    }
    public boolean used(Connection c,long exam) throws SQLException {
        return exists(c,"SELECT 1 FROM exam_course_assignments WHERE exam_id=? UNION ALL SELECT 1 FROM exam_student_assignments WHERE exam_id=? UNION ALL SELECT 1 FROM exam_attempts WHERE exam_id=? LIMIT 1",exam,exam,exam);
    }
    public long create(Connection c,long subject,long owner,String title,int duration,int attempts,BigDecimal pass) throws SQLException {
        return insert(c,"INSERT INTO exams(subject_id,created_by,title,duration_minutes,max_attempts,pass_percentage) VALUES(?,?,?,?,?,?)",subject,owner,title,duration,attempts,pass);
    }
    public void edit(Connection c,long exam,String title,int duration,int attempts,BigDecimal pass) throws SQLException {
        update(c,"UPDATE exams SET title=?,duration_minutes=?,max_attempts=?,pass_percentage=? WHERE id=?",title,duration,attempts,pass,exam);
    }
    public void transition(Connection c,long exam,String state) throws SQLException {
        if ("PUBLISHED".equals(state)) {
            // Preserve both exam configuration and the complete optional monitoring policy at publication.
            update(c,"""
                UPDATE exams e LEFT JOIN proctoring_policies p ON p.id=e.policy_id SET e.status='PUBLISHED',e.published_at=UTC_TIMESTAMP(3),
                e.policy_snapshot_json=JSON_OBJECT('duration_minutes',e.duration_minutes,'max_attempts',e.max_attempts,'pass_percentage',e.pass_percentage,
                'grading_policy',e.grading_policy,'attempt_score_policy',e.attempt_score_policy,'result_release_mode',e.result_release_mode,
                'answer_release_mode',e.answer_release_mode,'shuffle_questions',e.shuffle_questions,'shuffle_options',e.shuffle_options,
                'policy_id',p.id,'policy_name',p.policy_name,'require_fullscreen',p.require_fullscreen,'warn_on_focus_loss',p.warn_on_focus_loss,
                'detect_paste',p.detect_paste,'max_tab_switches',p.max_tab_switches,'max_fullscreen_exits',p.max_fullscreen_exits,
                'max_parallel_sessions',p.max_parallel_sessions,'require_camera',p.require_camera) WHERE e.id=?
                """,exam);
        } else update(c,"UPDATE exams SET status=? WHERE id=?",state,exam);
    }
    public void selection(Connection c,long id,String mode,Integer count)throws SQLException{
        update(c,"UPDATE exams SET selection_mode=?,question_count=? WHERE id=?",mode,count,id);
    }
    public void options(Connection c,long id,String instructions,boolean shuffleQuestions,boolean shuffleOptions,
                        String scorePolicy,String resultRelease,String answerRelease)throws SQLException{
        update(c,"UPDATE exams SET instructions=NULLIF(?,''),shuffle_questions=?,shuffle_options=?,attempt_score_policy=?,result_release_mode=?,answer_release_mode=? WHERE id=?",
                instructions,shuffleQuestions,shuffleOptions,scorePolicy,resultRelease,answerRelease,id);
    }
    public List<Map<String,Object>> policies(Connection c,long owner)throws SQLException{
        return rows(c,"SELECT id,policy_name FROM proctoring_policies WHERE created_by=? ORDER BY policy_name,id",owner);
    }
    public void policy(Connection c,long exam,long owner,Long policy)throws SQLException{
        if(policy!=null)one(c,"SELECT id FROM proctoring_policies WHERE id=? AND created_by=? FOR UPDATE",policy,owner);
        update(c,"UPDATE exams SET policy_id=? WHERE id=?",policy,exam);
    }
    public boolean ongoing(Connection c,long exam) throws SQLException {
        return exists(c,"""
            SELECT 1 FROM exam_attempts WHERE exam_id=? AND status='IN_PROGRESS'
            UNION ALL SELECT 1 FROM exam_course_assignments WHERE exam_id=? AND UTC_TIMESTAMP(3)>=opens_at AND UTC_TIMESTAMP(3)<closes_at
            UNION ALL SELECT 1 FROM exam_student_assignments WHERE exam_id=? AND UTC_TIMESTAMP(3)>=opens_at AND UTC_TIMESTAMP(3)<closes_at LIMIT 1
            """,exam,exam,exam);
    }
}
