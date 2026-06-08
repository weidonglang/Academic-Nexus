package weidonglang.tianshiwebside.ai;

import jakarta.validation.constraints.NotBlank;

public record LoadTestAnalysisRequest(
        @NotBlank
        String jsonName
) {
}
