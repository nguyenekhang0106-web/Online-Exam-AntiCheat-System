import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import vn.edu.onlineexam.util.PasswordHasher;

/** Reflection checks the compiled JavaBeans contract, including nullable Boolean properties. */
public class VerifyBeans {
    public static void main(String[] args) throws Exception {
        Map<String, List<String[]>> beans = new LinkedHashMap<>();
        for (String row : Files.readAllLines(Path.of("target/database-bean-contract.tsv"))) {
            String[] fields = row.split("\t");
            beans.computeIfAbsent(fields[0], key -> new ArrayList<>()).add(fields);
        }
        check(beans.size() == 34, "Expected 34 beans");
        int properties = 0;
        for (var entry : beans.entrySet()) {
            Class<?> type = Class.forName(entry.getKey());
            Object empty = type.getConstructor().newInstance();
            Map<String, PropertyDescriptor> descriptors = new LinkedHashMap<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors()) {
                descriptors.put(descriptor.getName(), descriptor);
            }
            check(descriptors.size() == entry.getValue().size(), "Unexpected Bean properties: " + type);
            List<Class<?>> types = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (String[] column : entry.getValue()) {
                PropertyDescriptor descriptor = descriptors.get(column[1]);
                check(descriptor != null && descriptor.getReadMethod() != null && descriptor.getWriteMethod() != null,
                        "Missing JavaBeans accessor: " + Arrays.toString(column));
                Class<?> fieldType = descriptor.getPropertyType();
                check(fieldType.getSimpleName().equals(column[2]), "Type mismatch");
                check(descriptor.getReadMethod().invoke(empty) == null, "New bean must preserve unset/null values");
                Object value = sample(fieldType, values.size() + 1);
                descriptor.getWriteMethod().invoke(empty, value);
                check(Objects.equals(value, descriptor.getReadMethod().invoke(empty)), "Getter/setter round trip");
                descriptor.getWriteMethod().invoke(empty, new Object[]{null});
                check(descriptor.getReadMethod().invoke(empty) == null, "Null round trip");
                types.add(fieldType);
                values.add(value);
                properties++;
            }
            Constructor<?> constructor = type.getConstructor(types.toArray(Class<?>[]::new));
            Object full = constructor.newInstance(values.toArray());
            for (int i = 0; i < entry.getValue().size(); i++) {
                String field = entry.getValue().get(i)[1];
                check(Objects.equals(values.get(i), descriptors.get(field).getReadMethod().invoke(full)), "Full constructor: " + field);
            }
            check(type.getMethod("toString").getDeclaringClass() == Object.class, "Sensitive toString override");
        }
        char[] password = java.util.UUID.randomUUID().toString().toCharArray();
        try {
            String hash = PasswordHasher.hash(password);
            check(hash.length() <= 255, "Password hash must fit column");
            check(PasswordHasher.verify(password, hash), "Password verification");
            check(!PasswordHasher.verify(new char[]{'x'}, hash), "Wrong password must fail");
            check(!hash.equals(PasswordHasher.hash(password)), "Random salts must differ");
            for (String bad : new String[]{"", "fake", "pbkdf2-sha256$999999999$bad$bad", "pbkdf2-sha256$600000$?$?"}) {
                check(!PasswordHasher.verify(password, bad), "Invalid encoding must fail");
            }
            check(!PasswordHasher.verify(password, null), "Null encoding");
            check(!PasswordHasher.verify(null, hash), "Null password");
        } finally { Arrays.fill(password, '\0'); }
        System.out.println("PASS: 34 compiled Beans, " + properties + " nullable properties, both constructors, JavaBeans accessors; password hash checks.");
    }

    private static Object sample(Class<?> type, int index) {
        if (type == Long.class) { return Long.valueOf(100L + index); }
        if (type == Short.class) { return Short.valueOf((short) index); }
        if (type == Integer.class) { return Integer.valueOf(index); }
        if (type == Boolean.class) { return index % 2 == 0; }
        if (type == BigDecimal.class) { return new BigDecimal("12.34").add(BigDecimal.valueOf(index)); }
        if (type == LocalDateTime.class) { return LocalDateTime.of(2026, 11, 20, 0, 0, 0, 123_000_000).plusSeconds(index); }
        if (type == String.class) { return "sample-" + index; }
        throw new AssertionError("Unexpected type: " + type);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
