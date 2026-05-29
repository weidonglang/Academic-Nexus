package weidonglang.tianshiwebside.dashboard;

import java.util.List;

public record DashboardOverview(
        int courseCount,
        int pendingEvaluationCount,
        int examCount,
        int earnedCredits,
        List<DashboardEventRow> recentEvents
) {
}
