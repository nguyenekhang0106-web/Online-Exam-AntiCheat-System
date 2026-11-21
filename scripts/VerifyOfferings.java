import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Extends the isolated V1–V4 fixture and HTTP sessions built by VerifySubjects. */
class VerifyOfferings {
    static void check(boolean ok,String label){AcceptAuth.require(ok,label);}
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long subject,long archived,long teacherId,
                    AuthUser actor,String ref,ConnectionFactory factory) throws Exception {
        String base="admin/offerings",create=base+"/new";
        check(new AcceptAuth().send(base,null).statusCode()==302,"guest");
        var form=admin.send(create,null);
        check(form.statusCode()==200,"creation form");
        String token=AcceptAuth.csrf(form.body());
        String fields="csrf="+token+"&schoolClass=1&teacher="+teacherId+"&semester="+AcceptAuth.enc("<1>")+"&year=2026-2027";
        check(admin.send(create,"").statusCode()==403 && admin.send(create,"csrf=invalid").statusCode()==403,"CSRF");
        check(admin.send(create,fields).statusCode()==400,"required field");
        for(long invalid:new long[]{archived,999999}) check(admin.send(create,fields+"&subject="+invalid).statusCode()==409,"invalid subject");
        String input=fields+"&subject="+subject;
        check(admin.send(create,input.replace("&teacher="+teacherId,"&teacher=999999")).statusCode()==409,"teacher");
        check(admin.send(create,input.replace("2026-2027","2026-202700")).statusCode()==400,"length");
        var reply=admin.send(create,input);check(reply.statusCode()==303,"create");
        String detail=reply.headers().firstValue("Location").orElseThrow();
        long id=Long.parseLong(detail.substring(detail.lastIndexOf('=')+1));
        check(admin.send(detail,null).body().contains("&lt;1&gt;"),"escaped semester");
        check(admin.send(create,input).statusCode()==303,"schema permits identical offerings");
        check(admin.send(base+"?subject="+subject+"&size=1&page=2&status=ACTIVE",null).body().contains("Trang 2 / 3"),"pagination");
        check(admin.send(base+"?q=%25",null).body().contains("0 lớp học phần"),"literal search");
        check(admin.send(base+"?sort=unknown",null).statusCode()==400,"sort whitelist");
        check(teacher.send("teacher/offerings/detail?id="+id,null).statusCode()==200,"assigned teacher");
        check(student.send("student/offerings/detail?id=1",null).statusCode()==200,"enrolled student");
        check(student.send("student/offerings/detail?id="+id+"&user_id=1",null).statusCode()==404,"unassigned student");
        check(teacher.send(base,null).statusCode()==403 && student.send(base,null).statusCode()==403,"admin routes");
        String edit=base+"/edit",version=VerifySubjects.version(admin.send(edit+"?id="+id,null).body());
        String update="csrf="+token+"&teacher="+teacherId+"&version="+version+"&id=";
        check(admin.send(edit,update+99999).statusCode()==404,"missing offering");
        var jdbc=new JdbcUnitOfWork(factory);
        var users=new UserManagementService(jdbc);
        long nextTeacher=users.create(actor,new UserDirectory.Profile("Other teacher","other@offering.invalid",null,null),
                VerifyRoles.password.toCharArray(),VerifyRoles.password.toCharArray(),"TEACHER");
        long otherAdmin=users.create(actor,new UserDirectory.Profile("Other admin","admin@offering.invalid",null,null),
                VerifyRoles.password.toCharArray(),VerifyRoles.password.toCharArray(),"ADMIN");
        String otherRef=VerifyRoles.ref();
        var other=new AuthenticationService(jdbc,new AuthDao()).login("admin@offering.invalid",VerifyRoles.password.toCharArray(),otherRef);
        check(other.id()==otherAdmin,"second admin");
        var service=new OfferingManagementService(jdbc);
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> writes=new ArrayList<>();
            for(boolean first:List.of(true,false))writes.add(pool.submit(()->{
                barrier.await();
                try{service.change(first?actor:other,first?ref:otherRef,id,Long.parseLong(version),nextTeacher,false);return true;}
                catch(OfferingManagementService.Conflict expected){return false;}
            }));
            check(writes.get(0).get(20,TimeUnit.SECONDS)!=writes.get(1).get(20,TimeUnit.SECONDS),"concurrent admins");
        }
        check(teacher.send("teacher/offerings/detail?id="+id+"&teacher_id="+nextTeacher,null).statusCode()==404,"reassigned teacher");
        check(admin.send(edit,update+id).statusCode()==409,"stale form");
        String current=VerifySubjects.version(admin.send(edit+"?id="+id,null).body());
        check(admin.send(edit,update.replace("version="+version,"version="+current)+id).statusCode()==303,"HTTP update");
        var broken=new OfferingManagementService(new JdbcUnitOfWork(()->{
            Connection real=factory.open();
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("prepareStatement") && a[0].toString().startsWith("INSERT INTO audit_logs")) throw new SQLException("Injected failure");
                try{return m.invoke(real,a);}catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
            });
        }));
        try{broken.create(actor,ref,subject,1,teacherId,"2026","test");throw new AssertionError("rollback");}
        catch(SQLException expected){check("Injected failure".equals(expected.getMessage()),"failure location");}
        check(admin.send(base,null).body().contains("3 lớp học phần"),"rolled back insert");
        String original=VerifySubjects.version(admin.send(base+"/edit?id=1",null).body());
        check(admin.send(base+"/close","csrf="+token+"&id=1&version="+original).statusCode()==303,"close enrolled offering");
        check(student.send("student/offerings/detail?id=1",null).body().contains("CLOSED"),"enrollment retained");
        check(student.send("student/subjects/detail?id="+subject,null).statusCode()==200,"subject retained");
        try(var c=factory.open();var p=c.prepareStatement("SELECT detail_json->>'$.enrollments',detail_json->>'$.examAssignments' FROM audit_logs WHERE target_type='COURSE_OFFERING' AND target_id=1 AND action_code='OFFERING_CLOSE'");var r=p.executeQuery()){
            check(r.next() && r.getInt(1)==1 && r.getInt(2)==0,"references at closure");
        }
        System.out.println("PASS offerings: MySQL, HTTP, concurrency, rollback, scoped reads and historical enrollment");
    }
}
