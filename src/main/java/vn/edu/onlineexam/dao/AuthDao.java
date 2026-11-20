package vn.edu.onlineexam.dao;

import java.sql.*;
import java.util.*;
import vn.edu.onlineexam.model.bean.auth.User;

public class AuthDao {
    public User byEmail(Connection c, String email) throws SQLException {
        try (var q = c.prepareStatement("SELECT id,password_hash,status FROM users WHERE email=? FOR UPDATE")) {
            q.setString(1,email);
            try (var r=q.executeQuery()) {
                if (!r.next()) return null;
                var user=new User();
                user.setId(r.getLong("id"));
                user.setPasswordHash(r.getString("password_hash"));
                user.setStatus(r.getString("status"));
                return user;
            }
        }
    }

    public Set<String> roles(Connection c,long id) throws SQLException {
        try (var q=c.prepareStatement("SELECT r.role_code FROM roles r JOIN user_roles ur ON r.id=ur.role_id WHERE ur.user_id=?")) {
            q.setLong(1,id);
            try (var r=q.executeQuery()) {
                Set<String> roles=new HashSet<>();
                while (r.next()) roles.add(r.getString(1));
                return roles;
            }
        }
    }

    public void start(Connection c,long id,String hash) throws SQLException {
        try (var q=c.prepareStatement("INSERT INTO login_sessions(user_id,session_hash,created_at,last_seen_at,expires_at) VALUES(?,?,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3),UTC_TIMESTAMP(3)+INTERVAL 8 HOUR)")) {
            q.setLong(1,id); q.setString(2,hash); q.executeUpdate();
        }
        try (var q=c.prepareStatement("UPDATE users SET last_login_at=UTC_TIMESTAMP(3) WHERE id=?")) {
            q.setLong(1,id); q.executeUpdate();
        }
    }

    public Long active(Connection c,String hash) throws SQLException {
        try (var q=c.prepareStatement("SELECT s.user_id FROM login_sessions s JOIN users u ON u.id=s.user_id WHERE s.session_hash=? AND s.revoked_at IS NULL AND s.expires_at>UTC_TIMESTAMP(3) AND s.last_seen_at>UTC_TIMESTAMP(3)-INTERVAL 30 MINUTE AND u.status='ACTIVE' FOR UPDATE")) {
            q.setString(1,hash);
            try (var r=q.executeQuery()) { if (!r.next()) return null; return r.getLong(1); }
        }
    }

    public void touch(Connection c,String hash) throws SQLException {
        try (var q=c.prepareStatement("UPDATE login_sessions SET last_seen_at=UTC_TIMESTAMP(3) WHERE session_hash=?")) {
            q.setString(1,hash); q.executeUpdate();
        }
    }

    public void revoke(Connection c,String hash) throws SQLException {
        try (var q=c.prepareStatement("UPDATE login_sessions SET revoked_at=UTC_TIMESTAMP(3) WHERE session_hash=? AND revoked_at IS NULL")) {
            q.setString(1,hash); q.executeUpdate();
        }
    }
}
