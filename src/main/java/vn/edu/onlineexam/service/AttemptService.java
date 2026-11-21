package vn.edu.onlineexam.service;

import java.sql.*;
import java.time.*;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import static vn.edu.onlineexam.dao.AttemptDao.num;

public final class AttemptService {
    private final JdbcUnitOfWork jdbc;
    private final AuthDao auth=new AuthDao();
    private final AttemptDao dao=new AttemptDao();
    public AttemptService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public record Admission(String title,long previous,int used,Integer limit,long open,String message){}
    public record Item(Map<String,Object> question,List<Map<String,Object>> options){}
    public record Paper(long id,String title,String state,long seconds,List<Item> items){}
    private <T>T run(Access a,boolean student,JdbcUnitOfWork.Work<T> task)throws SQLException{
        if(a==null || a.actor()==null || (student && !"STUDENT".equals(a.role())) || !a.actor().roles().contains(a.role()))throw new SecurityException();
        try{return jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            if(!Long.valueOf(a.actor().id()).equals(auth.active(c,a.sessionRef())) || !auth.roles(c,a.actor().id()).contains(a.role()))throw new SecurityException();
            return task.execute(c);
        });}catch(SQLException ex){if(Set.of(1205,1213,1062).contains(ex.getErrorCode()))throw new IllegalStateException("Dữ liệu vừa thay đổi. Hãy tải lại.");throw ex;}
    }
    private record Context(Admission admission,int nextNumber,LocalDateTime now,LocalDateTime end,Map<String,Object> exam,Map<String,Object> grant){}
    private Context admission(Connection c,long exam,long student)throws SQLException{
        var e=dao.exam(c,exam);var windows=dao.windows(c,exam,student);var attempts=dao.previous(c,exam,student);
        if(windows.isEmpty() && attempts.isEmpty())throw new SecurityException();
        var now=dao.now(c);
        var active=windows.stream().filter(w->!now.isBefore((LocalDateTime)w.get("opens_at")) && now.isBefore((LocalDateTime)w.get("closes_at"))).toList();
        boolean outsidePersonal=windows.stream().anyMatch(w->"PERSONAL".equals(w.get("scope"))) && active.stream().noneMatch(w->"PERSONAL".equals(w.get("scope")));
        long previous=attempts.isEmpty()?0:num(attempts.getFirst(),"id");
        long open=attempts.stream().filter(t->"IN_PROGRESS".equals(t.get("status"))).mapToLong(t->num(t,"id")).findFirst().orElse(0);
        Integer limit=null;LocalDateTime end=null;String message="";
        if(!"PUBLISHED".equals(e.get("status")) || e.get("policy_snapshot_json")==null || !"ALL_OR_NOTHING".equals(e.get("grading_policy")))message="Đề chưa đủ điều kiện làm bài.";
        else if(active.isEmpty() || outsidePersonal)message="Ngoài cửa sổ thi được phép.";
        else if(active.size()>1)message="Nhiều phân công đang có hiệu lực. Cần xác nhận quyền dự thi.";
        else if(attempts.stream().anyMatch(t->Set.of("CANCELLED","INVALIDATED").contains(t.get("status"))))message="Cần xác nhận cách tính lượt đã hủy hoặc vô hiệu.";
        else{
            var window=active.getFirst();limit=((Number)Objects.requireNonNullElse(window.get("max_attempts_override"),e.get("max_attempts"))).intValue();
            end=(LocalDateTime)window.get("closes_at");
            if(open==0 && attempts.size()>=limit)message="Đã sử dụng hết lượt thi.";
        }
        int next=attempts.isEmpty()?1:Math.toIntExact(num(attempts.getFirst(),"attempt_no")+1);
        if(next>Short.MAX_VALUE && open==0)message="Đã hết số thứ tự lượt thi.";
        return new Context(new Admission((String)e.get("title"),previous,attempts.size(),limit,open,message),next,now,end,e,active.size()==1?active.getFirst():Map.of());
    }
    public Admission preview(Access a,long exam)throws SQLException{return run(a,true,c->admission(c,exam,a.actor().id()).admission);}
    public long start(Access a,long exam,long expected)throws SQLException{
        return run(a,true,c->{
            long student=a.actor().id();var context=admission(c,exam,student);var p=context.admission;
            if(p.open!=0){
                var attempt=dao.lock(c,p.open,student);
                if(expired(attempt,context.now)){finish(c,attempt,student,context.now,true);return p.open;}
            }
            if(!p.message.isEmpty())throw new IllegalStateException(p.message);
            if(p.open!=0){
                if(!dao.originalGrant(c,p.open,context.grant))throw new IllegalStateException("Phân công của lượt thi đã thay đổi.");
                return p.open;
            }
            if(p.previous!=expected)throw new IllegalStateException("Yêu cầu bắt đầu đã cũ. Hãy xem lại lịch sử lượt thi.");
            var deadline=context.now.plusMinutes(num(context.exam,"duration_minutes"));
            long id=dao.create(c,exam,student,context.nextNumber,context.now,deadline.isBefore(context.end)?deadline:context.end);
            dao.snapshot(c,id,exam,enabled(context.exam.get("shuffle_questions")),enabled(context.exam.get("shuffle_options")));
            dao.auditStart(c,student,id,context.grant);return id;
        });
    }
    private static boolean enabled(Object v){return Boolean.TRUE.equals(v) || (v instanceof Number n && n.intValue()==1);}
    private static boolean expired(Map<String,Object> a,LocalDateTime now){return !now.isBefore((LocalDateTime)a.get("deadline_at"));}
    private void entitled(Connection c,Map<String,Object> attempt,long student,LocalDateTime now)throws SQLException{
        if(!"PUBLISHED".equals(attempt.get("exam_status")))throw new IllegalStateException("Đề không còn mở.");
        // The original authorization must still cover this instant. Never extend the stored deadline.
        var windows=dao.windows(c,num(attempt,"exam_id"),student);
        var active=windows.stream().filter(w->!now.isBefore((LocalDateTime)w.get("opens_at")) && now.isBefore((LocalDateTime)w.get("closes_at"))).toList();
        if(active.isEmpty())throw new SecurityException();
        if(active.size()!=1 || (windows.stream().anyMatch(w->"PERSONAL".equals(w.get("scope"))) && !"PERSONAL".equals(active.getFirst().get("scope"))))throw new IllegalStateException("Cần xác nhận phân công đang hiệu lực.");
        if(!dao.originalGrant(c,num(attempt,"id"),active.getFirst()))throw new IllegalStateException("Phân công của lượt thi đã thay đổi.");
    }
    public Paper paper(Access a,long id)throws SQLException{
        return run(a,true,c->{
            var attempt=dao.lock(c,id,a.actor().id());var now=dao.now(c);String state=(String)attempt.get("status");
            var items=new ArrayList<Item>();long seconds=0;
            if("IN_PROGRESS".equals(state)){
                if(expired(attempt,now))state="EXPIRED";
                else{
                    entitled(c,attempt,a.actor().id(),now);
                    seconds=(Duration.between(now,(LocalDateTime)attempt.get("deadline_at")).toMillis()+999)/1000;
                    for(var question:dao.questions(c,id))items.add(new Item(question,dao.options(c,id,num(question,"id"))));
                }
            }
            return new Paper(id,(String)attempt.get("title"),state,seconds,items);
        });
    }
    public long save(Access a,long id,long question,long revision,Set<Long> selected)throws SQLException{
        return run(a,true,c->{
            var attempt=dao.lock(c,id,a.actor().id());var now=dao.now(c);
            if(!"IN_PROGRESS".equals(attempt.get("status")) || expired(attempt,now))throw new IllegalStateException("Đã hết thời gian lưu bài.");
            entitled(c,attempt,a.actor().id(),now);
            var item=dao.questions(c,id).stream().filter(q->num(q,"id")==question).findFirst().orElseThrow();
            var options=dao.options(c,id,question);var allowed=new HashSet<Long>();var stored=new HashSet<Long>();
            for(var opt:options){long oid=num(opt,"id");allowed.add(oid);if(num(opt,"selected")!=0)stored.add(oid);}
            if(!allowed.containsAll(selected) || ("SINGLE_CHOICE".equals(item.get("question_type")) && selected.size()>1))throw new IllegalArgumentException();
            if(stored.equals(selected))return num(item,"revision");
            if(num(item,"revision")!=revision)throw new IllegalStateException("Câu này đã được lưu ở phiên khác. Hãy tải lại.");
            dao.save(c,id,question,selected,now);return dao.audit(c,a.actor().id(),"attempt_questions",question,"ATTEMPT_SAVE");
        });
    }
    private void finish(Connection c,Map<String,Object> attempt,long student,LocalDateTime now,boolean timeout)throws SQLException{
        dao.grade(c,attempt,now,timeout);dao.audit(c,student,"exam_attempts",num(attempt,"id"),timeout?"ATTEMPT_TIMEOUT":"ATTEMPT_SUBMIT");
    }
    public void submit(Access a,long id,boolean timeoutOnly)throws SQLException{
        run(a,true,c->{
            long student=a.actor().id();var attempt=dao.lock(c,id,student);var now=dao.now(c);
            if(Set.of("SUBMITTED","AUTO_SUBMITTED").contains(attempt.get("status")))return null;
            if(!"IN_PROGRESS".equals(attempt.get("status")))throw new IllegalStateException("Trạng thái lượt thi không cho phép nộp.");
            boolean timeout=expired(attempt,now);if(timeoutOnly && !timeout)throw new IllegalStateException("Lượt thi vẫn còn thời gian.");if(!timeout)entitled(c,attempt,student,now);
            finish(c,attempt,student,now,timeout);return null;
        });
    }
    public List<Map<String,Object>> history(Access a)throws SQLException{return run(a,false,c->dao.history(c,a.role(),a.actor().id()));}
    /** The container invokes this independently of a student's browser or login session. */
    public long expireBatch(long cursor)throws SQLException{
        var due=jdbc.read(c->dao.due(c,cursor));
        for(var row:due){
            long id=num(row,"id");
            try{jdbc.transaction(c->{
                var attempt=dao.lock(c,id,num(row,"student_id"));var now=dao.now(c);
                if("IN_PROGRESS".equals(attempt.get("status")) && expired(attempt,now)){
                    dao.grade(c,attempt,now,true);dao.audit(c,null,"exam_attempts",id,"ATTEMPT_TIMEOUT");
                }
                return null;
            });}catch(SQLException | IllegalStateException failure){
                System.getLogger(AttemptService.class.getName()).log(System.Logger.Level.WARNING,
                    "Lượt hết hạn chưa chấm được, sẽ thử lại sau. ID: "+id);
            }
        }
        return due.size()<50?0:num(due.getLast(),"id");
    }
}
