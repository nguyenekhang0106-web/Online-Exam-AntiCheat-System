import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.SubjectPage.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.*;

/** Run with VerifyRoles on a fresh role_test scratch schema containing V1–V4. */
class VerifySubjects {
    static void check(boolean result,String label){AcceptAuth.require(result,label);}
    static String version(String html){
        var m=Pattern.compile("name=\"version\" value=\"([^\"]+)\"").matcher(html);
        check(m.find(),"version in edit form"); return m.group(1);
    }
    public static void main(String[] args) throws Exception {
        // Includes server identity & empty-fixture checks before any write.
        VerifyRoles.main(args);
        var factory=ConnectionFactory.fromEnvironment();
        long adminId; String email;
        try(var c=factory.open();var s=c.createStatement();
            var r=s.executeQuery("SELECT u.id,u.email FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE r.role_code='ADMIN' AND u.status='ACTIVE'")){
            check(r.next(),"remaining admin"); adminId=r.getLong(1); email=r.getString(2);
        }
        var admin=VerifyRoles.login(email);
        String base="admin/subjects", create=base+"/new", edit=base+"/edit", archive=base+"/archive";
        check(new AcceptAuth().send(base,null).statusCode()==302,"guest");
        String csrf=AcceptAuth.csrf(admin.send(create,null).body());
        String form="csrf="+csrf+"&code=WEB&name="+AcceptAuth.enc("Công nghệ <Web>")+"&status=ARCHIVED&id=999";
        check(admin.send(create,"code=A").statusCode()==403 && admin.send(create,"csrf=wrong").statusCode()==403,"CSRF");
        check(admin.send(create,"csrf="+csrf+"&code=ABC").statusCode()==400,"required");
        check(admin.send(create,"csrf="+csrf+"&code="+"A".repeat(31)+"&name=x").statusCode()==400,"schema length");
        var created=admin.send(create,form); check(created.statusCode()==303,"create");
        String detail=created.headers().firstValue("Location").orElseThrow();
        long id=Long.parseLong(detail.substring(detail.lastIndexOf('=')+1));
        var page=admin.send(detail,null);
        check(page.statusCode()==200 && page.body().contains("&lt;Web&gt;") && page.body().contains("ACTIVE") && id!=999,"safe detail");
        check(admin.send(create,form).statusCode()==409,"unique");
        String v=version(admin.send(edit+"?id="+id,null).body());
        String update="csrf="+csrf+"&id="+id+"&version="+v+"&code=WEB&name=Web";
        check(admin.send(edit,update.replace("&id="+id+"&","&id=999999&")).statusCode()==404,"missing subject");
        try(var pool=Executors.newFixedThreadPool(2)){
            var gate=new CyclicBarrier(2);
            Callable<Integer> write=()->{gate.await();return admin.send(edit,update).statusCode();};
            var a=pool.submit(write);var b=pool.submit(write);
            check(Set.of(a.get(),b.get()).equals(Set.of(303,409)),"two competing edits");
        }
        check(admin.send(create,"csrf="+csrf+"&code=ZZ&name=Other").statusCode()==303,"second subject");
        check(admin.send(base+"?q=WEB&status=ACTIVE",null).body().contains("1 môn học phù hợp"),"search filter");
        check(admin.send(base+"?q=%25",null).body().contains("0 môn học"),"literal wildcard");
        check(admin.send(base+"?sort=code&direction=DESC&size=1&page=2",null).body().contains("WEB"),"sorted page");
        check(admin.send(base+"?sort=invalid",null).statusCode()==400,"sort whitelist");
        var actor=new AuthUser(adminId,Set.of("ADMIN"));
        var jdbc=new JdbcUnitOfWork(factory);
        String sessionRef=VerifyRoles.ref();
        new AuthenticationService(jdbc,new vn.edu.onlineexam.dao.AuthDao())
                .login(email,VerifyRoles.password.toCharArray(),sessionRef);
        var broken=new SubjectManagementService(new JdbcUnitOfWork(()->{
            Connection real=factory.open();
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class[]{Connection.class},(proxy,method,parameters)->{
                if(method.getName().equals("prepareStatement") && parameters[0].toString().startsWith("INSERT INTO audit_logs"))
                    throw new SQLException("Injected audit failure");
                try {return method.invoke(real,parameters);}
                catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
        }));
        try {
            broken.create(actor,sessionRef,new Input("ROLLBACK","Rollback",null));
            throw new AssertionError("audit failure expected");
        } catch(SQLException expected){check("Injected audit failure".equals(expected.getMessage()),"audit failure");}
        check(admin.send(base+"?q=ROLLBACK",null).body().contains("0 môn học"),"rollback subject insert");
        var userService=new UserManagementService(jdbc);
        long teacher=0,student=0;
        for(String role:List.of("TEACHER","STUDENT")){
            String mail=role+"@subject.invalid";
            long uid=userService.create(actor,new UserDirectory.Profile(role,mail,null,null),
                    VerifyRoles.password.toCharArray(),VerifyRoles.password.toCharArray(),role);
            if(role.equals("TEACHER")) teacher=uid; else student=uid;
        }
        var t=VerifyRoles.login("TEACHER@subject.invalid"); var s=VerifyRoles.login("STUDENT@subject.invalid");
        for(var client:List.of(t,s))check(client.send(base,null).statusCode()==403,"admin route denied");
        check(t.send("teacher/subjects/detail?id="+id,null).statusCode()==404 && s.send("student/subjects/detail?id="+id,null).statusCode()==404,"unassigned hidden");
        try(var c=factory.open();var st=c.createStatement()){
            st.executeUpdate("INSERT INTO classes(class_code,class_name,academic_year) VALUES('A','A','2026-2027')");
            try(var p=c.prepareStatement("INSERT INTO course_offerings(subject_id,class_id,teacher_id,academic_year,semester) VALUES(?,1,?,'2026-2027','1')")){
                p.setLong(1,id);p.setLong(2,teacher);p.executeUpdate();
            }
            try(var p=c.prepareStatement("INSERT INTO course_enrollments(offering_id,student_id,enrolled_at) VALUES(1,?,UTC_TIMESTAMP(3))")){
                p.setLong(1,student);p.executeUpdate();
            }
        }
        check(t.send("teacher/subjects",null).body().contains("1 môn học") && s.send("student/subjects/detail?id="+id,null).statusCode()==200,"scoped reads");
        v=version(admin.send(edit+"?id="+id,null).body());
        check(admin.send(archive,"csrf="+csrf+"&id="+id+"&version="+v).statusCode()==409,"referenced");
        long unused;
        try(var c=factory.open();var st=c.createStatement();var r=st.executeQuery("SELECT id FROM subjects WHERE subject_code='ZZ'")){r.next();unused=r.getLong(1);}
        v=version(admin.send(edit+"?id="+unused,null).body());
        check(admin.send(archive,"csrf="+csrf+"&id="+unused+"&version="+v).statusCode()==303,"archive");
        check(admin.send(base+"?status=ARCHIVED",null).body().contains("1 môn học"),"archived filter");
        try(var c=factory.open();var st=c.createStatement();var r=st.executeQuery("SELECT COUNT(*) FROM audit_logs WHERE target_type='SUBJECT'")){
            r.next();check(r.getLong(1)==4,"audit only successful mutations");
        }
        System.out.println("PASS subjects on isolated MySQL and Tomcat HTTP: scoped reads, forms, validation, CSRF, unique, search, paging, concurrent edits, archive, audit");
        boolean catalog=args.length>1 && args[1].equals("catalog");
        boolean classes=catalog || (args.length>1 && args[1].equals("classes"));
        if(args.length>1 && (args[1].equals("offerings") || args[1].equals("enrollments") || classes))
            VerifyOfferings.run(admin,t,s,id,unused,teacher,actor,sessionRef,factory);
        if(classes || (args.length>1 && args[1].equals("enrollments")))
            VerifyEnrollments.run(admin,t,s,teacher,student,actor,sessionRef,factory);
        if(classes) VerifyClasses.run(admin,t,s,student,actor,sessionRef,factory);
        if(args.length>2 && args[2].equals("exams")) VerifyExams.run(admin,t,s,teacher,student,factory);
        if(catalog) VerifyQuestionCatalog.run(admin,t,s,id,unused,teacher, factory);
    }
}
