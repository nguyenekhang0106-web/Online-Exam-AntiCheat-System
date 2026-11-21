import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.service.QuestionBankService.Access;
import vn.edu.onlineexam.model.QuestionCatalog.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Invoked after the guarded VerifySubjects scratch suite, never against development. */
class VerifyQuestionCatalog {
    static void check(boolean ok,String label){AcceptAuth.require(ok,label);}
    static long save(AcceptAuth client,String route,String form)throws Exception{
        var r=client.send(route,form);check(r.statusCode()==303,route+" returned "+r.statusCode());
        return Long.parseLong(r.headers().firstValue("Location").orElseThrow().split("id=")[1]);
    }
    static String stamp(AcceptAuth client,String base,long id)throws Exception{
        var r=client.send(base+"/edit?id="+id,null);
        var m=Pattern.compile("name=\"stamp\" value=\"([0-9]+)\"").matcher(r.body());
        check(r.statusCode()==200 && m.find(),"stamp for "+base);return m.group(1);
    }
    static long count(ConnectionFactory f,String table)throws Exception{
        try(var c=f.open();var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}
    }
    static void race(AcceptAuth client,String route,String body)throws Exception{
        try(var pool=Executors.newFixedThreadPool(2)){
            var barrier=new CyclicBarrier(2);Callable<Integer> work=()->{barrier.await();return client.send(route,body).statusCode();};
            var a=pool.submit(work);var b=pool.submit(work);
            check(Set.of(a.get(25,TimeUnit.SECONDS),b.get(25,TimeUnit.SECONDS)).equals(Set.of(303,409)),"concurrency "+route);
        }
    }
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long subject,long inactive,long teacherId,ConnectionFactory factory)throws Exception{
        String topics="teacher/topics",banks="teacher/banks",questions="teacher/questions";
        String csrf="csrf="+AcceptAuth.csrf(teacher.send("teacher/account",null).body());
        for(String base:List.of(topics,banks,questions)){
            check(new AcceptAuth().send(base,null).statusCode()==302,"guest");
            check(student.send(base+"?role=TEACHER",null).statusCode()==403,"student");
            check(teacher.send(base+"/new","").statusCode()==403 && teacher.send(base+"/new","csrf=x").statusCode()==403,"CSRF");
            check(teacher.send(base+"/detail?id=999999",null).statusCode()==404,"not found "+base);
        }
        String t=csrf+"&subject="+subject+"&code=T3&name="+AcceptAuth.enc("Chủ đề <A>")+"&order=1";
        check(teacher.send(topics+"/new",t.replace("subject="+subject,"subject=999999")).statusCode()==404,"missing subject");
        check(teacher.send(topics+"/new",t.replace("subject="+subject,"subject="+inactive)).statusCode()==409,"inactive subject");
        long topic=save(teacher,topics+"/new",t);
        check(teacher.send(topics+"/new",t).statusCode()==409,"duplicate topic");
        String edit=t+"&id="+topic+"&stamp="+stamp(teacher,topics,topic);
        check(teacher.send(topics+"/edit",edit+"&parent="+topic).statusCode()==400,"topic cycle");
        race(teacher,topics+"/edit",edit);
        long child=save(teacher,topics+"/new",t.replace("code=T3","code=T4")+"&parent="+topic);
        check(teacher.send(topics+"/edit",t+"&id="+topic+"&stamp="+stamp(teacher,topics,topic)+"&parent="+child).statusCode()==400,"descendant cycle");
        check(teacher.send(topics+"?subject="+subject+"&size=1&page=2",null).body().contains("T4"),"topic paging");
        String bankForm=csrf+"&subject="+subject+"&name=Bank3&visibility=PRIVATE";
        long bank=save(teacher,banks+"/new",bankForm);
        save(teacher,banks+"/new",bankForm);
        race(teacher,banks+"/edit",bankForm+"&id="+bank+"&stamp="+stamp(teacher,banks,bank));
        check(teacher.send(banks+"?q=Bank3&subject="+subject+"&size=1&page=2",null).body().contains("Bank3"),"bank paging");
        check(admin.send("admin/banks/new",null).statusCode()==403,"admin bank read-only");
        String marker="ANSWER-PRIVATE-3C";
        String q=csrf+"&bank="+bank+"&topic="+topic+"&type=SINGLE_CHOICE&difficulty=MEDIUM&points=2.50&content="+AcceptAuth.enc("Nội dung tiếng Việt <dài> ".repeat(14))+"&option="+marker+"&option=B&correct=0";
        check(teacher.send(questions+"/new",q.replace("&correct=0","")).statusCode()==400,"no correct");
        check(teacher.send(questions+"/new",q+"&correct=1").statusCode()==400,"single with two correct");
        check(teacher.send(questions+"/new",q.replace("&option=B","")).statusCode()==400,"one option");
        check(teacher.send(questions+"/new",q.replace("&option=B","&option="+marker)).statusCode()==400,"duplicate option");
        long question=save(teacher,questions+"/new",q);
        String multi=q.replace("SINGLE_CHOICE","MULTIPLE_CHOICE")+"&correct=1&option=C&option=D";
        long second=save(teacher,questions+"/new",multi);
        race(teacher,questions+"/edit",multi.replace("option=D","option=Revised")+"&id="+second+"&stamp="+stamp(teacher,questions,second));
        check(teacher.send(questions+"/detail?id="+second,null).body().contains("Revised"),"edited option rendered");
        check(teacher.send(questions+"?q="+AcceptAuth.enc("tiếng Việt")+"&subject="+subject+"&bank="+bank+"&topic="+topic+"&difficulty=MEDIUM&size=1&page=2",null).body().contains("&lt;dài&gt;"),"question filters");
        check(teacher.send(banks+"/detail?id="+bank,null).body().contains("2 câu hỏi"),"bank count");
        String detail=questions+"/detail?id="+question;
        check(teacher.send(detail,null).body().contains(marker),"owner sees key");
        var denied=student.send(detail+"&owner="+teacherId,null);check(denied.statusCode()==403 && !denied.body().contains(marker),"student key");
        var metadata=admin.send("admin/questions/detail?id="+question,null);
        check(metadata.statusCode()==200 && !metadata.body().contains(marker) && !metadata.body().contains("Nội dung tiếng Việt"),"admin metadata");
        String ref=VerifyRoles.ref();var auth=new AuthenticationService(new JdbcUnitOfWork(factory),new AuthDao());
        var actor=auth.login("TEACHER@subject.invalid",VerifyRoles.password.toCharArray(),ref);var access=new Access(actor,"TEACHER",ref);
        String otherRef=VerifyRoles.ref();var other=auth.login("other@offering.invalid",VerifyRoles.password.toCharArray(),otherRef);
        var service=new QuestionBankService(new JdbcUnitOfWork(factory));
        long foreignTopic;
        try(var c=factory.open()){
            VerifyEnrollments.exec(c,"INSERT INTO topics(subject_id,topic_code,topic_name,display_order) VALUES(?,'OTHER3','Other',0)",inactive);
            foreignTopic=VerifyEnrollments.scalar(c,"SELECT MAX(id) FROM topics WHERE subject_id=?",inactive);
        }
        check(teacher.send(questions+"/new",q.replace("&topic="+topic+"&","&topic="+foreignTopic+"&")).statusCode()==400,"different topic subject");
        check(teacher.send(banks+"/new",bankForm.replace("subject="+subject,"subject="+inactive)).statusCode()==409,"inactive bank subject");
        check(teacher.send(banks+"/new",bankForm.replace("subject="+subject,"subject=999999")).statusCode()==404,"missing bank subject");
        try{service.detail(new Access(other,"TEACHER",otherRef),vn.edu.onlineexam.model.QuestionCatalog.Module.questions,question);throw new AssertionError("other teacher key");}catch(NoSuchElementException expected){}
        for(String prefix:List.of("INSERT INTO question_version_options","INSERT INTO audit_logs")){
            var broken=new QuestionBankService(new JdbcUnitOfWork(()->{
                Connection real=factory.open();
                return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,args)->{
                    if(m.getName().equals("prepareStatement") && args[0].toString().startsWith(prefix))throw new SQLException("Injected failure");
                    try{return m.invoke(real,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
                });
            }));
            var draft=new Draft(bank,topic,"SINGLE_CHOICE","EASY","Rollback",null,BigDecimal.ONE,List.of(new Answer("a",true),new Answer("b",false)));
            var module=vn.edu.onlineexam.model.QuestionCatalog.Module.questions;
            var original=service.detail(access,module,second);
            long previous=((Number)original.row().get("stamp")).longValue();
            try{broken.question(access,second,previous,draft);throw new AssertionError("expected revision rollback");}
            catch(SQLException e){check(e.getMessage().equals("Injected failure"),"injected revise");}
            check(original.equals(service.detail(access,module,second)),"metadata, answers, version and stamp preserved");
            for(String entity:prefix.contains("audit")?List.of("topics","question_banks","questions"):List.of("questions")){
                long before=count(factory,entity),versions=count(factory,"question_versions"),options=count(factory,"question_version_options");
                try{
                    switch(entity){case "topics"->broken.topic(access,0,0,subject,null,"ROLLBACK","Rollback",0);case "question_banks"->broken.bank(access,0,0,subject,"Rollback",null,"PRIVATE");default->broken.question(access,0,0,draft);}
                    throw new AssertionError("missing rollback");
                }catch(SQLException e){check(e.getMessage().equals("Injected failure"),"injected statement");}
                check(count(factory,entity)==before && count(factory,"question_versions")==versions && count(factory,"question_version_options")==options,"atomic rollback");
            }
        }
        try(var c=factory.open()){
            VerifyEnrollments.exec(c,"INSERT INTO exams(subject_id,created_by,title,duration_minutes,pass_percentage) VALUES(?,?,'History fixture',30,50)",subject,teacherId);
            VerifyEnrollments.exec(c,"INSERT INTO exam_questions(exam_id,question_version_id,points,fixed_order) SELECT LAST_INSERT_ID(),id,1,1 FROM question_versions WHERE question_id=? AND version_no=1",question);
        }
        check(teacher.send(questions+"/edit",q+"&id="+question+"&stamp="+stamp(teacher,questions,question)).statusCode()==409,"exam version protected");
        check(teacher.send(topics+"/edit",t+"&id="+topic+"&stamp="+stamp(teacher,topics,topic)).statusCode()==409,"exam topic protected");
        check(teacher.send(banks+"/edit",bankForm+"&id="+bank+"&stamp="+stamp(teacher,banks,bank)).statusCode()==409,"used bank protected");
        save(teacher,questions+"/publish",csrf+"&id="+second+"&stamp="+stamp(teacher,questions,second));
        check(teacher.send(questions+"/edit",multi+"&id="+second+"&stamp="+stamp(teacher,questions,second)).statusCode()==409,"published immutable");
        save(teacher,questions+"/archive",csrf+"&id="+second+"&stamp="+stamp(teacher,questions,second));
        auth.logout(ref);
        try{service.detail(access,vn.edu.onlineexam.model.QuestionCatalog.Module.questions,question);throw new AssertionError("revoked reader");}catch(SecurityException expected){}
        try{service.bank(access,0,0,subject,"revoked",null,"PRIVATE");throw new AssertionError("revoked writer");}catch(SecurityException expected){}
        System.out.println("PASS topic trees, bank ownership, question versions, private keys, rollback, concurrency and referenced history");
        check(teacher.send("logout",csrf).statusCode()==302 && teacher.send(detail,null).statusCode()==302,"old HTTP session loses catalog");
    }
}
