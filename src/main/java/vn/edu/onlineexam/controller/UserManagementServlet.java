package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/users","/admin/users/new","/admin/users/edit","/admin/users/lock","/admin/users/unlock","/admin/users/roles"})
public final class UserManagementServlet extends HttpServlet {
    private final UserManagementService users = new UserManagementService(
            new JdbcUnitOfWork(() -> ConnectionFactory.fromEnvironment().open()));
    protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        try {
            var actor=(AuthUser)req.getAttribute("authenticatedUser");
            UserManagementService.requireAdmin(actor);
            if (req.getServletPath().equals("/admin/users/roles")) {
                req.setAttribute("membership", users.membership(actor, id(req)));
                req.getRequestDispatcher("/WEB-INF/views/admin/user-roles.jsp").forward(req, res);
                return;
            }
            if(statusRoute(req)) { res.setHeader("Allow","POST"); res.sendError(405); return; }
            if (!req.getServletPath().equals("/admin/users")) {
                if (req.getServletPath().endsWith("/edit")) {
                    var d=users.detail(actor,id(req));
                    req.setAttribute("profile",d.profile()); req.setAttribute("version",d.version().toString());
                }
                showForm(req,res); return;
            }
            var session=req.getSession(false);
            if(session!=null && session.getAttribute("statusMessage")!=null) {
                req.setAttribute("statusMessage",session.getAttribute("statusMessage"));
                session.removeAttribute("statusMessage");
            }
            if(session!=null && session.getAttribute("userSaved")!=null) {
                req.setAttribute("userSaved",true); session.removeAttribute("userSaved");
            }
            var result = users.search((AuthUser)req.getAttribute("authenticatedUser"),req.getParameter("q"),
                    req.getParameter("role"),req.getParameter("status"),req.getParameter("page"),
                    req.getParameter("pageSize"),req.getParameter("sort"),req.getParameter("direction"));
            req.setAttribute("directory",result);
            req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req,res);
        } catch (java.util.NoSuchElementException missing) { res.sendError(404); }
        catch (SecurityException denied) { res.sendError(403); }
        catch (IllegalArgumentException invalid) { res.sendError(400,"Tham số tìm kiếm không hợp lệ."); }
        catch (SQLException | IllegalStateException unavailable) { res.sendError(503); }
    }
    private long id(HttpServletRequest req) {
        try { return Long.parseLong(req.getParameter("id")); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("ID không hợp lệ."); }
    }
    private void showForm(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/admin/user-form.jsp").forward(req,res);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        res.setHeader("Cache-Control","no-store");
        if (req.getServletPath().equals("/admin/users/roles")) {
            changeRole(req, res);
            return;
        }
        if(statusRoute(req)) { changeStatus(req,res); return; }
        boolean create=req.getServletPath().endsWith("/new");
        if(!create && !req.getServletPath().endsWith("/edit")) { res.sendError(405); return; }
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        var p=new vn.edu.onlineexam.model.UserDirectory.Profile(req.getParameter("name"),req.getParameter("email"),
                req.getParameter("code"),req.getParameter("phone"));
        req.setAttribute("profile",p);
        req.setAttribute("version",req.getParameter("version"));
        try {
            if(create) {
                char[] password=chars(req,"password"), confirm=chars(req,"confirm");
                try { users.create(actor,p,password,confirm,req.getParameter("role")); }
                finally { java.util.Arrays.fill(password,'\0'); java.util.Arrays.fill(confirm,'\0'); }
            } else { users.update(actor,id(req),p,req.getParameter("version")); }
            req.getSession().setAttribute("userSaved",true);
            res.setStatus(303); res.setHeader("Location",req.getContextPath()+"/admin/users");
        } catch (SecurityException e) { res.sendError(403); }
        catch (java.util.NoSuchElementException e) { res.sendError(404); }
        catch (IllegalArgumentException | UserManagementService.Conflict e) {
            res.setStatus(e instanceof UserManagementService.Conflict ? 409 : 400);
            req.setAttribute("error",e.getMessage()); showForm(req,res);
        } catch (SQLException | IllegalStateException e) { res.sendError(503); }
    }
    private void changeRole(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            var actor = (AuthUser) req.getAttribute("authenticatedUser");
            UserManagementService.requireAdmin(actor);
            String action = req.getParameter("action"), code = req.getParameter("role");
            boolean grant = "grant".equals(action);
            if (!grant && !"revoke".equals(action)) throw new IllegalArgumentException("Thao tác không hợp lệ.");
            if (!grant && "ADMIN".equals(code) && !"yes".equals(req.getParameter("confirm")))
                throw new IllegalArgumentException("Cần xác nhận thu hồi quyền ADMIN.");
            long target = id(req);
            var session = req.getSession(false);
            users.changeRole(actor, (String) session.getAttribute("sessionRef"), target, code, grant);
            session.setAttribute("statusMessage", grant ? "Đã cấp vai trò. Tài khoản cần đăng nhập lại."
                    : "Đã thu hồi vai trò và các phiên đăng nhập.");
            res.setStatus(303);
            res.setHeader("Location", req.getContextPath() + "/admin/users");
        } catch (SecurityException e) { res.sendError(403); }
        catch (java.util.NoSuchElementException e) { res.sendError(404, "Không tìm thấy tài khoản hoặc vai trò."); }
        catch (IllegalArgumentException e) { res.sendError(400, e.getMessage()); }
        catch (UserManagementService.Conflict e) { res.sendError(409, e.getMessage()); }
        catch (SQLException | IllegalStateException e) { res.sendError(503); }
    }

    private char[] chars(HttpServletRequest req,String key) {
        String value=req.getParameter(key); return value==null ? new char[0] : value.toCharArray();
    }
    private boolean statusRoute(HttpServletRequest req) {
        return req.getServletPath().endsWith("/lock") || req.getServletPath().endsWith("/unlock");
    }
    private void changeStatus(HttpServletRequest req,HttpServletResponse res) throws IOException {
        try {
            boolean lock=req.getServletPath().endsWith("/lock");
            var actor=(AuthUser)req.getAttribute("authenticatedUser");
            users.changeStatus(actor,id(req),lock,req.getParameter("version"));
            req.getSession().setAttribute("statusMessage",lock?"Đã khóa tài khoản và thu hồi các phiên.":"Đã mở khóa. Người dùng cần đăng nhập lại.");
            var query=new java.util.StringJoiner("&");
            for(String key:java.util.List.of("q","role","status","page","pageSize","sort","direction")) {
                String value=req.getParameter(key);
                if(value!=null) query.add(key+"="+java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8));
            }
            res.setStatus(303);
            res.setHeader("Location",req.getContextPath()+"/admin/users"+(query.length()==0?"":"?"+query));
        } catch(SecurityException denied) { res.sendError(403); }
        catch(java.util.NoSuchElementException missing) { res.sendError(404); }
        catch(IllegalArgumentException invalid) { res.sendError(400); }
        catch(UserManagementService.Conflict conflict) { res.sendError(409,conflict.getMessage()); }
        catch(SQLException | IllegalStateException unavailable) { res.sendError(503); }
    }
}
