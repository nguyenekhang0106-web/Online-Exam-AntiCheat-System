package vn.edu.onlineexam.service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.SubjectPage.*;
import vn.edu.onlineexam.model.bean.academic.Subject;

public final class SubjectManagementService {
    private final JdbcUnitOfWork jdbc;
    private final SubjectDao dao=new SubjectDao();
    private final UserDao users=new UserDao();
    private final AuthDao sessions=new AuthDao();
    public SubjectManagementService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public static final class Conflict extends RuntimeException {
        public Conflict(String message){super(message);}
    }
    private void authorize(AuthUser actor,Scope access){
        if(actor==null || !actor.roles().contains(access.name())) throw new SecurityException();
    }
    public SubjectPage search(AuthUser actor, Scope access, String keyword, String status, String sort,
                              String direction,int page,int size) throws SQLException {
        authorize(actor,access);
        keyword=keyword.strip();
        if(keyword.length()>200 || !Set.of("","ACTIVE","INACTIVE","ARCHIVED").contains(status)
                || !Set.of("ASC","DESC").contains(direction) || page<1 || size<1 || size>100)
            throw new IllegalArgumentException("Bộ lọc hoặc phân trang không hợp lệ.");
        var query=new Query(keyword,status,Sort.valueOf(sort),direction.equals("DESC"),size);
        return jdbc.read(c->dao.search(c,access,actor.id(),query,page));
    }
    public Subject detail(AuthUser actor,Scope access,long id) throws SQLException {
        authorize(actor,access); checkId(id);
        return jdbc.read(c->dao.detail(c,access,actor.id(),id,false));
    }
    private static void checkId(long id){if(id<1) throw new IllegalArgumentException("ID môn không hợp lệ.");}
    private static String required(String value,int max,String field){
        String s=value==null?"":value.strip();
        if(s.isEmpty() || s.codePointCount(0,s.length())>max || s.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(field+" bắt buộc, tối đa "+max+" ký tự.");
        return s;
    }
    private static Input validate(Input in){
        String description=in.description()==null?"":in.description().strip();
        if(description.getBytes(StandardCharsets.UTF_8).length>65535)
            throw new IllegalArgumentException("Mô tả vượt giới hạn 65.535 byte UTF-8.");
        return new Input(required(in.code(),30,"Mã môn"),required(in.name(),200,"Tên môn"),description.isEmpty()?null:description);
    }
    private <T> T write(AuthUser actor,String ref,JdbcUnitOfWork.Work<T> work) throws SQLException {
        authorize(actor,Scope.ADMIN);
        try {
            return jdbc.transaction(c->{
                if(!users.lockActiveAdmins(c).contains(actor.id()) || ref==null ||
                        !Long.valueOf(actor.id()).equals(sessions.active(c,ref))) throw new SecurityException();
                return work.execute(c);
            });
        } catch(SQLException e){
            if(e.getErrorCode()==1062) throw new Conflict("Mã môn học đã tồn tại.");
            if(e.getErrorCode()==1213 || e.getErrorCode()==1205) throw new Conflict("Dữ liệu đang được cập nhật. Hãy tải lại.");
            throw e;
        }
    }
    public long create(AuthUser actor,String ref,Input raw) throws SQLException {
        authorize(actor,Scope.ADMIN);
        Input in=validate(raw);
        return write(actor,ref,c->{long id=dao.insert(c,in); dao.audit(c,actor.id(),id,"SUBJECT_CREATE"); return id;});
    }
    public void change(AuthUser actor,String ref,long id,LocalDateTime version,Input raw,boolean archive) throws SQLException {
        authorize(actor,Scope.ADMIN); checkId(id);
        Input in=archive?null:validate(raw);
        write(actor,ref,c->{
            var current=dao.detail(c,Scope.ADMIN,actor.id(),id,true);
            if(!current.getUpdatedAt().equals(version)) throw new Conflict("Môn đã thay đổi. Hãy mở lại trang.");
            if(archive){
                if(current.getStatus().equals("ARCHIVED")) throw new Conflict("Môn đã được lưu trữ.");
                if(dao.referenced(c,id)) throw new Conflict("Môn đang có lớp học phần, chủ đề, ngân hàng câu hỏi hoặc đề thi.");
                dao.archive(c,id);
            } else dao.update(c,id,in);
            dao.audit(c,actor.id(),id,archive?"SUBJECT_ARCHIVE":"SUBJECT_UPDATE");
            return null;
        });
    }
}
