package weidonglang.tianshiwebside.dashboard;

import java.time.LocalDateTime;

public record DashboardEventRow(
        String type,
        String title,
        LocalDateTime eventTime
) {
}
