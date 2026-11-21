package vn.edu.onlineexam.model;

import java.util.List;

public record OfferingPage(List<Row> rows, long total, int page, long pages, Query query) {
    public record Row(long id, long subjectId, String subject, String schoolClass, String teacher,
                      long teacherId, String year, String semester, String status, long version) {}
    public record Option(long id, String label) {}
    public record Choices(List<Option> subjects, List<Option> classes, List<Option> teachers) {}
    public record Query(String keyword, long subjectId, String status, Sort sort, boolean descending, int size) {}
    public enum Sort {
        id("o.id"), subject("s.subject_name"), schoolClass("k.class_name"), year("o.academic_year"), teacher("u.full_name");
        public final String column;
        Sort(String column){this.column=column;}
    }
}
