package vn.edu.onlineexam.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import vn.edu.onlineexam.service.AuthenticationService;

@WebFilter("/*")
public class AuthenticationFilter implements Filter {
    private final AuthenticationService auth;
    public AuthenticationFilter() { this(AuthenticationService.configured()); }
    protected AuthenticationFilter(AuthenticationService auth) { this.auth=auth; }
    public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException,ServletException {
        var req=(HttpServletRequest)request;
        var res=(HttpServletResponse)response;
        String path=req.getServletPath();
        var session=req.getSession(false);
        if (!java.util.Set.of("GET","HEAD","OPTIONS").contains(req.getMethod())) {
            Object expected=session==null ? null : session.getAttribute("csrf");
            if (expected==null || !expected.equals(req.getParameter("csrf"))) { res.sendError(403); return; }
        }
        if (path.equals("/login") || path.equals("/logout") || path.endsWith("/account")) res.setHeader("Cache-Control","no-store");
        if (path.equals("/login") && req.getMethod().equals("GET")) {
            session=req.getSession();
            if (session.getAttribute("csrf")==null) session.setAttribute("csrf",UUID.randomUUID().toString());
        }
        String required=null;
        for (String role : java.util.List.of("ADMIN","TEACHER","STUDENT")) {
            if (path.startsWith("/"+role.toLowerCase(java.util.Locale.ROOT)+"/")) required=role;
        }
        if (required!=null) {
            String hash=session==null ? null : (String)session.getAttribute("sessionRef");
            if (hash==null) { res.sendRedirect(req.getContextPath()+"/login"); return; }
            try {
                var user=auth.current(hash);
                if (user==null) { session.invalidate(); res.sendRedirect(req.getContextPath()+"/login"); return; }
                if (!user.roles().contains(required)) { res.sendError(403); return; }
                req.setAttribute("roleLabel",required);
            } catch (SQLException | IllegalStateException | IllegalArgumentException e) { res.sendError(503); return; }
        }
        chain.doFilter(req,res);
    }
}
