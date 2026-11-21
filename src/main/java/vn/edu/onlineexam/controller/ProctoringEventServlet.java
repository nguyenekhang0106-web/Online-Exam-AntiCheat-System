package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import vn.edu.onlineexam.dao.ProctoringEventDao.Filter;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/student/proctoring/events","/teacher/proctoring/events","/admin/proctoring/events"})
public final class ProctoringEventServlet extends HttpServlet {
    private final ProctoringService service=new ProctoringService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static Access access(HttpServletRequest r){
        return new Access((AuthUser)r.getAttribute("authenticatedUser"),(String)r.getAttribute("roleLabel"),(String)r.getSession(false).getAttribute("sessionRef"));
    }
    private static Long id(HttpServletRequest r,String key){
        String value=r.getParameter(key);if(value==null || value.isBlank())return null;
        long n=Long.parseLong(value);if(n<1)throw new IllegalArgumentException();return n;
    }
    private static LocalDateTime time(String value){
        if(value==null || value.isBlank())return null;
        if(!value.endsWith("Z"))throw new IllegalArgumentException();
        return LocalDateTime.ofInstant(Instant.parse(value),ZoneOffset.UTC);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{
        s.setHeader("Cache-Control","no-store");
        if(!r.getServletPath().startsWith("/student/")){s.sendError(403);return;}
        try{
            var allowed=Set.of("csrf","attempt","key","code","occurred");
            for(var field:r.getParameterMap().entrySet())if(!allowed.contains(field.getKey()) || field.getValue().length!=1)throw new IllegalArgumentException();
            Long attempt=id(r,"attempt");if(attempt==null)throw new IllegalArgumentException();
            boolean created=service.receive(access(r),attempt,r.getParameter("key"),r.getParameter("code"),time(r.getParameter("occurred")));
            s.setStatus(created?201:204);
        }catch(ProctoringService.Limited e){s.setHeader("Retry-After","60");s.sendError(429);}
        catch(IllegalArgumentException|DateTimeParseException e){s.sendError(400);}
        catch(NoSuchElementException e){s.sendError(404);}catch(SecurityException e){s.sendError(403);}
        catch(IllegalStateException e){s.sendError(409);}catch(SQLException e){s.sendError(503);}
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        s.setHeader("Cache-Control","no-store");
        if(r.getServletPath().startsWith("/student/")){s.sendError(403);return;}
        try{
            String sort=Objects.requireNonNullElse(r.getParameter("sort"),"newest");
            if(!Set.of("oldest","newest").contains(sort))throw new IllegalArgumentException();
            var f=new Filter(id(r,"id"),id(r,"attempt"),id(r,"student"),r.getParameter("code"),time(r.getParameter("from")),time(r.getParameter("until")),
                    Integer.parseInt(Objects.requireNonNullElse(r.getParameter("page"),"1")),Integer.parseInt(Objects.requireNonNullElse(r.getParameter("size"),"20")),sort.equals("oldest"));
            r.setAttribute("result",service.history(access(r),f));
            r.getRequestDispatcher("/WEB-INF/views/proctoring/events.jsp").forward(r,s);
        }catch(IllegalArgumentException|DateTimeParseException e){s.sendError(400);}catch(SecurityException e){s.sendError(403);}catch(SQLException e){s.sendError(503);}
    }
}
