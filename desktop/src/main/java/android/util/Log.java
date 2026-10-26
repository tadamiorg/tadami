package android.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/** The extensions' logcat, printed to the console. */
public final class Log {
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private Log() {
    }

    public static int v(String tag, String msg) { return println(VERBOSE, tag, msg); }
    public static int v(String tag, String msg, Throwable tr) { return println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr)); }
    public static int d(String tag, String msg) { return println(DEBUG, tag, msg); }
    public static int d(String tag, String msg, Throwable tr) { return println(DEBUG, tag, msg + '\n' + getStackTraceString(tr)); }
    public static int i(String tag, String msg) { return println(INFO, tag, msg); }
    public static int i(String tag, String msg, Throwable tr) { return println(INFO, tag, msg + '\n' + getStackTraceString(tr)); }
    public static int w(String tag, String msg) { return println(WARN, tag, msg); }
    public static int w(String tag, String msg, Throwable tr) { return println(WARN, tag, msg + '\n' + getStackTraceString(tr)); }
    public static int w(String tag, Throwable tr) { return println(WARN, tag, getStackTraceString(tr)); }
    public static int e(String tag, String msg) { return println(ERROR, tag, msg); }
    public static int e(String tag, String msg, Throwable tr) { return println(ERROR, tag, msg + '\n' + getStackTraceString(tr)); }
    public static int wtf(String tag, String msg) { return println(ASSERT, tag, msg); }
    public static int wtf(String tag, Throwable tr) { return println(ASSERT, tag, getStackTraceString(tr)); }
    public static int wtf(String tag, String msg, Throwable tr) { return println(ASSERT, tag, msg + '\n' + getStackTraceString(tr)); }

    public static boolean isLoggable(String tag, int level) {
        return level >= DEBUG;
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        StringWriter writer = new StringWriter();
        tr.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public static int println(int priority, String tag, String msg) {
        String line = "VDIWEA".charAt(Math.max(0, Math.min(5, priority - 2))) + "/" + tag + ": " + msg;
        (priority >= WARN ? System.err : System.out).println(line);
        return line.length();
    }
}
