package vn.edu.onlineexam.model;

import java.util.List;
import vn.edu.onlineexam.model.bean.academic.Subject;

public record SubjectPage(List<Subject> rows, long total, long page, long pages, Query query) {
    public record Query(String keyword, String status, Sort sort, boolean descending, int size) {}
    public enum Sort {
        code("s.subject_code"), name("s.subject_name"), updated("s.updated_at");
        public final String column;
        Sort(String column) { this.column = column; }
    }
    public enum Scope { ADMIN, TEACHER, STUDENT }
    public record Input(String code, String name, String description) {}
}
