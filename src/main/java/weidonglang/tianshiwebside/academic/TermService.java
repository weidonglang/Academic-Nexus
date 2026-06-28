package weidonglang.tianshiwebside.academic;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TermService {
    private static final String DEMO_FALLBACK_TERM = "2025-2026-2";

    private final JdbcTemplate jdbcTemplate;

    public TermService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String resolveTerm(String requestedTerm) {
        if (requestedTerm != null && !requestedTerm.isBlank()) {
            return requestedTerm.trim();
        }
        String configured = configuredCurrentTerm();
        if (!configured.isBlank()) {
            return configured;
        }
        String latest = latestOfferingTerm();
        if (!latest.isBlank()) {
            return latest;
        }
        return inferCurrentTerm(LocalDate.now());
    }

    public CurrentTerm currentTerm() {
        String term = resolveTerm(null);
        return new CurrentTerm(term, configuredCurrentTerm().isBlank() ? "inferred" : "system_config");
    }

    String inferCurrentTerm(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 8) {
            return year + "-" + (year + 1) + "-1";
        }
        if (month >= 2) {
            return (year - 1) + "-" + year + "-2";
        }
        return (year - 1) + "-" + year + "-1";
    }

    private String configuredCurrentTerm() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "select config_value from system_config where config_key = 'current_term'",
                    String.class
            );
            return value == null ? "" : value.trim();
        } catch (DataAccessException ex) {
            return "";
        }
    }

    private String latestOfferingTerm() {
        try {
            String value = jdbcTemplate.queryForObject(
                    "select term from course_offering group by term order by max(id) desc limit 1",
                    String.class
            );
            return value == null ? "" : value.trim();
        } catch (DataAccessException ex) {
            return "";
        }
    }

    public record CurrentTerm(String term, String source) {
    }
}
