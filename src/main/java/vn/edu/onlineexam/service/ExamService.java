package vn.edu.onlineexam.service;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.dao.ExamAssignmentDao.Scope;
import vn.edu.onlineexam.service.QuestionBankService.Access;

public final class ExamService {
    private final JdbcUnitOfWork jdbc;
    private final AuthDao auth=new AuthDao();
    private final ExamDao exams=new ExamDao();
    private final ExamQuestionDao questions=new ExamQuestionDao();
    private final ExamAssignmentDao assignments=new ExamAssignmentDao();
    public ExamService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public record Detail(Map<String,Object> exam,List<Map<String,Object>> items,List<Map<String,Object>> choices,
                         List<Map<String,Object>> schedule,List<Map<String,Object>> courses,List<Map<String,Object>> students){}
    public record Config(String title,int duration,int attempts,BigDecimal pass,String instructions,boolean shuffleQuestions,
                         boolean shuffleOptions,String scorePolicy,String resultRelease,String answerRelease,Long policy){
        public Config {
            if(title==null || title.isBlank() || title.strip().codePointCount(0,title.strip().length())>200)throw new IllegalArgumentException();
            title=title.strip(); positiveShort(duration);positiveShort(attempts);decimal(pass,new BigDecimal("100"));
            if(instructions==null || instructions.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65535
                    || !Set.of("HIGHEST","LATEST","FIRST").contains(scorePolicy)
                    || !Set.of("MANUAL","IMMEDIATE","AFTER_CLOSE").contains(resultRelease)
                    || !Set.of("NEVER","AFTER_CLOSE","MANUAL").contains(answerRelease))throw new IllegalArgumentException();
        }
    }
    private static void positiveShort(int n){if(n<1 || n>Short.MAX_VALUE)throw new IllegalArgumentException();}
    private static void decimal(BigDecimal n,BigDecimal max){if(n==null || n.scale()>2 || n.signum()<0 || n.compareTo(max)>0)throw new IllegalArgumentException();}
    private <T>T session(Access a,Set<String> allowed,JdbcUnitOfWork.Work<T> work)throws SQLException{
        if(a==null || a.actor()==null || !allowed.contains(a.role()) || !a.actor().roles().contains(a.role()))throw new SecurityException();
        try{return jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            if(!Long.valueOf(a.actor().id()).equals(auth.active(c,a.sessionRef())) || !auth.roles(c,a.actor().id()).contains(a.role()))throw new SecurityException();
            return work.execute(c);
        });}catch(SQLException ex){if(Set.of(1062,1205,1213,1451,1452).contains(ex.getErrorCode()))throw new IllegalStateException("Conflict");throw ex;}
    }
    private static final Set<String> STAFF=Set.of("ADMIN","TEACHER"), TEACHER=Set.of("TEACHER");
    private Map<String,Object> owned(Connection c,Access a,long id)throws SQLException{
        var e=exams.exam(c,id,true);
        if("TEACHER".equals(a.role()) && number(e,"created_by")!=a.actor().id())throw new SecurityException();
        return e;
    }
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    public List<Map<String,Object>> list(Access a,String term,String status,String sort,int page)throws SQLException{
        if(page<1 || term.length()>200 || !Set.of("","DRAFT","PUBLISHED","CLOSED","ARCHIVED").contains(status))throw new IllegalArgumentException();
        return session(a,STAFF,c->exams.list(c,"TEACHER".equals(a.role())?a.actor().id():null,term,status,sort,page));
    }
    public List<Map<String,Object>> subjects(Access a)throws SQLException{return session(a,TEACHER,c->QuestionCatalogDao.subjects(c,null));}
    public List<Map<String,Object>> policies(Access a)throws SQLException{return session(a,TEACHER,c->exams.policies(c,a.actor().id()));}
    public List<Map<String,Object>> calendar(Access a)throws SQLException{return session(a,Set.of("STUDENT"),c->assignments.schedule(c,null,a.actor().id()));}
    public Detail detail(Access a,long id,String difficulty,String bank,String topic)throws SQLException{
        return session(a,STAFF,c->{
            var e=owned(c,a,id);long subject=number(e,"subject_id"),actor=a.actor().id();
            boolean teacher="TEACHER".equals(a.role()),draft=teacher && "DRAFT".equals(e.get("status")),published=teacher && "PUBLISHED".equals(e.get("status"));
            return new Detail(e,teacher?questions.items(c,id):List.of(),draft?questions.choices(c,subject,actor,difficulty,bank,topic):List.of(),
                assignments.schedule(c,id,null),published?assignments.targets(c,subject,actor,Scope.COURSE):List.of(),published?assignments.targets(c,subject,actor,Scope.STUDENT):List.of());
        });
    }
    public long create(Access a,long subject,Config cfg)throws SQLException{
        return session(a,TEACHER,c->{
            QuestionCatalogDao.lockSubject(c,subject,null);
            long id=exams.create(c,subject,a.actor().id(),cfg.title,cfg.duration,cfg.attempts,cfg.pass);
            options(c,id,cfg);
            if(cfg.policy!=null)exams.policy(c,id,a.actor().id(),cfg.policy);
            exams.audit(c,a.actor().id(),id,"EXAM_CREATE");return id;
        });
    }
    @FunctionalInterface private interface Change {void apply(Connection c,Map<String,Object> e)throws SQLException;}
    private void change(Access a,long id,long stamp,String state,String action,Change work)throws SQLException{
        session(a,TEACHER,c->{
            var e=owned(c,a,id);
            if(number(e,"stamp")!=stamp || !state.equals(e.get("status")) || (state.equals("DRAFT") && exams.used(c,id)))throw new IllegalStateException();
            work.apply(c,e);exams.audit(c,a.actor().id(),id,action);return null;
        });
    }
    public void edit(Access a,long id,long stamp,Config cfg)throws SQLException{
        change(a,id,stamp,"DRAFT","EXAM_EDIT",(c,e)->{
            exams.edit(c,id,cfg.title,cfg.duration,cfg.attempts,cfg.pass);
            options(c,id,cfg);
            if(!Objects.equals(e.get("policy_id"),cfg.policy))exams.policy(c,id,a.actor().id(),cfg.policy);
        });
    }
    private void options(Connection c,long id,Config cfg)throws SQLException{
        exams.options(c,id,cfg.instructions,cfg.shuffleQuestions,cfg.shuffleOptions,cfg.scorePolicy,cfg.resultRelease,cfg.answerRelease);
    }
    public void item(Access a,long id,long stamp,String action,long version,int order,BigDecimal points)throws SQLException{
        if(!Set.of("add","remove","revise").contains(action) || order<0)throw new IllegalArgumentException();
        decimal(points,new BigDecimal("99999.99"));
        change(a,id,stamp,"DRAFT","EXAM_ITEM_"+action.toUpperCase(Locale.ROOT),(c,e)->{
            if(action.equals("remove"))questions.remove(c,id,version);
            else {
                questions.validate(c,number(e,"subject_id"),a.actor().id(),version);
                questions.save(c,id,version,order,points,action.equals("revise"));
            }
        });
    }
    public void transition(Access a,long id,long stamp,String next)throws SQLException{
        String from=switch(next){case "PUBLISHED"->"DRAFT";case "CLOSED"->"PUBLISHED";case "ARCHIVED"->"CLOSED";default->throw new IllegalArgumentException();};
        change(a,id,stamp,from,"EXAM_"+next,(c,e)->{
            if(next.equals("PUBLISHED")){
                QuestionCatalogDao.lockSubject(c,number(e,"subject_id"),null);
                validateComposition(c,e,a.actor().id());
            }else if(exams.ongoing(c,id))throw new IllegalStateException();
            exams.transition(c,id,next);
        });
    }
    private void validateComposition(Connection c,Map<String,Object> e,long owner)throws SQLException{
        BigDecimal total=BigDecimal.ZERO;
        for(var item:questions.items(c,number(e,"id"))){
            questions.validate(c,number(e,"subject_id"),owner,number(item,"question_version_id"));
            total=total.add((BigDecimal)item.get("points"));
        }
        if(total.signum()<=0 || !"ALL_OR_NOTHING".equals(e.get("grading_policy")))throw new IllegalStateException();
    }
    public void assign(Access a,long id,long stamp,String action,Scope scope,long target,LocalDateTime opens,LocalDateTime closes,Integer limit)throws SQLException{
        if(!Set.of("assign","reschedule","cancel").contains(action) || target<1)throw new IllegalArgumentException();
        if(!action.equals("cancel") && (opens==null || closes==null || !opens.isBefore(closes) || opens.getYear()<1000 || closes.getYear()>9999
                || opens.getNano()%1000000!=0 || closes.getNano()%1000000!=0))throw new IllegalArgumentException();
        if(limit!=null)positiveShort(limit);
        change(a,id,stamp,"PUBLISHED","EXAM_"+action.toUpperCase(Locale.ROOT),(c,e)->{
            if(!action.equals("cancel")){
                if(e.get("policy_snapshot_json")==null)throw new IllegalStateException();
                validateComposition(c,e,a.actor().id());
                assignments.validateTarget(c,number(e,"subject_id"),a.actor().id(),scope,target);
            }
            assignments.change(c,id,a.actor().id(),scope,target,opens,closes,limit,action);
        });
    }
}
