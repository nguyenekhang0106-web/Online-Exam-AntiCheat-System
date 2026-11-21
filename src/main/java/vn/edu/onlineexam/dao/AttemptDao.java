package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

/** All locks and writes belong to the service transaction. */
public final class AttemptDao extends QuestionCatalogDao {
    public static long num(Map<String,Object> row,String key){
        Object value=row.get(key);
        return value instanceof Boolean flag ? (flag?1:0) : ((Number)value).longValue();
    }
    public LocalDateTime now(Connection c)throws SQLException{
        try(var p=c.prepareStatement("SELECT UTC_TIMESTAMP(3)");var r=p.executeQuery()){r.next();return r.getObject(1,LocalDateTime.class);}
    }
    public Map<String,Object> exam(Connection c,long id)throws SQLException{
        return one(c,"SELECT id,title,status,max_attempts,duration_minutes,grading_policy,policy_snapshot_json,shuffle_questions,shuffle_options FROM exams WHERE id=? FOR UPDATE",id);
    }
    public List<Map<String,Object>> windows(Connection c,long exam,long student)throws SQLException{
        var personal=rows(c,"SELECT id grant_id,opens_at,closes_at,max_attempts_override,'PERSONAL' scope FROM exam_student_assignments WHERE exam_id=? AND student_id=? FOR UPDATE",exam,student);
        personal.addAll(rows(c,"""
            SELECT a.id grant_id,a.opens_at,a.closes_at,a.max_attempts_override,'COURSE' scope FROM exam_course_assignments a
            JOIN course_offerings o ON o.id=a.offering_id JOIN course_enrollments n ON n.offering_id=o.id
            JOIN exams e ON e.id=a.exam_id AND e.subject_id=o.subject_id
            WHERE a.exam_id=? AND n.student_id=? AND n.status='ENROLLED' AND o.status='ACTIVE' FOR UPDATE
            """,exam,student));
        return personal;
    }
    public List<Map<String,Object>> previous(Connection c,long exam,long student)throws SQLException{
        return rows(c,"SELECT id,attempt_no,status,deadline_at FROM exam_attempts WHERE exam_id=? AND student_id=? ORDER BY attempt_no DESC FOR UPDATE",exam,student);
    }
    public Map<String,Object> lock(Connection c,long id,long student)throws SQLException{
        return one(c,"""
            SELECT a.*,e.title,e.status exam_status,e.pass_percentage,e.grading_policy FROM exam_attempts a JOIN exams e ON e.id=a.exam_id
            WHERE a.id=? AND a.student_id=? FOR UPDATE
            """,id,student);
    }
    public long audit(Connection c,Long student,String type,long id,String action)throws SQLException{
        return insert(c,"INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code) VALUES(?,?,?,?)",student,type,id,action);
    }
    public void auditStart(Connection c,long student,long attempt,Map<String,Object> grant)throws SQLException{
        update(c,"""
            INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code,detail_json)
            VALUES(?,'exam_attempts',?,'ATTEMPT_START',JSON_OBJECT('scope',?,'assignment',?))
            """,student,attempt,grant.get("scope"),grant.get("grant_id"));
    }
    public boolean originalGrant(Connection c,long attempt,Map<String,Object> grant)throws SQLException{
        return exists(c,"""
            SELECT id FROM audit_logs WHERE target_type='exam_attempts' AND target_id=? AND action_code='ATTEMPT_START'
            AND detail_json->>'$.scope'=? AND detail_json->>'$.assignment'=? LIMIT 1
            """,attempt,grant.get("scope"),grant.get("grant_id").toString());
    }
    public List<Map<String,Object>> due(Connection c,long after)throws SQLException{
        return rows(c,"""
            SELECT a.id,a.student_id FROM exam_attempts a JOIN exams e ON e.id=a.exam_id
            WHERE a.id>? AND a.status='IN_PROGRESS' AND a.deadline_at<=UTC_TIMESTAMP(3)
            AND e.grading_policy='ALL_OR_NOTHING' AND EXISTS(SELECT 1 FROM attempt_questions q WHERE q.attempt_id=a.id)
            ORDER BY a.id LIMIT 50
            """,after);
    }
    public long create(Connection c,long exam,long student,int sequence,LocalDateTime now,LocalDateTime deadline)throws SQLException{
        return insert(c,"INSERT INTO exam_attempts(exam_id,student_id,attempt_no,started_at,deadline_at) VALUES(?,?,?,?,?)",exam,student,sequence,now,deadline);
    }
    private static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
    public void snapshot(Connection c,long attempt,long exam,boolean shuffleQuestions,boolean shuffleOptions)throws SQLException{
        var items=rows(c,"""
            SELECT x.id,x.fixed_order,x.points,x.question_version_id,v.question_id,e.selection_mode,e.question_count,q.question_type,b.subject_id=e.subject_id same_subject,
            EXISTS(SELECT 1 FROM question_attachments f WHERE f.question_version_id=v.id) attachment
            FROM exam_questions x JOIN exams e ON e.id=x.exam_id JOIN question_versions v ON v.id=x.question_version_id
            JOIN questions q ON q.id=v.question_id JOIN question_banks b ON b.id=q.bank_id WHERE x.exam_id=? ORDER BY x.fixed_order
            """,exam);
        if(!items.isEmpty() && "RANDOM_PER_ATTEMPT".equals(items.getFirst().get("selection_mode"))){
            int count=Math.toIntExact(num(items.getFirst(),"question_count"));
            var ids=new HashSet<Object>();var weight=(BigDecimal)items.getFirst().get("points");
            if(count<1 || count>items.size())throw new IllegalStateException("Số câu rút không hợp lệ.");
            for(var item:items){
                if(!ids.add(item.get("question_id")) || weight.compareTo((BigDecimal)item.get("points"))!=0)
                    throw new IllegalStateException("Tập nguồn không hợp lệ.");
            }
            Collections.shuffle(items,RANDOM);
            items=items.subList(0,count);
            if(!shuffleQuestions)items.sort(Comparator.comparingLong(row->num(row,"fixed_order")));
        }else if(shuffleQuestions)Collections.shuffle(items,RANDOM);
        int order=0;BigDecimal total=BigDecimal.ZERO;
        for(var item:items){
            if(num(item,"same_subject")!=1 || num(item,"attachment")!=0)throw new IllegalStateException("Nội dung đề chưa đủ điều kiện.");
            var options=rows(c,"SELECT id,CAST(is_correct AS UNSIGNED) correct FROM question_version_options WHERE question_version_id=? ORDER BY display_order",item.get("question_version_id"));
            long correct=options.stream().filter(o->num(o,"correct")==1).count();
            if(options.size()<2 || options.size()>32768 || correct==0 || ("SINGLE_CHOICE".equals(item.get("question_type")) && correct!=1))throw new IllegalStateException("Cấu hình phương án chưa hợp lệ.");
            if(shuffleOptions)Collections.shuffle(options,RANDOM);
            long aq=insert(c,"INSERT INTO attempt_questions(attempt_id,exam_question_id,display_order,points_snapshot) VALUES(?,?,?,?)",attempt,item.get("id"),++order,item.get("points"));
            int optionOrder=0;
            for(var opt:options)update(c,"INSERT INTO attempt_option_orders(attempt_question_id,option_id,display_order) VALUES(?,?,?)",aq,opt.get("id"),optionOrder++);
            total=total.add((BigDecimal)item.get("points"));
        }
        if(total.signum()<=0 || total.compareTo(new BigDecimal("9999999.99"))>0)throw new IllegalStateException("Tổng điểm đề chưa hợp lệ.");
    }
    private static final String ITEMS="""
        FROM attempt_questions aq JOIN exam_attempts a ON a.id=aq.attempt_id
        JOIN exam_questions x ON x.id=aq.exam_question_id AND x.exam_id=a.exam_id
        JOIN question_versions v ON v.id=x.question_version_id JOIN questions q ON q.id=v.question_id
        """;
    public List<Map<String,Object>> questions(Connection c,long attempt)throws SQLException{
        return rows(c,"SELECT aq.id,aq.display_order,aq.points_snapshot,v.content,q.question_type,"+
            "(SELECT COALESCE(MAX(l.id),0) FROM audit_logs l WHERE l.target_type='attempt_questions' AND l.target_id=aq.id) revision "+
            ITEMS+" WHERE aq.attempt_id=? ORDER BY aq.display_order",attempt);
    }
    public List<Map<String,Object>> options(Connection c,long attempt,long question)throws SQLException{
        return rows(c,"SELECT o.id,o.option_content,CAST(ch.option_id IS NOT NULL AS UNSIGNED) selected "+ITEMS+
            " JOIN attempt_option_orders ord ON ord.attempt_question_id=aq.id JOIN question_version_options o ON o.id=ord.option_id AND o.question_version_id=v.id LEFT JOIN attempt_answers ans ON ans.attempt_question_id=aq.id LEFT JOIN attempt_answer_choices ch ON ch.answer_id=ans.id AND ch.option_id=o.id WHERE aq.attempt_id=? AND aq.id=? ORDER BY ord.display_order",attempt,question);
    }
    public void save(Connection c,long attempt,long question,Set<Long> choices,LocalDateTime now)throws SQLException{
        update(c,"INSERT INTO attempt_answers(attempt_question_id,last_saved_at) VALUES(?,?) ON DUPLICATE KEY UPDATE last_saved_at=?",question,now,now);
        long answer=num(one(c,"SELECT id FROM attempt_answers WHERE attempt_question_id=?",question),"id");
        update(c,"DELETE FROM attempt_answer_choices WHERE answer_id=?",answer);
        for(long choice:choices)update(c,"INSERT INTO attempt_answer_choices(answer_id,option_id,selected_at) VALUES(?,?,?)",answer,choice,now);
        update(c,"UPDATE exam_attempts SET last_saved_at=? WHERE id=?",now,attempt);
    }
    public void grade(Connection c,Map<String,Object> attempt,LocalDateTime now,boolean timedOut)throws SQLException{
        long id=num(attempt,"id");
        if(!"ALL_OR_NOTHING".equals(attempt.get("grading_policy")))throw new IllegalStateException("Chưa có quy tắc chấm cho lượt này.");
        var items=rows(c,"""
            SELECT aq.id,aq.points_snapshot,q.question_type,
            (SELECT COUNT(*) FROM question_version_options o WHERE o.question_version_id=v.id) option_count,
            (SELECT COUNT(*) FROM question_version_options o WHERE o.question_version_id=v.id AND o.is_correct=1) key_count,
            (SELECT COUNT(*) FROM attempt_option_orders ord JOIN question_version_options o ON o.id=ord.option_id
             WHERE ord.attempt_question_id=aq.id AND o.question_version_id=v.id) snapshot_count,
            CAST(NOT EXISTS(SELECT 1 FROM question_version_options o LEFT JOIN attempt_answers ans ON ans.attempt_question_id=aq.id
                 LEFT JOIN attempt_answer_choices ch ON ch.answer_id=ans.id AND ch.option_id=o.id
                 WHERE o.question_version_id=v.id AND o.is_correct<>(ch.option_id IS NOT NULL)) AS UNSIGNED) correct
            """+ITEMS+" WHERE aq.attempt_id=?",id);
        BigDecimal max=BigDecimal.ZERO,total=BigDecimal.ZERO;
        for(var item:items){
            long keys=num(item,"key_count"),count=num(item,"option_count");
            if(count<2 || num(item,"snapshot_count")!=count || keys==0 || ("SINGLE_CHOICE".equals(item.get("question_type")) && keys!=1))
                throw new IllegalStateException("Phiên bản câu hỏi không đủ điều kiện chấm.");
            boolean correct=num(item,"correct")==1;
            var points=(BigDecimal)item.get("points_snapshot");var award=correct?points:BigDecimal.ZERO;
            update(c,"INSERT INTO attempt_answers(attempt_question_id,last_saved_at,graded_at,is_correct,awarded_points) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE graded_at=?,is_correct=?,awarded_points=?",item.get("id"),now,now,correct,award,now,correct,award);
            max=max.add(points);total=total.add(award);
        }
        if(max.signum()<=0)throw new IllegalStateException("Lượt thi thiếu điểm tối đa.");
        var percent=total.movePointRight(2).divide(max,2,java.math.RoundingMode.HALF_UP);
        update(c,"UPDATE exam_attempts SET status=?,submit_reason=?,submitted_at=?,graded_at=?,total_score=?,max_score=?,percentage=?,passed=? WHERE id=? AND status='IN_PROGRESS'",
            timedOut?"AUTO_SUBMITTED":"SUBMITTED",timedOut?"TIMEOUT":"STUDENT_SUBMIT",now,now,total,max,percent,percent.compareTo((BigDecimal)attempt.get("pass_percentage"))>=0,id);
    }
    public List<Map<String,Object>> history(Connection c,String role,long actor)throws SQLException{
        String release=role.equals("STUDENT")?"""
            (e.result_release_mode='IMMEDIATE' OR (e.result_release_mode='AFTER_CLOSE'
             AND NOT EXISTS(SELECT 1 FROM exam_student_assignments p WHERE p.exam_id=e.id AND p.student_id=a.student_id AND p.closes_at>UTC_TIMESTAMP(3))
             AND NOT EXISTS(SELECT 1 FROM exam_course_assignments p JOIN course_enrollments n ON n.offering_id=p.offering_id WHERE p.exam_id=e.id AND n.student_id=a.student_id AND n.status='ENROLLED' AND p.closes_at>UTC_TIMESTAMP(3))
             AND NOT EXISTS(SELECT 1 FROM exam_attempts t WHERE t.exam_id=e.id AND t.student_id=a.student_id AND t.status='IN_PROGRESS')))
            """:"TRUE";
        // V1–V7 have no separate administrative assignment for reviewing an attempt.
        // Exam ownership is a verifiable duty even for an account with several roles.
        String scope=switch(role){case "ADMIN","TEACHER"->"e.created_by=?";case "STUDENT"->"a.student_id=?";default->throw new SecurityException();};
        String sql="""
            WITH scored AS (SELECT a.id,a.exam_id,a.student_id,a.attempt_no,a.status,a.deadline_at,e.title,a.total_score,a.max_score,a.percentage,a.passed,
            (a.graded_at IS NOT NULL AND a.status IN ('SUBMITTED','AUTO_SUBMITTED')) eligible,
            """+release+"""
             visible,ROW_NUMBER() OVER(PARTITION BY e.id,a.student_id ORDER BY (a.graded_at IS NOT NULL AND a.status IN ('SUBMITTED','AUTO_SUBMITTED')) DESC,
             CASE WHEN e.attempt_score_policy='HIGHEST' THEN a.total_score END DESC,
             CASE WHEN e.attempt_score_policy='LATEST' THEN a.attempt_no END DESC,a.attempt_no) ranking
             FROM exam_attempts a JOIN exams e ON e.id=a.exam_id WHERE
            """+scope+"""
            ) SELECT id,student_id,attempt_no,status,deadline_at,title,IF(visible AND eligible,total_score,NULL) score,
            IF(visible AND eligible,max_score,NULL) maximum,IF(visible AND eligible,percentage,NULL) percentage,
            IF(visible AND eligible,passed,NULL) passed,IF(visible AND eligible AND ranking=1,1,0) chosen FROM scored ORDER BY id DESC
            """;
        return rows(c,sql,actor);
    }
}
