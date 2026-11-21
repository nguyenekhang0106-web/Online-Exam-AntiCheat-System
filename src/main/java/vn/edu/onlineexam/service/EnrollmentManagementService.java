package vn.edu.onlineexam.service;

import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.dao.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.EnrollmentPage.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;

public final class EnrollmentManagementService {
    private final JdbcUnitOfWork jdbc;
    private final EnrollmentDao dao=new EnrollmentDao();
    private final UserDao users=new UserDao();
    private final AuthDao sessions=new AuthDao();
    public EnrollmentManagementService(JdbcUnitOfWork jdbc){this.jdbc=jdbc;}
    public static final class Conflict extends RuntimeException {public Conflict(String message){super(message);}}
    private void allow(AuthUser actor,Scope role){if(actor==null || !actor.roles().contains(role.name()))throw new SecurityException();}
    private void keys(long offering,long student){if(offering<=0 || student<=0)throw new IllegalArgumentException("ID không hợp lệ.");}
    public EnrollmentPage search(AuthUser actor,Scope role,String term,long offering,String status,String sort,String direction,int page,int size) throws SQLException {
        allow(actor,role);
        if(term.length()>254 || offering<0 || page<1 || size<1 || size>100 || !Set.of("ASC","DESC").contains(direction)
                || !Set.of("","ENROLLED","WITHDRAWN","COMPLETED").contains(status))throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        var query=new Query(term.strip(),offering,status,Sort.valueOf(sort),direction.equals("DESC"),size);
        return jdbc.read(c->dao.list(c,role,actor.id(),query,page));
    }
    public Row detail(AuthUser actor,Scope role,long offering,long student) throws SQLException {
        allow(actor,role);keys(offering,student);return jdbc.read(c->dao.detail(c,role,actor.id(),offering,student));
    }
    public Choices choices(AuthUser actor) throws SQLException {
        allow(actor,Scope.ADMIN);return jdbc.read(dao::choices);
    }
    public void change(AuthUser actor,String ref,long offering,long student,String next) throws SQLException {
        allow(actor,Scope.ADMIN);keys(offering,student);
        if(!Set.of("ENROLLED","WITHDRAWN","COMPLETED").contains(next))throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        boolean create=next.equals("ENROLLED");
        try {
            jdbc.transaction(c->{
                if(!users.lockActiveAdmins(c).contains(actor.id()) || ref==null || !Long.valueOf(actor.id()).equals(sessions.active(c,ref)))throw new SecurityException();
                if(users.lockStatus(c,student)==null)throw new NoSuchElementException();
                boolean open=dao.openOffering(c,offering);
                if(create && (!open || !dao.eligibleStudent(c,student)))throw new Conflict("Cần lớp và môn đang mở cùng tài khoản STUDENT đang hoạt động.");
                if(!create){
                    var current=dao.detail(c,Scope.ADMIN,actor.id(),offering,student);
                    if(!current.status().equals("ENROLLED"))throw new Conflict("Ghi danh đã kết thúc. Hãy tải lại trang.");
                }
                if(dao.protectedExam(c,offering,student,!create))throw new Conflict("Không thể thay đổi ghi danh có quyền thi đang hiệu lực hoặc lượt thi liên quan.");
                if(create)dao.insert(c,offering,student);
                else if(!dao.finish(c,offering,student,next))throw new Conflict("Ghi danh đã thay đổi.");
                dao.audit(c,actor.id(),offering,student,next);return null;
            });
        } catch(SQLException e){
            if(e.getErrorCode()==1062)throw new Conflict("Sinh viên đã có ghi danh trong lớp này.");
            if(e.getErrorCode()==1452 || e.getErrorCode()==1213 || e.getErrorCode()==1205)throw new Conflict("Dữ liệu liên quan đã thay đổi. Hãy tải lại trang.");
            throw e;
        }
    }
}
