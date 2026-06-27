package weidonglang.tianshiwebside.ai;

import java.time.Instant;

public record AiModelRecord(
        Long id,
        String name,
        String provider,
        String modelName,
        String baseUrl,
        String apiKeyRef,
        String modelType,
        String purpose,
        boolean enabled,
        boolean defaultModel,
        String description,
        String lastStatus,
        Long lastLatencyMs,
        String lastError,
        Instant lastCheckedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
