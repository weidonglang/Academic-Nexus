package weidonglang.tianshiwebside.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SqlSchemaService {
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "student",
            "student_status_change_application",
            "student_registration_application",
            "course",
            "course_offering",
            "course_selection",
            "academic_grade",
            "exam_schedule",
            "classroom",
            "notice",
            "academic_warning",
            "graduation_audit",
            "teaching_plan_item",
            "teaching_feedback",
            "thesis_grade",
            "teaching_evaluation"
    );
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
            "password", "passwd", "pwd", "token", "secret", "api_key", "apikey", "access_key", "private_key",
            "hash", "salt", "phone", "mobile", "email", "address", "id_card", "identity", "idnumber",
            "id_number", "emergency_contact", "parent_phone"
    );

    private final JdbcTemplate jdbcTemplate;

    public SqlSchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TableSchema> allowedSchemas() {
        return jdbcTemplate.query("""
                        select table_name, column_name, data_type
                        from information_schema.columns
                        where table_schema = database()
                        order by table_name, ordinal_position
                        """,
                (rs, rowNum) -> new ColumnSchema(
                        rs.getString("table_name"),
                        rs.getString("column_name"),
                        rs.getString("data_type")
                ))
                .stream()
                .filter(column -> ALLOWED_TABLES.contains(column.tableName()))
                .collect(java.util.stream.Collectors.groupingBy(ColumnSchema::tableName, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new TableSchema(
                        entry.getKey(),
                        entry.getValue().stream()
                                .filter(column -> !isSensitiveColumn(column.columnName()))
                                .map(column -> new ColumnInfo(column.columnName(), column.dataType()))
                                .toList()
                ))
                .toList();
    }

    public Set<String> allowedTables() {
        return ALLOWED_TABLES;
    }

    public boolean isAllowedTable(String tableName) {
        return ALLOWED_TABLES.contains(tableName);
    }

    public boolean isSensitiveColumn(String columnName) {
        String lower = columnName.toLowerCase(java.util.Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private record ColumnSchema(String tableName, String columnName, String dataType) {
    }

    public record TableSchema(String tableName, List<ColumnInfo> columns) {
    }

    public record ColumnInfo(String columnName, String dataType) {
    }
}
