import java.util.*;
import vn.edu.onlineexam.model.*;
import vn.edu.onlineexam.service.*;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Reads the seeded DEV fixtures. Never logs in or updates a session. */
class VerifyUserDirectory {
    static final AuthUser admin = new AuthUser(1,Set.of("ADMIN"));
    static void check(boolean ok,String label) { if (!ok) throw new AssertionError(label); }

    public static void main(String[] args) throws Exception {
        check(vn.edu.onlineexam.util.Html.escape("<tag a=\"'&\">").equals("&lt;tag a=&quot;&#39;&amp;&quot;&gt;"),"HTML escaping");
        var offline = new UserManagementService(new JdbcUnitOfWork(() -> { throw new AssertionError("Unexpected JDBC"); }));
        for (String role : List.of("STUDENT","TEACHER")) {
            try {
                offline.search(new AuthUser(1,Set.of(role)),null,null,null,null,null,null,null);
                throw new AssertionError("Role accepted");
            } catch (SecurityException expected) {}
        }
        for (String[] p : new String[][]{{"0","1","name"},{"1","101","code"},{"1","0","name"},
                {"x","1","name"},{"1","1","u.name DESC"}}) {
            try { offline.search(admin,null,null,null,p[0],p[1],p[2],null); throw new AssertionError("Invalid input accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        System.out.println("PASS service authorization and invalid pagination/sort");
        var service = new UserManagementService(new JdbcUnitOfWork(ConnectionFactory.fromEnvironment()));
        Set<String> codes=new HashSet<>();
        for(int page=1;page<=3;page++) {
            var d=service.search(admin,"DEV_",null,"ACTIVE",""+page,"1","code","ASC");
            check(d.total()==3 && d.pages()==3 && d.page()==page && d.rows().size()==1,"page totals");
            codes.add(d.rows().get(0).code());
        }
        check(codes.equals(Set.of("DEV_ADMIN","DEV_TEACHER","DEV_STUDENT")),"distinct DEV accounts");
        for(String role:List.of("ADMIN","TEACHER","STUDENT")) {
            for(String term:List.of("Development "+role,"dev."+role.toLowerCase(Locale.ROOT)+"@example.invalid","DEV_"+role)) {
                var d=service.search(admin,term,role,"ACTIVE",null,null,null,null);
                check(d.total()==1 && d.rows().get(0).roles().contains(role),"combined filters");
            }
        }
        for(String literal:List.of("'","%","DEV_NOT_FOUND","<untrusted>")) {
            var d=service.search(admin,literal,null,null,null,null,null,null);
            check(d.rows().isEmpty() && d.pages()==0 && d.total()==0,"empty literal search");
        }
        check(service.search(admin,"DEV_","","LOCKED",null,null,null,null).total()==0,"status exclusion");
        System.out.println("PASS MySQL SELECT: DEV accounts, name/email/code, role/status, counts, pages and literal searches");
    }
}
