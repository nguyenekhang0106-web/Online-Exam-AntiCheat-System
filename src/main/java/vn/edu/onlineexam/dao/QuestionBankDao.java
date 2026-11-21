package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.QuestionCatalog.*;

public final class QuestionBankDao extends QuestionCatalogDao {
    private static final String SELECT="SELECT b.id,b.subject_id,b.owner_id,b.bank_name AS label,b.description,b.visibility,s.subject_code,"
            +"(SELECT COUNT(*) FROM questions q WHERE q.bank_id=b.id) AS question_count FROM question_banks b JOIN subjects s ON s.id=b.subject_id";
    public Page list(Connection c,Filter f,Long owner)throws SQLException{
        var q=new Search(SELECT);if(owner!=null)q.and("b.owner_id=?",owner);
        if(f.subject()>0)q.and("b.subject_id=?",f.subject());if(!f.state().isEmpty())q.and("b.visibility=?",f.state());
        q.term(f.term(),"b.bank_name");return q.page(c,f,f.sort().equals("name")?"b.bank_name":"b.id","b.id");
    }
    public Map<String,Object> detail(Connection c,long id,Long owner)throws SQLException{
        return owner==null?one(c,SELECT+" WHERE b.id=?",id):one(c,SELECT+" WHERE b.id=? AND b.owner_id=?",id,owner);
    }
    public void lockOwned(Connection c,long id,long owner)throws SQLException{
        one(c,"SELECT id FROM question_banks WHERE id=? AND owner_id=? FOR UPDATE",id,owner);
    }
    public boolean used(Connection c,long id)throws SQLException{
        return exists(c,"SELECT 1 FROM questions q JOIN question_versions v ON v.question_id=q.id JOIN exam_questions e ON e.question_version_id=v.id WHERE q.bank_id=? LIMIT 1",id);
    }
    public long save(Connection c,long id,long subject,long owner,String name,String description,String visibility)throws SQLException{
        if(id==0)return insert(c,"INSERT INTO question_banks(subject_id,owner_id,bank_name,description,visibility) VALUES(?,?,?,?,?)",subject,owner,name,description,visibility);
        update(c,"UPDATE question_banks SET bank_name=?,description=?,visibility=? WHERE id=?",name,description,visibility,id);return id;
    }
    public List<Map<String,Object>> owned(Connection c,long owner)throws SQLException{
        return rows(c,"SELECT b.id,CONCAT(s.subject_code,' · ',b.bank_name) AS label FROM question_banks b JOIN subjects s ON s.id=b.subject_id WHERE b.owner_id=? AND s.status='ACTIVE' ORDER BY label,b.id",owner);
    }
}
