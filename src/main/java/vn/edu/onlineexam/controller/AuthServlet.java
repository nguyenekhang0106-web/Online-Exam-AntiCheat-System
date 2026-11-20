package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.*;
import java.util.*;
import java.sql.SQLException;
import vn.edu.onlineexam.service.AuthenticationService;

@WebServlet({"/login","/logout","/admin/account","/teacher/account","/student/account"})
public class AuthServlet extends HttpServlet {
    private final AuthenticationService auth;
    public AuthServlet() { this(AuthenticationService.configured()); }
    protected AuthServlet(AuthenticationService auth) { this.auth=auth; }
    private final AuthenticationService.LoginLimit limit=new AuthenticationService.LoginLimit();
    private static final SecureRandom RANDOM=new SecureRandom();

    protected void doGet(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        String path=req.getServletPath();
        if (path.equals("/logout")) { res.sendError(405); return; }
        if (path.equals("/login") && req.getSession().getAttribute("sessionRef")!=null) {
            res.sendRedirect(req.getContextPath()+req.getSession().getAttribute("landing")); return;
        }
        req.getRequestDispatcher(path.equals("/login") ? "/WEB-INF/views/login.jsp" : "/WEB-INF/views/account.jsp").forward(req,res);
    }

    protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        var session=req.getSession(false);
        if (session==null) { res.sendError(403); return; }
        String path=req.getServletPath();
        try {
            if (path.equals("/logout")) {
                String hash=(String)session.getAttribute("sessionRef");
                try { if (hash!=null) auth.logout(hash); } finally { session.invalidate(); }
                res.sendRedirect(req.getContextPath()+"/login"); return;
            }
            if (!path.equals("/login")) { res.sendError(405); return; }
            if (session.getAttribute("sessionRef")!=null) { res.sendError(409); return; }
            if (!limit.allow(req.getRemoteAddr())) { res.sendError(429); return; }
            String input=req.getParameter("password");
            char[] password=input==null ? new char[0] : input.toCharArray();
            byte[] nonce=new byte[32]; RANDOM.nextBytes(nonce);
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(nonce));
            try {
                var user=auth.login(req.getParameter("email"),password,hash);
                if (user==null) {
                    res.setStatus(401); req.setAttribute("invalid",true);
                    req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req,res); return;
                }
                req.changeSessionId();
                session.setAttribute("csrf",UUID.randomUUID().toString());
                session.setAttribute("sessionRef",hash);
                session.setAttribute("landing",user.landing());
                res.sendRedirect(req.getContextPath()+user.landing());
            } finally { Arrays.fill(password,'\0'); }
        } catch (SQLException | GeneralSecurityException | IllegalStateException | IllegalArgumentException e) { res.sendError(503); }
    }
}
