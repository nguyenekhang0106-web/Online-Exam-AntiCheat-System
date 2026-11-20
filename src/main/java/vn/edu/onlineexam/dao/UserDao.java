package vn.edu.onlineexam.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import vn.edu.onlineexam.model.UserDirectory.*;

public final class UserDao {
    /** Serialize status transitions on the stable ADMIN role before locking users. */
    public Set<Long> lockActiveAdmins(Connection c) throws SQLException {
        try (var guard=c.prepareStatement("SELECT id FROM roles WHERE role_code='ADMIN' FOR UPDATE");
             var r=guard.executeQuery()) {
            if (!r.next()) throw new SQLException("Missing ADMIN role");
        }
        try (var ps=c.prepareStatement("SELECT u.id FROM users u JOIN user_roles ur ON ur.user_id=u.id "
                + "JOIN roles r ON r.id=ur.role_id WHERE r.role_code='ADMIN' AND u.status='ACTIVE' ORDER BY u.id FOR UPDATE");
             var rows=ps.executeQuery()) {
            Set<Long> admins=new HashSet<>();
            while(rows.next()) admins.add(rows.getLong(1));
            return admins;
        }
    }

    public String lockStatus(Connection c,long id) throws SQLException {
        try (var ps=c.prepareStatement("SELECT status FROM users WHERE id=? FOR UPDATE")) {
            ps.setLong(1,id);
            try(var rows=ps.executeQuery()) { return rows.next()?rows.getString(1):null; }
        }
    }

    public boolean changeStatus(Connection c,long id,String expected,String next,LocalDateTime version) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE users SET status=?,updated_at=GREATEST(UTC_TIMESTAMP(3),updated_at+INTERVAL 1000 MICROSECOND) "
                + "WHERE id=? AND status=? AND updated_at=?")) {
            ps.setString(1,next); ps.setLong(2,id); ps.setString(3,expected); ps.setObject(4,version);
            return ps.executeUpdate()==1;
        }
    }

    public void revokeSessions(Connection c,long id) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE login_sessions SET revoked_at=UTC_TIMESTAMP(3) WHERE user_id=? AND revoked_at IS NULL")) {
            ps.setLong(1,id); ps.executeUpdate();
        }
    }
    public Detail find(Connection c,long id,boolean lock) throws SQLException {
        String sql="SELECT full_name,email,user_code,phone,updated_at,email_verified_at IS NOT NULL FROM users WHERE id=?";
        try(var ps=c.prepareStatement(lock ? sql+" FOR UPDATE" : sql)) {
            ps.setLong(1,id);
            try(var r=ps.executeQuery()) {
                if(!r.next()) return null;
                return new Detail(new Profile(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),
                        r.getObject(5,LocalDateTime.class),r.getBoolean(6));
            }
        }
    }

    public boolean duplicate(Connection c,Profile p,long exclude) throws SQLException {
        try(var ps=c.prepareStatement("SELECT 1 FROM users WHERE (email=? OR user_code=?) AND id<>? LIMIT 1")) {
            ps.setString(1,p.email()); ps.setString(2,p.code()); ps.setLong(3,exclude);
            try(var r=ps.executeQuery()) { return r.next(); }
        }
    }

    private void bindProfile(PreparedStatement ps,Profile p) throws SQLException {
        ps.setString(1,p.name()); ps.setString(2,p.email()); ps.setString(3,p.code()); ps.setString(4,p.phone());
    }

    public long insert(Connection c,Profile p,String hash) throws SQLException {
        try(var ps=c.prepareStatement("INSERT INTO users(full_name,email,user_code,phone,password_hash) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            bindProfile(ps,p); ps.setString(5,hash); ps.executeUpdate();
            try(var r=ps.getGeneratedKeys()) { if(!r.next()) throw new SQLException("Missing generated ID"); return r.getLong(1); }
        }
    }

    public boolean assignInitialRole(Connection c,long id,String role,long actor) throws SQLException {
        try(var ps=c.prepareStatement("INSERT INTO user_roles(user_id,role_id,assigned_at,assigned_by) SELECT ?,id,UTC_TIMESTAMP(3),? FROM roles WHERE role_code=?")) {
            ps.setLong(1,id); ps.setLong(2,actor); ps.setString(3,role); return ps.executeUpdate()==1;
        }
    }

    public void update(Connection c,long id,Profile p) throws SQLException {
        try(var ps=c.prepareStatement("UPDATE users SET full_name=?,email=?,user_code=?,phone=?,updated_at=GREATEST(UTC_TIMESTAMP(3),updated_at+INTERVAL 1000 MICROSECOND) WHERE id=?")) {
            bindProfile(ps,p); ps.setLong(5,id); ps.executeUpdate();
        }
    }

    public void audit(Connection c,long actor,long target,String action) throws SQLException {
        try(var ps=c.prepareStatement("INSERT INTO audit_logs(actor_user_id,target_type,target_id,action_code) VALUES(?,'USER',?,?)")) {
            ps.setLong(1,actor); ps.setLong(2,target); ps.setString(3,action); ps.executeUpdate();
        }
    }
    private record Filter(String sql, List<String> values) {
        int bind(PreparedStatement ps) throws SQLException {
            int index = 1;
            for (String value : values) ps.setString(index++, value);
            return index;
        }
    }

    private Filter filter(Query q) {
        List<String> conditions = new ArrayList<>(), values = new ArrayList<>();
        if (!q.keyword().isEmpty()) {
            conditions.add("(u.full_name LIKE ? ESCAPE '!' OR u.email LIKE ? ESCAPE '!' OR u.user_code LIKE ? ESCAPE '!')");
            String pattern = "%" + q.keyword().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            values.addAll(Collections.nCopies(3, pattern));
        }
        if (!q.role().isEmpty()) {
            conditions.add("EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=u.id AND r.role_code=?)");
            values.add(q.role());
        }
        if (!q.status().isEmpty()) {
            conditions.add("u.status=?");
            values.add(q.status());
        }
        return new Filter(" FROM users u" + (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions)), values);
    }

    public long count(Connection c, Query q) throws SQLException {
        Filter f = filter(q);
        try (var ps = c.prepareStatement("SELECT COUNT(*)" + f.sql())) {
            f.bind(ps);
            try (var rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    public List<Row> list(Connection c, Query q, long offset) throws SQLException {
        Filter f = filter(q);
        String sql = "SELECT u.id,u.user_code,u.full_name,u.email,u.status,u.created_at,u.updated_at,"
                + "(SELECT GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ', ') FROM user_roles ur "
                + "JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=u.id) AS role_names"
                + f.sql() + " ORDER BY " + q.sort().column + " " + q.direction().name() + ",u.id ASC LIMIT ? OFFSET ?";
        try (var ps = c.prepareStatement(sql)) {
            int i = f.bind(ps);
            ps.setInt(i++, q.size());
            ps.setLong(i, offset);
            try (var rs = ps.executeQuery()) {
                List<Row> rows = new ArrayList<>();
                while (rs.next()) rows.add(new Row(rs.getLong("id"),rs.getString("user_code"),rs.getString("full_name"),
                        rs.getString("email"),rs.getString("role_names"),rs.getString("status"),
                        rs.getObject("created_at",LocalDateTime.class),rs.getObject("updated_at",LocalDateTime.class)));
                return List.copyOf(rows);
            }
        }
    }
}
