import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.model.SubjectPage.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Extends the verified, isolated VerifyRoles/VerifySubjects/VerifyOfferings fixture. */
class VerifyEnrollments {
    static void check(boolean ok,String label){AcceptAuth.require(ok,label);}
    static void exec(Connection c,String sql,Object...args) throws SQLException {
        try(var p=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);p.executeUpdate();}
    }
    static long scalar(Connection c,String sql,long id) throws SQLException {
        try(var p=c.prepareStatement(sql)){p.setLong(1,id);try(var r=p.executeQuery()){r.next();return r.getLong(1);}}
    }
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long teacherId,long studentId,
                    AuthUser actor,String ref,ConnectionFactory factory) throws Exception {
        var jdbc=new JdbcUnitOfWork(factory);
        var subjects=new SubjectManagementService(jdbc);
        var offerings=new OfferingManagementService(jdbc);
        long subject=subjects.create(actor,ref,new Input("ENR","Môn <mới>",null));
        long offering=offerings.create(actor,ref,subject,1,teacherId,"2026","2");
        String base="admin/enrollments",create=base+"/new";
        var form=admin.send(create,null);
        check(form.statusCode()==200,"admin form");
        String csrf=AcceptAuth.csrf(form.body()),pair="offering="+offering+"&student="+studentId,body="csrf="+csrf+"&"+pair;
        check(new AcceptAuth().send(base,null).statusCode()==302,"guest");
        check(teacher.send(base,null).statusCode()==403 && student.send(base,null).statusCode()==403,"admin only");
        check(admin.send(create,pair).statusCode()==403 && admin.send(create,pair+"&csrf=x").statusCode()==403,"CSRF");
        check(admin.send(create,"csrf="+csrf).statusCode()==400,"keys");
        check(admin.send(create,body.replace("offering="+offering,"offering=900000")).statusCode()==404,"missing offering");
        check(admin.send(create,body.replace("student="+studentId,"student=900000")).statusCode()==404,"missing student");
        check(admin.send(create,body.replace("student="+studentId,"student="+teacherId)).statusCode()==409,"wrong role");
        check(admin.send(create,body.replace("offering="+offering,"offering=1")).statusCode()==409,"closed offering");
        try(var c=factory.open()){
            exec(c,"UPDATE subjects SET status='INACTIVE' WHERE id=?",subject);
            check(admin.send(create,body).statusCode()==409,"inactive subject");
            exec(c,"UPDATE subjects SET status='ACTIVE' WHERE id=?",subject);
        }
        try(var pool=Executors.newFixedThreadPool(2)){
            var barrier=new CyclicBarrier(2);
            Callable<Integer> register=()->{barrier.await();return admin.send(create,body+"&status=COMPLETED").statusCode();};
            var a=pool.submit(register);var b=pool.submit(register);
            check(Set.of(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS)).equals(Set.of(303,409)),"unique enrollment race");
        }
        String detail=base+"/detail?"+pair;
        check(admin.send(detail,null).body().contains("&lt;mới&gt;"),"escaped detail");
        check(teacher.send("teacher/enrollments/detail?"+pair,null).statusCode()==200,"teacher detail");
        check(student.send("student/enrollments/detail?"+pair,null).statusCode()==200,"student detail");
        check(student.send("student/subjects/detail?id="+subject,null).statusCode()==200,"new subject access");
        check(admin.send(base+"?offering="+offering+"&q=subject.invalid&status=ENROLLED&size=1",null).body().contains("1 ghi danh phù hợp"),"filters");
        check(admin.send(base+"?q=%25",null).body().contains("0 ghi danh phù hợp"),"literal wildcard");
        check(admin.send(base+"?sort=unknown",null).statusCode()==400,"invalid sort");
        check(admin.send(base+"/withdraw",body).statusCode()==303,"withdraw");
        check(student.send("student/enrollments/detail?"+pair,null).body().contains("WITHDRAWN"),"withdrawn history");
        check(student.send("student/subjects/detail?id="+subject,null).statusCode()==404 &&
              student.send("student/offerings/detail?id="+offering,null).statusCode()==404,"withdrawn scope");
        check(admin.send(base+"/complete",body).statusCode()==409 && admin.send(create,body).statusCode()==409,"terminal state");
        long next=offerings.create(actor,ref,subject,1,teacherId,"2026","3");
        long otherTeacher;
        try(var c=factory.open()){otherTeacher=scalar(c,"SELECT id FROM users WHERE email='other@offering.invalid' AND id<>?",teacherId);}
        long unrelated=offerings.create(actor,ref,subject,1,otherTeacher,"2026","4");
        var service=new EnrollmentManagementService(jdbc);
        service.change(actor,ref,unrelated,studentId,"ENROLLED");
        check(teacher.send("teacher/enrollments?offering="+unrelated+"&teacher_id="+otherTeacher,null).body().contains("0 ghi danh phù hợp"),"teacher roster scope");
        check(teacher.send("teacher/enrollments/detail?offering="+unrelated+"&student="+studentId,null).statusCode()==404,"teacher detail scope");
        service.change(actor,ref,unrelated,studentId,"COMPLETED");
        check(student.send("student/offerings/detail?id="+unrelated,null).statusCode()==200 && student.send("student/subjects/detail?id="+subject,null).statusCode()==200,"completed scope");
        long extra=new UserManagementService(jdbc).create(actor,new UserDirectory.Profile("Extra","extra@enrollment.invalid",null,null),
                VerifyRoles.password.toCharArray(),VerifyRoles.password.toCharArray(),"STUDENT");
        service.change(actor,ref,next,extra,"ENROLLED");
        check(student.send("student/enrollments/detail?offering="+next+"&student="+extra,null).statusCode()==404,"forged student");
        var broken=new EnrollmentManagementService(new JdbcUnitOfWork(()->{
            Connection real=factory.open();
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("prepareStatement") && a[0].toString().startsWith("INSERT INTO audit_logs"))throw new SQLException("Injected audit failure");
                try{return m.invoke(real,a);}catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
            });
        }));
        try{broken.change(actor,ref,next,studentId,"ENROLLED");throw new AssertionError("rollback");}
        catch(SQLException e){check(e.getMessage().equals("Injected audit failure"),"audit injection");}
        try(var c=factory.open()){
            check(scalar(c,"SELECT COUNT(*) FROM course_enrollments WHERE offering_id=?",next)==1,"audit rollback");
            exec(c,"INSERT INTO exams(subject_id,created_by,title,duration_minutes,pass_percentage,status) VALUES(?,?,'Fixture',30,50,'PUBLISHED')",subject,teacherId);
            long exam=scalar(c,"SELECT id FROM exams WHERE subject_id=?",subject);
            exec(c,"INSERT INTO exam_course_assignments(exam_id,offering_id,opens_at,closes_at,assigned_by) VALUES(?,?,UTC_TIMESTAMP()-INTERVAL 1 DAY,UTC_TIMESTAMP()+INTERVAL 1 DAY,?)",exam,next,actor.id());
            String extraBody="csrf="+csrf+"&offering="+next+"&student="+extra;
            check(admin.send(base+"/withdraw",extraBody).statusCode()==409,"effective exam eligibility");
            exec(c,"UPDATE exams SET status='CLOSED' WHERE id=?",exam);
            exec(c,"INSERT INTO exam_attempts(exam_id,student_id,attempt_no,started_at,deadline_at) VALUES(?,?,1,UTC_TIMESTAMP(),UTC_TIMESTAMP()+INTERVAL 1 HOUR)",exam,extra);
            check(admin.send(base+"/complete",extraBody).statusCode()==409,"attempt history");
            long version=offerings.detail(actor,Scope.ADMIN,next).version();
            try(var pool=Executors.newFixedThreadPool(2)){
                var gate=new CyclicBarrier(2);
                var registration=pool.submit(()->{gate.await();try{service.change(actor,ref,next,studentId,"ENROLLED");return 1;}
                    catch(EnrollmentManagementService.Conflict expected){return 0;}});
                var closure=pool.submit(()->{gate.await();offerings.change(actor,ref,next,version,0,true);return true;});
                int count=1+registration.get(25,TimeUnit.SECONDS);check(closure.get(25,TimeUnit.SECONDS),"close");
                check(scalar(c,"SELECT COUNT(*) FROM course_enrollments WHERE offering_id=?",next)==count,"close preserves roster");
                check(scalar(c,"SELECT detail_json->>'$.enrollments' FROM audit_logs WHERE target_type='COURSE_OFFERING' AND action_code='OFFERING_CLOSE' AND target_id=?",next)==count,"race ordering");
                check(scalar(c,"SELECT detail_json->>'$.examAssignments' FROM audit_logs WHERE target_type='COURSE_OFFERING' AND action_code='OFFERING_CLOSE' AND target_id=?",next)==1,"close assigned exam");
            }
        }
        System.out.println("PASS enrollments: HTTP, scoped history, exam guards, concurrency, atomic audit and closing with an assigned exam");
    }
}
