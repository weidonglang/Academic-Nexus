package weidonglang.tianshiwebside.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NaturalSqlService {
    private static final int MAX_ROWS = 100;
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?i)\\b(from|join)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+(\\d+)");
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|replace|grant|revoke|call|exec|merge|load|outfile|infile)\\b"
    );

    private final JdbcTemplate jdbcTemplate;
    private final SqlSchemaService schemaService;
    private final AiRemoteClient remoteClient;
    private final NaturalSqlFallbackService fallbackService;
    private final AuditLogService auditLogService;
    private final AiCallLogService callLogService;
    private final AiModelRegistryService modelRegistryService;

    public NaturalSqlService(
            JdbcTemplate jdbcTemplate,
            SqlSchemaService schemaService,
            AiRemoteClient remoteClient,
            NaturalSqlFallbackService fallbackService,
            AuditLogService auditLogService,
            AiCallLogService callLogService,
            AiModelRegistryService modelRegistryService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaService = schemaService;
        this.remoteClient = remoteClient;
        this.fallbackService = fallbackService;
        this.auditLogService = auditLogService;
        this.callLogService = callLogService;
        this.modelRegistryService = modelRegistryService;
    }

    public NaturalSqlGenerateResponse generate(String question, Principal principal) {
        long start = System.nanoTime();
        try {
            rejectUnsafeQuestion(question);
            List<SqlSchemaService.TableSchema> schemas = schemaService.allowedSchemas();
            NaturalSqlGenerateResponse response = remoteClient.generateSql(question, schemas)
                    .orElseGet(() -> fallbackService.generate(
                            question,
                            schemas.stream().map(SqlSchemaService.TableSchema::tableName).toList()
                    ));
            String safeSql = validateAndNormalize(response.sql()).sql();
            NaturalSqlGenerateResponse safeResponse = new NaturalSqlGenerateResponse(
                    safeSql,
                    response.explanation(),
                    response.warnings(),
                    response.allowedTables(),
                    response.serviceMode()
            );
            auditLogService.record(operator(principal), "AI_SQL_GENERATE", "AI_SQL", null,
                    "question=" + truncate(question, 180) + "; sql=" + truncate(safeSql, 300), null);
            callLogService.record(principal, "SQL_GENERATE", question,
                    modelRegistryService.defaultModelName("SQL", response.serviceMode()), elapsedMillis(start), true, null);
            return safeResponse;
        } catch (RuntimeException ex) {
            callLogService.record(principal, "SQL_GENERATE", question, "security-check", elapsedMillis(start), false, ex.getMessage());
            throw ex;
        }
    }

    public NaturalSqlExecuteResponse execute(String sql, Principal principal) {
        long start = System.nanoTime();
        try {
            SqlValidation validation = validateAndNormalize(sql);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(validation.sql())
                    .stream()
                    .map(this::maskSensitiveColumns)
                    .toList();
            List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
            auditLogService.record(operator(principal), "AI_SQL_EXECUTE", "AI_SQL", null,
                    "rows=" + rows.size() + "; sql=" + truncate(validation.sql(), 500), null);
            callLogService.record(principal, "SQL_EXECUTE", truncate(validation.sql(), 500), "mysql-readonly", elapsedMillis(start), true, null);
            return new NaturalSqlExecuteResponse(validation.sql(), columns, rows, rows.size(), validation.warnings());
        } catch (RuntimeException ex) {
            callLogService.record(principal, "SQL_EXECUTE", truncate(sql, 500), "mysql-readonly", elapsedMillis(start), false, ex.getMessage());
            throw ex;
        }
    }

    SqlValidation validateAndNormalize(String sql) {
        String normalized = normalizeSql(sql);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SQL 不能为空");
        }
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("select ")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "自然语言查库只允许 SELECT 查询");
        }
        if (normalized.contains(";")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "禁止执行多语句 SQL");
        }
        if (FORBIDDEN_PATTERN.matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SQL 包含写操作或高风险关键字");
        }
        List<String> tables = extractTables(normalized);
        if (tables.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未识别到查询表");
        }
        for (String table : tables) {
            if (!schemaService.isAllowedTable(table)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不允许查询表：" + table);
            }
        }

        List<String> warnings = new ArrayList<>();
        Matcher limitMatcher = LIMIT_PATTERN.matcher(normalized);
        if (limitMatcher.find()) {
            int limit = Integer.parseInt(limitMatcher.group(1));
            if (limit > MAX_ROWS) {
                normalized = limitMatcher.replaceFirst("limit " + MAX_ROWS);
                warnings.add("原 SQL 的 LIMIT 超过 " + MAX_ROWS + "，已自动收缩。");
            }
        } else {
            normalized = normalized + " limit " + MAX_ROWS;
            warnings.add("已自动追加 LIMIT " + MAX_ROWS + "，避免一次性读取过多数据。");
        }
        return new SqlValidation(normalized, warnings);
    }

    private void rejectUnsafeQuestion(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "查询问题不能为空");
        }
        if (q.contains("删除") || q.contains("清空") || q.contains("修改") || q.contains("更新")
                || q.contains("新增") || q.contains("插入") || q.contains("重置") || q.contains("drop")
                || q.contains("delete") || q.contains("update") || q.contains("insert") || q.contains("truncate")
                || q.contains("alter") || q.contains("create")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "自然语言查库只支持只读统计和查询，不生成写操作 SQL");
        }
        if (q.contains("密码") || q.contains("password") || q.contains("token") || q.contains("密钥")
                || q.contains("password_hash")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "自然语言查库不允许查询密码、Token 或密钥等敏感字段");
        }
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        String withoutBlockComments = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        String withoutLineComments = withoutBlockComments.replaceAll("(?m)--.*?$", " ");
        return withoutLineComments.replaceAll("\\s+", " ").trim();
    }

    private List<String> extractTables(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        List<String> tables = new ArrayList<>();
        while (matcher.find()) {
            tables.add(matcher.group(2));
        }
        return tables;
    }

    private Map<String, Object> maskSensitiveColumns(Map<String, Object> row) {
        Map<String, Object> masked = new LinkedHashMap<>();
        row.forEach((key, value) -> masked.put(key, schemaService.isSensitiveColumn(key) && value != null ? "******" : value));
        return masked;
    }

    private String operator(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private long elapsedMillis(long startNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    record SqlValidation(String sql, List<String> warnings) {
    }
}
