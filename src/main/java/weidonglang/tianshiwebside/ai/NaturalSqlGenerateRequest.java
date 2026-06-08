package weidonglang.tianshiwebside.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalSqlGenerateRequest(
        @NotBlank
        @Size(max = 500)
        String question
) {
}
