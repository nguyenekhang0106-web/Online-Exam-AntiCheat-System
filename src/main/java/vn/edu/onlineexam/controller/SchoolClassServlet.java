package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.ClassDirectory.Query;
import vn.edu.onlineexam.model.SubjectPage.Scope;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/classes","/teacher/classes","/student/classes","/admin/classes/detail","/teacher/classes/detail",
        "/student/classes/detail","/admin/classes/new","/admin/classes/edit","/admin/classes/archive","/admin/classes/membership"})
public final class SchoolClassServlet extends HttpServlet {
    private final ClassManagementService service=new ClassManagementService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private Query query(HttpServletRequest req){
        String dir=Objects.requireNonNullElse(req.getParameter("direction"),"ASC");
        if(!Set.of("ASC","DESC").contains(dir))throw new IllegalArgumentException();
        return new Query(Objects.requireNonNullElse(req.getParameter("q"),"").strip(),Objects.requireNonNullElse(req.getParameter("status"),""),
                Integer.parseInt(Objects.requireNonNullElse(req.getParameter("page"),"1")),Integer.parseInt(Objects.requireNonNullElse(req.getParameter("size"),"20")),dir.equals("DESC"));
    }
    private void view(HttpServletRequest req,HttpServletResponse res,String name) throws IOException,ServletException {
        req.getRequestDispatcher("/WEB-INF/views/academic/class-"+name+".jsp").forward(req,res);
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        Scope role=Scope.valueOf(path.split("/")[1].toUpperCase(Locale.ROOT));
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        try{
            if(path.endsWith("/classes")){req.setAttribute("result",service.list(actor,role,query(req)));view(req,res,"list");return;}
            if(path.endsWith("/new")){UserManagementService.requireAdmin(actor);view(req,res,"form");return;}
            if(!path.endsWith("/detail") && !path.endsWith("/edit")){res.setHeader("Allow","POST");res.sendError(405);return;}
            long id=Long.parseLong(req.getParameter("id"));
            req.setAttribute("schoolClass",service.detail(actor,role,id));
            if(path.endsWith("/edit")){view(req,res,"form");return;}
            req.setAttribute("result",service.roster(actor,role,id,query(req)));
            var session=req.getSession(false);
            req.setAttribute("notice",session.getAttribute("classNotice"));session.removeAttribute("classNotice");
            view(req,res,"detail");
        }catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(IllegalArgumentException e){res.sendError(400,"Tham số không hợp lệ.");}
        catch(SQLException|IllegalStateException e){res.sendError(503);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        if(!Set.of("/admin/classes/new","/admin/classes/edit","/admin/classes/archive","/admin/classes/membership").contains(path)){
            res.setHeader("Allow","GET");res.sendError(405);return;
        }
        try{
            var actor=(AuthUser)req.getAttribute("authenticatedUser");
            UserManagementService.requireAdmin(actor);
            var session=req.getSession(false);String ref=(String)session.getAttribute("sessionRef");
            boolean create=path.endsWith("/new");
            Long id=create?null:Long.valueOf(req.getParameter("id"));
            LocalDateTime version=create?null:LocalDateTime.parse(Objects.requireNonNullElse(req.getParameter("version"),""));
            if(create || path.endsWith("/edit"))id=service.save(actor,ref,id,version,req.getParameter("code"),req.getParameter("name"),req.getParameter("year"));
            else if(path.endsWith("/archive"))service.archive(actor,ref,id,version);
            else service.membership(actor,ref,id,version,Long.parseLong(req.getParameter("student")),Objects.requireNonNullElse(req.getParameter("action"),""));
            session.setAttribute("classNotice","Đã lưu thay đổi.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/classes/detail?id="+id);
        }catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(NumberFormatException|java.time.format.DateTimeParseException e){res.sendError(400,"ID hoặc phiên bản không hợp lệ.");}
        catch(IllegalArgumentException e){res.sendError(400,e.getMessage());}
        catch(ClassManagementService.Conflict e){res.sendError(409,e.getMessage());}
        catch(SQLException|IllegalStateException e){res.sendError(503);}
    }
}
