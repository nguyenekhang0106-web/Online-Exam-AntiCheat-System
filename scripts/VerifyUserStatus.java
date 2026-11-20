import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.UserDirectory.Profile;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Destructive fixtures allowed only in the dedicated loopback scratch instance. */
class VerifyUserStatus {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    interface Action { void run() throws Exception; }
    static void expect(Class<?> kind,Action work) throws Exception {
        try { work.run(); } catch(Exception ex) { if(kind.isInstance(ex)) return; throw ex; }
        throw new AssertionError("Expected "+kind.getSimpleName());
    }
    public static void main(String[] args) throws Exception {
        check(System.getenv("DB_URL").matches("jdbc:mysql://127[.]0[.]0[.]1:13367/status_test(?:_[0-9a-f]{32})?"));
        ConnectionFactory factory=ConnectionFactory.fromEnvironment();
        var jdbc=new JdbcUnitOfWork(factory);
        var users=new UserManagementService(jdbc);
        var auth=new AuthenticationService(jdbc,new AuthDao());
        char[] password=UUID.randomUUID().toString().toCharArray();
        try(var c=factory.open(); var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT DATABASE(), @@port, (SELECT COUNT(*) FROM users)")) {
                r.next(); check(r.getString(1).matches("status_test(?:_[0-9a-f]{32})?") && r.getInt(2)==13367 && r.getInt(3)==0);
            }
            s.executeUpdate("INSERT INTO roles(role_code,role_name) VALUES ('ADMIN','Admin'),('TEACHER','Teacher'),('STUDENT','Student')");
            try(var p=c.prepareStatement("INSERT INTO users(id,full_name,email,password_hash) VALUES(1,'First','first@example.invalid',?)")) {
                p.setString(1,vn.edu.onlineexam.util.PasswordHasher.hash(password)); p.executeUpdate();
            }
            s.executeUpdate("INSERT INTO user_roles(user_id,role_id,assigned_at) SELECT 1,id,UTC_TIMESTAMP(3) FROM roles WHERE role_code='ADMIN'");
        }
        var admin=new AuthUser(1,Set.of("ADMIN"));
        expect(UserManagementService.Conflict.class,()->users.changeStatus(admin,1,true,users.detail(admin,1).version().toString()));
        long second=users.create(admin,new Profile("Second","second@example.invalid","SECOND",null),password,password,"ADMIN");
        long student=users.create(admin,new Profile("Student","student@example.invalid","STUDENT",null),password,password,"STUDENT");
        var other=new AuthUser(second,Set.of("ADMIN"));
        String token="a".repeat(64),fresh="b".repeat(64);
        check(auth.login("student@example.invalid",password,token)!=null && auth.current(token)!=null);
        var profile=users.detail(admin,student);
        String initial=profile.version().toString();
        for(String role:List.of("TEACHER","STUDENT"))
            expect(SecurityException.class,()->users.changeStatus(new AuthUser(student,Set.of(role)),student,true,initial));
        expect(UserManagementService.Conflict.class,()->users.changeStatus(admin,1,true,initial));
        expect(UserManagementService.Conflict.class,()->users.changeStatus(admin,student,false,initial));
        for(boolean lock:List.of(true,false))
            expect(NoSuchElementException.class,()->users.changeStatus(admin,9999,lock,initial));
        expect(IllegalArgumentException.class,()->users.changeStatus(admin,0,true,initial));
        for(String prefix:List.of("UPDATE login_sessions","INSERT INTO audit_logs")) {
            var failing=new UserManagementService(new JdbcUnitOfWork(()->{
                Connection real=factory.open();
                return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                    if(m.getName().equals("prepareStatement") && a[0].toString().startsWith(prefix)) throw new SQLException("Injected failure");
                    try {return m.invoke(real,a);} catch(InvocationTargetException ex){throw ex.getCause();}
                });
            }));
            expect(SQLException.class,()->failing.changeStatus(admin,student,true,initial));
            check(auth.current(token)!=null && initial.equals(users.detail(admin,student).version().toString()));
        }
        users.changeStatus(admin,student,true,initial);
        check(auth.current(token)==null && auth.login("student@example.invalid",password,fresh)==null);
        expect(UserManagementService.Conflict.class,()->users.changeStatus(admin,student,true,initial));
        expect(UserManagementService.Conflict.class,()->users.update(admin,student,profile.profile(),initial));
        expect(UserManagementService.Conflict.class,()->users.changeStatus(admin,student,false,initial));
        users.changeStatus(admin,student,false,users.detail(admin,student).version().toString());
        check(auth.current(token)==null && auth.login("student@example.invalid",password,fresh)!=null);
        auth.logout(fresh);
        String v1=users.detail(admin,1).version().toString(),v2=users.detail(admin,second).version().toString();
        var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Boolean>>();
            for(boolean first:List.of(true,false)) futures.add(pool.submit(()->{
                barrier.await();
                try { users.changeStatus(first?admin:other,first?second:1,true,first?v2:v1); return true; }
                catch(SecurityException denied){return false;}
            }));
            check(futures.get(0).get(30,TimeUnit.SECONDS)!=futures.get(1).get(30,TimeUnit.SECONDS));
        }
        try(var c=factory.open(); var s=c.createStatement(); var r=s.executeQuery("SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.status='ACTIVE' AND r.role_code='ADMIN'")) {
            r.next(); check(r.getLong(1)==1);
        }
        System.out.println("PASS isolated MySQL transitions, session lifecycle, authorization, stale updates, failure rollback and concurrent admins");
    }
}
