package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public final class ProctoringEventDao extends QuestionCatalogDao {
    public record Filter(Long id,Long attempt,Long student,String code,LocalDateTime from,LocalDateTime until,int page,int size,boolean oldest){}
    public record Page(long count,List<Map<String,Object>> rows){}
    public Map<String,Object> lock(Connection c,long attempt,long student)throws SQLException{
        return one(c,"""
            SELECT a.status,a.deadline_at,e.policy_snapshot_json->>'$.warn_on_focus_loss' focus_policy,
            e.policy_snapshot_json->>'$.require_fullscreen' fullscreen_policy,e.policy_snapshot_json->>'$.detect_paste' paste_policy
            FROM exam_attempts a JOIN exams e ON e.id=a.exam_id WHERE a.id=? AND a.student_id=? FOR UPDATE
            """,attempt,student);
    }
    public List<Map<String,Object>> previous(Connection c,long attempt,String key)throws SQLException{
        return rows(c,"SELECT event_code,client_occurred_at FROM proctoring_events WHERE attempt_id=? AND client_event_id=?",attempt,key);
    }
    public boolean full(Connection c,long attempt)throws SQLException{
        return exists(c,"SELECT id FROM proctoring_events WHERE attempt_id=? AND server_received_at>UTC_TIMESTAMP(3)-INTERVAL 1 MINUTE LIMIT 1 OFFSET 29",attempt);
    }
    public void append(Connection c,long attempt,String key,String code,LocalDateTime occurred)throws SQLException{
        try(var p=c.prepareStatement("""
            INSERT INTO proctoring_events(attempt_id,client_event_id,event_code,severity,client_occurred_at,server_received_at,detail_json)
            SELECT id,?,?,'INFO',?,UTC_TIMESTAMP(3),JSON_OBJECT('source','BROWSER') FROM exam_attempts
            WHERE id=? AND status='IN_PROGRESS' AND deadline_at>UTC_TIMESTAMP(3)
            """)){
            p.setString(1,key);p.setString(2,code);p.setObject(3,occurred);p.setLong(4,attempt);
            if(p.executeUpdate()!=1)throw new IllegalStateException();
        }
    }
    public Page history(Connection c,long owner,Filter f)throws SQLException{
        var where=new StringBuilder(" FROM proctoring_events p JOIN exam_attempts a ON a.id=p.attempt_id JOIN exams e ON e.id=a.exam_id WHERE e.created_by=?");
        var args=new ArrayList<Object>();args.add(owner);
        Object[] values={f.id,f.attempt,f.student,f.code,f.from,f.until};
        String[] clauses={"p.id=?","a.id=?","a.student_id=?","p.event_code=?","p.server_received_at>=?","p.server_received_at<?"};
        for(int i=0;i<values.length;i++)if(values[i]!=null){where.append(" AND ").append(clauses[i]);args.add(values[i]);}
        long count=((Number)one(c,"SELECT COUNT(*) n"+where,args.toArray()).get("n")).longValue();
        args.add(f.size);args.add((f.page-1L)*f.size);
        String sort=f.oldest?" ASC":" DESC";
        return new Page(count,rows(c,"SELECT p.id,p.attempt_id,a.student_id,e.title,p.event_code,p.severity,p.server_received_at,p.client_occurred_at,p.detail_json->>'$.source' source,"+
            "EXISTS(SELECT 1 FROM violation_reviews v WHERE v.related_event_id=p.id AND v.attempt_id=a.id) reviewed"+where+" ORDER BY p.server_received_at"+sort+",p.id"+sort+" LIMIT ? OFFSET ?",args.toArray()));
    }
}
