import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.UserDirectory.Profile;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.*;

// Requires a fresh loopback scratch schema with the five identity tables.
class VerifyRoles {
    static void check(boolean ok, String label) { AcceptAuth.require(ok, label); }
    static final String route = "admin/users/roles";
    static final String password = UUID.randomUUID().toString();
    static AcceptAuth login(String email) throws Exception {
        var client = new AcceptAuth();
        var csrf = AcceptAuth.csrf(client.send("login", null).body());
        check(client.send("login", AcceptAuth.form(email, password, csrf)).statusCode()==302, "login");
        return client;
    }
    static String ref() { return UUID.randomUUID().toString().replace("-", "").repeat(2); }
    public static void main(String[] args) throws Exception {
        String url = System.getenv("DB_URL");
        check(url != null && url.matches("jdbc:mysql://127[.]0[.]0[.]1:13367/role_test_[0-9a-f]{32}"), "scratch URL");
        var factory = ConnectionFactory.fromEnvironment();
        try (var c = factory.open(); var s = c.createStatement()) {
            try (var r = s.executeQuery("SELECT DATABASE(),@@port,(SELECT COUNT(*) FROM users),(SELECT COUNT(*) FROM roles)")) {
                check(r.next() && url.endsWith("/"+r.getString(1)) && r.getInt(2)==13367 && r.getLong(3)==0 && r.getLong(4)==0, "empty scratch");
            }
            s.executeUpdate("INSERT INTO roles(role_code,role_name) VALUES ('ADMIN','Admin'),('STUDENT','Student')");
            try (var ps = c.prepareStatement("INSERT INTO users(id,full_name,email,password_hash) VALUES(1,'Admin','admin@example.invalid',?)")) {
                ps.setString(1, PasswordHasher.hash(password.toCharArray())); ps.executeUpdate();
            }
            s.executeUpdate("INSERT INTO user_roles(user_id,role_id,assigned_at) SELECT 1,id,UTC_TIMESTAMP(3) FROM roles WHERE role_code='ADMIN'");
        }
        var jdbc = new JdbcUnitOfWork(factory);
        var auth = new AuthenticationService(jdbc,new AuthDao());
        var service = new UserManagementService(jdbc);
        var actor = new AuthUser(1,Set.of("ADMIN"));
        long target = service.create(actor,new Profile("<Student>","s@example.invalid","TARGET",null),
                password.toCharArray(),password.toCharArray(),"STUDENT");
        AcceptAuth.base = URI.create("http://127.0.0.1:"+Integer.parseInt(args[0])+"/online-exam/");
        var admin = login("admin@example.invalid");
        var student = login("s@example.invalid");
        String page = route+"?id="+target;
        check(student.send(page,null).statusCode()==403,"student denied");
        var view = admin.send(page+"&action=revoke&role=STUDENT",null);
        check(view.statusCode()==200 && view.body().contains("&lt;Student&gt;") && view.body().contains("STUDENT"),"escaped view");
        String csrf = AcceptAuth.csrf(view.body());
        String form = "csrf="+csrf+"&id="+target;
        check(admin.send(route,form+"&action=grant&role=TEACHER").statusCode()==404,"missing catalog entry");
        try (var c=factory.open(); var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO roles(role_code,role_name) VALUES ('TEACHER','Teacher')");
        }
        check(admin.send(route,form+"&action=revoke&role=TEACHER").statusCode()==409,"missing membership");
        check(admin.send(route,form+"&action=grant&role=STUDENT").statusCode()==409,"duplicate");
        check(admin.send(route,form+"&action=grant&role=UNKNOWN").statusCode()==400,"unknown code");
        check(admin.send(route,"csrf="+csrf+"&id=9999&action=grant&role=ADMIN").statusCode()==404,"missing user");
        check(admin.send(route,"csrf="+csrf+"&id=1&action=revoke&role=ADMIN&confirm=yes").statusCode()==409,"last active admin");
        var grant = admin.send(route,form+"&action=grant&role=TEACHER");
        check(grant.statusCode()==303 && admin.send("admin/users",null).body().contains("Đã cấp vai trò"),"grant and flash");
        check(student.send("student/account",null).statusCode()==302,"old session after grant");
        var logoutClient = login("s@example.invalid");
        String logoutToken = AcceptAuth.csrf(logoutClient.send("student/account",null).body());
        check(logoutClient.send("logout","csrf="+logoutToken).statusCode()==302 &&
                logoutClient.send("student/account",null).statusCode()==302,"logout");
        var teacher = login("s@example.invalid");
        check(teacher.send("teacher/account",null).statusCode()==200 && teacher.send("student/account",null).statusCode()==200
                && teacher.send(page,null).statusCode()==403,"multi-role");
        check(admin.send(route,form+"&action=grant&role=ADMIN").statusCode()==303,"grant admin");
        var second = login("s@example.invalid");
        check(second.send(page,null).statusCode()==200,"new admin session");
        check(admin.send(route,form+"&action=revoke&role=ADMIN").statusCode()==400,"confirmation");
        check(admin.send(route,"csrf="+csrf+"&id=1&action=revoke&role=ADMIN&confirm=yes").statusCode()==409,"self revoke");
        check(admin.send(route,form+"&action=revoke&role=ADMIN&confirm=yes").statusCode()==303,"revoke admin");
        check(second.send(page,null).statusCode()==302,"revoked admin session");

        String firstRef=ref(), secondRef=ref();
        auth.login("admin@example.invalid",password.toCharArray(),firstRef);
        for (String prefix : List.of("UPDATE login_sessions","INSERT INTO audit_logs")) {
            var broken=new UserManagementService(new JdbcUnitOfWork(()->{
                Connection real=factory.open();
                return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                    if(m.getName().equals("prepareStatement") && a[0].toString().startsWith(prefix)) throw new SQLException("Injected failure");
                    try {return m.invoke(real,a);} catch(java.lang.reflect.InvocationTargetException ex){throw ex.getCause();}
                });
            }));
            String active=ref();
            auth.login("s@example.invalid",password.toCharArray(),active);
            try { broken.changeRole(actor,firstRef,target,"TEACHER",false); throw new AssertionError("expected rollback"); }
            catch(SQLException expected) { check("Injected failure".equals(expected.getMessage()),"injected step"); }
            check(auth.current(active).roles().contains("TEACHER"),"rollback membership and session");
        }
        service.changeStatus(actor,target,true,service.detail(actor,target).version().toString());
        try {
            service.changeRole(new AuthUser(target,Set.of("ADMIN")),secondRef,1,"TEACHER",true);
            throw new AssertionError("locked actor allowed");
        } catch (SecurityException expected) { }
        service.changeRole(actor,firstRef,target,"ADMIN",true);
        check(auth.login("s@example.invalid",password.toCharArray(),ref())==null,"locked account");
        service.changeStatus(actor,target,false,service.detail(actor,target).version().toString());
        check(second.send(page,null).statusCode()==302,"old session stays revoked");
        var other=auth.login("s@example.invalid",password.toCharArray(),secondRef);
        long third=service.create(actor,new Profile("Third","third@example.invalid",null,null),
                password.toCharArray(),password.toCharArray(),"STUDENT");
        var simultaneous=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var writes=new ArrayList<Future<Boolean>>();
            for(boolean first:List.of(true,false)) writes.add(pool.submit(()->{
                simultaneous.await();
                try { service.changeRole(first?actor:other,first?firstRef:secondRef,third,"TEACHER",true); return true; }
                catch(UserManagementService.Conflict duplicate){return false;}
            }));
            check(writes.get(0).get(20,TimeUnit.SECONDS)!=writes.get(1).get(20,TimeUnit.SECONDS),"same grant serialized");
        }

        // Pause a request after its actor was authenticated, before the transaction takes the ADMIN lock.
        var started=new CountDownLatch(1);
        var resume=new CountDownLatch(1);
        var paused=new UserManagementService(new JdbcUnitOfWork(()->{
            started.countDown();
            try { if(!resume.await(10,TimeUnit.SECONDS)) throw new SQLException("fixture timeout"); }
            catch(InterruptedException e) {Thread.currentThread().interrupt(); throw new SQLException(e);}
            return factory.open();
        }));
        try(var pool=Executors.newSingleThreadExecutor()){
            var request=pool.submit(()->{
                try {paused.changeRole(other,secondRef,third,"TEACHER",false); return false;}
                catch(SecurityException denied){return true;}
            });
            try {
                check(started.await(10,TimeUnit.SECONDS),"pending actor request");
                service.changeStatus(actor,target,true,service.detail(actor,target).version().toString());
            } finally {resume.countDown();}
            check(request.get(10,TimeUnit.SECONDS),"actor locked while request pending");
        }
        service.changeStatus(actor,target,false,service.detail(actor,target).version().toString());
        String renewedRef=ref();
        auth.login("s@example.invalid",password.toCharArray(),renewedRef);
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var results=new ArrayList<Future<Boolean>>();
            for(boolean first:List.of(true,false)) results.add(pool.submit(()->{
                barrier.await();
                try { service.changeRole(first?actor:other,first?firstRef:renewedRef,first?target:1,"ADMIN",false); return true; }
                catch(SecurityException denied){return false;}
            }));
            check(results.get(0).get(20,TimeUnit.SECONDS)!=results.get(1).get(20,TimeUnit.SECONDS),"cross revoke");
        }
        try(var c=factory.open(); var s=c.createStatement(); var r=s.executeQuery("SELECT COUNT(*) FROM audit_logs WHERE action_code='ROLE_REVOKE_ADMIN'")){
            check(r.next() && r.getInt(1)==2,"audit successful revocations");
        }
        System.out.println("PASS isolated MySQL and HTTP: role management, CSRF-protected forms, session revocation, rollback, lock/unlock and concurrent cross-revocation");
    }
}
