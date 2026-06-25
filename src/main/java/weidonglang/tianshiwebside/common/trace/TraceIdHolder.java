package weidonglang.tianshiwebside.common.trace;

public final class TraceIdHolder {
    public static final String TRACE_ID_ATTRIBUTE = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceIdHolder() {
    }

    public static void set(String traceId) {
        CURRENT.set(traceId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
