package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/student/attempts","/teacher/attempts","/admin/attempts"})
public final class AttemptServlet extends HttpServlet {
    private final AttemptService service=new AttemptService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static long number(HttpServletRequest r,String name){return Long.parseLong(r.getParameter(name));}
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,false);}
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,true);}
    private void handle(HttpServletRequest r,HttpServletResponse s,boolean post)throws ServletException,IOException{
        r.setCharacterEncoding("UTF-8");s.setHeader("Cache-Control","no-store");
        var a=new Access((AuthUser)r.getAttribute("authenticatedUser"),(String)r.getAttribute("roleLabel"),(String)r.getSession(false).getAttribute("sessionRef"));
        String action=Objects.requireNonNullElse(r.getParameter("action"),"history");
        try{
            if(!post){
                switch(action){
                    case "history" -> r.setAttribute("history",service.history(a));
                    case "start" -> {long exam=number(r,"exam");r.setAttribute("admission",service.preview(a,exam));r.setAttribute("exam",exam);}
                    case "take","confirm" -> r.setAttribute("paper",service.paper(a,number(r,"id")));
                    default -> {s.sendError(404);return;}
                }
                r.getRequestDispatcher("/WEB-INF/views/attempt/"+action+".jsp").forward(r,s);return;
            }
            String location=r.getContextPath()+r.getServletPath();long id;
            switch(action){
                case "start" -> {id=service.start(a,number(r,"exam"),number(r,"previous"));location+="?action=take&id="+id;}
                case "save" -> {
                    id=number(r,"id");long question=number(r,"question");var selected=new HashSet<Long>();
                    for(String value:Objects.requireNonNullElse(r.getParameterValues("option"),new String[0]))if(!selected.add(Long.valueOf(value)))throw new IllegalArgumentException();
                    long version=service.save(a,id,question,number(r,"revision"),selected);
                    if("application/json".equals(r.getHeader("Accept"))){
                        s.setContentType("application/json");s.getWriter().print("{\"revision\":\""+version+"\"}");return;
                    }
                    location+="?action=take&id="+id+"#q"+question;
                }
                case "timeout","submit" -> service.submit(a,number(r,"id"),action.equals("timeout"));
                default -> {s.sendError(405);return;}
            }
            s.setStatus(303);s.setHeader("Location",location);
        }catch(NoSuchElementException e){s.sendError(404);}catch(SecurityException e){s.sendError(403);}
        catch(IllegalArgumentException e){s.sendError(400,"Dữ liệu không hợp lệ.");}
        catch(IllegalStateException e){s.sendError(409,"Thao tác chưa thể thực hiện. Hãy kiểm tra lịch và tải lại lượt thi.");}
        catch(SQLException e){s.sendError(503);}
    }
}
