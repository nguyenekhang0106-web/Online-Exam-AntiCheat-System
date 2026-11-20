package vn.edu.onlineexam.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import vn.edu.onlineexam.model.UserDirectory.Membership;
import vn.edu.onlineexam.model.UserDirectory.RoleChoice;
import vn.edu.onlineexam.model.bean.auth.Role;

public final class RoleDao {

    public Membership membership(Connection c, long id) throws SQLException {
        String sql = """
                SELECT u.full_name, u.status, r.role_code, r.role_name, ur.user_id
                FROM users u LEFT JOIN roles r ON TRUE
                LEFT JOIN user_roles ur ON ur.role_id=r.id AND ur.user_id=u.id
                WHERE u.id=? ORDER BY r.role_code
                """;
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rows = ps.executeQuery()) {
                if (!rows.next()) throw new java.util.NoSuchElementException();
                String name = rows.getString(1), status = rows.getString(2);
                var choices = new java.util.ArrayList<RoleChoice>();
                do {
                    if (rows.getString(3) != null)
                        choices.add(new RoleChoice(rows.getString(3), rows.getString(4), rows.getObject(5) != null));
                } while (rows.next());
                return new Membership(id, name, status, java.util.List.copyOf(choices));
            }
        }
    }

    public boolean remove(Connection c, long user, short role) throws SQLException {
        try (var ps = c.prepareStatement("DELETE FROM user_roles WHERE user_id=? AND role_id=?")) {
            ps.setLong(1, user);
            ps.setShort(2, role);
            return ps.executeUpdate() == 1;
        }
    }

    public Optional<Role> findByCode(Connection connection, String code) throws SQLException {
        String sql = "SELECT id, role_code, role_name, description FROM roles WHERE role_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(new Role(rows.getObject("id", Short.class), rows.getString("role_code"),
                        rows.getString("role_name"), rows.getString("description")));
            }
        }
    }
}
