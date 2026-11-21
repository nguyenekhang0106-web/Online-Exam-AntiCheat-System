package vn.edu.onlineexam.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.edu.onlineexam.dao.*;

public final class AttemptContactService {
    public static final class InvalidSession extends RuntimeException {}
    private final JdbcUnitOfWork jdbc;
    private final AuthDao auth=new AuthDao();
    private final AttemptDao attempts=new AttemptDao();
    private final AttemptSessionDao sessions=new AttemptSessionDao();
    private final AttemptService exams;
    public AttemptContactService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;exams=new AttemptService(jdbc);}

    public void heartbeat(long attempt,String ref)throws SQLException {
        jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            Long student=auth.active(c,ref);
            if(student==null)throw new InvalidSession();
            if(!auth.roles(c,student).contains("STUDENT"))throw new SecurityException();
            var row=attempts.lock(c,attempt,student);
            var now=attempts.now(c);
            if(!"IN_PROGRESS".equals(row.get("status")) || !now.isBefore((LocalDateTime)row.get("deadline_at")))throw new IllegalStateException();
            exams.entitled(c,row,student,now);
            var prior=sessions.session(c,attempt,ref);
            var cutoff=now.minusSeconds(90);
            long id;
            boolean joining;
            if(prior.isEmpty()){
                id=sessions.open(c,attempt,ref,now);
                joining=true;
            }else{
                var session=prior.getFirst();
                id=AttemptDao.num(session,"id");
                var seen=(LocalDateTime)session.get("last_seen_at");
                joining=!seen.isAfter(cutoff);
                if(joining){
                    if(session.get("close_reason")==null)sessions.event(c,attempt,id,"HEARTBEAT_TIMEOUT","DERIVED");
                    sessions.event(c,attempt,id,"CONNECTION_RESTORED","SERVER");
                }
                sessions.seen(c,id,now);
            }
            if(joining && sessions.otherRecent(c,attempt,id,cutoff))sessions.event(c,attempt,id,"CONCURRENT_SESSION_DETECTED","SERVER");
            return null;
        });
    }
    public void inspect()throws SQLException {
        var candidates=jdbc.transaction(sessions::pending);
        for(var item:candidates){
            jdbc.transaction(c->{
                c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                long attempt=AttemptDao.num(item,"attempt"), id=AttemptDao.num(item,"id");
                attempts.lock(c,attempt,AttemptDao.num(item,"student_id"));
                String reason;
                try{reason=sessions.reason(c,id);}catch(NoSuchElementException changed){return null;}
                if(reason==null)return null;
                sessions.close(c,id,reason,attempts.now(c));
                switch(reason){
                    case "TIMEOUT" -> sessions.event(c,attempt,id,"HEARTBEAT_TIMEOUT","DERIVED");
                    case "REVOKED" -> sessions.event(c,attempt,id,"SESSION_REVOKED","SERVER");
                    case "ACCESS_CHANGED" -> sessions.event(c,attempt,id,"SESSION_ACCESS_CHANGED","SERVER");
                    case "EXPIRED" -> sessions.event(c,attempt,id,"SESSION_EXPIRED","SERVER");
                }
                return null;
            });
        }
    }
}
