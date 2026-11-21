package vn.edu.onlineexam.service;

import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.OfferingPage.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;

public final class OfferingManagementService {
    private final JdbcUnitOfWork jdbc;
    private final OfferingDao dao=new OfferingDao();
    private final UserDao users=new UserDao();
    private final AuthDao sessions=new AuthDao();
    public OfferingManagementService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public static final class Conflict extends RuntimeException {public Conflict(String reason){super(reason);}}
    private void allow(AuthUser actor,Scope role){if(actor==null || !actor.roles().contains(role.name())) throw new SecurityException();}
    private void id(long id){if(id<=0) throw new IllegalArgumentException("ID không hợp lệ.");}
    public OfferingPage search(AuthUser actor,Scope role,String term,long subject,String status,String sort,String direction,int page,int size) throws SQLException {
        allow(actor,role);
        if(term.length()>200 || subject<0 || page<1 || size<1 || size>100 || !Set.of("ASC","DESC").contains(direction)
                || !Set.of("","ACTIVE","CLOSED","ARCHIVED").contains(status)) throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        var query=new Query(term.strip(),subject,status,Sort.valueOf(sort),direction.equals("DESC"),size);
        return jdbc.read(c->dao.search(c,role,actor.id(),query,page));
    }
    public Row detail(AuthUser actor,Scope role,long id) throws SQLException {
        allow(actor,role);id(id);return jdbc.read(c->dao.detail(c,role,actor.id(),id));
    }
    public Choices choices(AuthUser actor,boolean create) throws SQLException {
        allow(actor,Scope.ADMIN);return jdbc.read(c->dao.choices(c,create));
    }
    private <T>T write(AuthUser actor,String ref,JdbcUnitOfWork.Work<T> work) throws SQLException {
        allow(actor,Scope.ADMIN);
        try{return jdbc.transaction(c->{
            if(!users.lockActiveAdmins(c).contains(actor.id()) || ref==null || !Long.valueOf(actor.id()).equals(sessions.active(c,ref))) throw new SecurityException();
            return work.execute(c);
        });} catch(SQLException e){if(e.getErrorCode()==1213 || e.getErrorCode()==1205) throw new Conflict("Có cập nhật đồng thời. Hãy tải lại trang.");throw e;}
    }
    private String required(String text,int limit){
        String s=text==null?"":text.strip();
        if(s.isEmpty() || s.codePointCount(0,s.length())>limit || s.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Kiểm tra năm học (1–9 ký tự) và học kỳ (1–20 ký tự).");
        return s;
    }
    public long create(AuthUser actor,String ref,long subject,long schoolClass,long teacher,String rawYear,String rawSemester) throws SQLException {
        allow(actor,Scope.ADMIN);id(subject);id(schoolClass);id(teacher);
        String year=required(rawYear,9),semester=required(rawSemester,20);
        return write(actor,ref,c->{
            if(!dao.eligibleParents(c,subject,schoolClass)) throw new Conflict("Môn và lớp hành chính phải tồn tại và đang hoạt động.");
            if(!dao.teacher(c,teacher)) throw new Conflict("Giảng viên phải đang hoạt động và có vai trò TEACHER.");
            long id=dao.insert(c,subject,schoolClass,teacher,year,semester);
            dao.audit(c,actor.id(),id,"OFFERING_CREATE");return id;
        });
    }
    public void change(AuthUser actor,String ref,long target,long version,long teacher,boolean close) throws SQLException {
        allow(actor,Scope.ADMIN);id(target);if(!close) id(teacher);
        if(version<0) throw new IllegalArgumentException("Phiên bản không hợp lệ.");
        write(actor,ref,c->{
            if(!dao.lock(c,target)) throw new NoSuchElementException();
            var row=dao.detail(c,Scope.ADMIN,actor.id(),target);
            if(row.version()!=version || !row.status().equals("ACTIVE")) throw new Conflict("Lớp đã thay đổi hoặc đã đóng. Hãy mở lại chi tiết.");
            if(close) dao.close(c,target);
            else {
                if(!dao.teacher(c,teacher)) throw new Conflict("Giảng viên không đủ điều kiện.");
                dao.assign(c,target,teacher);
            }
            dao.audit(c,actor.id(),target,close?"OFFERING_CLOSE":"OFFERING_ASSIGN");return null;
        });
    }
}
