import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.controller.AuthServlet;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.filter.AuthenticationFilter;
import vn.edu.onlineexam.model.bean.auth.User;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.PasswordHasher;

/** Standalone test with in-memory sessions, servlet doubles and an isolated DAO. */
class VerifyAuth {
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
    }
    static class Store extends AuthDao {
        User user=new User();
        Set<String> roles=Set.of("STUDENT");
        String live;
        public User byEmail(Connection c,String email) { return email.equals("student@example.invalid") ? user : null; }
        public Set<String> roles(Connection c,long id) { return roles; }
        public void start(Connection c,long id,String ref) { live=ref; }
        public Long active(Connection c,String ref) { return ref.equals(live) ? 1L : null; }
        public void touch(Connection c,String ref) {}
        public void revoke(Connection c,String ref) { check(ref.equals(live)); live=null; }
    }
    static class Controller extends AuthServlet {
        Controller(AuthenticationService service) { super(service); }
        void post(HttpServletRequest req,HttpServletResponse res) throws Exception { doPost(req,res); }
    }
    static class Guard extends AuthenticationFilter { Guard(AuthenticationService s) { super(s); } }

    public static void main(String[] args) throws Exception {
        Store db=new Store();
        var jdbc=new JdbcUnitOfWork(() -> stub(Connection.class,(p,m,a)->null));
        var auth=new AuthenticationService(jdbc,db);
        String email="student@example.invalid", password=UUID.randomUUID().toString();
        db.user.setId(1L); db.user.setPasswordHash(PasswordHasher.hash(password.toCharArray()));
        db.user.setStatus("ACTIVE");
        check(auth.login(email,new char[]{'x'},"unused")==null);
        check(auth.login("missing",password.toCharArray(),"unused")==null);
        for (String state:List.of("LOCKED","INACTIVE")) {
            db.user.setStatus(state); check(auth.login(email,password.toCharArray(),"unused")==null);
        }
        db.user.setStatus("ACTIVE"); db.roles=Set.of();
        check(auth.login(email,password.toCharArray(),"unused")==null && db.live==null);
        Map<String,Object> attrs=new HashMap<>();
        var session=stub(HttpSession.class,(p,m,a)->switch(m.getName()) {
            case "getAttribute" -> attrs.get(a[0]);
            case "setAttribute" -> attrs.put((String)a[0],a[1]);
            case "invalidate" -> {attrs.clear(); yield null;}
            default -> null;
        });
        String[] path={"/login"}, method={"POST"}, result={""};
        int[] rotations={0};
        var params=new HashMap<>(Map.of("email",email,"password",password));
        var req=stub(HttpServletRequest.class,(p,m,a)->switch(m.getName()) {
            case "getServletPath" -> path[0];
            case "getMethod" -> method[0];
            case "getRemoteAddr" -> "127.0.0.1";
            case "getContextPath" -> "/exam";
            case "getSession" -> session;
            case "getParameter" -> params.get(a[0]);
            case "changeSessionId" -> {rotations[0]++; yield "new";}
            default -> null;
        });
        var res=stub(HttpServletResponse.class,(p,m,a)->{
            if (m.getName().equals("sendError") || m.getName().equals("sendRedirect")) result[0]=a[0].toString();
            return null;
        });
        var guard=new Guard(auth);
        var controller=new Controller(auth);
        for (String role:List.of("STUDENT","ADMIN","TEACHER")) {
            db.roles=Set.of(role); path[0]="/login"; method[0]="POST"; attrs.put("csrf","before");
            params.put("csrf","before"); controller.post(req,res);
            String landing="/"+role.toLowerCase(Locale.ROOT)+"/account";
            check(result[0].equals("/exam"+landing) && attrs.get("sessionRef").equals(db.live));
            check(attrs.keySet().equals(Set.of("csrf","sessionRef","landing")) && !attrs.containsValue(db.user.getPasswordHash()));
            check(!attrs.get("csrf").equals("before"));
            path[0]=landing; method[0]="GET";
            guard.doFilter(req,res,(r,s)->result[0]="allowed"); check(result[0].equals("allowed"));
            path[0]=role.equals("ADMIN")?"/student/account":"/admin/account";
            guard.doFilter(req,res,(r,s)->{throw new AssertionError();}); check(result[0].equals("403"));
            path[0]="/logout"; method[0]="POST"; params.put("csrf",(String)attrs.get("csrf"));
            controller.post(req,res); check(db.live==null && attrs.isEmpty());
            attrs.put("sessionRef","expired"); path[0]=landing; method[0]="GET";
            guard.doFilter(req,res,(r,s)->{throw new AssertionError();});
            check(attrs.isEmpty() && result[0].equals("/exam/login"));
        }
        check(rotations[0]==3);
        var limit=new AuthenticationService.LoginLimit();
        for(int i=0;i<10;i++) check(limit.allow("peer"));
        check(!limit.allow("peer") && limit.allow("other"));
        String input="mail'with-quote@example.invalid";
        var rows=stub(ResultSet.class,(p,m,a)->m.getName().equals("next") ? false : null);
        boolean[] bound={false};
        var statement=stub(PreparedStatement.class,(p,m,a)->{
            if(m.getName().equals("setString")) bound[0]=a[0].equals(1) && a[1].equals(input);
            return m.getName().equals("executeQuery") ? rows : null;
        });
        var conn=stub(Connection.class,(p,m,a)->{
            check(a[0].toString().contains("email=?") && !a[0].toString().contains(input));
            return statement;
        });
        check(new AuthDao().byEmail(conn,input)==null && bound[0]);
        System.out.println("PASS authentication, failure paths, session rotation/revocation/expiry, roles, limiter and JDBC parameter binding");
    }
}
