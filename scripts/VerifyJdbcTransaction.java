import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.onlineexam.service.JdbcUnitOfWork;

public class VerifyJdbcTransaction {
    public static void main(String[] args) throws Exception {
        List<String> calls = new ArrayList<>();
        Connection stub = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, values) -> {
                    calls.add(method.getName() + (values == null ? "" : ":" + values[0]));
                    return null;
                });
        var jdbc = new JdbcUnitOfWork(() -> stub);
        jdbc.transaction(connection -> { if (connection != stub) throw new AssertionError(); return null; });
        expect(calls, "setAutoCommit:false", "commit", "close");
        SQLException original = new SQLException();
        try {
            jdbc.transaction(connection -> { throw original; });
            throw new AssertionError("Failure swallowed");
        } catch (SQLException caught) { if (caught != original) throw new AssertionError(); }
        expect(calls, "setAutoCommit:false", "rollback", "close");
        jdbc.read(connection -> "ok");
        expect(calls, "setReadOnly:true", "close");
        System.out.println("PASS: commit, rollback, read-only and connection ownership");
    }

    private static void expect(List<String> calls, String... expected) {
        if (!calls.equals(List.of(expected))) throw new AssertionError(calls);
        calls.clear();
    }
}
