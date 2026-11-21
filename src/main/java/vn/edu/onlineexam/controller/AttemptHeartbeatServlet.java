package vn.edu.onlineexam.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet("/student/proctoring/heartbeat")
public final class AttemptHeartbeatServlet extends HttpServlet {
    private final AttemptContactService service=new AttemptContactService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException {
        response.setHeader("Cache-Control","no-store");
        try{
            var params=request.getParameterMap();
            if(!params.keySet().equals(Set.of("attempt","csrf")) || params.values().stream().anyMatch(v->v.length!=1))throw new IllegalArgumentException();
            long id=Long.parseLong(request.getParameter("attempt"));
            if(id<1)throw new IllegalArgumentException();
            var session=request.getSession(false);
            String ref=session==null?null:(String)session.getAttribute("sessionRef");
            if(ref==null){response.setStatus(401);return;}
            service.heartbeat(id,ref);
            response.setStatus(204);
        }catch(AttemptContactService.InvalidSession expired){response.setStatus(401);}
        catch(SecurityException denied){response.setStatus(403);}
        catch(NoSuchElementException missing){response.setStatus(404);}
        catch(IllegalArgumentException invalid){response.setStatus(400);}
        catch(IllegalStateException closed){response.setStatus(409);}
        catch(SQLException unavailable){response.setStatus(503);}
    }
}
