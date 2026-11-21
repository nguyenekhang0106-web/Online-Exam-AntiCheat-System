package vn.edu.onlineexam.service;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.QuestionCatalog.*;
import vn.edu.onlineexam.model.QuestionCatalog.Module;

public final class QuestionBankService {
    private final JdbcUnitOfWork jdbc;
    private final AuthDao auth=new AuthDao();
    private final TopicDao topics=new TopicDao();
    private final QuestionBankDao banks=new QuestionBankDao();
    private final QuestionDao questions=new QuestionDao();
    public QuestionBankService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public static final class Conflict extends RuntimeException {public Conflict(String s){super(s);}}
    public record Access(AuthUser actor,String role,String sessionRef) {
        Long owner(){return "TEACHER".equals(role)?actor.id():null;}
    }
    private void allow(Access a,Module m,boolean write){
        if(a==null || a.actor()==null || !Set.of("ADMIN","TEACHER").contains(a.role()) || !a.actor().roles().contains(a.role())
                || (write && m!=Module.topics && a.owner()==null))throw new SecurityException();
    }
    private <T>T write(Access a,Module m,JdbcUnitOfWork.Work<T> work)throws SQLException{
        allow(a,m,true);
        try{return jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            current(c,a);
            return work.execute(c);
        });}catch(SQLException e){
            if(Set.of(1062,1452,1213,1205).contains(e.getErrorCode()))throw new Conflict("Mã trùng hoặc dữ liệu vừa thay đổi. Hãy tải lại.");
            throw e;
        }
    }
    private void current(Connection c,Access a)throws SQLException{
        if(a.sessionRef()==null || !Long.valueOf(a.actor().id()).equals(auth.active(c,a.sessionRef()))
                || !auth.roles(c,a.actor().id()).contains(a.role()))throw new SecurityException();
    }
    private <T>T read(Access a,JdbcUnitOfWork.Work<T> work)throws SQLException{
        return jdbc.transaction(c->{
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            current(c,a);
            return work.execute(c);
        });
    }
    private static long n(Map<String,Object> row,String field){Object value=row.get(field);return value==null?0:((Number)value).longValue();}
    private static String text(String s,int max){
        if(s==null || s.isBlank() || s.codePointCount(0,s.length())>max)throw new IllegalArgumentException("Nội dung bắt buộc hoặc vượt giới hạn.");
        return s.strip();
    }
    private static void value(String s,String... choices){if(s==null || !List.of(choices).contains(s))throw new IllegalArgumentException("Giá trị không hợp lệ.");}
    private void stamp(Connection c,Module m,long id,long expected)throws SQLException{
        if(id!=0 && QuestionCatalogDao.stamp(c,m,id)!=expected)throw new Conflict("Dữ liệu đã thay đổi. Hãy mở lại trang.");
    }
    public Page list(Access a,Module m,Filter f)throws SQLException{
        allow(a,m,false);value(f.sort(),"id","name");
        if(f.page()<1 || f.size()<1 || f.size()>100 || f.term().length()>254 || f.subject()<0 || f.bank()<0 || f.topic()<0)throw new IllegalArgumentException();
        switch(m){case topics->value(f.state(),"");case banks->value(f.state(),"","PRIVATE","SHARED");case questions->value(f.state(),"","DRAFT","PUBLISHED","ARCHIVED");}
        value(f.type(),"","SINGLE_CHOICE","MULTIPLE_CHOICE");value(f.difficulty(),"","EASY","MEDIUM","HARD");
        return read(a,c->switch(m){case topics->topics.list(c,f,a.owner());case banks->banks.list(c,f,a.owner());case questions->questions.list(c,f,a.owner());});
    }
    public Detail detail(Access a,Module m,long id)throws SQLException{
        allow(a,m,false);if(id<1)throw new IllegalArgumentException();
        // One consistent snapshot pairs the editor's version token with its content.
        return read(a,c->{
            var row=switch(m){case topics->topics.detail(c,id,a.owner());case banks->banks.detail(c,id,a.owner());case questions->questions.detail(c,id,a.owner());};
            row.put("stamp",QuestionCatalogDao.stamp(c,m,id));
            return new Detail(row,m==Module.questions && a.owner()!=null?questions.options(c,n(row,"version_id")):List.of());
        });
    }
    public List<Map<String,Object>> choices(Access a,Module m)throws SQLException{
        allow(a,m,true);
        return read(a,c->m==Module.questions?banks.owned(c,a.actor().id()):QuestionCatalogDao.subjects(c,m==Module.topics?a.owner():null));
    }
    private void subject(Connection c,long id,Long teacher)throws SQLException{
        try{QuestionCatalogDao.lockSubject(c,id,teacher);}catch(IllegalStateException inactive){throw new Conflict(inactive.getMessage());}
    }
    public long topic(Access a,long id,long expected,long subject,Long parent,String code,String name,int order)throws SQLException{
        String key=text(code,30),label=text(name,150);if(order<0 || subject<1)throw new IllegalArgumentException();
        return write(a,Module.topics,c->{
            subject(c,subject,a.owner());
            if(id!=0 && n(topics.detail(c,id,a.owner()),"subject_id")!=subject)throw new Conflict("Môn của chủ đề đã được cố định.");
            stamp(c,Module.topics,id,expected);topics.parent(c,subject,parent,id);
            if(id!=0 && topics.usedInExam(c,id))throw new Conflict("Chủ đề có câu hỏi đã được đưa vào đề thi.");
            long saved=topics.save(c,id,subject,parent,key,label,order);QuestionCatalogDao.audit(c,Module.topics,a.actor().id(),saved,id==0?"TOPIC_CREATE":"TOPIC_UPDATE");return saved;
        });
    }
    public long bank(Access a,long id,long expected,long subject,String name,String description,String visibility)throws SQLException{
        String label=text(name,200);value(visibility,"PRIVATE","SHARED");
        if(subject<1 || (description!=null && description.getBytes(StandardCharsets.UTF_8).length>65535))throw new IllegalArgumentException();
        return write(a,Module.banks,c->{
            subject(c,subject,null);
            if(id!=0){
                banks.lockOwned(c,id,a.actor().id());if(n(banks.detail(c,id,a.owner()),"subject_id")!=subject || banks.used(c,id))
                    throw new Conflict("Ngân hàng đã dùng trong đề hoặc môn không khớp.");
                stamp(c,Module.banks,id,expected);
            }
            long saved=banks.save(c,id,subject,a.actor().id(),label,description,visibility);QuestionCatalogDao.audit(c,Module.banks,a.actor().id(),saved,id==0?"BANK_CREATE":"BANK_UPDATE");return saved;
        });
    }
    private void validate(Draft d){
        value(d.type(),"SINGLE_CHOICE","MULTIPLE_CHOICE");value(d.difficulty(),"EASY","MEDIUM","HARD");
        if(d.content()==null || d.content().isBlank() || d.points()==null || d.points().signum()<0 || d.points().scale()>2 || d.points().compareTo(new java.math.BigDecimal("99999.99"))>0)
            throw new IllegalArgumentException("Kiểm tra nội dung và điểm mặc định.");
        if(d.options().size()<2 || d.options().size()>32768)throw new IllegalArgumentException("Cần ít nhất hai phương án có thứ tự hợp lệ.");
        var seen=new HashSet<String>();int correct=0;
        for(var opt:d.options()){
            if(opt.text()==null || opt.text().isBlank() || !seen.add(opt.text().strip()))throw new IllegalArgumentException("Phương án rỗng hoặc trùng.");
            if(opt.correct())correct++;
        }
        if(correct==0 || (d.type().equals("SINGLE_CHOICE") && correct!=1))throw new IllegalArgumentException("Số đáp án đúng không phù hợp loại câu hỏi.");
    }
    public long question(Access a,long id,long expected,Draft d)throws SQLException{
        allow(a,Module.questions,true);validate(d);
        return write(a,Module.questions,c->{
            var bank=banks.detail(c,d.bank(),a.owner());long subject=n(bank,"subject_id");subject(c,subject,null);
            banks.lockOwned(c,d.bank(),a.actor().id());
            if(d.topic()!=0 && n(topics.detail(c,d.topic(),null),"subject_id")!=subject)throw new IllegalArgumentException("Chủ đề phải cùng môn với ngân hàng.");
            int number=1;
            if(id!=0){
                questions.lock(c,id);var row=questions.detail(c,id,a.owner());stamp(c,Module.questions,id,expected);
                if(n(row,"bank_id")!=d.bank() || !"DRAFT".equals(row.get("status")) || questions.used(c,id) || questions.attachments(c,n(row,"version_id")))
                    throw new Conflict("Chỉ sửa câu nháp chưa dùng, trong cùng ngân hàng và chưa có tệp đính kèm.");
                number=Math.toIntExact(n(row,"version_no")+1);
            }
            long saved=questions.save(c,id,a.actor().id(),d,number);QuestionCatalogDao.audit(c,Module.questions,a.actor().id(),saved,id==0?"QUESTION_CREATE":"QUESTION_REVISE");return saved;
        });
    }
    public void transition(Access a,long id,long expected,String next)throws SQLException{
        value(next,"PUBLISHED","ARCHIVED");
        write(a,Module.questions,c->{
            questions.lock(c,id);var row=questions.detail(c,id,a.owner());stamp(c,Module.questions,id,expected);
            String current=(String)row.get("status");
            if(current.equals("ARCHIVED") || (next.equals("PUBLISHED") && !current.equals("DRAFT")) || questions.used(c,id))throw new Conflict("Trạng thái hoặc tham chiếu đề thi không cho phép thao tác.");
            if(next.equals("PUBLISHED"))validate(new Draft(n(row,"bank_id"),n(row,"topic_id"),(String)row.get("question_type"),(String)row.get("difficulty"),
                    (String)row.get("label"),(String)row.get("explanation"),(java.math.BigDecimal)row.get("default_points"),questions.options(c,n(row,"version_id"))));
            questions.status(c,id,next);QuestionCatalogDao.audit(c,Module.questions,a.actor().id(),id,"QUESTION_"+next);return null;
        });
    }
}
