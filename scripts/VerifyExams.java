import java.sql.*;
import java.util.regex.Pattern;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Uses the guarded isolated role_test schema prepared by VerifySubjects. */
class VerifyExams {
    static final String route="teacher/exams";
    static void check(boolean ok,String name){AcceptAuth.require(ok,name);}
    static void layout(String name,String html)throws Exception{
        var directory=java.nio.file.Path.of("target","exam-layout");
        java.nio.file.Files.createDirectories(directory);
        String safe=html.replaceAll("name=\"csrf\" value=\"[^\"]*\"","name=\"csrf\" value=\"\"")
                .replace("../assets/css/app.css","../online-exam/assets/css/app.css");
        java.nio.file.Files.writeString(directory.resolve(name+".html"),safe);
    }
    static String form(AcceptAuth teacher,long exam,String csrf)throws Exception{
        var page=teacher.send(route+"?id="+exam,null);
        var token=Pattern.compile("name=\"stamp\" value=\"(\\d+)\"").matcher(page.body());
        check(page.statusCode()==200 && token.find(),"exam form HTTP "+page.statusCode());
        return csrf+"&id="+exam+"&stamp="+token.group(1);
    }
    static void race(AcceptAuth first,AcceptAuth second,long exam,String action)throws Exception{
        String firstCsrf="csrf="+AcceptAuth.csrf(first.send("teacher/account",null).body());
        String secondCsrf="csrf="+AcceptAuth.csrf(second.send("teacher/account",null).body());
        String body1=form(first,exam,firstCsrf)+action,body2=form(second,exam,secondCsrf)+action;
        var gate=new java.util.concurrent.CyclicBarrier(2);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var one=pool.submit(()->{gate.await();return first.send(route,body1).statusCode();});
            var two=pool.submit(()->{gate.await();return second.send(route,body2).statusCode();});
            var result=java.util.List.of(one.get(25,java.util.concurrent.TimeUnit.SECONDS),two.get(25,java.util.concurrent.TimeUnit.SECONDS));
            check(result.contains(303) && result.contains(409),"independent session concurrency");
        }
    }
    static void rollback(ConnectionFactory factory,AcceptAuth teacher,long id,String csrf,String operation,String countSql)throws Exception{
        String body=form(teacher,id,csrf)+operation;
        try(var c=factory.open();var sql=c.createStatement()){
            long before=VerifyEnrollments.scalar(c,countSql,id);
            sql.execute("CREATE TRIGGER exam_audit_failure BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'");
            try{
                check(teacher.send(route,body).statusCode()==503,"injected audit error");
                check(VerifyEnrollments.scalar(c,countSql,id)==before,"aggregate rollback");
            }finally{sql.execute("DROP TRIGGER exam_audit_failure");}
        }
    }
    static long publishedVersion(AcceptAuth teacher,String csrf,long subject,ConnectionFactory factory)throws Exception{
        long bank=VerifyQuestionCatalog.save(teacher,"teacher/banks/new",csrf+"&subject="+subject+"&name=Scope&visibility=SHARED");
        long question=VerifyQuestionCatalog.save(teacher,"teacher/questions/new",csrf+"&bank="+bank+"&type=SINGLE_CHOICE&difficulty=EASY&points=1&content=scope&option=A&option=B&correct=0");
        VerifyQuestionCatalog.save(teacher,"teacher/questions/publish",csrf+"&id="+question+"&stamp="+VerifyQuestionCatalog.stamp(teacher,"teacher/questions",question));
        try(var c=factory.open()){return VerifyEnrollments.scalar(c,"SELECT id FROM question_versions WHERE question_id=?",question);}
    }
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long teacherId,long studentId,ConnectionFactory factory)throws Exception{
        String marker="Exam4-private-text";
        String csrf="csrf="+AcceptAuth.csrf(teacher.send("teacher/account",null).body());
        check(teacher.send(route,null).statusCode()==200 && new AcceptAuth().send(route,null).statusCode()==302,"list and guest");
        check(student.send(route,null).statusCode()==403 && teacher.send(route,"csrf=wrong").statusCode()==403,"RBAC and CSRF");
        long subject,offering,policy;
        try(var c=factory.open()){
            VerifyEnrollments.exec(c,"INSERT INTO subjects(subject_code,subject_name) VALUES('E4','Exam test')");
            subject=VerifyEnrollments.scalar(c,"SELECT id FROM subjects WHERE subject_code='E4' AND id>?",0);
            VerifyEnrollments.exec(c,"INSERT INTO course_offerings(subject_id,class_id,teacher_id,academic_year,semester) VALUES(?,1,?,'2026-2027','1')",subject,teacherId);
            offering=VerifyEnrollments.scalar(c,"SELECT id FROM course_offerings WHERE subject_id=?",subject);
            VerifyEnrollments.exec(c,"INSERT INTO course_enrollments(offering_id,student_id,enrolled_at) VALUES(?,?,UTC_TIMESTAMP())",offering,studentId);
            VerifyEnrollments.exec(c,"INSERT INTO proctoring_policies(policy_name,max_tab_switches,max_fullscreen_exits,max_parallel_sessions,created_by) VALUES('E4',3,2,1,?)",teacherId);
            policy=VerifyEnrollments.scalar(c,"SELECT id FROM proctoring_policies WHERE policy_name='E4' AND created_by=?",teacherId);
        }
        String settings="&title=Exam4&duration=30&attempts=3&pass=50&scorePolicy=LATEST&shuffleQuestions=on&resultRelease=AFTER_CLOSE&policy="+policy;
        String create=csrf+"&action=create&subject="+subject+settings;
        check(teacher.send(route,create.replace("attempts=3","attempts=0")).statusCode()==400,"zero attempts");
        check(teacher.send(route,create.replace("attempts=3","attempts=32768")).statusCode()==400,"SMALLINT limit");
        check(teacher.send(route,create.replace("duration=30","duration=0")).statusCode()==400,"duration");
        long id=VerifyQuestionCatalog.save(teacher,route,create);
        layout("list",teacher.send(route,null).body());
        check(teacher.send(route+"?id=99999999",null).statusCode()==404,"missing exam");
        var other=VerifyRoles.login("other@offering.invalid");
        check(other.send(route+"?id="+id,null).statusCode()==403,"other teacher exam");
        check(admin.send("admin/exams","csrf="+AcceptAuth.csrf(admin.send("admin/account",null).body())+"&action=create").statusCode()==403,"admin lookup only");
        String prefix=form(teacher,id,csrf);
        try(var c=factory.open();var statement=c.createStatement()){
            statement.execute("CREATE TRIGGER exam_audit_failure BEFORE INSERT ON audit_logs FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'");
            try{
                check(teacher.send(route,prefix+"&action=edit"+settings.replace("attempts=3","attempts=7")).statusCode()==503,"audit error");
                check(VerifyEnrollments.scalar(c,"SELECT max_attempts FROM exams WHERE id=?",id)==3,"configuration rolled back");
            }finally{statement.execute("DROP TRIGGER exam_audit_failure");}
        }
        check(teacher.send(route,prefix+"&action=PUBLISHED").statusCode()==409,"empty publish");
        var secondSession=VerifyRoles.login("teacher@subject.invalid");
        race(teacher,secondSession,id,"&action=edit"+settings);
        long bank=VerifyQuestionCatalog.save(teacher,"teacher/banks/new",csrf+"&subject="+subject+"&name=Exam4&visibility=PRIVATE");
        long question=VerifyQuestionCatalog.save(teacher,"teacher/questions/new",csrf+"&bank="+bank+"&type=SINGLE_CHOICE&difficulty=EASY&points=1&content="+marker+"&option=A&option=B&correct=0");
        long version;
        try(var c=factory.open()){version=VerifyEnrollments.scalar(c,"SELECT id FROM question_versions WHERE question_id=?",question);}
        String item="&action=add&version="+version+"&position=1&points=2";
        check(teacher.send(route,form(teacher,id,csrf)+item).statusCode()==404,"draft excluded");
        check(teacher.send(route,form(teacher,id,csrf)+item.replace("version="+version,"version=99999999")).statusCode()==404,"version missing");
        long otherSubject;
        try(var c=factory.open()){otherSubject=VerifyEnrollments.scalar(c,"SELECT id FROM subjects WHERE subject_code='WEB' AND id<>?",subject);}
        long wrongSubject=publishedVersion(teacher,csrf,otherSubject,factory);
        String otherCsrf="csrf="+AcceptAuth.csrf(other.send("teacher/account",null).body());
        long foreign=publishedVersion(other,otherCsrf,subject,factory);
        for(long excluded:new long[]{wrongSubject,foreign})
            check(teacher.send(route,form(teacher,id,csrf)+item.replace("version="+version,"version="+excluded)).statusCode()==404,"version scope");
        long withAttachment=publishedVersion(teacher,csrf,subject,factory);
        try(var c=factory.open()){
            VerifyEnrollments.exec(c,"INSERT INTO question_attachments(question_version_id,storage_key,original_filename,media_type,size_bytes,sha256) VALUES(?,'phase4-fixture','fixture.txt','text/plain',0,REPEAT('0',64))",withAttachment);
        }
        check(teacher.send(route,form(teacher,id,csrf)+item.replace("version="+version,"version="+withAttachment)).statusCode()==404,"attachment excluded");
        VerifyQuestionCatalog.save(teacher,"teacher/questions/publish",csrf+"&id="+question+"&stamp="+VerifyQuestionCatalog.stamp(teacher,"teacher/questions",question));
        rollback(factory,teacher,id,csrf,item,"SELECT COUNT(*) FROM exam_questions WHERE exam_id=?");
        race(teacher,secondSession,id,item);
        layout("draft",teacher.send(route+"?id="+id,null).body());
        check(teacher.send(route,form(teacher,id,csrf)+item).statusCode()==409,"duplicate question");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+"&action=remove&version="+version);
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+item);
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+"&action=PUBLISHED");
        check(teacher.send(route,form(teacher,id,csrf)+"&action=edit"+settings).statusCode()==409,"published immutable");
        String window="&opens=2090-01-01T09:00&closes=2090-01-01T12:00&limit=2";
        String course="&action=assign&scope=COURSE&target="+offering+window;
        rollback(factory,teacher,id,csrf,course,"SELECT COUNT(*) FROM exam_course_assignments WHERE exam_id=?");
        check(teacher.send(route,form(teacher,id,csrf)+course.replace("T12:00","T08:00")).statusCode()==400,"reversed UTC window");
        check(teacher.send(route,form(teacher,id,csrf)+course.replace("target="+offering,"target=999999999")).statusCode()==404,"unknown offering");
        race(teacher,secondSession,id,course);
        check(teacher.send(route,form(teacher,id,csrf)+course).statusCode()==409,"duplicate assignment");
        String individual="&action=assign&scope=STUDENT&target="+studentId+window;
        check(teacher.send(route,form(teacher,id,csrf)+individual.replace("target="+studentId,"target="+teacherId)).statusCode()==404,"ineligible individual");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+individual);
        var schedule=student.send("student/schedule",null);
        layout("schedule",schedule.body());
        layout("assignments",teacher.send(route+"?id="+id,null).body());
        check(schedule.statusCode()==200 && schedule.body().contains("Exam4") && schedule.body().contains("Cá nhân") && schedule.body().contains("Lớp học phần") && !schedule.body().contains(marker),"overlapping schedule without content");
        check(!admin.send("admin/exams?id="+id,null).body().contains(marker),"admin metadata");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+"&action=cancel&scope=STUDENT&target="+studentId);
        String activeWindow="&opens=2020-01-01T00:00&closes=2090-01-01T12:00&limit=2";
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+course.replace("action=assign","action=reschedule").replace(window,activeWindow));
        String withdraw="csrf="+AcceptAuth.csrf(admin.send("admin/account",null).body())+"&offering="+offering+"&student="+studentId;
        check(admin.send("admin/enrollments/withdraw",withdraw).statusCode()==409,"active class-only eligibility");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,id,csrf)+individual.replace(window,activeWindow));
        check(admin.send("admin/enrollments/withdraw",withdraw).statusCode()==303,"overlapping individual schedule preserves eligibility");
        check(student.send("student/schedule",null).body().contains("Exam4"),"individual schedule survives withdrawal");
        check(teacher.send(route,form(teacher,id,csrf)+"&action=CLOSED").statusCode()==409,"protect active window");
        try(var c=factory.open()){
            check(VerifyEnrollments.scalar(c,"SELECT max_attempts FROM exams WHERE id=?",id)==3,"persisted limit");
            check(VerifyEnrollments.scalar(c,"SELECT duration_minutes=30 AND shuffle_questions=1 AND attempt_score_policy='LATEST' AND result_release_mode='AFTER_CLOSE' FROM exams WHERE id=?",id)==1,"persisted options");
            check(VerifyEnrollments.scalar(c,"SELECT JSON_LENGTH(policy_snapshot_json) FROM exams WHERE id=?",id)>0,"policy snapshot");
            check(VerifyEnrollments.scalar(c,"SELECT policy_snapshot_json->>'$.max_tab_switches' FROM exams WHERE id=?",id)==3,"published policy fields");
            VerifyEnrollments.exec(c,"INSERT INTO exam_attempts(exam_id,student_id,attempt_no,started_at,deadline_at) VALUES(?,?,1,UTC_TIMESTAMP(),UTC_TIMESTAMP()+INTERVAL 30 MINUTE)",id,studentId);
        }
        check(teacher.send(route,form(teacher,id,csrf)+"&action=cancel&scope=STUDENT&target="+studentId).statusCode()==409,"protect attempt");
        long lifecycle=VerifyQuestionCatalog.save(teacher,route,create);
        VerifyQuestionCatalog.save(teacher,route,form(teacher,lifecycle,csrf)+item);
        VerifyQuestionCatalog.save(teacher,route,form(teacher,lifecycle,csrf)+"&action=PUBLISHED");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,lifecycle,csrf)+"&action=CLOSED");
        VerifyQuestionCatalog.save(teacher,route,form(teacher,lifecycle,csrf)+"&action=ARCHIVED");
        System.out.println("PASS exam configuration, content, publish, concurrent writes, UTC course/individual schedules and restricted views");
    }
}
