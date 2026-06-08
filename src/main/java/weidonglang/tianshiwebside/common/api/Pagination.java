package weidonglang.tianshiwebside.common.api;

import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static int safePage(int page) {
        return Math.max(page, 1);
    }

    public static int safeSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    public static int offset(int page, int size) {
        return (safePage(page) - 1) * safeSize(size);
    }

    public static <T> PageResponse<T> slice(List<T> rows, int page, int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        int total = rows == null ? 0 : rows.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        return new PageResponse<>(rows == null ? List.of() : rows.subList(from, to), safePage, safeSize, total);
    }
}
