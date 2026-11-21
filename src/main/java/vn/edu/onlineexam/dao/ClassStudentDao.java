package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.ClassDirectory;
import vn.edu.onlineexam.model.ClassDirectory.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;

public final class ClassStudentDao {
    public ClassDirectory<Member> list(Connection c,long id,Scope scope,long actor,Query q) throws SQLException {
        var args=new ArrayList<Object>();args.add(id);
        String sql="SELECT cs.student_id,u.user_code,u.full_name,cs.status,cs.joined_at,cs.left_at FROM class_students cs "
                +"JOIN classes k ON k.id=cs.class_id JOIN users u ON u.id=cs.student_id WHERE cs.class_id=? AND "+SchoolClassDao.scope(scope,actor,args);
        if(scope==Scope.STUDENT){sql+=" AND cs.student_id=?";args.add(actor);}
        if(!q.term().isEmpty()){sql+=" AND (u.user_code LIKE ? ESCAPE '!' OR u.full_name LIKE ? ESCAPE '!')";args.add(q.pattern());args.add(q.pattern());}
        if(!q.status().isEmpty()){sql+=" AND cs.status=?";args.add(q.status());}
        sql+=" ORDER BY u.full_name "+(q.descending()?"DESC":"ASC")+",cs.student_id LIMIT ? OFFSET ?";
        args.add(q.size()+1);args.add(q.offset());
        var rows=new ArrayList<Member>();
        try(var p=c.prepareStatement(sql)){SchoolClassDao.bind(p,args);try(var r=p.executeQuery()){while(r.next())
            rows.add(new Member(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getObject(5,LocalDateTime.class),r.getObject(6,LocalDateTime.class)));}}
        return ClassDirectory.of(rows,q);
    }
    /** Snapshot the interval before leave/rejoin replaces its timestamps. Rolled back on a rejected change. */
    public void auditChange(Connection c,long actor,long id,long student,String action) throws SQLException {
        String sql="""
                INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code,detail_json)
                SELECT ?,'CLASS',?,?,JSON_OBJECT('studentId',?,'previousStatus',cs.status,
                       'previousJoinedAt',cs.joined_at,'previousLeftAt',cs.left_at)
                FROM (SELECT 1) anchor LEFT JOIN class_students cs ON cs.class_id=? AND cs.student_id=?
                """;
        try(var p=c.prepareStatement(sql)){
            SchoolClassDao.bind(p,List.of(actor,id,"CLASS_"+action.toUpperCase(Locale.ROOT),student,id,student));
            p.executeUpdate();
        }
    }
    public boolean change(Connection c,long id,long student,String action) throws SQLException {
        String sql=switch(action){
            case "join" -> "INSERT INTO class_students(class_id,student_id,joined_at,status) VALUES(?,?,UTC_TIMESTAMP(3),'ACTIVE')";
            case "leave" -> "UPDATE class_students SET left_at=GREATEST(joined_at,UTC_TIMESTAMP(3)),status='LEFT' WHERE class_id=? AND student_id=? AND status='ACTIVE'";
            case "rejoin" -> "UPDATE class_students SET joined_at=GREATEST(left_at,UTC_TIMESTAMP(3)),left_at=NULL,status='ACTIVE' WHERE class_id=? AND student_id=? AND status='LEFT'";
            default -> throw new IllegalArgumentException();
        };
        try(var p=c.prepareStatement(sql)){p.setLong(1,id);p.setLong(2,student);return p.executeUpdate()==1;}
    }
}
