package vn.edu.onlineexam.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionFactory {
    /** Returns a new caller-owned connection in auto-commit mode. */
    Connection open() throws SQLException;

    static ConnectionFactory fromEnvironment() {
        var config = JdbcSettings.load(System.getenv());
        String url = config.get("DB_URL");
        String user = config.get("DB_USER");
        String password = config.get("DB_PASSWORD");
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("DB_URL must be a MySQL JDBC URL");
        }
        return () -> {
            Connection connection = DriverManager.getConnection(url, user, password);
            try (PreparedStatement statement = connection.prepareStatement("SET SESSION time_zone = '+00:00'")) {
                statement.execute();
                return connection;
            } catch (SQLException | RuntimeException ex) {
                try { connection.close(); } catch (SQLException closeError) { ex.addSuppressed(closeError); }
                throw ex;
            }
        };
    }

}
