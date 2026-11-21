package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Callers serialize transitions on the attempt row before inspecting session state. */
public final class AttemptSessionDao extends QuestionCatalogDao {
    private static final String LOGIN_KEY = "SHA2(CONCAT(a.id,':',l.session_hash),256)";
    public List<Map<String,Object>> session(Connection c,long attempt,String ref)throws SQLException {
        return rows(c,"SELECT id,last_seen_at,close_reason FROM attempt_sessions WHERE attempt_id=? AND session_token_hash=SHA2(CONCAT(?,':',?),256)",attempt,attempt,ref);
    }
    public long open(Connection c,long attempt,String ref,LocalDateTime now)throws SQLException {
        return insert(c,"INSERT INTO attempt_sessions(attempt_id,session_token_hash,opened_at,last_seen_at) VALUES(?,SHA2(CONCAT(?,':',?),256),?,?)",attempt,attempt,ref,now,now);
    }
    public void seen(Connection c,long id,LocalDateTime now)throws SQLException {
        update(c,"UPDATE attempt_sessions SET last_seen_at=?,closed_at=NULL,close_reason=NULL WHERE id=?",now,id);
    }
    public boolean otherRecent(Connection c,long attempt,long id,LocalDateTime cutoff)throws SQLException {
        return exists(c,"SELECT s.id FROM attempt_sessions s JOIN exam_attempts a ON a.id=s.attempt_id JOIN login_sessions l ON l.user_id=a.student_id AND s.session_token_hash="+LOGIN_KEY+
            " WHERE a.id=? AND s.id<>? AND s.closed_at IS NULL AND s.last_seen_at>? AND l.revoked_at IS NULL AND l.expires_at>UTC_TIMESTAMP(3) AND l.last_seen_at>UTC_TIMESTAMP(3)-INTERVAL 30 MINUTE LIMIT 1",attempt,id,cutoff);
    }
    public void close(Connection c,long id,String reason,LocalDateTime now)throws SQLException {
        update(c,"UPDATE attempt_sessions SET closed_at=?,close_reason=? WHERE id=?",now,reason,id);
    }
    public void event(Connection c,long attempt,long id,String code,String source)throws SQLException {
        update(c,"INSERT INTO proctoring_events(attempt_id,attempt_session_id,event_code,severity,server_received_at,detail_json) VALUES(?,?,?,'INFO',UTC_TIMESTAMP(3),JSON_OBJECT('source',?))",attempt,id,code,source);
    }
    private static final String CANDIDATES = " FROM attempt_sessions s JOIN exam_attempts a ON a.id=s.attempt_id JOIN login_sessions l ON l.user_id=a.student_id AND s.session_token_hash="+LOGIN_KEY;
    private static final String ELIGIBLE = " (s.closed_at IS NULL OR s.close_reason='TIMEOUT') ";
    private static final String REASON = """
        CASE WHEN a.status<>'IN_PROGRESS' OR a.deadline_at<=UTC_TIMESTAMP(3) THEN 'ENDED'
             WHEN l.revoked_at IS NOT NULL THEN 'REVOKED'
             WHEN l.expires_at<=UTC_TIMESTAMP(3) OR l.last_seen_at<=UTC_TIMESTAMP(3)-INTERVAL 30 MINUTE THEN 'EXPIRED'
             WHEN NOT EXISTS(SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
                 WHERE u.id=l.user_id AND u.status='ACTIVE' AND r.role_code='STUDENT') THEN 'ACCESS_CHANGED'
             WHEN s.closed_at IS NULL AND s.last_seen_at<=UTC_TIMESTAMP(3)-INTERVAL 90 SECOND THEN 'TIMEOUT' END
        """;
    public List<Map<String,Object>> pending(Connection c)throws SQLException {
        return rows(c,"SELECT s.id,a.id attempt,a.student_id"+CANDIDATES+
            " WHERE "+ELIGIBLE+" AND ("+REASON+") IS NOT NULL ORDER BY s.id LIMIT 50");
    }
    public String reason(Connection c,long id)throws SQLException {
        return (String)one(c,"SELECT "+REASON+" reason"+CANDIDATES+" WHERE s.id=? AND "+ELIGIBLE,id).get("reason");
    }
}
