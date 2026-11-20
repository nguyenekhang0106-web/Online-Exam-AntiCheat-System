package vn.edu.onlineexam.util;

/** Escape a display value for HTML text or a quoted HTML attribute. */
public final class Html {
    private Html() {}
    public static String escape(Object value) {
        return value == null ? "—" : value.toString().replace("&","&amp;").replace("<","&lt;")
                .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}
