package vn.edu.onlineexam.model;

import java.util.Set;

/** Safe identity projection. Role membership is reloaded for protected requests. */
public record AuthUser(long id, Set<String> roles) {
    public AuthUser { roles = Set.copyOf(roles); }
    public String landing() {
        if (roles.contains("ADMIN")) return "/admin/account";
        if (roles.contains("TEACHER")) return "/teacher/account";
        if (roles.contains("STUDENT")) return "/student/account";
        throw new IllegalStateException("No supported role");
    }
}
