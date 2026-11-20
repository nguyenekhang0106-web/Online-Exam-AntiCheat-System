package vn.edu.onlineexam.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import vn.edu.onlineexam.model.bean.auth.Role;

public final class RoleDao {
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
