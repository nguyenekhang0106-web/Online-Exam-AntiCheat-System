package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public final class ExamQuestionDao extends QuestionCatalogDao {
    private static final String SOURCE = """
        FROM question_versions v JOIN questions q ON q.id=v.question_id JOIN question_banks b ON b.id=q.bank_id
        WHERE b.subject_id=? AND b.owner_id=? AND q.author_id=? AND q.status='PUBLISHED'
        AND v.version_no=(SELECT MAX(v2.version_no) FROM question_versions v2 WHERE v2.question_id=q.id)
        AND NOT EXISTS(SELECT 1 FROM question_attachments a WHERE a.question_version_id=v.id)
        AND TRIM(v.content)<>''
        AND (SELECT COUNT(DISTINCT NULLIF(TRIM(o.option_content),'')) FROM question_version_options o WHERE o.question_version_id=v.id)>=2
        AND (SELECT COUNT(*)=COUNT(DISTINCT TRIM(o.option_content)) FROM question_version_options o WHERE o.question_version_id=v.id)
        AND NOT EXISTS(SELECT 1 FROM question_version_options o WHERE o.question_version_id=v.id AND TRIM(o.option_content)='')
        AND (SELECT COUNT(*) FROM question_version_options o WHERE o.question_version_id=v.id AND o.is_correct=1)
            BETWEEN 1 AND IF(q.question_type='SINGLE_CHOICE',1,2147483647)
        """;
    public List<Map<String,Object>> choices(Connection c,long subject,long owner,String difficulty,String bank,String topic) throws SQLException {
        return rows(c,"SELECT v.id,v.content,b.bank_name,q.difficulty,v.version_no "+SOURCE+
                " AND (?='' OR q.difficulty=?) AND LOCATE(?,b.bank_name)>0 AND (?='' OR EXISTS(SELECT 1 FROM topics t WHERE t.id=q.topic_id AND LOCATE(?,t.topic_name)>0)) ORDER BY b.bank_name,v.id",
                subject,owner,owner,difficulty,difficulty,bank,topic,topic);
    }
    public void validate(Connection c,long subject,long owner,long version) throws SQLException {
        one(c,"SELECT v.id "+SOURCE+" AND v.id=? FOR UPDATE",subject,owner,owner,version);
    }
    public List<Map<String,Object>> items(Connection c,long exam) throws SQLException {
        return rows(c,"SELECT x.id,x.question_version_id,x.fixed_order,x.points,v.content,v.version_no FROM exam_questions x JOIN question_versions v ON v.id=x.question_version_id WHERE x.exam_id=? ORDER BY x.fixed_order",exam);
    }
    public void save(Connection c,long exam,long version,int position,BigDecimal points,boolean existing) throws SQLException {
        if(existing) {
            one(c,"SELECT id FROM exam_questions WHERE exam_id=? AND question_version_id=? FOR UPDATE",exam,version);
            update(c,"UPDATE exam_questions SET fixed_order=?,points=? WHERE exam_id=? AND question_version_id=?",position,points,exam,version);
        } else insert(c,"INSERT INTO exam_questions(exam_id,question_version_id,fixed_order,points) VALUES(?,?,?,?)",exam,version,position,points);
    }
    public void remove(Connection c,long exam,long version) throws SQLException {
        one(c,"SELECT id FROM exam_questions WHERE exam_id=? AND question_version_id=? FOR UPDATE",exam,version);
        update(c,"DELETE FROM exam_questions WHERE exam_id=? AND question_version_id=?",exam,version);
    }
}
