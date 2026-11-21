package vn.edu.onlineexam.service;

import java.sql.*;
import java.util.*;
import java.time.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;

public final class ProctoringService {
    public static final Set<String> CODES=Set.of("CONNECTION_LOST","CONNECTION_RESTORED","WINDOW_BLUR","TAB_HIDDEN","FULLSCREEN_EXIT","COPY_ATTEMPT","PASTE_ATTEMPT");
    public static final class Limited extends RuntimeException {}
    private final JdbcUnitOfWork jdbc;
    private final AuthDao auth=new AuthDao();
    private final ProctoringEventDao dao=new ProctoringEventDao();
    public ProctoringService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    private void current(Connection c,Access a,Set<String> roles)throws SQLException{
        if(a==null || a.actor()==null || !roles.contains(a.role()) || !a.actor().roles().contains(a.role())
            || !Long.valueOf(a.actor().id()).equals(auth.active(c,a.sessionRef())) || !auth.roles(c,a.actor().id()).contains(a.role()))throw new SecurityException();
    }
    public boolean receive(Access a,long attempt,String key,String code,LocalDateTime occurred)throws SQLException{
        if(attempt<=0 || key==null || !UUID.fromString(key).toString().equals(key) || code==null || !CODES.contains(code)
            || occurred!=null && (occurred.getNano()%1000000!=0 || occurred.getYear()<1000 || occurred.getYear()>9999))throw new IllegalArgumentException();
        return jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            current(c,a,Set.of("STUDENT"));
            var row=dao.lock(c,attempt,a.actor().id());
            var previous=dao.previous(c,attempt,key);
            if(!previous.isEmpty()){
                var event=previous.getFirst();
                if(!code.equals(event.get("event_code")) || !Objects.equals(occurred,event.get("client_occurred_at")))throw new IllegalStateException();
                return false;
            }
            if(!"IN_PROGRESS".equals(row.get("status")) || !new AttemptDao().now(c).isBefore((LocalDateTime)row.get("deadline_at")))throw new IllegalStateException();
            String policy=switch(code){case "WINDOW_BLUR","TAB_HIDDEN"->"focus_policy";case "FULLSCREEN_EXIT"->"fullscreen_policy";case "PASTE_ATTEMPT"->"paste_policy";default->null;};
            if(policy!=null && !Set.of("true","1").contains(String.valueOf(row.get(policy))))throw new IllegalStateException();
            if(dao.full(c,attempt))throw new Limited();
            dao.append(c,attempt,key,code,occurred);return true;
        });
    }
    public ProctoringEventDao.Page history(Access a,ProctoringEventDao.Filter filter)throws SQLException{
        if(filter.page()<1 || filter.size()<1 || filter.size()>50 || filter.code()!=null && !CODES.contains(filter.code())
            || filter.from()!=null && filter.until()!=null && !filter.from().isBefore(filter.until()))throw new IllegalArgumentException();
        return jdbc.transaction(c->{current(c,a,Set.of("TEACHER","ADMIN"));return dao.history(c,a.actor().id(),filter);});
    }
}
