package vn.edu.onlineexam.service;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import vn.edu.onlineexam.dao.AuthDao;
import vn.edu.onlineexam.model.AuthUser;
import vn.edu.onlineexam.util.*;

public final class AuthenticationService {
    private static final String DUMMY = PasswordHasher.hash(UUID.randomUUID().toString().toCharArray());
    private final JdbcUnitOfWork jdbc;
    private final AuthDao dao;

    public AuthenticationService(JdbcUnitOfWork jdbc, AuthDao dao) { this.jdbc=jdbc; this.dao=dao; }

    public static AuthenticationService configured() {
        // Resolve deployment configuration only when a database operation is needed.
        return new AuthenticationService(new JdbcUnitOfWork(() -> ConnectionFactory.fromEnvironment().open()),new AuthDao());
    }

    public AuthUser login(String email,char[] password,String sessionHash) throws SQLException {
        if (email == null || email.isBlank() || email.length()>254 || password.length==0 || password.length>1024) return null;
        return jdbc.transaction(c -> {
            var account=dao.byEmail(c,email.strip());
            boolean valid=PasswordHasher.verify(password,account==null ? DUMMY : account.getPasswordHash());
            if (!valid || account==null || !"ACTIVE".equals(account.getStatus())) return null;
            var roles=dao.roles(c,account.getId());
            if (roles.isEmpty()) return null;
            dao.start(c,account.getId(),sessionHash);
            return new AuthUser(account.getId(),roles);
        });
    }

    public AuthUser current(String hash) throws SQLException { return current(hash,true); }

    public AuthUser current(String hash,boolean userActivity) throws SQLException {
        return jdbc.transaction(c -> {
            Long id=dao.active(c,hash);
            if (id==null) return null;
            var roles=dao.roles(c,id);
            if (roles.isEmpty()) return null;
            if(userActivity)dao.touch(c,hash);
            return new AuthUser(id,roles);
        });
    }

    public void logout(String hash) throws SQLException {
        jdbc.transaction(c -> { dao.revoke(c,hash); return null; });
    }

    /** Bounded, per-JVM fixed windows. Use the connector peer address, never a client-supplied header. */
    public static final class LoginLimit {
        private record Window(long start,int count) {}
        private final Map<String,Window> windows=new HashMap<>();
        public synchronized boolean allow(String peer) {
            long now=System.nanoTime(), span=TimeUnit.MINUTES.toNanos(5);
            var w=windows.get(peer);
            if (w==null || now-w.start()>=span) {
                if (windows.size()>=4096) windows.values().removeIf(old -> now-old.start()>=span);
                if (w==null && windows.size()>=4096) return false;
                w=new Window(now,0);
            }
            if (w.count()>=10) return false;
            windows.put(peer,new Window(w.start(),w.count()+1));
            return true;
        }
    }
}
