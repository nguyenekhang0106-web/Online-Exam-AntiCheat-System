package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.QuestionCatalog.*;
import vn.edu.onlineexam.model.QuestionCatalog.Module;

/** Prepared queries shared by the three catalog DAOs; the caller owns the connection. */
public abstract class QuestionCatalogDao {
    protected static List<Map<String,Object>> rows(Connection c,String sql,Object... args) throws SQLException {
        try(var p=c.prepareStatement(sql)){
            bind(p,args);
            try(var r=p.executeQuery()){
                var result=new ArrayList<Map<String,Object>>();
                var meta=r.getMetaData();
                while(r.next()){
                    var row=new LinkedHashMap<String,Object>();
                    for(int i=1;i<=meta.getColumnCount();i++)row.put(meta.getColumnLabel(i),r.getObject(i));
                    result.add(row);
                }
                return result;
            }
        }
    }
    protected static Map<String,Object> one(Connection c,String sql,Object... args) throws SQLException {
        var result=rows(c,sql,args);
        if(result.isEmpty())throw new NoSuchElementException();
        return result.get(0);
    }
    protected static boolean exists(Connection c,String sql,Object... args) throws SQLException { return !rows(c,sql,args).isEmpty(); }
    private static void bind(PreparedStatement p,Object[] args) throws SQLException {
        for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);
    }
    protected static long insert(Connection c,String sql,Object... args) throws SQLException {
        try(var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            bind(p,args);p.executeUpdate();try(var r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Missing generated ID");return r.getLong(1);}
        }
    }
    protected static void update(Connection c,String sql,Object... args) throws SQLException {
        try(var p=c.prepareStatement(sql)){bind(p,args);p.executeUpdate();}
    }
    protected static final class Search {
        private String sql;
        private final List<Object> args=new ArrayList<>();
        Search(String sql){this.sql=sql+" WHERE 1=1";}
        void and(String condition,Object... values){sql+=" AND ("+condition+")";Collections.addAll(args,values);}
        void term(String term,String... columns){
            if(term.isEmpty())return;
            String pattern="%"+term.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            var clauses=new ArrayList<String>();for(String column:columns)clauses.add(column+" LIKE ? ESCAPE '!'");
            and(String.join(" OR ",clauses),Collections.nCopies(columns.length,pattern).toArray());
        }
        Page page(Connection c,Filter f,String order,String id) throws SQLException {
            args.add(f.size()+1);args.add((f.page()-1L)*f.size());
            var data=rows(c,sql+" ORDER BY "+order+(f.descending()?" DESC":" ASC")+","+id+" LIMIT ? OFFSET ?",args.toArray());
            boolean more=data.size()>f.size();if(more)data.remove(data.size()-1);
            return new Page(data,more,f);
        }
    }
    public static long stamp(Connection c,Module module,long id) throws SQLException {
        return ((Number)one(c,"SELECT COALESCE(MAX(id),0) AS stamp FROM audit_logs WHERE target_type=? AND target_id=?",module.name(),id).get("stamp")).longValue();
    }
    public static void audit(Connection c,Module module,long actor,long id,String action) throws SQLException {
        update(c,"INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code) VALUES(?,?,?,?)",actor,module.name(),id,action);
    }
    public static List<Map<String,Object>> subjects(Connection c,Long teacher) throws SQLException {
        String sql="SELECT s.id,CONCAT(s.subject_code,' · ',s.subject_name) AS label FROM subjects s WHERE s.status='ACTIVE'";
        return teacher==null?rows(c,sql+" ORDER BY label"):rows(c,sql+" AND EXISTS(SELECT 1 FROM course_offerings o WHERE o.subject_id=s.id AND o.teacher_id=?) ORDER BY label",teacher);
    }
    public static void lockSubject(Connection c,long id,Long teacher) throws SQLException {
        var row=one(c,"SELECT status FROM subjects WHERE id=? FOR UPDATE",id);
        if(!"ACTIVE".equals(row.get("status")))throw new IllegalStateException("Môn học đã ngừng hoạt động.");
        if(teacher!=null && !exists(c,"SELECT id FROM course_offerings WHERE subject_id=? AND teacher_id=? LIMIT 1 FOR UPDATE",id,teacher))throw new SecurityException();
    }
}
