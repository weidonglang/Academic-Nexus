package weidonglang.tianshiwebside.ai;

import java.time.Instant;

public record AiCallLogRow(
        Long id,
        String username,
        String roleCodes,
        String functionType,
        String promptSummary,
        String modelName,
        Long durationMs,
        Boolean success,
        String errorMessage,
        Instant createdAt
) {
}
