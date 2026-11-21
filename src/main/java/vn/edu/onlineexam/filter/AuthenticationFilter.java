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
        boolean heartbeat="/student/proctoring/heartbeat".equals(req.getServletPath());
        if(heartbeat && !"POST".equals(req.getMethod())){
            res.setHeader("Allow","POST");res.setStatus(405);return;
        }
        if (!"POST".equals(req.getMethod()) || (!heartbeat && !"/student/proctoring/events".equals(req.getServletPath()))) {
            authenticated(request,response,chain);return;
        }
        res.setHeader("Cache-Control","no-store");
        if(req.getContentLengthLong()<0 || req.getContentLengthLong()>1024){res.sendError(413);return;}
        if(req.getQueryString()!=null || req.getContentType()==null ||
                !req.getContentType().split(";",2)[0].equalsIgnoreCase("application/x-www-form-urlencoded")){res.sendError(400);return;}
        if(!eventSlots.tryAcquire()){res.sendError(429);return;}
        try{
            var session=req.getSession(false);
            if(session==null){res.sendError(heartbeat?401:403);return;}
            synchronized(session){
                long now=System.nanoTime();
                String window=heartbeat?"heartbeatWindow":"eventWindow";
                Long start=(Long)session.getAttribute(window+"Start");
                int count=start==null || now-start>=60_000_000_000L?0:(Integer)session.getAttribute(window+"Count");
                if(count>=120){res.setHeader("Retry-After","60");res.sendError(429);return;}
                if(count==0)session.setAttribute(window+"Start",now);
                session.setAttribute(window+"Count",count+1);
            }
            authenticated(request,response,chain);
        }finally{eventSlots.release();}
    }
    private final java.util.concurrent.Semaphore eventSlots=new java.util.concurrent.Semaphore(4);
    private void authenticated(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException,ServletException {
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
        // Heartbeats validate login and role inside their transaction without touching login idle time.
        if(path.equals("/student/proctoring/heartbeat") && req.getMethod().equals("POST")){
            chain.doFilter(req,res);return;
        }
        String required=null;
        for (String role : java.util.List.of("ADMIN","TEACHER","STUDENT")) {
            if (path.startsWith("/"+role.toLowerCase(java.util.Locale.ROOT)+"/")) required=role;
        }
        if (required!=null) {
            String hash=session==null ? null : (String)session.getAttribute("sessionRef");
            if (hash==null) { res.sendRedirect(req.getContextPath()+"/login"); return; }
            try {
                var user=auth.current(hash,!path.equals("/student/proctoring/events"));
                if (user==null) { session.invalidate(); res.sendRedirect(req.getContextPath()+"/login"); return; }
                if (!user.roles().contains(required)) { res.sendError(403); return; }
                req.setAttribute("roleLabel",required);
                req.setAttribute("authenticatedUser",user);
            } catch (SQLException | IllegalStateException | IllegalArgumentException e) { res.sendError(503); return; }
        }
        chain.doFilter(req,res);
    }
}
