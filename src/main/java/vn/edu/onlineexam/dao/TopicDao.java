package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.QuestionCatalog.*;

public final class TopicDao extends QuestionCatalogDao {
    private static final String SELECT="SELECT t.id,t.subject_id,t.parent_topic_id,t.topic_code,t.topic_name AS label,t.display_order,s.subject_code FROM topics t JOIN subjects s ON s.id=t.subject_id";
    private static final String SCOPE="EXISTS(SELECT 1 FROM course_offerings o WHERE o.subject_id=t.subject_id AND o.teacher_id=?)";
    public Page list(Connection c,Filter f,Long teacher)throws SQLException{
        var q=new Search(SELECT);if(teacher!=null)q.and(SCOPE,teacher);
        if(f.subject()>0)q.and("t.subject_id=?",f.subject());q.term(f.term(),"t.topic_code","t.topic_name");
        return q.page(c,f,f.sort().equals("name")?"t.topic_name":"t.id","t.id");
    }
    public Map<String,Object> detail(Connection c,long id,Long teacher)throws SQLException{
        return teacher==null?one(c,SELECT+" WHERE t.id=?",id):one(c,SELECT+" WHERE t.id=? AND "+SCOPE,id,teacher);
    }
    public void parent(Connection c,long subject,Long parent,long id)throws SQLException{
        Set<Long> visited=new HashSet<>();
        while(parent!=null){
            if(parent==id || !visited.add(parent))throw new IllegalArgumentException("Quan hệ cha tạo chu trình.");
            var row=one(c,"SELECT subject_id,parent_topic_id FROM topics WHERE id=?",parent);
            if(((Number)row.get("subject_id")).longValue()!=subject)throw new IllegalArgumentException("Chủ đề cha phải cùng môn.");
            Number p=(Number)row.get("parent_topic_id");parent=p==null?null:p.longValue();
        }
    }
    public boolean usedInExam(Connection c,long topic)throws SQLException{
        return exists(c,"SELECT 1 FROM questions q JOIN question_versions v ON v.question_id=q.id "
                +"JOIN exam_questions e ON e.question_version_id=v.id WHERE q.topic_id=? LIMIT 1",topic);
    }
    public long save(Connection c,long id,long subject,Long parent,String code,String name,int order)throws SQLException{
        if(id==0)return insert(c,"INSERT INTO topics(subject_id,parent_topic_id,topic_code,topic_name,display_order) VALUES(?,?,?,?,?)",subject,parent,code,name,order);
        update(c,"UPDATE topics SET parent_topic_id=?,topic_code=?,topic_name=?,display_order=? WHERE id=?",parent,code,name,order,id);return id;
    }
}
