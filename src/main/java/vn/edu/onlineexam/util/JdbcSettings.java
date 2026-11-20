package vn.edu.onlineexam.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JdbcSettings {
    private static final List<String> KEYS = List.of("DB_URL", "DB_USER", "DB_PASSWORD");

    static Map<String, String> load(Map<String, String> environment) {
        var values = new HashMap<String, String>();
        for (String key : KEYS) {
            String value = environment.get(key);
            if (value != null && !value.isBlank()) values.put(key, value);
        }
        if (values.size() == KEYS.size()) return values;

        String location = System.getProperty("onlineexam.envFile", environment.get("ONLINE_EXAM_ENV_FILE"));
        try {
            Path file;
            if (location != null) {
                file = Path.of(location);
                if (!file.isAbsolute()) throw new IllegalStateException("Use an absolute path for ONLINE_EXAM_ENV_FILE");
            } else {
                if (!Files.isRegularFile(Path.of("pom.xml"))) {
                    throw new IllegalStateException("Set ONLINE_EXAM_ENV_FILE or run from the project root");
                }
                file = Path.of(".env");
            }
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                int split = line.indexOf('=');
                if (split < 1) throw new IllegalStateException("Invalid .env entry; expected KEY=value");
                String key = line.substring(0, split).strip();
                if (KEYS.contains(key)) values.putIfAbsent(key, line.substring(split + 1));
            }
        } catch (IOException | InvalidPathException | SecurityException ex) {
            throw new IllegalStateException("Cannot read .env; check ONLINE_EXAM_ENV_FILE and file permissions");
        }
        for (String key : KEYS) {
            if (values.getOrDefault(key, "").isBlank()) throw new IllegalStateException("Missing " + key + " in environment or .env");
        }
        return values;
    }
}
