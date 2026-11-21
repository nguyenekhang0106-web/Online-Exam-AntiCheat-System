package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.EnrollmentPage;
import vn.edu.onlineexam.model.EnrollmentPage.*;
import vn.edu.onlineexam.model.OfferingPage.Option;
import vn.edu.onlineexam.model.SubjectPage.Scope;

public final class EnrollmentDao {
    private static final String FROM=" FROM course_enrollments e JOIN users u ON u.id=e.student_id "
            +"JOIN course_offerings o ON o.id=e.offering_id JOIN subjects s ON s.id=o.subject_id JOIN classes k ON k.id=o.class_id ";
    private static final String FIELDS="SELECT o.id,u.id,s.subject_name,k.class_code,o.academic_year,o.semester,u.user_code,u.full_name,e.status,e.enrolled_at";
    private static String scope(Scope role,long actor,List<Object> args){
        if(role==Scope.ADMIN)return "1=1";
        args.add(actor);
        return role==Scope.TEACHER?"o.teacher_id=?":"e.student_id=?";
    }
    private static void bind(PreparedStatement p,List<Object> args) throws SQLException {
        for(int i=0;i<args.size();i++)p.setObject(i+1,args.get(i));
    }
    private static Row row(ResultSet r) throws SQLException {
        return new Row(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),
                r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getObject(10,LocalDateTime.class));
    }
    public EnrollmentPage list(Connection c,Scope role,long actor,Query q,int requested) throws SQLException {
        var args=new ArrayList<Object>();
        String where=FROM+"WHERE "+scope(role,actor,args);
        if(q.offering()>0){where+=" AND e.offering_id=?";args.add(q.offering());}
        if(!q.status().isEmpty()){where+=" AND e.status=?";args.add(q.status());}
        if(!q.keyword().isEmpty()){
            String term="%"+q.keyword().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            where+=" AND (u.user_code LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!' OR u.email LIKE ? ESCAPE '!')";
            args.addAll(Collections.nCopies(3,term));
        }
        long total;
        try(var p=c.prepareStatement("SELECT COUNT(*)"+where)){bind(p,args);try(var r=p.executeQuery()){r.next();total=r.getLong(1);}}
        long pages=total==0?0:1+(total-1)/q.size(),page=Math.min(requested,Math.max(1,pages));
        args.add(q.size());args.add((page-1)*q.size());
        String order=" ORDER BY "+q.sort().column+(q.descending()?" DESC":" ASC")+",e.offering_id,e.student_id LIMIT ? OFFSET ?";
        var rows=new ArrayList<Row>();
        try(var p=c.prepareStatement(FIELDS+where+order)){bind(p,args);try(var r=p.executeQuery()){while(r.next())rows.add(row(r));}}
        return new EnrollmentPage(List.copyOf(rows),total,page,pages,q);
    }
    public Row detail(Connection c,Scope role,long actor,long offering,long student) throws SQLException {
        var args=new ArrayList<Object>(List.of(offering,student));
        try(var p=c.prepareStatement(FIELDS+FROM+"WHERE e.offering_id=? AND e.student_id=? AND "+scope(role,actor,args))){
            bind(p,args);try(var r=p.executeQuery()){if(!r.next())throw new NoSuchElementException();return row(r);}
        }
    }
    private List<Option> options(Connection c,String sql) throws SQLException {
        var options=new ArrayList<Option>();
        try(var p=c.prepareStatement(sql);var r=p.executeQuery()){while(r.next())options.add(new Option(r.getLong(1),r.getString(2)));}
        return List.copyOf(options);
    }
    private static final String STUDENTS=" FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.status='ACTIVE' AND r.role_code='STUDENT'";
    public Choices choices(Connection c) throws SQLException {
        return new Choices(options(c,"SELECT o.id,CONCAT('#',o.id,' · ',s.subject_code,' · ',k.class_code,' · ',o.academic_year,' / ',o.semester) "
                +"FROM course_offerings o JOIN subjects s ON s.id=o.subject_id JOIN classes k ON k.id=o.class_id WHERE o.status='ACTIVE' AND s.status='ACTIVE' ORDER BY s.subject_code,o.id"),
                options(c,"SELECT u.id,CONCAT(u.full_name,' · ',COALESCE(u.user_code,u.email))"+STUDENTS+" ORDER BY u.full_name,u.id"));
    }
    public boolean eligibleStudent(Connection c,long student) throws SQLException {
        try(var p=c.prepareStatement("SELECT u.id"+STUDENTS+" AND u.id=? FOR UPDATE")){
            p.setLong(1,student);try(var r=p.executeQuery()){return r.next();}
        }
    }
    public boolean openOffering(Connection c,long offering) throws SQLException {
        try(var p=c.prepareStatement("SELECT o.status,s.status FROM course_offerings o JOIN subjects s ON s.id=o.subject_id WHERE o.id=? FOR UPDATE")){
            p.setLong(1,offering);try(var r=p.executeQuery()){
                if(!r.next())throw new NoSuchElementException();
                return "ACTIVE".equals(r.getString(1)) && "ACTIVE".equals(r.getString(2));
            }
        }
    }
    public boolean protectedExam(Connection c,long offering,long student,boolean removing) throws SQLException {
        // An attempt has no source-offering FK; preserve eligibility for every linked exam with an existing attempt.
        String sql="SELECT 1 FROM exam_course_assignments a JOIN exams x ON x.id=a.exam_id WHERE a.offering_id=? AND ("
                +"EXISTS(SELECT 1 FROM exam_attempts t WHERE t.exam_id=x.id AND t.student_id=?)"
                +(removing?" OR (x.status='PUBLISHED' AND a.opens_at<=UTC_TIMESTAMP(3) AND a.closes_at>UTC_TIMESTAMP(3) "
                    +"AND NOT EXISTS(SELECT 1 FROM exam_student_assignments p WHERE p.exam_id=x.id AND p.student_id=?))":"")+") LIMIT 1";
        try(var p=c.prepareStatement(sql)){
            p.setLong(1,offering);p.setLong(2,student);if(removing)p.setLong(3,student);
            try(var r=p.executeQuery()){return r.next();}
        }
    }
    public void insert(Connection c,long offering,long student) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO course_enrollments(offering_id,student_id,enrolled_at) VALUES(?,?,UTC_TIMESTAMP(3))")){
            p.setLong(1,offering);p.setLong(2,student);p.executeUpdate();
        }
    }
    public boolean finish(Connection c,long offering,long student,String next) throws SQLException {
        try(var p=c.prepareStatement("UPDATE course_enrollments SET status=? WHERE offering_id=? AND student_id=? AND status='ENROLLED'")){
            p.setString(1,next);p.setLong(2,offering);p.setLong(3,student);return p.executeUpdate()==1;
        }
    }
    public void audit(Connection c,long actor,long offering,long student,String status) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code,detail_json) "
                +"VALUES(?,'COURSE_ENROLLMENT',?,?,JSON_OBJECT('studentId',?,'status',?))")){
            bind(p,List.of(actor,offering,"ENROLLMENT_"+status,student,status));p.executeUpdate();
        }
    }
}
