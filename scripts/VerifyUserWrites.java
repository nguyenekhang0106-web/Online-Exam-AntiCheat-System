import java.lang.reflect.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.model.UserDirectory.Profile;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.PasswordHasher;

/** Statement-level JDBC double; no driver, credentials or database required. */
class VerifyUserWrites {
    static final AuthUser ADMIN=new AuthUser(7,Set.of("ADMIN"));
    static final Profile PROFILE=new Profile("O'Neil <test>","person@example.invalid","TEST_1",null);
    static final LocalDateTime VERSION=LocalDateTime.of(2026,1,1,0,0);
    final char[] secret=UUID.randomUUID().toString().toCharArray();
    boolean exists=true,duplicate,race,missingRole,committed,rolledBack;
    int audit;
    String hash;
    Map<Integer,Object> update;

    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    interface Operation { void run() throws Exception; }
    static void expect(Class<? extends Exception> type,Operation action) throws Exception {
        try { action.run(); } catch(Exception e) { if(type.isInstance(e)) return; throw e; }
        throw new AssertionError("Expected "+type.getSimpleName());
    }
    static <T> T mock(Class<T> type,InvocationHandler calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},calls));
    }
    Connection connection() {
        return mock(Connection.class,(p,m,a)->switch(m.getName()) {
            case "prepareStatement" -> statement((String)a[0]);
            case "commit" -> committed=true;
            case "rollback" -> rolledBack=true;
            case "close","setAutoCommit","setReadOnly" -> null;
            default -> throw new AssertionError(m);
        });
    }
    ResultSet row(boolean present) {
        return mock(ResultSet.class,(p,m,a)->switch(m.getName()) {
            case "next" -> present;
            case "getString" -> switch((int)a[0]) { case 1 -> PROFILE.name(); case 2 -> PROFILE.email(); default -> null; };
            case "getObject" -> VERSION;
            case "getBoolean" -> false;
            case "getLong" -> 88L;
            case "close" -> null;
            default -> throw new AssertionError(m);
        });
    }
    PreparedStatement statement(String sql) {
        check(!sql.contains(PROFILE.name()) && !sql.contains(PROFILE.email()));
        Map<Integer,Object> args=new HashMap<>();
        return mock(PreparedStatement.class,(p,m,a)->switch(m.getName()) {
            case "setLong","setString" -> args.put((int)a[0],a[1]);
            case "executeQuery" -> row(sql.startsWith("SELECT 1")?duplicate:exists);
            case "getGeneratedKeys" -> row(true);
            case "executeUpdate" -> {
                if(sql.startsWith("INSERT INTO users(")) {
                    if(race) throw new SQLException("duplicate","23000",1062);
                    check(PROFILE.name().equals(args.get(1)));
                    hash=(String)args.get(5);
                } else if(sql.startsWith("UPDATE users")) {
                    for(String forbidden:List.of("password_hash","last_login_at","created_at","status","role_id"))
                        check(!sql.contains(forbidden));
                    update=args;
                } else if(sql.startsWith("INSERT INTO user_roles")) {
                    check(args.get(2).equals(ADMIN.id()) && args.get(3).equals("STUDENT"));
                    if(missingRole) yield 0;
                } else if(sql.startsWith("INSERT INTO audit_logs")) audit++;
                else throw new AssertionError("Unexpected write");
                yield 1;
            }
            case "close" -> null;
            default -> throw new AssertionError(m);
        });
    }
    void run() throws Exception {
        var service=new UserManagementService(new JdbcUnitOfWork(this::connection));
        Operation create=()->service.create(ADMIN,PROFILE,secret,secret,"STUDENT");
        create.run(); check(committed && audit==2 && PasswordHasher.verify(secret,hash));
        String original=hash;
        service.update(ADMIN,88,PROFILE,VERSION.toString());
        check(update.get(5).equals(88L) && hash.equals(original) && audit==3);
        duplicate=true;
        expect(UserManagementService.Conflict.class,create);
        expect(UserManagementService.Conflict.class,()->service.update(ADMIN,88,PROFILE,VERSION.toString()));
        duplicate=false; race=true; rolledBack=false;
        expect(UserManagementService.Conflict.class,create); check(rolledBack);
        race=false; missingRole=true; rolledBack=false;
        expect(UserManagementService.Conflict.class,create); check(rolledBack);
        expect(UserManagementService.Conflict.class,()->service.update(ADMIN,88,PROFILE,"stale"));
        exists=false;
        expect(NoSuchElementException.class,()->service.update(ADMIN,99,PROFILE,VERSION.toString()));
        for(String role:List.of("TEACHER","STUDENT")) {
            var actor=new AuthUser(1,Set.of(role));
            expect(SecurityException.class,()->service.create(actor,PROFILE,secret,secret,"STUDENT"));
            expect(SecurityException.class,()->service.update(actor,88,PROFILE,VERSION.toString()));
        }
        for(Profile invalid:List.of(new Profile("","a@example.org",null,null),new Profile("Name","bad",null,null)))
            expect(IllegalArgumentException.class,()->service.create(ADMIN,invalid,secret,secret,"STUDENT"));
        expect(IllegalArgumentException.class,()->service.create(ADMIN,PROFILE,new char[1],new char[1],"STUDENT"));
        expect(IllegalArgumentException.class,()->service.create(ADMIN,PROFILE,secret,new char[1],"STUDENT"));
        expect(IllegalArgumentException.class,()->service.create(ADMIN,PROFILE,secret,secret,""));
        System.out.println("PASS isolated create/update, hashing, validation, authorization, binding, conflicts and rollback");
    }
    public static void main(String[] args) throws Exception { new VerifyUserWrites().run(); }
}
