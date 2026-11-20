import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import vn.edu.onlineexam.util.PasswordHasher;

/** Generates local SQL only. Never connects to or modifies a database. */
public class SeedAccounts {
    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run in an interactive terminal; passwords are not accepted as arguments.");
        }
        Path output = Path.of(".local", "development-accounts.sql");
        if (Files.exists(output)) { throw new IllegalStateException("Output already exists: " + output); }
        StringBuilder sql = new StringBuilder("-- PRIVATE generated account hashes. Do not commit.\n"
                + "SET NAMES utf8mb4;\nSET SESSION time_zone = '+00:00';\nSTART TRANSACTION;\n");
        for (String role : new String[]{"ADMIN", "TEACHER", "STUDENT"}) {
            char[] password = console.readPassword("Choose development %s password (12+ characters): ", role);
            char[] confirmation = console.readPassword("Confirm password: ");
            try {
                if (password == null || password.length < 12 || !Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("Passwords must match and contain at least 12 characters.");
                }
                String hash = PasswordHasher.hash(password);
                if (!PasswordHasher.verify(password, hash)) { throw new IllegalStateException("Hash self-check failed"); }
                // All SQL text is fixed or produced by the Base64 password encoder, never user-supplied text.
                sql.append("INSERT INTO users (user_code, full_name, email, password_hash) VALUES ('DEV_")
                        .append(role).append("', 'Development ").append(role).append("', 'dev.")
                        .append(role.toLowerCase(java.util.Locale.ROOT)).append("@example.invalid', '")
                        .append(hash).append("');\n");
            } finally {
                if (password != null) { Arrays.fill(password, '\0'); }
                if (confirmation != null) { Arrays.fill(confirmation, '\0'); }
            }
        }
        sql.append("COMMIT;\n");
        Files.createDirectories(output.getParent());
        Files.writeString(output, sql, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        console.printf("Created %s. No SQL has been executed. Keep this file private.%n", output);
    }
}
