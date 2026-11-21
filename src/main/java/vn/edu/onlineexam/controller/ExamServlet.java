package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import vn.edu.onlineexam.dao.ExamAssignmentDao.Scope;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/exams","/teacher/exams","/student/schedule"})
public final class ExamServlet extends HttpServlet {
    private final ExamService service=new ExamService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static String p(HttpServletRequest r,String key){return Objects.requireNonNullElse(r.getParameter(key),"");}
    private static long number(HttpServletRequest r,String key){return Long.parseLong(p(r,key));}
    private static ExamService.Config config(HttpServletRequest r){
        return new ExamService.Config(p(r,"title"),Integer.parseInt(p(r,"duration")),Integer.parseInt(p(r,"attempts")),new BigDecimal(p(r,"pass")),
            p(r,"instructions"),"on".equals(p(r,"shuffleQuestions")),"on".equals(p(r,"shuffleOptions")),
            Objects.requireNonNullElse(r.getParameter("scorePolicy"),"HIGHEST"),Objects.requireNonNullElse(r.getParameter("resultRelease"),"MANUAL"),
            Objects.requireNonNullElse(r.getParameter("answerRelease"),"NEVER"),p(r,"policy").isEmpty()?null:number(r,"policy"),
            Objects.requireNonNullElse(r.getParameter("mode"),"FIXED"),p(r,"count").isBlank()?null:Integer.valueOf(p(r,"count")));
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,false);}
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,true);}
    private void handle(HttpServletRequest r,HttpServletResponse s,boolean post)throws ServletException,IOException{
        r.setCharacterEncoding("UTF-8");s.setHeader("Cache-Control","no-store");
        var access=new Access((AuthUser)r.getAttribute("authenticatedUser"),(String)r.getAttribute("roleLabel"),(String)r.getSession(false).getAttribute("sessionRef"));
        String view="list",action=p(r,"action");
        try{
            if(post){
                if(!"TEACHER".equals(access.role()))throw new SecurityException();
                long id;
                if(action.equals("create"))id=service.create(access,number(r,"subject"),config(r));
                else {
                    id=number(r,"id");long stamp=number(r,"stamp");
                    switch(action){
                        case "edit" -> service.edit(access,id,stamp,config(r));
                        case "PUBLISHED","CLOSED","ARCHIVED" -> service.transition(access,id,stamp,action);
                        case "add","revise","remove" -> service.item(access,id,stamp,action,number(r,"version"),
                            action.equals("remove")?0:Integer.parseInt(p(r,"position")),action.equals("remove")?BigDecimal.ZERO:new BigDecimal(p(r,"points")));
                        case "assign","reschedule","cancel" -> service.assign(access,id,stamp,action,Scope.valueOf(p(r,"scope")),number(r,"target"),
                            action.equals("cancel")?null:LocalDateTime.parse(p(r,"opens")),action.equals("cancel")?null:LocalDateTime.parse(p(r,"closes")),
                            p(r,"limit").isBlank()?null:Integer.valueOf(p(r,"limit")));
                        default -> {s.sendError(404);return;}
                    }
                }
                s.setStatus(303);s.setHeader("Location",r.getContextPath()+"/teacher/exams?id="+id);return;
            }
            if("STUDENT".equals(access.role())){
                r.setAttribute("schedule",service.calendar(access));view="schedule";
            }else if(!p(r,"id").isEmpty()){
                var detail=service.detail(access,number(r,"id"),p(r,"difficulty"),p(r,"bank"),p(r,"topic"));
                r.setAttribute("detail",detail);r.setAttribute("schedule",detail.schedule());view="detail";
            }else{
                int page=p(r,"page").isBlank()?1:Integer.parseInt(p(r,"page"));
                r.setAttribute("pageNumber",page);
                r.setAttribute("rows",service.list(access,p(r,"q"),p(r,"status"),p(r,"sort").isEmpty()?"latest":p(r,"sort"),page));
                if("TEACHER".equals(access.role()))r.setAttribute("subjects",service.subjects(access));
            }
            if("TEACHER".equals(access.role()))r.setAttribute("policies",service.policies(access));
            r.getRequestDispatcher("/WEB-INF/views/exam/"+view+".jsp").forward(r,s);
        }catch(SecurityException e){s.sendError(403);}catch(NoSuchElementException e){s.sendError(404);}
        catch(ExamService.SelectionError e){s.sendError(400,e.getMessage());}
        catch(IllegalArgumentException|DateTimeParseException e){s.sendError(400,"Dữ liệu không hợp lệ.");}
        catch(IllegalStateException e){s.sendError(409,"Thao tác xung đột với trạng thái hiện tại. Hãy tải lại trang.");}
        catch(SQLException e){s.sendError(503);}
    }
}
