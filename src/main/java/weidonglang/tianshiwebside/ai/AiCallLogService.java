package weidonglang.tianshiwebside.ai;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Service
public class AiCallLogService {
    private final JdbcTemplate jdbcTemplate;

    public AiCallLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(Principal principal, String functionType, String promptSummary, String modelName,
                       long durationMs, boolean success, String errorMessage) {
        String username = principal == null ? "anonymous" : principal.getName();
        Long userId = findUserId(username);
        String roles = roles(principal);
        jdbcTemplate.update("""
                        insert into ai_call_log
                          (user_id, username, role_codes, function_type, prompt_summary, model_name,
                           duration_ms, success, error_message, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                username,
                truncate(roles, 240),
                functionType,
                truncate(promptSummary, 500),
                truncate(modelName, 120),
                durationMs,
                success,
                truncate(errorMessage, 500),
                Instant.now()
        );
    }

    public List<AiCallLogRow> recentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.query("""
                        select id, username, role_codes, function_type, prompt_summary, model_name,
                               duration_ms, success, error_message, created_at
                        from ai_call_log
                        order by created_at desc
                        limit ?
                        """,
                (rs, rowNum) -> new AiCallLogRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role_codes"),
                        rs.getString("function_type"),
                        rs.getString("prompt_summary"),
                        rs.getString("model_name"),
                        rs.getLong("duration_ms"),
                        rs.getBoolean("success"),
                        rs.getString("error_message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                safeLimit
        );
    }

    public List<AiCallLogRow> logs(int size, int offset) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safeOffset = Math.max(0, offset);
        return jdbcTemplate.query("""
                        select id, username, role_codes, function_type, prompt_summary, model_name,
                               duration_ms, success, error_message, created_at
                        from ai_call_log
                        order by created_at desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> new AiCallLogRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role_codes"),
                        rs.getString("function_type"),
                        rs.getString("prompt_summary"),
                        rs.getString("model_name"),
                        rs.getLong("duration_ms"),
                        rs.getBoolean("success"),
                        rs.getString("error_message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                safeSize,
                safeOffset
        );
    }

    public long countLogs() {
        Long total = jdbcTemplate.queryForObject("select count(*) from ai_call_log", Long.class);
        return total == null ? 0 : total;
    }

    private Long findUserId(String username) {
        if (username == null || username.equals("anonymous")) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, username);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private String roles(Principal principal) {
        if (principal instanceof Authentication authentication) {
            return String.join(",", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .sorted()
                    .toList());
        }
        return "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
