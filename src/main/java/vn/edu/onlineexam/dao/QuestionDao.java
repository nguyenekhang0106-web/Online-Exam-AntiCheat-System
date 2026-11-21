package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.QuestionCatalog.*;

public final class QuestionDao extends QuestionCatalogDao {
    private static final String META="q.id,q.bank_id,q.topic_id,q.author_id,q.question_type,q.difficulty,q.status,b.subject_id,b.bank_name,s.subject_code";
    private static final String FROM=" FROM questions q JOIN question_banks b ON b.id=q.bank_id JOIN subjects s ON s.id=b.subject_id";
    private static final String LATEST=" LEFT JOIN question_versions v ON v.id=(SELECT vv.id FROM question_versions vv WHERE vv.question_id=q.id ORDER BY vv.version_no DESC LIMIT 1)";
    private String select(boolean authoring){
        return "SELECT "+META+(authoring?",v.content AS label,v.explanation,v.default_points,v.id AS version_id,v.version_no":",CONCAT('Câu hỏi #',q.id) AS label")+FROM+(authoring?LATEST:"");
    }
    public Page list(Connection c,Filter f,Long owner)throws SQLException{
        // The admin projection deliberately excludes the version and option tables.
        String projection=owner==null?select(false):"SELECT "+META+",LEFT(v.content,300) AS label"+FROM+LATEST;
        var q=new Search(projection);if(owner!=null)q.and("b.owner_id=?",owner);
        if(f.subject()>0)q.and("b.subject_id=?",f.subject());if(f.bank()>0)q.and("q.bank_id=?",f.bank());if(f.topic()>0)q.and("q.topic_id=?",f.topic());
        if(!f.state().isEmpty())q.and("q.status=?",f.state());if(!f.type().isEmpty())q.and("q.question_type=?",f.type());
        if(!f.difficulty().isEmpty())q.and("q.difficulty=?",f.difficulty());
        if(owner==null)q.term(f.term(),"b.bank_name");else q.term(f.term(),"v.content","b.bank_name");
        return q.page(c,f,f.sort().equals("name")?"b.bank_name":"q.id","q.id");
    }
    public Map<String,Object> detail(Connection c,long id,Long owner)throws SQLException{
        return owner==null?one(c,select(false)+" WHERE q.id=?",id):one(c,select(true)+" WHERE q.id=? AND b.owner_id=?",id,owner);
    }
    public List<Answer> options(Connection c,long version)throws SQLException{
        var answers=new ArrayList<Answer>();
        try(var p=c.prepareStatement("SELECT option_content,is_correct FROM question_version_options WHERE question_version_id=? ORDER BY display_order")){
            p.setLong(1,version);try(var r=p.executeQuery()){while(r.next())answers.add(new Answer(r.getString(1),r.getBoolean(2)));}
        }
        return List.copyOf(answers);
    }
    public void lock(Connection c,long id)throws SQLException{one(c,"SELECT id FROM questions WHERE id=? FOR UPDATE",id);}
    public boolean used(Connection c,long id)throws SQLException{
        return exists(c,"SELECT 1 FROM question_versions v JOIN exam_questions e ON e.question_version_id=v.id WHERE v.question_id=? LIMIT 1",id);
    }
    public boolean attachments(Connection c,long version)throws SQLException{
        return exists(c,"SELECT 1 FROM question_attachments WHERE question_version_id=? LIMIT 1",version);
    }
    public long save(Connection c,long id,long actor,Draft d,int number)throws SQLException{
        Long topic=d.topic()==0?null:d.topic();
        if(id==0)id=insert(c,"INSERT INTO questions(bank_id,topic_id,author_id,question_type,difficulty) VALUES(?,?,?,?,?)",d.bank(),topic,actor,d.type(),d.difficulty());
        else update(c,"UPDATE questions SET topic_id=?,question_type=?,difficulty=? WHERE id=?",topic,d.type(),d.difficulty(),id);
        long version=insert(c,"INSERT INTO question_versions(question_id,version_no,content,explanation,default_points,created_by) VALUES(?,?,?,?,?,?)",
                id,number,d.content(),d.explanation(),d.points(),actor);
        try(var p=c.prepareStatement("INSERT INTO question_version_options(question_version_id,option_content,is_correct,display_order) VALUES(?,?,?,?)")){
            int order=0;for(var a:d.options()){p.setLong(1,version);p.setString(2,a.text());p.setBoolean(3,a.correct());p.setInt(4,order++);p.addBatch();}p.executeBatch();
        }
        return id;
    }
    public void status(Connection c,long id,String next)throws SQLException{update(c,"UPDATE questions SET status=? WHERE id=?",next,id);}
}
