import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Independent, synthetic fixtures on the dedicated loopback MySQL instance. */
class VerifyAttempts {
    static final String route="student/attempts";
    static void check(boolean value,String label){AcceptAuth.require(value,label);}
    static String csrf(AcceptAuth client)throws Exception{return "csrf="+AcceptAuth.csrf(client.send("student/account",null).body());}
    public static void main(String[] args)throws Exception{
        String url=System.getenv("DB_URL");
        check(url!=null && url.matches("jdbc:mysql://127[.]0[.]0[.]1:13367/role_test_[0-9a-f]{32}"),"scratch URL");
        var factory=ConnectionFactory.fromEnvironment();
        try(var c=factory.open();var s=c.createStatement();var r=s.executeQuery("SELECT DATABASE(),@@port,(SELECT COUNT(*) FROM users),(SELECT COUNT(*) FROM roles),(SELECT COUNT(*) FROM exams)")){
            check(r.next() && url.endsWith("/"+r.getString(1)) && r.getInt(2)==13367 && r.getInt(3)==0 && r.getInt(4)==0 && r.getInt(5)==0,"empty scratch server");
        }
        try(var c=factory.open()){
            sql(c,"INSERT INTO roles(id,role_code,role_name) VALUES(1,'ADMIN','Admin'),(2,'TEACHER','Teacher'),(3,'STUDENT','Student')");
            String hash=vn.edu.onlineexam.util.PasswordHasher.hash(VerifyRoles.password.toCharArray());
            int id=0;
            for(String email:List.of("admin@attempt.invalid","teacher@attempt.invalid","student@subject.invalid","extra@enrollment.invalid")){
                id++;sql(c,"INSERT INTO users(id,full_name,email,password_hash) VALUES(?,?,?,?)",id,"Fixture "+id,email,hash);
                sql(c,"INSERT INTO user_roles(user_id,role_id,assigned_at) VALUES(?,?,UTC_TIMESTAMP())",id,Math.min(id,3));
            }
            sql(c,"INSERT INTO subjects(id,subject_code,subject_name) VALUES(1,'A5','Attempt fixture')");
            sql(c,"INSERT INTO question_banks(id,subject_id,owner_id,bank_name) VALUES(1,1,2,'Fixture')");
            sql(c,"INSERT INTO questions(id,bank_id,author_id,question_type,difficulty,status) VALUES(1,1,2,'SINGLE_CHOICE','EASY','PUBLISHED')");
            sql(c,"INSERT INTO question_versions(id,question_id,version_no,content,explanation,default_points,created_by) VALUES(1,1,1,'Exam4-private-text','HIDDEN-KEY-5',2,2)");
            sql(c,"INSERT INTO question_version_options(question_version_id,option_content,is_correct,display_order) VALUES(1,'A',1,1),(1,'B',0,2)");
            sql(c,"INSERT INTO questions(id,bank_id,author_id,question_type,difficulty,status) VALUES(2,1,2,'MULTIPLE_CHOICE','MEDIUM','PUBLISHED')");
            sql(c,"INSERT INTO question_versions(id,question_id,version_no,content,explanation,default_points,created_by) VALUES(2,2,1,'Multi fixture','HIDDEN-KEY-5',3,2)");
            sql(c,"INSERT INTO question_version_options(question_version_id,option_content,is_correct,display_order) VALUES(2,'C',1,1),(2,'D',1,2),(2,'E',0,3)");
            sql(c,"INSERT INTO exams(subject_id,created_by,title,duration_minutes,max_attempts,pass_percentage) VALUES(1,2,'Exam4',20,2,50)");
        }
        AcceptAuth.base=java.net.URI.create("http://127.0.0.1:"+Integer.parseInt(args[0])+"/online-exam/");
        run(VerifyRoles.login("admin@attempt.invalid"),VerifyRoles.login("teacher@attempt.invalid"),VerifyRoles.login("student@subject.invalid"),2,3,factory);
    }
    static void status(AcceptAuth client,String body,int expected)throws Exception{
        var response=client.send(route,body);
        check(response.statusCode()==expected,"attempt POST expected "+expected+" got "+response.statusCode());
    }
    static long scalar(Connection c,String sql,long arg)throws SQLException{return VerifyEnrollments.scalar(c,sql,arg);}
    static void sql(Connection c,String sql,Object... args)throws SQLException{VerifyEnrollments.exec(c,sql,args);}
    static List<Integer> race(AcceptAuth first,String firstBody,AcceptAuth second,String secondBody)throws Exception{
        var gate=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{gate.await();return first.send(route,firstBody).statusCode();});
            var b=pool.submit(()->{gate.await();return second.send(route,secondBody).statusCode();});
            return List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
        }
    }
    static long field(String html,String name){
        var m=Pattern.compile("name=\""+name+"\" value=\"(\\d+)\"").matcher(html);
        check(m.find(),"missing "+name);return Long.parseLong(m.group(1));
    }
    static void fault(Connection c,String table,AcceptAuth client,String body)throws Exception{
        try(var s=c.createStatement()){
            s.execute("CREATE TRIGGER attempt_fixture_fail BEFORE INSERT ON "+table+" FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'");
            try{status(client,body,503);}finally{s.execute("DROP TRIGGER attempt_fixture_fail");}
        }
    }
    static void revokeDuringStart(Connection c,AcceptAuth student,String begin,String mutation,long id)throws Exception{
        c.setAutoCommit(false);
        try(var pool=Executors.newSingleThreadExecutor()){
            sql(c,mutation,id);
            var request=pool.submit(()->student.send(route,begin).statusCode());
            try{request.get(250,TimeUnit.MILLISECONDS);throw new AssertionError("request did not wait for eligibility lock");}
            catch(TimeoutException expected){c.commit();}
            check(request.get(20,TimeUnit.SECONDS)==403,"revocation wins");
        }finally{c.rollback();c.setAutoCommit(true);}
    }
    static void browser(long id,long question,long correct,long wrong,AcceptAuth existing,String mode)throws Exception{
        if(!"1".equals(System.getenv("ATTEMPT_BROWSER")))return;
        var process=new ProcessBuilder("node","scripts/verify-attempt-browser.mjs",mode).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try(var out=process.outputWriter()){
            String cookie=existing==null?"":",\"fixtureCookie\":\""+existing.id()+"\"";
            out.write("{\"base\":\""+AcceptAuth.base+"\",\"id\":"+id+",\"question\":"+question+",\"correct\":"+correct+",\"wrong\":"+wrong+",\"password\":\""+VerifyRoles.password+"\""+cookie+"}");
        }
        if(!process.waitFor(90,TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("browser timeout");}
        check(process.exitValue()==0,"live browser");
    }
    static AcceptAuth selection(Connection c,AcceptAuth teacher,AcceptAuth a,AcceptAuth parallel,AcceptAuth b,long student,long owner)throws Exception{
        String tc="csrf="+AcceptAuth.csrf(teacher.send("teacher/account",null).body());
        for(int i=100;i<150;i++){
            sql(c,"INSERT INTO questions(id,bank_id,author_id,question_type,difficulty,status) VALUES(?,1,2,'SINGLE_CHOICE','EASY','PUBLISHED')",i);
            sql(c,"INSERT INTO question_versions(id,question_id,version_no,content,explanation,default_points,created_by) VALUES(?,?,1,?,'KEY-5D',1,2)",i,i,"Source item "+i);
            sql(c,"INSERT INTO question_version_options(question_version_id,option_content,is_correct,display_order) VALUES(?,'A',1,1),(?,'B',0,2)",i,i);
        }
        for(boolean random:List.of(false,true)){
            String settings="&title=Selection&duration=30&attempts=2&pass=50&shuffleQuestions=on&shuffleOptions=on";
            long exam=VerifyQuestionCatalog.save(teacher,"teacher/exams",tc+"&action=create&subject=1"+settings);
            sql(c,"INSERT INTO exam_questions(exam_id,question_version_id,points,fixed_order) SELECT ?,id,1,id FROM question_versions WHERE id BETWEEN 100 AND ?",exam,random?148:138);
            int last=random?149:139;
            VerifyQuestionCatalog.save(teacher,"teacher/exams",VerifyExams.form(teacher,exam,tc)+"&action=add&version="+last+"&position="+last+"&points=1");
            if(random){
                String edit=VerifyExams.form(teacher,exam,tc)+"&action=edit"+settings+"&mode=RANDOM_PER_ATTEMPT&count=";
                for(int n:List.of(0,51))check(teacher.send("teacher/exams",edit+n).statusCode()==400,"invalid draw size");
                sql(c,"UPDATE exam_questions SET points=2 WHERE exam_id=? AND fixed_order=100",exam);
                check(teacher.send("teacher/exams",edit+40).statusCode()==400,"mixed weights");
                sql(c,"UPDATE exam_questions SET points=1 WHERE exam_id=?",exam);
                VerifyQuestionCatalog.save(teacher,"teacher/exams",edit+40);
            }
            VerifyQuestionCatalog.save(teacher,"teacher/exams",VerifyExams.form(teacher,exam,tc)+"&action=PUBLISHED");
            for(long who:List.of(student,4L))sql(c,"INSERT INTO exam_student_assignments(exam_id,student_id,opens_at,closes_at,assigned_by) VALUES(?,?,UTC_TIMESTAMP()-INTERVAL 1 HOUR,UTC_TIMESTAMP()+INTERVAL 1 HOUR,?)",exam,who,owner);
            String begin="&action=start&exam="+exam+"&previous=0";
            if(random){fault(c,"attempt_questions",a,csrf(a)+begin);fault(c,"audit_logs",a,csrf(a)+begin);check(scalar(c,"SELECT COUNT(*) FROM exam_attempts WHERE exam_id=?",exam)==0,"draw rollback");}
            check(race(a,csrf(a)+begin,parallel,csrf(parallel)+begin).equals(List.of(303,303)),"single draw for concurrent starts");
            status(b,csrf(b)+begin,303);
            for(var client:List.of(a,b)){
                long who=client==a?student:4L;
                long id=scalar(c,"SELECT id FROM exam_attempts WHERE student_id="+who+" AND exam_id=?",exam);
                check(scalar(c,"SELECT COUNT(DISTINCT v.question_id) FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id JOIN question_versions v ON v.id=x.question_version_id WHERE x.exam_id="+exam+" AND v.id BETWEEN 100 AND "+(random?149:139)+" AND aq.attempt_id=?",id)==40,"distinct source members");
                String page=route+"?action=take&id="+id;
                var html=client.send(page,null).body();
                check(html.split("Source item ",-1).length==41 && !html.contains("KEY-5D"),"student sees only assigned questions");
                var pattern=Pattern.compile("name=\\\"(?:option|question)\\\" value=\\\"[0-9]+\\\"");
                check(pattern.matcher(html).results().map(m->m.group()).toList().equals(pattern.matcher(client.send(page,null).body()).results().map(m->m.group()).toList()),"durable order");
                long qid=scalar(c,"SELECT MIN(id) FROM attempt_questions WHERE attempt_id=?",id);
                long key=scalar(c,"SELECT ord.option_id FROM attempt_option_orders ord JOIN question_version_options o ON o.id=ord.option_id WHERE ord.attempt_question_id=? AND o.is_correct=1",qid);
                String save=csrf(client)+"&action=save&id="+id+"&question="+qid+"&revision=0&option=";
                status(client,save+key,303);
                if(random){
                    long missing=scalar(c,"SELECT MIN(o.id) FROM question_version_options o JOIN exam_questions x ON x.question_version_id=o.question_version_id WHERE x.exam_id="+exam+" AND NOT EXISTS(SELECT 1 FROM attempt_questions aq WHERE aq.attempt_id=? AND aq.exam_question_id=x.id)",id);
                    status(client,save+missing,400);
                    if(client==a){
                        sql(c,"UPDATE exams SET policy_snapshot_json=JSON_SET(policy_snapshot_json,'$.warn_on_focus_loss',1,'$.detect_paste',1,'$.require_fullscreen',1) WHERE id=?",exam);
                        long wrong=scalar(c,"SELECT ord.option_id FROM attempt_option_orders ord JOIN question_version_options o ON o.id=ord.option_id WHERE ord.attempt_question_id=? AND o.is_correct=0",qid);
                        browser(id,qid,key,wrong,null,"--paper");
                        if("1".equals(System.getenv("ATTEMPT_BROWSER"))){
                            check(scalar(c,"SELECT COUNT(*) FROM attempt_sessions WHERE attempt_id=?",id)==2,"two profiles, shared tabs");
                            check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE attempt_id=? AND event_code='CONCURRENT_SESSION_DETECTED'",id)==1,"browser overlap recorded once");
                            for(String code:List.of("TAB_HIDDEN","WINDOW_BLUR","FULLSCREEN_EXIT","COPY_ATTEMPT","PASTE_ATTEMPT"))
                                check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='"+code+"' AND attempt_id=?",id)>0,"browser event "+code);
                        }
                        check(parallel.send("logout",csrf(parallel)).statusCode()==302,"logout during draw");
                        parallel=VerifyRoles.login("student@subject.invalid");
                        status(parallel,csrf(parallel)+begin,303);
                        check(pattern.matcher(html).results().map(m->m.group()).toList().equals(pattern.matcher(parallel.send(page,null).body()).results().map(m->m.group()).toList()),"resume same subset after login");
                    }
                }
                if(random && client==b){
                    browser(id,qid,key,0,b,"--slow-submit");
                    if("1".equals(System.getenv("ATTEMPT_BROWSER")))check(scalar(c,"SELECT status='SUBMITTED' FROM exam_attempts WHERE id=?",id)==1,"browser submit during slow heartbeat");
                }
                status(client,csrf(client)+"&action=submit&id="+id,303);
                status(client,csrf(client)+"&action=submit&id="+id,303);
                check(scalar(c,"SELECT max_score=40 AND total_score=1 FROM exam_attempts WHERE id=?",id)==1,"grade only sampled rows");
                if(client==a){
                    status(client,csrf(client)+"&action=start&exam="+exam+"&previous="+id,303);
                    long retry=scalar(c,"SELECT MAX(id) FROM exam_attempts WHERE exam_id=?",exam);
                    check(retry!=id && scalar(c,"SELECT COUNT(DISTINCT exam_question_id) FROM attempt_questions WHERE attempt_id=?",retry)==40,"new attempt samples once");
                    status(client,csrf(client)+"&action=submit&id="+retry,303);
                }
            }
            System.out.println("PASS "+(random?"RANDOM_PER_ATTEMPT":"FIXED")+" 40/50 two students, snapshot and grading");
        }
        return parallel;
    }
    static final String heartbeat="student/proctoring/heartbeat";
    static void contacts(Connection c,long id,AcceptAuth first,String token,AcceptAuth second,String other,AcceptAuth outsider)throws Exception {
        String body="&attempt="+id;
        check(first.send(heartbeat,null).statusCode()==405,"heartbeat POST only");
        check(first.send(heartbeat,body).statusCode()==403,"heartbeat CSRF");
        check(first.send(heartbeat,token+body+"&session_id=1").statusCode()==400,"client session rejected");
        check(outsider.send(heartbeat,csrf(outsider)+body).statusCode()==404,"heartbeat ownership");
        long deadline=scalar(c,"SELECT UNIX_TIMESTAMP(deadline_at)*1000 FROM exam_attempts WHERE id=?",id);
        long lastLogin=scalar(c,"SELECT MAX(UNIX_TIMESTAMP(last_seen_at)*1000) FROM login_sessions WHERE user_id=?",3);
        check(first.send(heartbeat,token+body).statusCode()==204,"first heartbeat");
        try(var pool=Executors.newFixedThreadPool(2)){
            var gate=new CyclicBarrier(2);
            var a=pool.submit(()->{gate.await();return second.send(heartbeat,other+body).statusCode();});
            var b=pool.submit(()->{gate.await();return second.send(heartbeat,other+body).statusCode();});
            check(a.get()==204 && b.get()==204,"concurrent heartbeat retries");
        }
        check(scalar(c,"SELECT COUNT(*) FROM attempt_sessions WHERE attempt_id=?",id)==2,"two login identities");
        check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='CONCURRENT_SESSION_DETECTED' AND attempt_id=?",id)==1,"one overlap");
        check(lastLogin==scalar(c,"SELECT MAX(UNIX_TIMESTAMP(last_seen_at)*1000) FROM login_sessions WHERE user_id=?",3),"idle clock unchanged");
        check(deadline==scalar(c,"SELECT UNIX_TIMESTAMP(deadline_at)*1000 FROM exam_attempts WHERE id=?",id),"deadline unchanged");
        sql(c,"UPDATE attempt_sessions SET opened_at=UTC_TIMESTAMP(3)-INTERVAL 2 MINUTE,last_seen_at=UTC_TIMESTAMP(3)-INTERVAL 100 SECOND WHERE attempt_id=?",id);
        try(var s=c.createStatement()){
            s.execute("CREATE TRIGGER contact_fail BEFORE INSERT ON proctoring_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'");
            try{check(first.send(heartbeat,token+body).statusCode()==503,"contact rollback");}finally{s.execute("DROP TRIGGER contact_fail");}
        }
        check(scalar(c,"SELECT COUNT(*) FROM attempt_sessions WHERE last_seen_at>UTC_TIMESTAMP()-INTERVAL 90 SECOND AND attempt_id=?",id)==0,"failed contact not committed");
        var service=new vn.edu.onlineexam.service.AttemptContactService(new vn.edu.onlineexam.service.JdbcUnitOfWork(ConnectionFactory.fromEnvironment()));
        service.inspect();service.inspect();
        check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='HEARTBEAT_TIMEOUT' AND attempt_id=?",id)==2,"timeout once per session");
        check(first.send(heartbeat,token+body).statusCode()==204 && first.send(heartbeat,token+body).statusCode()==204,"resumed contact");
        check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='CONNECTION_RESTORED' AND detail_json->>'$.source'='SERVER' AND attempt_id=?",id)==1,"single server restoration");
        check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE attempt_id=? AND attempt_session_id IS NOT NULL AND (JSON_LENGTH(detail_json)<>1 OR detail_json->>'$.source' NOT IN ('SERVER','DERIVED') OR client_event_id IS NOT NULL OR client_occurred_at IS NOT NULL)",id)==0,"minimal backend metadata");
        System.out.println("PASS contact identity, ownership, CSRF, rollback, concurrency, timeout, recovery and clocks");
    }
    static void events(Connection c,long id,AcceptAuth student,AcceptAuth second,AcceptAuth outsider,AcceptAuth teacher,AcceptAuth admin)throws Exception{
        String route="student/proctoring/events",key=UUID.randomUUID().toString(),token=csrf(student);
        String form="&attempt="+id+"&code=CONNECTION_RESTORED&key="+key;
        check(student.send(route,form).statusCode()==403 && student.send(route,"csrf=x"+form).statusCode()==403,"event CSRF");
        check(new AcceptAuth().send(route,form).statusCode()==403,"event guest");
        check(outsider.send(route,csrf(outsider)+form).statusCode()==404,"event ownership");
        check(student.send(route,token+form+"&severity=HIGH").statusCode()==400,"event strict fields");
        check(student.send(route,token+form+"&x="+"a".repeat(1200)).statusCode()==413,"event size");
        check(student.send(route,token+form.replace("CONNECTION_RESTORED","UNKNOWN")).statusCode()==400,"event code");
        check(student.send(route,token).statusCode()==400 && student.send(route,token+form.replace(key,"invalid")).statusCode()==400,"missing event and invalid UUID");
        check(student.send(route,token+form.replace("attempt="+id,"attempt=90000000")).statusCode()==404,"missing attempt");
        check(teacher.send("teacher/proctoring/events?sort=bad",null).statusCode()==400,"order whitelist");
        check(student.send(route,token+form.replace("CONNECTION_RESTORED","PASTE_ATTEMPT")).statusCode()==409,"snapshot disables paste signal");
        check(student.send(route,token+form+"&occurred=bad").statusCode()==400,"client UTC syntax");
        try(var st=c.createStatement()){
            st.execute("CREATE TRIGGER event_fixture BEFORE INSERT ON proctoring_events FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'");
            try{check(student.send(route,token+form).statusCode()==503,"event write failure");}finally{st.execute("DROP TRIGGER event_fixture");}
        }
        check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE attempt_id=?",id)==0,"event rollback");
        try(var pool=Executors.newFixedThreadPool(2)){
            var gate=new CyclicBarrier(2);String otherToken=csrf(second);
            var a=pool.submit(()->{gate.await();return student.send(route,token+form).statusCode();});
            var b=pool.submit(()->{gate.await();return second.send(route,otherToken+form).statusCode();});
            check(Set.of(a.get(),b.get()).equals(Set.of(201,204)),"parallel duplicate");
        }
        check(student.send(route,token+form).statusCode()==204,"event retry");
        check(student.send(route,token+form.replace("RESTORED","LOST")).statusCode()==409,"event key reused");
        String query="/proctoring/events?attempt="+id+"&code=CONNECTION_RESTORED&size=1";
        var history=teacher.send("teacher"+query,null);
        check(history.statusCode()==200 && history.body().contains("1 sự kiện") && history.body().contains("BROWSER"),"teacher event history");
        VerifyExams.layout("events",history.body());
        check(admin.send("admin"+query,null).body().contains("0 sự kiện"),"admin scope");
        check(teacher.send("teacher"+query+"&page=2",null).body().contains("1 sự kiện") && !teacher.send("teacher"+query+"&page=2",null).body().contains("<article"),"scope count across pages");
        check(teacher.send("teacher"+query+"&from=2099-01-01T00:00:00Z",null).body().contains("0 sự kiện"),"server date filter");
        sql(c,"UPDATE exams SET policy_snapshot_json=JSON_SET(policy_snapshot_json,'$.detect_paste',1) WHERE id=(SELECT exam_id FROM exam_attempts WHERE id=?)",id);
        String enabled=token+form.replace(key,UUID.randomUUID().toString()).replace("CONNECTION_RESTORED","PASTE_ATTEMPT")+"&occurred=2020-01-01T00:00:00.123Z";
        check(student.send(route,enabled).statusCode()==201 && student.send(route,enabled).statusCode()==204,"enabled policy and client timestamp retry");
        sql(c,"UPDATE exams SET created_by=1 WHERE id=(SELECT exam_id FROM exam_attempts WHERE id=?)",id);
        try{check(teacher.send("teacher"+query,null).body().contains("0 sự kiện"),"foreign teacher exam");}
        finally{sql(c,"UPDATE exams SET created_by=2 WHERE id=(SELECT exam_id FROM exam_attempts WHERE id=?)",id);}
        check(student.send("teacher"+query,null).statusCode()==403,"history RBAC");
        for(int i=1;i<=29;i++)check(student.send(route,token+form.replace(key,UUID.randomUUID().toString())).statusCode()==(i==29?429:201),"shared DB quota");
        check(second.send(route,csrf(second)+form.replace(key,UUID.randomUUID().toString())).statusCode()==429,"quota shared by sessions");
        String repeat=csrf(second)+form;
        int response=204;
        for(int i=0;i<121 && response==204;i++)response=second.send(route,repeat).statusCode();
        check(response==429 && scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE attempt_id=?",id)==30,"duplicate traffic bounded");
        System.out.println("PASS event intake, policy, ownership, history, duplicate race, payload limits, database quota and rollback");
    }
    static void run(AcceptAuth admin,AcceptAuth teacher,AcceptAuth student,long teacherId,long studentId,ConnectionFactory factory)throws Exception{
        var second=VerifyRoles.login("student@subject.invalid");var outsider=VerifyRoles.login("extra@enrollment.invalid");
        String token=csrf(student),secondToken=csrf(second),other=csrf(outsider);
        try(var c=factory.open()){
            second=selection(c,teacher,student,second,outsider,studentId,teacherId);
            secondToken=csrf(second);
            sql(c,"INSERT INTO exams(subject_id,created_by,title,duration_minutes,max_attempts,pass_percentage,shuffle_questions,shuffle_options,result_release_mode,policy_snapshot_json,status) SELECT subject_id,created_by,'Attempt5',20,4,50,1,1,'IMMEDIATE',JSON_OBJECT(),'PUBLISHED' FROM exams WHERE title='Exam4' LIMIT 1");
            long exam=scalar(c,"SELECT id FROM exams WHERE title='Attempt5' AND created_by=?",teacherId);
            sql(c,"INSERT INTO exam_questions(exam_id,question_version_id,points,fixed_order) SELECT ?,v.id,2,1 FROM question_versions v WHERE content='Exam4-private-text' LIMIT 1",exam);
            sql(c,"INSERT INTO exam_questions(exam_id,question_version_id,points,fixed_order) VALUES(?,2,3,2)",exam);
            String begin="&action=start&exam="+exam+"&previous=0";
            status(student,token+begin,403);
            sql(c,"INSERT INTO exam_student_assignments(exam_id,student_id,opens_at,closes_at,max_attempts_override,assigned_by) VALUES(?,?,UTC_TIMESTAMP()+INTERVAL 1 HOUR,UTC_TIMESTAMP()+INTERVAL 2 HOUR,2,?)",exam,studentId,teacherId);
            status(student,token+begin,409);
            sql(c,"UPDATE exam_student_assignments SET opens_at=UTC_TIMESTAMP()-INTERVAL 1 HOUR WHERE exam_id=?",exam);
            revokeDuringStart(c,student,token+begin,"DELETE FROM exam_student_assignments WHERE exam_id=?",exam);
            sql(c,"INSERT INTO classes(id,class_code,class_name,academic_year) VALUES(1,'A','A','2026-2027')");
            sql(c,"INSERT INTO course_offerings(id,subject_id,class_id,teacher_id,academic_year,semester) VALUES(1,1,1,2,'2026-2027','1')");
            sql(c,"INSERT INTO course_enrollments(offering_id,student_id,enrolled_at) VALUES(1,?,UTC_TIMESTAMP())",studentId);
            sql(c,"INSERT INTO exam_course_assignments(exam_id,offering_id,assigned_by,opens_at,closes_at) VALUES(?,1,2,UTC_TIMESTAMP()-INTERVAL 1 HOUR,UTC_TIMESTAMP()+INTERVAL 1 HOUR)",exam);
            revokeDuringStart(c,student,token+begin,"UPDATE course_enrollments SET status='WITHDRAWN' WHERE student_id=?",studentId);
            sql(c,"UPDATE course_enrollments SET status='ENROLLED' WHERE student_id=?",studentId);
            sql(c,"INSERT INTO exam_student_assignments(exam_id,student_id,opens_at,closes_at,max_attempts_override,assigned_by) VALUES(?,?,UTC_TIMESTAMP()-INTERVAL 1 HOUR,UTC_TIMESTAMP()+INTERVAL 1 HOUR,2,?)",exam,studentId,teacherId);
            status(student,token+begin,409);
            sql(c,"DELETE FROM exam_course_assignments WHERE exam_id=?",exam);
            sql(c,"UPDATE exams SET status='DRAFT' WHERE id=?",exam);
            status(student,token+begin,409);
            sql(c,"UPDATE exams SET status='PUBLISHED',grading_policy='PARTIAL_CREDIT' WHERE id=?",exam);
            status(student,token+begin,409);
            sql(c,"UPDATE exams SET grading_policy='ALL_OR_NOTHING' WHERE id=?",exam);
            var preview=student.send(route+"?action=start&exam="+exam,null);
            check(preview.statusCode()==200 && preview.body().contains("Bắt đầu lượt thi"),"admission page");
            VerifyExams.layout("attempt-start",preview.body());
            fault(c,"audit_logs",student,token+begin);
            fault(c,"attempt_option_orders",student,token+begin);
            check(scalar(c,"SELECT COUNT(*) FROM exam_attempts WHERE exam_id=?",exam)==0,"atomic start");
            check(race(student,token+begin,second,secondToken+begin).equals(List.of(303,303)),"simultaneous start and resume");
            check(scalar(c,"SELECT COUNT(*) FROM exam_attempts WHERE exam_id=?",exam)==1,"one attempt");
            long id=scalar(c,"SELECT id FROM exam_attempts WHERE exam_id=?",exam);
            events(c,id,student,second,outsider,teacher,admin);
            String take=route+"?action=take&id="+id,submit="&action=submit&id="+id;
            var paper=student.send(take,null);
            sql(c,"UPDATE exam_student_assignments SET opens_at=UTC_TIMESTAMP()+INTERVAL 5 MINUTE WHERE exam_id=?",exam);
            check(student.send(take,null).statusCode()==403,"eligibility rechecked after start");
            sql(c,"UPDATE exam_student_assignments SET opens_at=UTC_TIMESTAMP()-INTERVAL 1 HOUR WHERE exam_id=?",exam);
            long grant=scalar(c,"SELECT id FROM exam_student_assignments WHERE exam_id=?",exam);
            sql(c,"UPDATE exam_student_assignments SET id=id+10000 WHERE exam_id=?",exam);
            check(student.send(take,null).statusCode()==409,"replacement grant cannot take over existing attempt");
            sql(c,"UPDATE exam_student_assignments SET id=? WHERE exam_id=?",grant,exam);
            check(paper.statusCode()==200 && paper.body().contains("Exam4-private-text") && !paper.body().contains("HIDDEN-KEY-5") && !paper.body().contains("is_correct") && !paper.body().contains("explanation"),"student projection");
            VerifyExams.layout("attempt-take",paper.body());
            var orderPattern=Pattern.compile("name=\"(?:option|question)\" value=\"[0-9]+\"");
            check(orderPattern.matcher(paper.body()).results().map(m->m.group()).toList().equals(
                orderPattern.matcher(student.send(take,null).body()).results().map(m->m.group()).toList()),"stable shuffled question and option order");
            check(second.send(take,null).body().replaceAll("name=\"csrf\" value=\"[^\"]+\"","").contains("Exam4-private-text"),"second session");
            long question=scalar(c,"SELECT aq.id FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id WHERE x.fixed_order=1 AND aq.attempt_id=?",id),correct=scalar(c,"SELECT o.id FROM attempt_option_orders ord JOIN question_version_options o ON o.id=ord.option_id WHERE ord.attempt_question_id=? AND o.is_correct=1",question);
            long multi=scalar(c,"SELECT aq.id FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id WHERE x.fixed_order=2 AND aq.attempt_id=?",id);
            long optC=scalar(c,"SELECT MIN(id) FROM question_version_options WHERE is_correct=1 AND question_version_id=?",2);
            long optD=scalar(c,"SELECT MAX(id) FROM question_version_options WHERE is_correct=1 AND question_version_id=?",2);
            long wrong=scalar(c,"SELECT o.id FROM attempt_option_orders ord JOIN question_version_options o ON o.id=ord.option_id WHERE ord.attempt_question_id=? AND o.is_correct=0",question);
            String save="&action=save&id="+id+"&question="+question+"&revision=0&option=";
            check(outsider.send(take,null).statusCode()==404,"other student read");
            status(outsider,other+save+correct,404);status(outsider,other+submit,404);status(student,"csrf=bad"+submit,403);
            status(student,token+save+"999999999",400);status(student,token+save+optC,400);
            status(student,token+save.replace("question="+question,"question=999999999")+correct,404);
            status(student,token+save+correct,303);status(student,token+save+correct,303);
            String multiSave=save.replace("question="+question,"question="+multi);
            status(student,token+multiSave+optC+"&option="+optD,303);
            status(student,token+save+wrong,409);
            var restored=student.send(take,null).body();
            check(restored.contains("value=\""+correct+"\" checked") && field(restored,"revision")>0,"stored answer after reload");
            browser(id,question,correct,wrong,student,"--paper");
            VerifyExams.layout("attempt-confirm",student.send(route+"?action=confirm&id="+id,null).body());
            fault(c,"audit_logs",student,token+submit);
            fault(c,"attempt_answers",student,token+submit);
            check(scalar(c,"SELECT COUNT(*) FROM exam_attempts WHERE id=? AND status='IN_PROGRESS' AND graded_at IS NULL",id)==1,"submit rollback");
            check(race(student,token+submit,second,secondToken+submit).equals(List.of(303,303)),"concurrent submit");
            check(scalar(c,"SELECT total_score=5 AND max_score=5 AND passed AND percentage=100 FROM exam_attempts WHERE id=?",id)==1,"graded exact match");
            String signal="&code=CONNECTION_LOST&key="+UUID.randomUUID()+"&attempt=";
            String eventsRoute="student/proctoring/events";
            check(student.send(eventsRoute,token+signal+id).statusCode()==409,"submitted attempt rejects new signal");
            status(student,token+save+wrong,409);status(student,token+begin,409);
            var history=student.send(route,null);
            check(history.statusCode()==200 && history.body().contains("Kết quả được chọn"),"immediate results");
            VerifyExams.layout("attempt-history",history.body());
            var administrative=admin.send("admin/attempts",null);
            check(teacher.send("teacher/attempts",null).body().contains("Attempt5") && administrative.statusCode()==200 && !administrative.body().contains("Attempt5"),"staff duty scope");
            status(student,token+begin.replace("previous=0","previous="+id),303);
            long next=scalar(c,"SELECT MAX(id) FROM exam_attempts WHERE exam_id=?",exam);
            long nextMulti=scalar(c,"SELECT aq.id FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id WHERE x.fixed_order=2 AND aq.attempt_id=?",next);
            status(student,token+"&action=save&id="+next+"&question="+nextMulti+"&revision=0&option="+optC,303);
            long nextSingle=scalar(c,"SELECT aq.id FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id WHERE x.fixed_order=1 AND aq.attempt_id=?",next);
            status(student,token+"&action=save&id="+next+"&question="+nextSingle+"&revision=0&option="+wrong,303);
            sql(c,"UPDATE exam_attempts SET started_at=UTC_TIMESTAMP()-INTERVAL 2 MINUTE,deadline_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id=?",next);
            check(student.send(heartbeat,token+"&attempt="+next).statusCode()==409,"heartbeat after deadline");
            check(student.send(eventsRoute,token+signal+next).statusCode()==409,"expired attempt rejects new signal");
            status(student,token+save.replace("id="+id,"id="+next)+correct,409);
            status(student,token+"&action=timeout&id="+next,303);
            check(scalar(c,"SELECT total_score=0 AND status='AUTO_SUBMITTED' FROM exam_attempts WHERE id=?",next)==1,"unanswered expiry");
            for(String policy:List.of("FIRST","HIGHEST","LATEST")){
                sql(c,"UPDATE exams SET attempt_score_policy=? WHERE id=?",policy,exam);
                String html=student.send(route,null).body();
                var best=Arrays.stream(html.split("<article>")).filter(section->section.contains("Kết quả được chọn")).findFirst().orElseThrow();
                check(best.contains("Lượt "+(policy.equals("LATEST")?2:1)+" "),"score policy "+policy);
            }
            status(student,token+begin.replace("previous=0","previous="+next),409);
            sql(c,"UPDATE exam_attempts SET status='CANCELLED' WHERE id=?",next);
            check(student.send(route+"?action=start&exam="+exam,null).body().contains("Cần xác nhận cách tính lượt"),"cancelled blocker");
            sql(c,"UPDATE exams SET result_release_mode='MANUAL' WHERE id=?",exam);
            check(!student.send(route,null).body().contains("Kết quả được chọn"),"manual withheld");
            sql(c,"UPDATE exams SET result_release_mode='AFTER_CLOSE' WHERE id=?",exam);
            check(!student.send(route,null).body().contains("Kết quả được chọn"),"window still open");
            sql(c,"UPDATE exam_student_assignments SET opens_at=UTC_TIMESTAMP()-INTERVAL 2 HOUR,closes_at=UTC_TIMESTAMP()-INTERVAL 1 HOUR WHERE exam_id=?",exam);
            check(student.send(route,null).body().contains("Kết quả được chọn"),"after-close release");
            sql(c,"UPDATE exam_attempts SET status='AUTO_SUBMITTED' WHERE id=?",next);
            sql(c,"UPDATE exam_student_assignments SET closes_at=UTC_TIMESTAMP()+INTERVAL 5 MINUTE,max_attempts_override=4 WHERE exam_id=?",exam);
            check(race(student,token+begin.replace("previous=0","previous="+next),second,secondToken+begin.replace("previous=0","previous="+next)).equals(List.of(303,303)),"resume under limit");
            long third=scalar(c,"SELECT MAX(id) FROM exam_attempts WHERE exam_id=?",exam);
            check(scalar(c,"SELECT deadline_at=(SELECT closes_at FROM exam_student_assignments p WHERE p.exam_id=a.exam_id AND p.student_id=a.student_id) FROM exam_attempts a WHERE a.id=?",third)==1,"deadline capped by assignment");
            contacts(c,third,student,token,second,secondToken,outsider);
            check(teacher.send("teacher/proctoring/events?attempt="+third+"&source=DERIVED&code=HEARTBEAT_TIMEOUT",null).body().contains("2 sự kiện"),"teacher source filter");
            long thirdQ=scalar(c,"SELECT aq.id FROM attempt_questions aq JOIN exam_questions x ON x.id=aq.exam_question_id WHERE x.fixed_order=1 AND aq.attempt_id=?",third);
            List<Integer> competition;
            try(var pool=Executors.newFixedThreadPool(2)){
                var beat=pool.submit(()->student.send(heartbeat,token+"&attempt="+third).statusCode());
                var reporting=pool.submit(()->student.send(eventsRoute,token+signal+third).statusCode());
                competition=race(student,token+"&action=save&id="+third+"&question="+thirdQ+"&revision=0&option="+correct,second,secondToken+"&action=submit&id="+third);
                check(Set.of(204,409).contains(beat.get(20,TimeUnit.SECONDS)),"heartbeat vs submit");
                check(Set.of(201,409).contains(reporting.get(20,TimeUnit.SECONDS)),"event vs submit");
            }
            check(competition.get(1)==303 && Set.of(303,409).contains(competition.get(0)),"save/submit serialization");
            check(scalar(c,"SELECT total_score FROM exam_attempts WHERE id=?",third)==(competition.get(0)==303?2:0),"grade reflects winning write");
            check(student.send(heartbeat,token+"&attempt="+third).statusCode()==409,"heartbeat after submit");
            String last=begin.replace("previous=0","previous="+third);
            check(race(student,token+last,second,secondToken+last).equals(List.of(303,303)),"one slot remains");
            long fourth=scalar(c,"SELECT MAX(id) FROM exam_attempts WHERE exam_id=?",exam);
            String fourthPage=route+"?action=take&id="+fourth;
            c.setAutoCommit(false);
            try(var pool=Executors.newSingleThreadExecutor()){
                sql(c,"UPDATE login_sessions SET revoked_at=UTC_TIMESTAMP(3) WHERE user_id=? AND revoked_at IS NULL",4L);
                var pending=pool.submit(()->outsider.send(eventsRoute,other+signal+fourth).statusCode());
                try{pending.get(250,TimeUnit.MILLISECONDS);throw new AssertionError("event should wait on session lock");}
                catch(TimeoutException expected){c.commit();}
                check(pending.get(20,TimeUnit.SECONDS)==302,"revocation wins pending event");
            }finally{c.rollback();c.setAutoCommit(true);}
            check(student.send(heartbeat,token+"&attempt="+fourth).statusCode()==204,"owner contact for role change");
            check(second.send(heartbeat,secondToken+"&attempt="+fourth).statusCode()==204,"open watched session");
            c.setAutoCommit(false);
            try(var pool=Executors.newSingleThreadExecutor()){
                sql(c,"UPDATE login_sessions l JOIN attempt_sessions s ON s.session_token_hash=SHA2(CONCAT(s.attempt_id,':',l.session_hash),256) SET l.revoked_at=UTC_TIMESTAMP(3) WHERE s.attempt_id=? AND s.id=(SELECT watched.id FROM (SELECT MAX(id) id FROM attempt_sessions WHERE attempt_id=?) watched)",fourth,fourth);
                var sessionClient=second;String sessionCsrf=secondToken;
                var pending=pool.submit(()->sessionClient.send(heartbeat,sessionCsrf+"&attempt="+fourth).statusCode());
                try{pending.get(250,TimeUnit.MILLISECONDS);throw new AssertionError("heartbeat did not wait for session");}
                catch(TimeoutException waiting){c.commit();}
                check(pending.get(20,TimeUnit.SECONDS)==401,"revocation wins heartbeat");
            }finally{c.rollback();c.setAutoCommit(true);}
            new vn.edu.onlineexam.service.AttemptContactService(new vn.edu.onlineexam.service.JdbcUnitOfWork(factory)).inspect();
            check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='SESSION_REVOKED' AND attempt_id=?",fourth)==1,"server revocation event");
            check(second.send("logout",secondToken).statusCode()==302 && second.send(fourthPage,null).statusCode()==302,"logout during paper");
            sql(c,"DELETE FROM user_roles WHERE role_id=3 AND user_id=?",studentId);
            new vn.edu.onlineexam.service.AttemptContactService(new vn.edu.onlineexam.service.JdbcUnitOfWork(factory)).inspect();
            check(scalar(c,"SELECT COUNT(*) FROM proctoring_events WHERE event_code='SESSION_ACCESS_CHANGED' AND attempt_id=?",fourth)==1,"role change event");
            check(student.send(heartbeat,token+"&attempt="+fourth).statusCode()==403,"heartbeat role revoked");
            check(student.send(eventsRoute,token+signal+fourth).statusCode()==302,"revoked role cannot report");
            check(student.send(fourthPage,null).statusCode()==302,"revoked last role invalidates session");
            sql(c,"INSERT INTO user_roles(user_id,role_id,assigned_at) VALUES(?,3,UTC_TIMESTAMP())",studentId);
            var restoredStudent=VerifyRoles.login("student@subject.invalid");
            String reportingToken=csrf(restoredStudent);
            sql(c,"UPDATE login_sessions SET created_at=UTC_TIMESTAMP()-INTERVAL 1 HOUR,last_seen_at=UTC_TIMESTAMP()-INTERVAL 31 MINUTE WHERE user_id=? AND revoked_at IS NULL",studentId);
            check(restoredStudent.send(heartbeat,reportingToken+"&attempt="+fourth).statusCode()==401,"idle heartbeat rejected");
            check(restoredStudent.send(eventsRoute,reportingToken+signal+fourth).statusCode()==302,"expired session cannot report");
            check(restoredStudent.send(fourthPage,null).statusCode()==302,"idle session expires");
            restoredStudent=VerifyRoles.login("student@subject.invalid");
            reportingToken=csrf(restoredStudent);
            sql(c,"UPDATE users SET status='LOCKED' WHERE id=?",studentId);
            check(restoredStudent.send(heartbeat,reportingToken+"&attempt="+fourth).statusCode()==401,"heartbeat locked");
            check(restoredStudent.send(eventsRoute,reportingToken+signal+fourth).statusCode()==302,"locked account cannot report");
            check(restoredStudent.send(fourthPage,null).statusCode()==302,"locked student");
            sql(c,"UPDATE exam_attempts SET started_at=UTC_TIMESTAMP()-INTERVAL 2 MINUTE,deadline_at=UTC_TIMESTAMP()-INTERVAL 1 SECOND WHERE id=?",fourth);
            for(int i=0;i<45 && scalar(c,"SELECT graded_at IS NULL FROM exam_attempts WHERE id=?",fourth)==1;i++)Thread.sleep(500);
            check(scalar(c,"SELECT status='AUTO_SUBMITTED' AND total_score=0 FROM exam_attempts WHERE id=?",fourth)==1,"disconnected deadline worker");
        }
        System.out.println("PASS attempts: admission, independent sessions, snapshot, save/retry, ownership, deadlines, grading, result visibility and rollback");
    }
}
