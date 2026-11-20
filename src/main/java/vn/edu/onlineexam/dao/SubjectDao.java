package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.SubjectPage.*;
import vn.edu.onlineexam.model.SubjectPage;
import vn.edu.onlineexam.model.bean.academic.Subject;

public final class SubjectDao {
    private static final String COLUMNS = "s.id,s.subject_code,s.subject_name,s.description,s.status,s.created_at,s.updated_at";
    private static String scope(Scope scope) {
        return switch (scope) {
            case ADMIN -> "1=1";
            case TEACHER -> "EXISTS (SELECT 1 FROM course_offerings o WHERE o.subject_id=s.id AND o.teacher_id=?)";
            case STUDENT -> "EXISTS (SELECT 1 FROM course_offerings o JOIN course_enrollments e ON e.offering_id=o.id "
                    + "WHERE o.subject_id=s.id AND e.student_id=? AND e.status IN ('ENROLLED','COMPLETED'))";
        };
    }
    private static Subject row(ResultSet r) throws SQLException {
        return new Subject(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),
                r.getObject(6,LocalDateTime.class),r.getObject(7,LocalDateTime.class));
    }
    private static int bind(PreparedStatement ps,List<Object> args) throws SQLException {
        int i=1;
        for(Object value:args) ps.setObject(i++,value);
        return i;
    }
    public SubjectPage search(Connection c, Scope access, long actor, Query q, int requested) throws SQLException {
        String where=" FROM subjects s WHERE "+scope(access);
        var args=new ArrayList<Object>();
        if(access!=Scope.ADMIN) args.add(actor);
        if(!q.keyword().isEmpty()) {
            String pattern="%"+q.keyword().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            where+=" AND (s.subject_code LIKE ? ESCAPE '!' OR s.subject_name LIKE ? ESCAPE '!')";
            args.add(pattern); args.add(pattern);
        }
        if(!q.status().isEmpty()) { where+=" AND s.status=?"; args.add(q.status()); }
        long total;
        try(var ps=c.prepareStatement("SELECT COUNT(*)"+where)) {
            bind(ps,args);
            try(var r=ps.executeQuery()){r.next(); total=r.getLong(1);}
        }
        long pages=total==0?0:1+(total-1)/q.size(), page=Math.min(requested,Math.max(1,pages));
        var rows=new ArrayList<Subject>();
        if(total>0) {
            String order=" ORDER BY "+q.sort().column+(q.descending()?" DESC":" ASC")+",s.id LIMIT ? OFFSET ?";
            try(var ps=c.prepareStatement("SELECT "+COLUMNS+where+order)) {
                int i=bind(ps,args); ps.setInt(i++,q.size()); ps.setLong(i,(page-1)*q.size());
                try(var r=ps.executeQuery()){while(r.next()) rows.add(row(r));}
            }
        }
        return new SubjectPage(List.copyOf(rows),total,page,pages,q);
    }
    public Subject detail(Connection c,Scope access,long actor,long id,boolean lock) throws SQLException {
        String sql="SELECT "+COLUMNS+" FROM subjects s WHERE s.id=? AND "+scope(access)+(lock?" FOR UPDATE":"");
        try(var ps=c.prepareStatement(sql)) {
            ps.setLong(1,id);
            if(access!=Scope.ADMIN) ps.setLong(2,actor);
            try(var r=ps.executeQuery()){if(!r.next()) throw new NoSuchElementException(); return row(r);}
        }
    }
    public boolean referenced(Connection c,long id) throws SQLException {
        String sql="""
                SELECT EXISTS(SELECT 1 FROM course_offerings WHERE subject_id=?)
                    OR EXISTS(SELECT 1 FROM topics WHERE subject_id=?)
                    OR EXISTS(SELECT 1 FROM question_banks WHERE subject_id=?)
                    OR EXISTS(SELECT 1 FROM exams WHERE subject_id=?)
                """;
        try(var ps=c.prepareStatement(sql)) {
            for(int i=1;i<=4;i++) ps.setLong(i,id);
            try(var r=ps.executeQuery()){r.next(); return r.getBoolean(1);}
        }
    }
    private void fields(PreparedStatement ps,Input in) throws SQLException {
        ps.setString(1,in.code()); ps.setString(2,in.name()); ps.setString(3,in.description());
    }
    public long insert(Connection c,Input in) throws SQLException {
        try(var ps=c.prepareStatement("INSERT INTO subjects(subject_code,subject_name,description) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            fields(ps,in); ps.executeUpdate();
            try(var r=ps.getGeneratedKeys()){if(!r.next()) throw new SQLException("Missing subject ID"); return r.getLong(1);}
        }
    }
    private static final String VERSION="updated_at=GREATEST(UTC_TIMESTAMP(3), updated_at + INTERVAL 1000 MICROSECOND)";
    public void update(Connection c,long id,Input in) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE subjects SET subject_code=?,subject_name=?,description=?,"+VERSION+" WHERE id=?")) {
            fields(ps,in); ps.setLong(4,id); ps.executeUpdate();
        }
    }
    public void archive(Connection c,long id) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE subjects SET status='ARCHIVED',"+VERSION+" WHERE id=?")) {
            ps.setLong(1,id); ps.executeUpdate();
        }
    }
    public void audit(Connection c,long actor,long target,String action) throws SQLException {
        try(var ps=c.prepareStatement("INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code) VALUES(?,'SUBJECT',?,?)")) {
            ps.setLong(1,actor); ps.setLong(2,target); ps.setString(3,action); ps.executeUpdate();
        }
    }
}
