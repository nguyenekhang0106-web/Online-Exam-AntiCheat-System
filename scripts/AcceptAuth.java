import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Local acceptance client. Redirects are examined before following them. */
class AcceptAuth {
    static URI base;
    CookieManager jar = new CookieManager(null,CookiePolicy.ACCEPT_ALL);
    HttpClient http = HttpClient.newBuilder().cookieHandler(jar).build();
    static void require(boolean ok,String label) { if(!ok) throw new AssertionError(label); }
    HttpResponse<String> send(String path,String form) throws Exception {
        var b=HttpRequest.newBuilder(base.resolve(path));
        if(form!=null) b.header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        return http.send(b.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    static String csrf(String page) {
        var m=Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(page);
        require(m.find(),"CSRF field missing"); return m.group(1);
    }
    String id() {
        return jar.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }
    static String enc(String s) {return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    static String form(String email,String password,String token) {
        return "email="+enc(email)+"&password="+enc(password)+"&csrf="+enc(token);
    }
    void negative() throws Exception {
        var page=send("login",null);
        String cookieFlags=String.join(";",page.headers().allValues("Set-Cookie")).toLowerCase(Locale.ROOT);
        require(cookieFlags.contains("httponly") && cookieFlags.contains("samesite=lax"),"Cookie flags");
        String token=csrf(page.body()), wrong=UUID.randomUUID().toString();
        for(String email:List.of("dev.student@example.invalid",UUID.randomUUID()+"@example.invalid")) {
            var bad=send("login",form(email,wrong,token));
            require(bad.statusCode()==401 && bad.body().contains("Không thể đăng nhập"),"Generic login denial: HTTP "+bad.statusCode());
            require(!Pattern.compile("pbkdf2-sha256|SQLException|stack trace|SELECT id",Pattern.CASE_INSENSITIVE).matcher(bad.body()).find(),"Internal error disclosure");
        }
        require(send("login",form("x","x","invalid")).statusCode()==403,"Incorrect CSRF");
        System.out.println("PASS MySQL-backed negative logins, cookie flags and incorrect CSRF");
    }
    record State(long latest,LocalDateTime lastLogin) {}
    State state(Connection db,String email) throws Exception {
        try(var q=db.prepareStatement("SELECT u.status,u.last_login_at,COALESCE(MAX(s.id),0) FROM users u LEFT JOIN login_sessions s ON s.user_id=u.id WHERE u.email=? GROUP BY u.id")) {
            q.setString(1,email);
            try(var r=q.executeQuery()){require(r.next() && "ACTIVE".equals(r.getString(1)),"Expected ACTIVE DEV account");
                return new State(r.getLong(3),r.getObject(2,LocalDateTime.class));}
        }
    }
    void positive(Connection db,String role) throws Exception {
        String name=role.toLowerCase(Locale.ROOT),email="dev."+name+"@example.invalid",path=name+"/account";
        var before=state(db,email);
        var login=send("login",null); String first=id();
        char[] password=System.console().readPassword("Local password for %s: ",email);
        require(password!=null,"Password input cancelled");
        HttpResponse<String> reply;
        try {reply=send("login",form(email,new String(password),csrf(login.body())));}
        finally {Arrays.fill(password,'\0');}
        require(reply.statusCode()==302 && URI.create(reply.headers().firstValue("Location").orElseThrow()).getPath().equals(base.resolve(path).getPath()),"Role redirect "+role);
        require(!first.equals(id()),"Session ID rotation");
        var after=state(db,email);
        require(after.latest()>before.latest() && after.lastLogin()!=null && (before.lastLogin()==null || after.lastLogin().isAfter(before.lastLogin())),"New session and last_login_at");
        String oldSession=id();
        try(var q=db.prepareStatement("SELECT revoked_at IS NULL AND expires_at>UTC_TIMESTAMP(3) AND CHAR_LENGTH(session_hash)=64 FROM login_sessions WHERE id=?")) {
            q.setLong(1,after.latest());try(var r=q.executeQuery()){require(r.next() && r.getBoolean(1),"Live DB session");}
        }
        var account=send(path,null); require(account.statusCode()==200 && account.body().contains(role),"Role page");
        try {
            for(String other:List.of("admin","teacher","student")) if(!name.equals(other))
                require(send(other+"/account",null).statusCode()==403,"Cross-role access");
        } finally {
            require(send("logout","csrf="+enc(csrf(account.body()))).statusCode()==302,"Logout");
        }
        try(var q=db.prepareStatement("SELECT revoked_at IS NOT NULL AND expires_at>created_at AND last_seen_at>=created_at FROM login_sessions WHERE id=?")) {
            q.setLong(1,after.latest());try(var r=q.executeQuery()){require(r.next() && r.getBoolean(1),"DB revocation");}
        }
        var replay=HttpRequest.newBuilder(base.resolve(path)).header("Cookie","JSESSIONID="+oldSession).GET().build();
        var denied=HttpClient.newHttpClient().send(replay,HttpResponse.BodyHandlers.discarding());
        require(denied.statusCode()==302 && URI.create(denied.headers().firstValue("Location").orElseThrow()).getPath().equals(base.resolve("login").getPath()),"Replay after logout");
        System.out.println("PASS "+role+" login, rotation, role access, last_login_at, session creation and revocation, logout");
    }
    public static void main(String[] args) throws Exception {
        int port=Integer.parseInt(args[0]); base=URI.create("http://127.0.0.1:"+port+"/online-exam/");
        new AcceptAuth().negative();
        if(args.length>1) {
            require(System.console()!=null,"Run in a local interactive terminal");
            try(var db=ConnectionFactory.fromEnvironment().open()){
                db.setReadOnly(true);
                require("online_exam_dev".equals(db.getCatalog()),"Wrong schema");
                for(String role:List.of("ADMIN","TEACHER","STUDENT")) new AcceptAuth().positive(db,role);
            }
        }
    }
}
