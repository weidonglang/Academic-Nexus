package weidonglang.tianshiwebside.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalSqlExecuteRequest(
        @NotBlank
        @Size(max = 3000)
        String sql
) {
}
