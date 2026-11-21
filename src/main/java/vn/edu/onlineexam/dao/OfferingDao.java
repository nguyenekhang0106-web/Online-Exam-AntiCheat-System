package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.OfferingPage;
import vn.edu.onlineexam.model.OfferingPage.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;

public final class OfferingDao {
    private static final String FROM=" FROM course_offerings o JOIN subjects s ON s.id=o.subject_id "
            +"JOIN classes k ON k.id=o.class_id JOIN users u ON u.id=o.teacher_id ";
    private static final String FIELDS="SELECT o.id,s.id,CONCAT(s.subject_code,' · ',s.subject_name),"
            +"CONCAT(k.class_code,' · ',k.class_name),u.full_name,u.id,o.academic_year,o.semester,o.status,";
    private static final String VERSION="COALESCE((SELECT MAX(a.id) FROM audit_logs a WHERE a.target_type='COURSE_OFFERING' AND a.target_id=o.id),0)";
    private static String scope(Scope role,List<Object> args,long actor){
        if(role==Scope.ADMIN) return "1=1";
        args.add(actor);
        if(role==Scope.TEACHER) return "o.teacher_id=?";
        return "EXISTS(SELECT 1 FROM course_enrollments e WHERE e.offering_id=o.id AND e.student_id=? AND e.status IN ('ENROLLED','COMPLETED'))";
    }
    private static void bind(PreparedStatement p,List<Object> args) throws SQLException {
        for(int i=0;i<args.size();i++) p.setObject(i+1,args.get(i));
    }
    private static Row row(ResultSet r) throws SQLException {
        return new Row(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getLong(6),
                r.getString(7),r.getString(8),r.getString(9),r.getLong(10));
    }
    public OfferingPage search(Connection c,Scope role,long actor,Query q,int requested) throws SQLException {
        var args=new ArrayList<Object>();
        String where=FROM+"WHERE "+scope(role,args,actor);
        if(q.subjectId()>0){where+=" AND s.id=?";args.add(q.subjectId());}
        if(!q.status().isEmpty()){where+=" AND o.status=?";args.add(q.status());}
        if(!q.keyword().isEmpty()){
            String pattern="%"+q.keyword().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            where+=" AND (s.subject_code LIKE ? ESCAPE '!' OR s.subject_name LIKE ? ESCAPE '!' OR k.class_code LIKE ? ESCAPE '!' OR k.class_name LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!')";
            args.addAll(Collections.nCopies(5,pattern));
        }
        long total;
        try(var p=c.prepareStatement("SELECT COUNT(*)"+where)){bind(p,args);try(var r=p.executeQuery()){r.next();total=r.getLong(1);}}
        long pages=total==0?0:1+(total-1)/q.size();
        int page=(int)Math.min(requested,Math.max(1,pages));
        String sql=FIELDS+"0"+where+" ORDER BY "+q.sort().column+(q.descending()?" DESC":" ASC")+",o.id LIMIT ? OFFSET ?";
        args.add(q.size());args.add((page-1L)*q.size());
        var result=new ArrayList<Row>();
        try(var p=c.prepareStatement(sql)){bind(p,args);try(var r=p.executeQuery()){while(r.next()) result.add(row(r));}}
        return new OfferingPage(List.copyOf(result),total,page,pages,q);
    }
    public Row detail(Connection c,Scope role,long actor,long id) throws SQLException {
        var args=new ArrayList<Object>();args.add(id);
        try(var p=c.prepareStatement(FIELDS+VERSION+FROM+"WHERE o.id=? AND "+scope(role,args,actor))){
            bind(p,args);try(var r=p.executeQuery()){if(!r.next()) throw new NoSuchElementException();return row(r);}
        }
    }
    private static final String TEACHERS=" FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.status='ACTIVE' AND r.role_code='TEACHER'";
    private List<Option> options(Connection c,String sql) throws SQLException {
        var rows=new ArrayList<Option>();
        try(var p=c.prepareStatement(sql);var r=p.executeQuery()){while(r.next()) rows.add(new Option(r.getLong(1),r.getString(2)));}
        return List.copyOf(rows);
    }
    public Choices choices(Connection c,boolean create) throws SQLException {
        return new Choices(create?options(c,"SELECT id,CONCAT(subject_code,' · ',subject_name) FROM subjects WHERE status='ACTIVE' ORDER BY subject_code"):List.of(),
                create?options(c,"SELECT id,CONCAT(class_code,' · ',class_name) FROM classes WHERE status='ACTIVE' ORDER BY class_code"):List.of(),
                options(c,"SELECT u.id,CONCAT(u.full_name,' · ',u.email)"+TEACHERS+" ORDER BY u.full_name,u.id"));
    }
    private boolean exists(Connection c,String sql,long id) throws SQLException {
        try(var p=c.prepareStatement(sql)){p.setLong(1,id);try(var r=p.executeQuery()){return r.next();}}
    }
    public boolean eligibleParents(Connection c,long subject,long schoolClass) throws SQLException {
        return exists(c,"SELECT id FROM subjects WHERE id=? AND status='ACTIVE' FOR UPDATE",subject)
                && exists(c,"SELECT id FROM classes WHERE id=? AND status='ACTIVE' FOR UPDATE",schoolClass);
    }
    public boolean teacher(Connection c,long id) throws SQLException {return exists(c,"SELECT u.id"+TEACHERS+" AND u.id=? FOR UPDATE",id);}
    public boolean lock(Connection c,long id) throws SQLException {return exists(c,"SELECT id FROM course_offerings WHERE id=? FOR UPDATE",id);}
    public long insert(Connection c,long subject,long schoolClass,long teacher,String year,String semester) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO course_offerings(subject_id,class_id,teacher_id,academic_year,semester) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            bind(p,List.of(subject,schoolClass,teacher,year,semester));p.executeUpdate();
            try(var r=p.getGeneratedKeys()){if(!r.next()) throw new SQLException("Missing offering ID");return r.getLong(1);}
        }
    }
    public void assign(Connection c,long id,long teacher) throws SQLException {
        try(var p=c.prepareStatement("UPDATE course_offerings SET teacher_id=? WHERE id=?")){p.setLong(1,teacher);p.setLong(2,id);p.executeUpdate();}
    }
    public void close(Connection c,long id) throws SQLException {
        try(var p=c.prepareStatement("UPDATE course_offerings SET status='CLOSED' WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
    }
    public void audit(Connection c,long actor,long id,String action) throws SQLException {
        String details="NULL";
        var args=new ArrayList<Object>(List.of(actor,id,action));
        if(action.equals("OFFERING_CLOSE")){
            details="JSON_OBJECT('enrollments',(SELECT COUNT(*) FROM course_enrollments WHERE offering_id=?),"
                    +"'examAssignments',(SELECT COUNT(*) FROM exam_course_assignments WHERE offering_id=?))";
            args.add(id);args.add(id);
        }
        try(var p=c.prepareStatement("INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code,detail_json) "
                +"VALUES(?,'COURSE_OFFERING',?,?,"+details+")")){
            bind(p,args);p.executeUpdate();
        }
    }
}
