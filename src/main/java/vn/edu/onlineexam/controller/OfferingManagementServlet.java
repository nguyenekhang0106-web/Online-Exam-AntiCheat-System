package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.SubjectPage.Scope;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/offerings","/teacher/offerings","/student/offerings","/admin/offerings/detail",
        "/teacher/offerings/detail","/student/offerings/detail","/admin/offerings/new","/admin/offerings/edit","/admin/offerings/close"})
public final class OfferingManagementServlet extends HttpServlet {
    private final OfferingManagementService service=new OfferingManagementService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static String value(HttpServletRequest req,String name,String fallback){return Objects.requireNonNullElse(req.getParameter(name),fallback);}
    private static long number(HttpServletRequest req,String name){return Long.parseLong(req.getParameter(name));}
    private void view(HttpServletRequest req,HttpServletResponse res,String name) throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/academic/offering-"+name+".jsp").forward(req,res);
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        var role=Scope.valueOf(path.split("/")[1].toUpperCase(Locale.ROOT));
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        try {
            if(path.endsWith("/close")){res.setHeader("Allow","POST");res.sendError(405);return;}
            if(path.endsWith("/offerings")){
                req.setAttribute("result",service.search(actor,role,value(req,"q",""),Long.parseLong(value(req,"subject","0")),value(req,"status",""),
                        value(req,"sort","id"),value(req,"direction","ASC"),Integer.parseInt(value(req,"page","1")),Integer.parseInt(value(req,"size","20"))));
                view(req,res,"list");return;
            }
            boolean create=path.endsWith("/new");
            if(!create) req.setAttribute("row",service.detail(actor,role,number(req,"id")));
            if(path.endsWith("/detail")){
                var session=req.getSession(false);
                req.setAttribute("notice",session.getAttribute("offeringNotice"));session.removeAttribute("offeringNotice");
                view(req,res,"detail");return;
            }
            req.setAttribute("choices",service.choices(actor,create));
            view(req,res,"form");
        } catch(NoSuchElementException e){res.sendError(404);}
        catch(SecurityException e){res.sendError(403);}
        catch(IllegalArgumentException e){res.sendError(400,"Tham số không hợp lệ.");}
        catch(SQLException | IllegalStateException e){res.sendError(503);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        boolean create=path.equals("/admin/offerings/new"),close=path.equals("/admin/offerings/close");
        if(!create && !close && !path.equals("/admin/offerings/edit")){res.setHeader("Allow","GET");res.sendError(405);return;}
        try{
            var actor=(AuthUser)req.getAttribute("authenticatedUser");
            UserManagementService.requireAdmin(actor);
            var session=req.getSession(false);
            String ref=(String)session.getAttribute("sessionRef");
            long id;
            if(create) id=service.create(actor,ref,number(req,"subject"),number(req,"schoolClass"),number(req,"teacher"),req.getParameter("year"),req.getParameter("semester"));
            else {
                id=number(req,"id");
                service.change(actor,ref,id,number(req,"version"),close?0:number(req,"teacher"),close);
            }
            session.setAttribute("offeringNotice",create?"Đã tạo lớp học phần.":close?"Đã đóng lớp học phần.":"Đã cập nhật phân công.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/offerings/detail?id="+id);
        }catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(NumberFormatException e){res.sendError(400,"Thiếu hoặc sai định dạng ID.");}
        catch(IllegalArgumentException e){res.sendError(400,e.getMessage());}
        catch(OfferingManagementService.Conflict e){res.sendError(409,e.getMessage());}
        catch(SQLException | IllegalStateException e){res.sendError(503);}
    }
}
