package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.ClassDirectory;
import vn.edu.onlineexam.model.ClassDirectory.Query;
import vn.edu.onlineexam.model.SubjectPage.Scope;
import vn.edu.onlineexam.model.bean.academic.SchoolClass;

public final class SchoolClassDao {
    private static final String FIELDS="SELECT k.id,k.class_code,k.class_name,k.academic_year,k.status,k.created_at,k.updated_at FROM classes k WHERE ";
    static String scope(Scope scope,long actor,List<Object> args) {
        if(scope==Scope.ADMIN) return "1=1";
        args.add(actor);
        return scope==Scope.TEACHER ? "EXISTS(SELECT 1 FROM course_offerings o WHERE o.class_id=k.id AND o.teacher_id=?)"
                : "EXISTS(SELECT 1 FROM class_students cs WHERE cs.class_id=k.id AND cs.student_id=?)";
    }
    static void bind(PreparedStatement ps,List<?> args) throws SQLException {
        for(int i=0;i<args.size();i++) ps.setObject(i+1,args.get(i));
    }
    private SchoolClass row(ResultSet r) throws SQLException {
        return new SchoolClass(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),
                r.getObject(6,LocalDateTime.class),r.getObject(7,LocalDateTime.class));
    }
    public ClassDirectory<SchoolClass> list(Connection c,Scope scope,long actor,Query q) throws SQLException {
        var args=new ArrayList<Object>();
        String sql=FIELDS+scope(scope,actor,args);
        if(!q.term().isEmpty()){sql+=" AND (k.class_code LIKE ? ESCAPE '!' OR k.class_name LIKE ? ESCAPE '!')";args.add(q.pattern());args.add(q.pattern());}
        if(!q.status().isEmpty()){sql+=" AND k.status=?";args.add(q.status());}
        sql+=" ORDER BY k.class_code "+(q.descending()?"DESC":"ASC")+",k.id LIMIT ? OFFSET ?";
        args.add(q.size()+1);args.add(q.offset());
        var rows=new ArrayList<SchoolClass>();
        try(var p=c.prepareStatement(sql)){bind(p,args);try(var r=p.executeQuery()){while(r.next())rows.add(row(r));}}
        return ClassDirectory.of(rows,q);
    }
    public SchoolClass get(Connection c,long id,Scope scope,long actor,boolean lock) throws SQLException {
        var args=new ArrayList<Object>();args.add(id);
        String sql=FIELDS+"k.id=? AND "+scope(scope,actor,args)+(lock?" FOR UPDATE":"");
        try(var p=c.prepareStatement(sql)){bind(p,args);try(var r=p.executeQuery()){if(!r.next())throw new NoSuchElementException();return row(r);}}
    }
    public long insert(Connection c,String code,String name,String year) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO classes(class_code,class_name,academic_year) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            p.setString(1,code);p.setString(2,name);p.setString(3,year);p.executeUpdate();
            try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Missing class id");return r.getLong(1);}
        }
    }
    private static final String VERSION="updated_at=GREATEST(UTC_TIMESTAMP(3),updated_at+INTERVAL 1000 MICROSECOND)";
    public void update(Connection c,long id,String code,String name,String year) throws SQLException {
        try(var p=c.prepareStatement("UPDATE classes SET class_code=?,class_name=?,academic_year=?,"+VERSION+" WHERE id=?")){
            p.setString(1,code);p.setString(2,name);p.setString(3,year);p.setLong(4,id);p.executeUpdate();
        }
    }
    public void archive(Connection c,long id) throws SQLException {
        try(var p=c.prepareStatement("UPDATE classes SET status='ARCHIVED',"+VERSION+" WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
    }
    public void touch(Connection c,long id) throws SQLException {
        try(var p=c.prepareStatement("UPDATE classes SET "+VERSION+" WHERE id=?")){p.setLong(1,id);p.executeUpdate();}
    }
    public void audit(Connection c,long actor,long id,String action,Long student) throws SQLException {
        try(var p=c.prepareStatement("INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code,detail_json) VALUES(?,'CLASS',?,?,JSON_OBJECT('studentId',?))")){
            p.setLong(1,actor);p.setLong(2,id);p.setString(3,action);p.setObject(4,student);p.executeUpdate();
        }
    }
}
