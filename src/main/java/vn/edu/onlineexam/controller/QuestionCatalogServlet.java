package vn.edu.onlineexam.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.QuestionCatalog.*;
import vn.edu.onlineexam.model.QuestionCatalog.Module;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import vn.edu.onlineexam.util.ConnectionFactory;

@WebServlet({"/admin/topics/*","/teacher/topics/*","/admin/banks/*","/teacher/banks/*","/admin/questions/*","/teacher/questions/*"})
public final class QuestionCatalogServlet extends HttpServlet {
    private final QuestionBankService catalog=new QuestionBankService(new JdbcUnitOfWork(()->ConnectionFactory.fromEnvironment().open()));
    private static String p(HttpServletRequest r,String key,String def){return Objects.requireNonNullElse(r.getParameter(key),def);}
    private static long num(HttpServletRequest r,String key,long def){String s=r.getParameter(key);return s==null || s.isBlank()?def:Long.parseLong(s);}
    private static long id(HttpServletRequest r){long id=num(r,"id",0);if(id<=0)throw new IllegalArgumentException();return id;}
    private static Draft draft(HttpServletRequest r){
        var lines=r.getParameterValues("option");if(lines==null)throw new IllegalArgumentException("Thiếu phương án.");
        var correct=new HashSet<Integer>();
        for(String index:Objects.requireNonNullElse(r.getParameterValues("correct"),new String[0])){
            int n=Integer.parseInt(index);if(n<0 || n>=lines.length || !correct.add(n))throw new IllegalArgumentException("Đáp án không hợp lệ.");
        }
        var options=new ArrayList<Answer>();for(int i=0;i<lines.length;i++)options.add(new Answer(lines[i].strip(),correct.contains(i)));
        return new Draft(num(r,"bank",0),num(r,"topic",0),p(r,"type",""),p(r,"difficulty",""),p(r,"content",""),p(r,"explanation",""),
                new BigDecimal(p(r,"points","")),List.copyOf(options));
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,false);}
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{handle(r,s,true);}
    private void handle(HttpServletRequest r,HttpServletResponse s,boolean post)throws ServletException,IOException{
        s.setHeader("Cache-Control","no-store");
        String[] route=r.getServletPath().split("/");Module m=Module.valueOf(route[2]);
        String op=Objects.requireNonNullElse(r.getPathInfo(),"/");
        var session=r.getSession(false);
        var a=new Access((AuthUser)r.getAttribute("authenticatedUser"),route[1].toUpperCase(Locale.ROOT),session==null?null:(String)session.getAttribute("sessionRef"));
        String base=r.getContextPath()+r.getServletPath();
        boolean manage=m==Module.topics || "TEACHER".equals(a.role());
        r.setAttribute("base",base);r.setAttribute("module",m.name());r.setAttribute("manage",manage);
        try{
            if(!Set.of("/","/detail","/new","/edit","/publish","/archive").contains(op)){s.sendError(404);return;}
            if((op.equals("/publish") || op.equals("/archive")) && m!=Module.questions){s.sendError(404);return;}
            if(!post){
                String view;
                switch(op){
                    case "/" -> {
                        String direction=p(r,"direction","ASC");if(!Set.of("ASC","DESC").contains(direction))throw new IllegalArgumentException();
                        var filter=new vn.edu.onlineexam.model.QuestionCatalog.Filter(p(r,"q",""),num(r,"subject",0),num(r,"bank",0),num(r,"topic",0),p(r,"state",""),p(r,"type",""),p(r,"difficulty",""),
                                p(r,"sort","id"),direction.equals("DESC"),Math.toIntExact(num(r,"page",1)),Math.toIntExact(num(r,"size",20)));
                        r.setAttribute("result",catalog.list(a,m,filter));view="list";
                    }
                    case "/detail" -> {
                        r.setAttribute("detail",catalog.detail(a,m,id(r)));
                        r.setAttribute("notice",session.getAttribute("catalogNotice"));session.removeAttribute("catalogNotice");view="detail";
                    }
                    case "/new","/edit" -> {
                        if(!manage)throw new SecurityException();
                        if(op.equals("/edit"))r.setAttribute("detail",catalog.detail(a,m,id(r)));
                        r.setAttribute("choices",catalog.choices(a,m));view="form";
                    }
                    default -> {s.setHeader("Allow","POST");s.sendError(405);return;}
                }
                r.getRequestDispatcher("/WEB-INF/views/question/"+view+".jsp").forward(r,s);return;
            }
            if(op.equals("/") || op.equals("/detail")){s.setHeader("Allow","GET");s.sendError(405);return;}
            if(!manage)throw new SecurityException();
            long id=op.equals("/new")?0:id(r),stamp=num(r,"stamp",-1);
            if(op.equals("/publish") || op.equals("/archive"))catalog.transition(a,id,stamp,op.equals("/publish")?"PUBLISHED":"ARCHIVED");
            else id=switch(m){
                case topics -> {long parent=num(r,"parent",0);yield catalog.topic(a,id,stamp,num(r,"subject",0),parent==0?null:parent,p(r,"code",""),p(r,"name",""),Math.toIntExact(num(r,"order",0)));}
                case banks -> catalog.bank(a,id,stamp,num(r,"subject",0),p(r,"name",""),p(r,"description",""),p(r,"visibility","PRIVATE"));
                case questions -> catalog.question(a,id,stamp,draft(r));
            };
            session.setAttribute("catalogNotice","Đã lưu thay đổi.");s.setStatus(303);s.setHeader("Location",base+"/detail?id="+id);
        }catch(SecurityException e){s.sendError(403);}
        catch(NoSuchElementException e){s.sendError(404);}
        catch(QuestionBankService.Conflict e){s.sendError(409,e.getMessage());}
        catch(NumberFormatException|ArithmeticException e){s.sendError(400,"Giá trị số không hợp lệ.");}
        catch(IllegalArgumentException e){s.sendError(400,e.getMessage()==null?"Dữ liệu không hợp lệ.":e.getMessage());}
        catch(SQLException|IllegalStateException e){s.sendError(503);}
    }
}
