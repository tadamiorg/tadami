package android.os;

/** Android's clocks: the time since the JVM started stands for the time since boot. */
public final class SystemClock {
    private static final long START_NANOS = System.nanoTime();

    private SystemClock() {
    }

    public static long elapsedRealtime() {
        return (System.nanoTime() - START_NANOS) / 1_000_000L;
    }

    public static long elapsedRealtimeNanos() {
        return System.nanoTime() - START_NANOS;
    }

    public static long uptimeMillis() {
        return elapsedRealtime();
    }

    public static void sleep(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (true) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) return;
            try {
                Thread.sleep(left);
            } catch (InterruptedException ignored) {
                // Like Android's: keeps sleeping
            }
        }
    }
}
