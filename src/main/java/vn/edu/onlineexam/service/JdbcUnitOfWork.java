package vn.edu.onlineexam.service;

import java.sql.Connection;
import java.sql.SQLException;
import vn.edu.onlineexam.util.ConnectionFactory;

public final class JdbcUnitOfWork {
    private final ConnectionFactory connections;

    public JdbcUnitOfWork(ConnectionFactory connections) {
        this.connections = java.util.Objects.requireNonNull(connections);
    }

    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection) throws SQLException;
    }

    public <T> T read(Work<T> work) throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setReadOnly(true);
            return work.execute(connection);
        }
    }

    public <T> T transaction(Work<T> work) throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error failure) {
                try { connection.rollback(); } catch (SQLException rollbackError) { failure.addSuppressed(rollbackError); }
                throw failure;
            }
        }
    }
}
