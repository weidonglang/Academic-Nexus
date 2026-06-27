package weidonglang.tianshiwebside.ai;

import jakarta.validation.constraints.NotBlank;

public record AiModelRequest(
        @NotBlank String name,
        @NotBlank String provider,
        @NotBlank String modelName,
        String baseUrl,
        String apiKeyRef,
        @NotBlank String modelType,
        String purpose,
        boolean enabled,
        boolean defaultModel,
        String description
) {
}
