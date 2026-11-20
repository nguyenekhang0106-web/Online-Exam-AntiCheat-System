package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.SubjectPage.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/subjects","/teacher/subjects","/student/subjects",
    "/admin/subjects/detail","/teacher/subjects/detail","/student/subjects/detail",
    "/admin/subjects/new","/admin/subjects/edit","/admin/subjects/archive"})
public final class SubjectManagementServlet extends HttpServlet {
    private final SubjectManagementService subjects=new SubjectManagementService(
            new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static String param(HttpServletRequest req,String key,String fallback){
        return Objects.requireNonNullElse(req.getParameter(key),fallback);
    }
    private static void view(HttpServletRequest req,HttpServletResponse res,String name) throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/academic/"+name+".jsp").forward(req,res);
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        Scope scope=Scope.valueOf(path.split("/")[1].toUpperCase(Locale.ROOT));
        try {
            if(actor==null || !actor.roles().contains(scope.name())) throw new SecurityException();
            if(path.endsWith("/archive")){res.setHeader("Allow","POST"); res.sendError(405); return;}
            if(path.endsWith("/subjects")){
                var page=subjects.search(actor,scope,param(req,"q",""),param(req,"status",""),param(req,"sort","code"),
                        param(req,"direction","ASC"),Integer.parseInt(param(req,"page","1")),Integer.parseInt(param(req,"size","20")));
                req.setAttribute("directory",page); view(req,res,"subjects"); return;
            }
            if(!path.endsWith("/new")){
                var subject=subjects.detail(actor,scope,Long.parseLong(req.getParameter("id")));
                req.setAttribute("subject",subject);
                req.setAttribute("input",new Input(subject.getSubjectCode(),subject.getSubjectName(),subject.getDescription()));
                req.setAttribute("version",subject.getUpdatedAt());
            }
            if(path.endsWith("/detail")){
                var session=req.getSession(false);
                req.setAttribute("notice",session.getAttribute("subjectNotice")); session.removeAttribute("subjectNotice");
                view(req,res,"subject-detail");
            } else view(req,res,"subject-form");
        } catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(IllegalArgumentException e){res.sendError(400,"Tham số không hợp lệ.");}
        catch(SQLException | IllegalStateException e){res.sendError(503);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        boolean create=path.equals("/admin/subjects/new"),archive=path.equals("/admin/subjects/archive");
        if(!create && !archive && !path.equals("/admin/subjects/edit")){res.setHeader("Allow","GET");res.sendError(405);return;}
        var in=new Input(req.getParameter("code"),req.getParameter("name"),req.getParameter("description"));
        req.setAttribute("input",in);
        req.setAttribute("version",req.getParameter("version"));
        try {
            UserManagementService.requireAdmin(actor);
            String ref=(String)req.getSession(false).getAttribute("sessionRef");
            long id;
            if(create) id=subjects.create(actor,ref,in);
            else {
                id=Long.parseLong(req.getParameter("id"));
                LocalDateTime version;
                try {version=LocalDateTime.parse(param(req,"version",""));}
                catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Phiên bản không hợp lệ.");}
                subjects.change(actor,ref,id,version,in,archive);
            }
            req.getSession(false).setAttribute("subjectNotice",create?"Đã tạo môn học.":archive?"Đã lưu trữ môn học.":"Đã cập nhật môn học.");
            res.setStatus(303); res.setHeader("Location",req.getContextPath()+"/admin/subjects/detail?id="+id);
        } catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(IllegalArgumentException | SubjectManagementService.Conflict e){
            int status=e instanceof SubjectManagementService.Conflict?409:400;
            if(archive){res.sendError(status,e.getMessage());return;}
            res.setStatus(status); req.setAttribute("error",e instanceof NumberFormatException?"ID không hợp lệ.":e.getMessage());
            view(req,res,"subject-form");
        } catch(SQLException | IllegalStateException e){res.sendError(503);}
    }
}
