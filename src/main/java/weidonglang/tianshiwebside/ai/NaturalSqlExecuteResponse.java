package weidonglang.tianshiwebside.ai;

import java.util.List;
import java.util.Map;

public record NaturalSqlExecuteResponse(
        String sql,
        List<String> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        List<String> warnings
) {
}
