package vn.edu.onlineexam.service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.ClassDirectory.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;
import vn.edu.onlineexam.model.bean.academic.SchoolClass;

public final class ClassManagementService {
    private final JdbcUnitOfWork jdbc;
    private final SchoolClassDao classes=new SchoolClassDao();
    private final ClassStudentDao members=new ClassStudentDao();
    private final UserDao users=new UserDao();
    private final AuthDao sessions=new AuthDao();
    private final EnrollmentDao students=new EnrollmentDao();
    public ClassManagementService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public static final class Conflict extends RuntimeException { public Conflict(String msg){super(msg);} }
    private void allow(AuthUser actor,Scope role){if(actor==null || !actor.roles().contains(role.name()))throw new SecurityException();}
    private void checkId(long id){if(id<=0)throw new IllegalArgumentException("ID phải là số dương.");}
    private Query validate(Query q,boolean roster){
        var states=roster?Set.of("","ACTIVE","LEFT"):Set.of("","ACTIVE","INACTIVE","ARCHIVED");
        if(q.term().length()>200 || !states.contains(q.status()) || q.page()<1 || q.size()<1 || q.size()>100)
            throw new IllegalArgumentException("Bộ lọc hoặc phân trang không hợp lệ.");
        return q;
    }
    public ClassDirectory<SchoolClass> list(AuthUser actor,Scope role,Query q) throws SQLException {
        allow(actor,role);validate(q,false);return jdbc.read(c->classes.list(c,role,actor.id(),q));
    }
    public SchoolClass detail(AuthUser actor,Scope role,long id) throws SQLException {
        allow(actor,role);checkId(id);return jdbc.read(c->classes.get(c,id,role,actor.id(),false));
    }
    public ClassDirectory<Member> roster(AuthUser actor,Scope role,long id,Query q) throws SQLException {
        allow(actor,role);checkId(id);validate(q,true);return jdbc.read(c->members.list(c,id,role,actor.id(),q));
    }
    private String field(String raw,int limit,boolean required){
        String s=raw==null?"":raw.strip();
        if((required && s.isEmpty()) || s.codePointCount(0,s.length())>limit || s.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Kiểm tra mã lớp (30 ký tự), tên lớp (150) và năm học (9).");
        return s.isEmpty()?null:s;
    }
    private <T>T write(AuthUser actor,String ref,JdbcUnitOfWork.Work<T> work) throws SQLException {
        allow(actor,Scope.ADMIN);
        try{return jdbc.transaction(c->{
            if(!users.lockActiveAdmins(c).contains(actor.id()) || ref==null || !Long.valueOf(actor.id()).equals(sessions.active(c,ref)))throw new SecurityException();
            return work.execute(c);
        });}catch(SQLException e){
            if(e.getErrorCode()==1062)throw new Conflict("Mã lớp hoặc thành viên đã tồn tại.");
            if(e.getErrorCode()==1213 || e.getErrorCode()==1205)throw new Conflict("Đang có cập nhật khác. Hãy tải lại trang.");
            throw e;
        }
    }
    private SchoolClass lock(Connection c,long id,AuthUser actor,LocalDateTime version) throws SQLException {
        var current=classes.get(c,id,Scope.ADMIN,actor.id(),true);
        if(!current.getUpdatedAt().equals(version))throw new Conflict("Lớp đã thay đổi. Hãy tải lại trang.");
        return current;
    }
    public long save(AuthUser actor,String ref,Long id,LocalDateTime version,String rawCode,String rawName,String rawYear) throws SQLException {
        allow(actor,Scope.ADMIN);
        String code=field(rawCode,30,true),name=field(rawName,150,true),year=field(rawYear,9,false);
        if(id!=null)checkId(id);
        return write(actor,ref,c->{
            long target;
            if(id==null)target=classes.insert(c,code,name,year);
            else{target=id;lock(c,id,actor,version);classes.update(c,id,code,name,year);}
            classes.audit(c,actor.id(),target,id==null?"CLASS_CREATE":"CLASS_UPDATE",null);return target;
        });
    }
    public void archive(AuthUser actor,String ref,long id,LocalDateTime version) throws SQLException {
        checkId(id);write(actor,ref,c->{
            if(lock(c,id,actor,version).getStatus().equals("ARCHIVED"))throw new Conflict("Lớp đã lưu trữ.");
            classes.archive(c,id);classes.audit(c,actor.id(),id,"CLASS_ARCHIVE",null);return null;
        });
    }
    public void membership(AuthUser actor,String ref,long id,LocalDateTime version,long student,String action) throws SQLException {
        allow(actor,Scope.ADMIN);checkId(id);checkId(student);
        if(!Set.of("join","leave","rejoin").contains(action))throw new IllegalArgumentException("Thao tác không hợp lệ.");
        write(actor,ref,c->{
            if(users.lockStatus(c,student)==null)throw new NoSuchElementException();
            var k=lock(c,id,actor,version);
            if(!action.equals("leave") && (!k.getStatus().equals("ACTIVE") || !students.eligibleStudent(c,student)))
                throw new Conflict("Cần lớp đang hoạt động và tài khoản STUDENT đang hoạt động.");
            members.auditChange(c,actor.id(),id,student,action);
            if(!members.change(c,id,student,action))throw new Conflict("Tư cách thành viên không phù hợp với thao tác.");
            classes.touch(c,id);
            return null;
        });
    }
}
