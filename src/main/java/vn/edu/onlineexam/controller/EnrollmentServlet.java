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

@WebServlet({"/admin/enrollments","/teacher/enrollments","/student/enrollments","/admin/enrollments/detail",
        "/teacher/enrollments/detail","/student/enrollments/detail","/admin/enrollments/new","/admin/enrollments/withdraw","/admin/enrollments/complete"})
public final class EnrollmentServlet extends HttpServlet {
    private final EnrollmentManagementService service=new EnrollmentManagementService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private String val(HttpServletRequest req,String key,String fallback){return Objects.requireNonNullElse(req.getParameter(key),fallback);}
    private long key(HttpServletRequest req,String field){return Long.parseLong(req.getParameter(field));}
    private void view(HttpServletRequest req,HttpServletResponse res,String name) throws ServletException,IOException {
        req.getRequestDispatcher("/WEB-INF/views/academic/enrollment-"+name+".jsp").forward(req,res);
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getServletPath();
        var role=Scope.valueOf(path.split("/")[1].toUpperCase(Locale.ROOT));
        var actor=(AuthUser)req.getAttribute("authenticatedUser");
        try{
            if(path.endsWith("/enrollments")){
                req.setAttribute("result",service.search(actor,role,val(req,"q",""),Long.parseLong(val(req,"offering","0")),
                        val(req,"status",""),val(req,"sort","enrolled"),val(req,"direction","DESC"),
                        Integer.parseInt(val(req,"page","1")),Integer.parseInt(val(req,"size","20"))));
                view(req,res,"list");
            }else if(path.endsWith("/detail")){
                req.setAttribute("row",service.detail(actor,role,key(req,"offering"),key(req,"student")));
                var session=req.getSession(false);
                req.setAttribute("notice",session.getAttribute("enrollmentNotice"));session.removeAttribute("enrollmentNotice");
                view(req,res,"detail");
            }else if(path.endsWith("/new")){
                req.setAttribute("choices",service.choices(actor));view(req,res,"form");
            }else{res.setHeader("Allow","POST");res.sendError(405);}
        }catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404);}
        catch(IllegalArgumentException e){res.sendError(400,"Tham số không hợp lệ.");}
        catch(SQLException|IllegalStateException e){res.sendError(503);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control","no-store");
        String next=switch(req.getServletPath()){
            case "/admin/enrollments/new" -> "ENROLLED";
            case "/admin/enrollments/withdraw" -> "WITHDRAWN";
            case "/admin/enrollments/complete" -> "COMPLETED";
            default -> null;
        };
        if(next==null){res.setHeader("Allow","GET");res.sendError(405);return;}
        try{
            var actor=(AuthUser)req.getAttribute("authenticatedUser");
            UserManagementService.requireAdmin(actor);
            var session=req.getSession(false);
            long offering=key(req,"offering"),student=key(req,"student");
            service.change(actor,(String)session.getAttribute("sessionRef"),offering,student,next);
            session.setAttribute("enrollmentNotice","Đã lưu thay đổi ghi danh.");
            res.setStatus(303);res.setHeader("Location",req.getContextPath()+"/admin/enrollments/detail?offering="+offering+"&student="+student);
        }catch(SecurityException e){res.sendError(403);}
        catch(NoSuchElementException e){res.sendError(404,"Không tìm thấy lớp học phần, sinh viên hoặc ghi danh.");}
        catch(IllegalArgumentException e){res.sendError(400,"Thông tin ghi danh không hợp lệ.");}
        catch(EnrollmentManagementService.Conflict e){res.sendError(409,e.getMessage());}
        catch(SQLException|IllegalStateException e){res.sendError(503);}
    }
}
