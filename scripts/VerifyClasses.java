import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.SubjectPage.Scope;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Uses only accounts from the guarded scratch fixture of VerifySubjects. */
class VerifyClasses {
    static void check(boolean ok,String label){AcceptAuth.require(ok,label);}
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long studentId,AuthUser actor,String ref,ConnectionFactory factory) throws Exception {
        String base="admin/classes",create=base+"/new",action=base+"/membership";
        check(new AcceptAuth().send(base,null).statusCode()==302,"guest");
        var form=admin.send(create,null);check(form.statusCode()==200,"class form");
        String csrf=AcceptAuth.csrf(form.body());
        String body="csrf="+csrf+"&code=CLS2G&name="+AcceptAuth.enc("Lớp <2G>")+"&year=2026-2027";
        check(admin.send(create,"code=wrong").statusCode()==403 && admin.send(create,"csrf=wrong").statusCode()==403,"class CSRF");
        check(admin.send(create,"csrf="+csrf+"&name=x").statusCode()==400,"required code");
        var response=admin.send(create,body);check(response.statusCode()==303,"class create");
        String detail=response.headers().firstValue("Location").orElseThrow();
        long id=Long.parseLong(detail.substring(detail.lastIndexOf('=')+1));
        check(admin.send(create,body).statusCode()==409,"unique code");
        String html=admin.send(detail,null).body();check(html.contains("&lt;2G&gt;"),"escaped name");
        String edit="csrf="+csrf+"&id="+id+"&version="+VerifySubjects.version(html)+"&code=CLS2G&name=Revised";
        try(var pool=Executors.newFixedThreadPool(2)){
            var gate=new CyclicBarrier(2);
            Callable<Integer> save=()->{gate.await();return admin.send(base+"/edit",edit).statusCode();};
            var a=pool.submit(save);var b=pool.submit(save);
            check(Set.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)).equals(Set.of(303,409)),"competing class edits");
        }
        check(admin.send(base+"?q=CLS2G&size=1",null).body().contains("Revised"),"class search");
        check(admin.send(base,null).body().contains("Mã hoặc tên lớp"),"filter fragment UTF-8");
        check(admin.send(base+"?q=%25",null).body().contains("Không tìm thấy lớp"),"literal wildcard");
        check(admin.send(base+"?page=0",null).statusCode()==400,"pagination");
        check(admin.send(base+"?size=1&page=2",null).body().contains("CLS2G"),"second class page");
        check(admin.send(base+"/detail?id=999999",null).statusCode()==404,"missing class");
        var t=teacher.send("teacher/classes/detail?id=1",null);check(t.statusCode()==200,"assigned teacher");
        check(teacher.send("teacher/classes/detail?id="+id,null).statusCode()==404,"other class");
        check(student.send("student/classes/detail?id="+id,null).statusCode()==404,"nonmember");
        check(teacher.send(create,null).statusCode()==403 && student.send(create,null).statusCode()==403,"admin form denied");
        String version=VerifySubjects.version(admin.send(detail,null).body());
        String join="id="+id+"&student="+studentId+"&action=join&version="+version;
        check(admin.send(action,join).statusCode()==403 && admin.send(action,join+"&csrf=x").statusCode()==403,"membership CSRF");
        String post="csrf="+csrf+"&"+join;
        try(var pool=Executors.newFixedThreadPool(2)){
            var gate=new CyclicBarrier(2);Callable<Integer> work=()->{gate.await();return admin.send(action,post).statusCode();};
            var a=pool.submit(work);var b=pool.submit(work);
            check(Set.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS)).equals(Set.of(303,409)),"competing joins");
        }
        var jdbc=new JdbcUnitOfWork(factory);var svc=new ClassManagementService(jdbc);
        var other=new UserManagementService(jdbc).create(actor,new UserDirectory.Profile("Hidden Peer","peer@class.invalid",null,null),
                VerifyRoles.password.toCharArray(),VerifyRoles.password.toCharArray(),"STUDENT");
        svc.membership(actor,ref,id,svc.detail(actor,Scope.ADMIN,id).getUpdatedAt(),other,"join");
        var mine=student.send("student/classes/detail?id="+id+"&student="+other,null);
        check(mine.statusCode()==200 && !mine.body().contains("Hidden Peer"),"private roster");
        String current="csrf="+csrf+"&id="+id+"&version="+svc.detail(actor,Scope.ADMIN,id).getUpdatedAt();
        check(admin.send(action,current+"&action=join&student="+studentId).statusCode()==409,"duplicate with fresh version");
        check(admin.send(action,current+"&action=join&student=9999999").statusCode()==404,"missing student");
        check(admin.send(action,current+"&action=other&student="+studentId).statusCode()==400,"invalid operation");
        check(teacher.send(action,post.replace(csrf,AcceptAuth.csrf(teacher.send("teacher/account",null).body()))).statusCode()==403,"teacher write denied");
        check(admin.send(detail+"&q=Hidden&size=1",null).body().contains("Hidden Peer"),"member filter");
        check(admin.send(detail+"&size=1&page=2",null).body().contains("STUDENT"),"member paging");
        var broken=new ClassManagementService(new JdbcUnitOfWork(()->{
            Connection real=factory.open();
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("prepareStatement") && a[0].toString().startsWith("INSERT INTO audit_logs"))throw new SQLException("Injected audit failure");
                try{return m.invoke(real,a);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
        }));
        var ver=svc.detail(actor,Scope.ADMIN,id).getUpdatedAt();
        try{broken.save(actor,ref,id,ver,"CLS2G","Broken",null);throw new AssertionError("class rollback");}
        catch(SQLException e){check(e.getMessage().equals("Injected audit failure"),"class audit");}
        try{broken.membership(actor,ref,id,ver,studentId,"leave");throw new AssertionError("member rollback");}
        catch(SQLException e){check(e.getMessage().equals("Injected audit failure"),"member audit");}
        check(svc.detail(actor,Scope.ADMIN,id).getClassName().equals("Revised"),"class rollback state");
        check(admin.send(action,"csrf="+csrf+"&id="+id+"&version="+ver+"&student="+studentId+"&action=leave").statusCode()==303,"leave after rollback");
        check(student.send("student/classes/detail?id="+id,null).body().contains("LEFT"),"left history");
        version=VerifySubjects.version(admin.send(detail,null).body());
        check(admin.send(action,"csrf="+csrf+"&id="+id+"&version="+version+"&student="+studentId+"&action=rejoin").statusCode()==303,"rejoin");
        try(var c=factory.open();var p=c.prepareStatement("SELECT status,left_at FROM class_students WHERE class_id=? AND student_id=?")){
            p.setLong(1,id);p.setLong(2,studentId);try(var r=p.executeQuery()){check(r.next() && "ACTIVE".equals(r.getString(1)) && r.getObject(2)==null && !r.next(),"single membership");}
        }
        long linked;
        try(var c=factory.open();var p=c.prepareStatement("SELECT subject_id,teacher_id FROM course_offerings WHERE id=1");var r=p.executeQuery()){
            check(r.next(),"offering fixture");
            linked=new OfferingManagementService(jdbc).create(actor,ref,r.getLong(1),id,r.getLong(2),"2026","1");
        }
        check(teacher.send("teacher/classes/detail?id="+id,null).body().contains("Hidden Peer"),"assigned full roster");
        version=VerifySubjects.version(admin.send(detail,null).body());
        check(admin.send(base+"/archive","csrf="+csrf+"&id="+id+"&version="+version).statusCode()==303,"archive");
        check(teacher.send("teacher/offerings/detail?id="+linked,null).statusCode()==200,"linked offering preserved");
        check(student.send("student/classes/detail?id="+id,null).body().contains("ARCHIVED"),"archive retains history");
        try(var c=factory.open();var p=c.prepareStatement("SELECT detail_json->>'$.previousLeftAt' FROM audit_logs WHERE target_type='CLASS' AND target_id=? AND action_code='CLASS_REJOIN'")){
            p.setLong(1,id);try(var r=p.executeQuery()){check(r.next() && r.getString(1)!=null && !r.getString(1).equals("null"),"prior departure retained");}
        }
        System.out.println("PASS classes and membership: MySQL, HTTP, scopes, paging, conflicts, history and atomic audit");
    }
}
