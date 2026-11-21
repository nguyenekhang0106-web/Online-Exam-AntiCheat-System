package vn.edu.onlineexam.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class QuestionCatalog {
    private QuestionCatalog() {}
    public enum Module { topics, banks, questions }
    public record Filter(String term,long subject,long bank,long topic,String state,String type,String difficulty,
                         String sort,boolean descending,int page,int size) {}
    public record Page(List<Map<String,Object>> rows,boolean more,Filter filter) {}
    public record Answer(String text,boolean correct) {}
    public record Draft(long bank,long topic,String type,String difficulty,String content,String explanation,
                        BigDecimal points,List<Answer> options) {}
    public record Detail(Map<String,Object> row,List<Answer> options) {}
}
