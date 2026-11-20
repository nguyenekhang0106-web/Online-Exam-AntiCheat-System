import java.sql.SQLException;
import vn.edu.onlineexam.dao.RoleDao;
import vn.edu.onlineexam.service.JdbcUnitOfWork;
import vn.edu.onlineexam.util.ConnectionFactory;

/** Run only with credentials for the authorized test schema. */
public class JdbcSmoke {
    public static void main(String[] args) {
        try {
            var jdbc = new JdbcUnitOfWork(ConnectionFactory.fromEnvironment());
            jdbc.read(connection -> {
                try (var query = connection.prepareStatement(
                        "SELECT DATABASE(), @@session.time_zone, @@character_set_client, @@character_set_connection");
                     var row = query.executeQuery()) {
                    row.next();
                    if (!"online_exam_dev".equals(row.getString(1))) throw new IllegalStateException("Wrong schema");
                    if (!"+00:00".equals(row.getString(2))) throw new IllegalStateException("Expected UTC");
                    if (!"utf8mb4".equals(row.getString(3)) || !"utf8mb4".equals(row.getString(4))) {
                        throw new IllegalStateException("Expected utf8mb4");
                    }
                }
                System.out.println("PASS: online_exam_dev, UTC, utf8mb4; driver " + connection.getMetaData().getDriverVersion());
                try (var query = connection.prepareStatement("SELECT TABLE_NAME FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME");
                     var rows = query.executeQuery()) {
                    int count = 0;
                    while (rows.next()) { System.out.println("TABLE " + rows.getString(1)); count++; }
                    System.out.println("Visible base tables: " + count);
                }
                var dao = new RoleDao();
                try (var query = connection.prepareStatement("SELECT role_code FROM roles ORDER BY id LIMIT 1");
                     var rows = query.executeQuery()) {
                    if (rows.next()) {
                        String code = rows.getString(1);
                        var role = dao.findByCode(connection, code).orElseThrow(() -> new SQLException("Missing role"));
                        if (!code.equals(role.getRoleCode()) || role.getId() == null || role.getRoleName() == null) {
                            throw new SQLException("Unexpected role mapping");
                        }
                        System.out.println("PASS: RoleDao mapped existing role");
                    } else {
                        if (dao.findByCode(connection, "STUDENT").isPresent()) throw new SQLException("Unexpected role");
                        System.out.println("PASS: roles empty, RoleDao returned Optional.empty");
                    }
                }
                if (dao.findByCode(connection, "__ABSENT__").isPresent()) throw new SQLException("Expected absent role");
                System.out.println("PASS: JDBC read-only smoke");
                return null;
            });
        } catch (SQLException | IllegalStateException | IllegalArgumentException failure) {
            System.err.println("FAIL: " + (failure instanceof SQLException sql
                    ? "SQLState=" + sql.getSQLState() + " vendorCode=" + sql.getErrorCode() : failure.getMessage()));
            System.exit(1);
        }
    }
}
