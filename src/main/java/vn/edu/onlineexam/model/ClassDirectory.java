package vn.edu.onlineexam.model;

import java.time.LocalDateTime;
import java.util.List;

public record ClassDirectory<T>(List<T> rows, boolean hasNext, Query query) {
    public record Query(String term, String status, int page, int size, boolean descending) {
        public long offset() { return (page-1L)*size; }
        public String pattern() { return "%"+term.replace("!","!!").replace("%","!%").replace("_","!_")+"%"; }
    }
    public record Member(long studentId,String code,String name,String status,LocalDateTime joined,LocalDateTime left) {}
    public static <T> ClassDirectory<T> of(List<T> rows,Query q) {
        boolean more=rows.size()>q.size();
        return new ClassDirectory<>(List.copyOf(more?rows.subList(0,q.size()):rows),more,q);
    }
}
