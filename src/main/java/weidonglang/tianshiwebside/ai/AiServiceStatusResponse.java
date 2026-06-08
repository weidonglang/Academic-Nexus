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
        Instant checkedAt
) {
}
