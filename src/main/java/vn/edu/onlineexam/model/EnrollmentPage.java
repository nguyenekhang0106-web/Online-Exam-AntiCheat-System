package vn.edu.onlineexam.model;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.onlineexam.model.OfferingPage.Option;

public record EnrollmentPage(List<Row> rows,long total,long page,long pages,Query query) {
    public record Row(long offering,long student,String subject,String schoolClass,String year,String semester,
                      String code,String name,String status,LocalDateTime enrolledAt) {}
    public record Choices(List<Option> offerings,List<Option> students) {}
    public record Query(String keyword,long offering,String status,Sort sort,boolean descending,int size) {}
    public enum Sort {
        name("u.full_name"), code("u.user_code"), offering("o.id"), enrolled("e.enrolled_at");
        public final String column;
        Sort(String column){this.column=column;}
    }
}
