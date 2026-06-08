package weidonglang.tianshiwebside.ai;

import java.util.List;

public record LoadTestAnalysisResponse(
        String conclusion,
        List<String> bottlenecks,
        List<String> suggestions,
        String riskLevel,
        String serviceMode
) {
}
