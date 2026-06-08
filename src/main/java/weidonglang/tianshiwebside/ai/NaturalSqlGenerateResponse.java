package weidonglang.tianshiwebside.ai;

import java.util.List;

public record NaturalSqlGenerateResponse(
        String sql,
        String explanation,
        List<String> warnings,
        List<String> allowedTables,
        String serviceMode
) {
}
