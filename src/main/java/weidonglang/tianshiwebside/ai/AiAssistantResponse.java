package weidonglang.tianshiwebside.ai;

import java.util.List;

public record AiAssistantResponse(
        String answer,
        List<AiSourceDocument> sources,
        String serviceMode,
        String answerType,
        String refusalReason,
        String confidenceLevel,
        double confidenceScore,
        String modelName,
        boolean realModel,
        boolean fallbackUsed,
        Long latencyMs,
        String traceId,
        Long callLogId
) {
}
