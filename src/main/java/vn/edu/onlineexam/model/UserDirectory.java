package vn.edu.onlineexam.model;

import java.time.LocalDateTime;
import java.util.List;

public record UserDirectory(List<Row> rows, long total, long pages, long page, Query query) {
    public record Row(long id,String code, String name, String email, String roles, String status, LocalDateTime created, LocalDateTime version) {}
    public record Profile(String name,String email,String code,String phone) {}
    public record Detail(Profile profile,LocalDateTime version,boolean verified) {}
    public enum Sort {
        created("u.created_at"), name("u.full_name"), email("u.email"), code("u.user_code");
        public final String column;
        Sort(String column) { this.column = column; }
    }
    public enum Direction { ASC, DESC }
    public record Query(String keyword, String role, String status, int size, Sort sort, Direction direction) {}
}
