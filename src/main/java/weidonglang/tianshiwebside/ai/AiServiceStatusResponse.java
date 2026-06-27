package weidonglang.tianshiwebside.ai;

import java.time.Instant;

public record AiServiceStatusResponse(
        boolean aiServiceOnline,
        boolean ollamaEnabled,
        boolean ollamaReachable,
        String chatModel,
        String sqlModel,
        String currentMode,
        long lastLatencyMs,
        String lastError,
        String serviceName,
        boolean discoveryEnabled,
        String baseUrl,
        String defaultChatModel,
        String defaultRagModel,
        String defaultSqlModel,
        boolean searchEnabled,
        String searchProvider,
        String searchStatus,
        Instant checkedAt
) {
}
